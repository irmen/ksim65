package razorvine.c64emu

import razorvine.ksim65.components.MemoryComponent
import java.awt.*
import java.awt.image.BufferedImage
import java.awt.event.*
import javax.swing.*
import javax.swing.Timer


/**
 * Define the C64 character screen matrix: 320x200 pixels,
 * 40x25 characters (of 8x8 pixels), and a colored border.
 */
object ScreenDefs {
    const val SCREEN_WIDTH_CHARS = 40
    const val SCREEN_HEIGHT_CHARS = 25
    const val SCREEN_WIDTH = SCREEN_WIDTH_CHARS * 8
    const val SCREEN_HEIGHT = SCREEN_HEIGHT_CHARS * 8
    const val DISPLAY_PIXEL_SCALING: Double = 3.0

    val colorPalette = listOf(         // this is Pepto's Commodore-64 palette  http://www.pepto.de/projects/colorvic/
        Color(0x000000),  // 0 = black
        Color(0xFFFFFF),  // 1 = white
        Color(0x813338),  // 2 = red
        Color(0x75cec8),  // 3 = cyan
        Color(0x8e3c97),  // 4 = purple
        Color(0x56ac4d),  // 5 = green
        Color(0x2e2c9b),  // 6 = blue
        Color(0xedf171),  // 7 = yellow
        Color(0x8e5029),  // 8 = orange
        Color(0x553800),  // 9 = brown
        Color(0xc46c71),  // 10 = light red
        Color(0x4a4a4a),  // 11 = dark grey
        Color(0x7b7b7b),  // 12 = medium grey
        Color(0xa9ff9f),  // 13 = light green
        Color(0x706deb),  // 14 = light blue
        Color(0xb2b2b2)   // 15 = light grey
    )
}

private class BitmapScreenPanel(val chargenData: ByteArray, val ram: MemoryComponent) : JPanel() {

    private val image = BufferedImage(ScreenDefs.SCREEN_WIDTH, ScreenDefs.SCREEN_HEIGHT, BufferedImage.TYPE_INT_ARGB)
    private val g2d = image.graphics as Graphics2D
    private val normalCharacters = loadCharacters(false)
    private val shiftedCharacters = loadCharacters(true)

    init {
        val size = Dimension(
            (image.width * ScreenDefs.DISPLAY_PIXEL_SCALING).toInt(),
            (image.height * ScreenDefs.DISPLAY_PIXEL_SCALING).toInt()
        )
        minimumSize = size
        maximumSize = size
        preferredSize = size
        isFocusable = true
        requestFocusInWindow()
    }

    private fun loadCharacters(shifted: Boolean): Array<BufferedImage> {
        val chars = Array(256) { BufferedImage(8, 8, BufferedImage.TYPE_BYTE_BINARY) }
        val offset = if(shifted) 256*8 else 0
        // val color = ScreenDefs.colorPalette[14].rgb
        for(char in 0..255) {
            for(line in 0..7) {
                val charbyte = chargenData[offset + char*8 + line].toInt()
                for(x in 0..7) {
                    if(charbyte and (0b10000000 ushr x) !=0 )
                        chars[char].setRGB(x, line, 0xffffff)
                }
            }
        }
        return chars
    }

    override fun paint(graphics: Graphics?) {
        redrawCharacters()
        val g2d = graphics as Graphics2D?
        g2d!!.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE)
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g2d.drawImage(
            image, 0, 0, (image.width * ScreenDefs.DISPLAY_PIXEL_SCALING).toInt(),
            (image.height * ScreenDefs.DISPLAY_PIXEL_SCALING).toInt(), null
        )
    }

    private fun redrawCharacters() {
        val screen = 0x0400
        val colors = 0xd800
        val shifted = (ram[0xd018].toInt() and 0b00000010) != 0
        g2d.background = ScreenDefs.colorPalette[ram[0xd021].toInt() and 15]
        g2d.clearRect(0, 0, ScreenDefs.SCREEN_WIDTH, ScreenDefs.SCREEN_HEIGHT)
        for(y in 0 until ScreenDefs.SCREEN_HEIGHT_CHARS) {
            for(x in 0 until ScreenDefs.SCREEN_WIDTH_CHARS) {
                val char = ram[screen + x + y*ScreenDefs.SCREEN_WIDTH_CHARS].toInt()
                val color = ram[colors + x + y*ScreenDefs.SCREEN_WIDTH_CHARS].toInt()
                drawColoredChar(x, y, char, color and 15, shifted)
            }
        }
    }

    private val coloredCharacters = mutableMapOf<Triple<Int, Int, Boolean>, BufferedImage>()

    private fun drawColoredChar(x: Int, y: Int, char: Int, color: Int, shifted: Boolean) {
        var cached = coloredCharacters[Triple(char, color, shifted)]
        if(cached==null) {
            cached = if(shifted) shiftedCharacters[char] else normalCharacters[char]
            val colored = g2d.deviceConfiguration.createCompatibleImage(8, 8, BufferedImage.BITMASK)
            val sourceRaster = cached.raster
            val coloredRaster = colored.raster
            val pixelArray = IntArray(4)
            val javaColor = ScreenDefs.colorPalette[color]
            val coloredPixel = listOf(javaColor.red, javaColor.green, javaColor.blue, javaColor.alpha).toIntArray()
            for(pixelY in 0..7) {
                for(pixelX in 0..7) {
                    val source = sourceRaster.getPixel(pixelX, pixelY, pixelArray)
                    if(source[0]!=0) {
                        coloredRaster.setPixel(pixelX, pixelY, coloredPixel)
                    }
                }
            }
            coloredCharacters[Triple(char, color, shifted)] = colored
            cached = colored
        }
        g2d.drawImage(cached, x * 8, y * 8, null)
    }
}

class MainC64Window(
    title: String,
    chargenData: ByteArray,
    val ram: MemoryComponent,
    val keypressCia: Cia
) : JFrame(title), KeyListener {
    private val canvas = BitmapScreenPanel(chargenData, ram)
    private var borderTop: JPanel
    private var borderBottom: JPanel
    private var borderLeft: JPanel
    private var borderRight: JPanel

    init {
        val borderWidth = 24
        layout = GridBagLayout()
        defaultCloseOperation = EXIT_ON_CLOSE
        isResizable = false
        isFocusable = true

        // the borders (top, left, right, bottom)
        borderTop = JPanel().apply {
            preferredSize = Dimension(
                (ScreenDefs.DISPLAY_PIXEL_SCALING * (ScreenDefs.SCREEN_WIDTH + 2 * borderWidth)).toInt(),
                (ScreenDefs.DISPLAY_PIXEL_SCALING * borderWidth).toInt()
            )
            background = ScreenDefs.colorPalette[14]
        }
        borderBottom = JPanel().apply {
            preferredSize = Dimension(
                (ScreenDefs.DISPLAY_PIXEL_SCALING * (ScreenDefs.SCREEN_WIDTH + 2 * borderWidth)).toInt(),
                (ScreenDefs.DISPLAY_PIXEL_SCALING * borderWidth).toInt()
            )
            background = ScreenDefs.colorPalette[14]
        }
        borderLeft = JPanel().apply {
            preferredSize = Dimension(
                (ScreenDefs.DISPLAY_PIXEL_SCALING * borderWidth).toInt(),
                (ScreenDefs.DISPLAY_PIXEL_SCALING * ScreenDefs.SCREEN_HEIGHT).toInt()
            )
            background = ScreenDefs.colorPalette[14]
        }
        borderRight = JPanel().apply {
            preferredSize = Dimension(
                (ScreenDefs.DISPLAY_PIXEL_SCALING * borderWidth).toInt(),
                (ScreenDefs.DISPLAY_PIXEL_SCALING * ScreenDefs.SCREEN_HEIGHT).toInt()
            )
            background = ScreenDefs.colorPalette[14]
        }
        var c = GridBagConstraints()
        c.gridx = 0; c.gridy = 1; c.gridwidth = 3
        add(borderTop, c)
        c = GridBagConstraints()
        c.gridx = 0; c.gridy = 2
        add(borderLeft, c)
        c = GridBagConstraints()
        c.gridx = 2; c.gridy = 2
        add(borderRight, c)
        c = GridBagConstraints()
        c.gridx = 0; c.gridy = 3; c.gridwidth = 3
        add(borderBottom, c)
        // the screen canvas(bitmap)
        c = GridBagConstraints()
        c.gridx = 1; c.gridy = 2
        add(canvas, c)
        addKeyListener(this)
        pack()
        setLocationRelativeTo(null)
        location = Point(location.x/2, location.y)
        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, mutableSetOf())
        setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, mutableSetOf())
        requestFocusInWindow()
    }

    fun start() {
        // repaint the screen's back buffer ~60 times per second
        val repaintTimer = Timer(1000 / 60) {
            repaint()
            borderTop.background = ScreenDefs.colorPalette[ram[0xd020].toInt() and 15]
            borderBottom.background = ScreenDefs.colorPalette[ram[0xd020].toInt() and 15]
            borderLeft.background = ScreenDefs.colorPalette[ram[0xd020].toInt() and 15]
            borderRight.background = ScreenDefs.colorPalette[ram[0xd020].toInt() and 15]
        }
        repaintTimer.initialDelay = 0
        repaintTimer.start()
    }

    // keyboard events:
    override fun keyTyped(event: KeyEvent) {}

    override fun keyPressed(event: KeyEvent) {
        keypressCia.hostKeyPressed(event)
    }
    override fun keyReleased(event: KeyEvent) {
        keypressCia.hostKeyPressed(event)
    }
}
