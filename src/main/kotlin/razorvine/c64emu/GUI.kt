package razorvine.c64emu

import razorvine.ksim65.Cpu6502
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
    const val BORDER_SIZE = 24

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

    private val fullscreenImage = BufferedImage(ScreenDefs.SCREEN_WIDTH + 2*ScreenDefs.BORDER_SIZE,
        ScreenDefs.SCREEN_HEIGHT + 2*ScreenDefs.BORDER_SIZE, BufferedImage.TYPE_INT_ARGB)
    private val fullscreenG2d = fullscreenImage.graphics as Graphics2D
    private val normalCharacters = loadCharacters(false)
    private val shiftedCharacters = loadCharacters(true)

    init {
        val size = Dimension(
            fullscreenImage.width * ScreenDefs.DISPLAY_PIXEL_SCALING.toInt(),
            fullscreenImage.height*ScreenDefs.DISPLAY_PIXEL_SCALING.toInt()
        )
        minimumSize = size
        maximumSize = size
        preferredSize = size
        isFocusable = true
        requestFocusInWindow()
    }

    private fun loadCharacters(shifted: Boolean): Array<BufferedImage> {
        val chars = Array(256) { BufferedImage(8, 8, BufferedImage.TYPE_BYTE_BINARY) }
        val offset = if (shifted) 256 * 8 else 0
        // val color = ScreenDefs.colorPalette[14].rgb
        for (char in 0..255) {
            for (line in 0..7) {
                val charbyte = chargenData[offset + char * 8 + line].toInt()
                for (x in 0..7) {
                    if (charbyte and (0b10000000 ushr x) != 0)
                        chars[char].setRGB(x, line, 0xffffff)
                }
            }
        }
        return chars
    }

    override fun paint(graphics: Graphics?) {
        redrawCharacters()
        val g2d = graphics as Graphics2D
        g2d.background = ScreenDefs.colorPalette[ram[0xd020].toInt() and 15]
        g2d.clearRect(0, 0, width, height)
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.drawImage(
            fullscreenImage, 0, 0, (fullscreenImage.width * ScreenDefs.DISPLAY_PIXEL_SCALING).toInt(),
            (fullscreenImage.height * ScreenDefs.DISPLAY_PIXEL_SCALING).toInt(), null
        )

        // simulate a slight scan line effect
        g2d.color = Color(0, 0, 0, 40)
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        val width = fullscreenImage.width * ScreenDefs.DISPLAY_PIXEL_SCALING.toInt()
        val height = fullscreenImage.height * ScreenDefs.DISPLAY_PIXEL_SCALING.toInt()
        for (y in 0 until height step ScreenDefs.DISPLAY_PIXEL_SCALING.toInt()) {
            g2d.drawLine(0, y, width, y)
        }
    }

    private fun redrawCharacters() {
        val screen = 0x0400
        val colors = 0xd800
        val shifted = (ram[0xd018].toInt() and 0b00000010) != 0
        fullscreenG2d.background = ScreenDefs.colorPalette[ram[0xd021].toInt() and 15]
        fullscreenG2d.clearRect(ScreenDefs.BORDER_SIZE, ScreenDefs.BORDER_SIZE, ScreenDefs.SCREEN_WIDTH, ScreenDefs.SCREEN_HEIGHT)
        for (y in 0 until ScreenDefs.SCREEN_HEIGHT_CHARS) {
            for (x in 0 until ScreenDefs.SCREEN_WIDTH_CHARS) {
                val char = ram[screen + x + y * ScreenDefs.SCREEN_WIDTH_CHARS].toInt()
                val color = ram[colors + x + y * ScreenDefs.SCREEN_WIDTH_CHARS].toInt()
                drawColoredChar(x, y, char, color and 15, shifted)
            }
        }
    }

    private val coloredCharacters = mutableMapOf<Triple<Int, Int, Boolean>, BufferedImage>()

    private fun drawColoredChar(x: Int, y: Int, char: Int, color: Int, shifted: Boolean) {
        var cached = coloredCharacters[Triple(char, color, shifted)]
        if (cached == null) {
            cached = if (shifted) shiftedCharacters[char] else normalCharacters[char]
            val colored = fullscreenG2d.deviceConfiguration.createCompatibleImage(8, 8, BufferedImage.BITMASK)
            val sourceRaster = cached.raster
            val coloredRaster = colored.raster
            val pixelArray = IntArray(4)
            val javaColor = ScreenDefs.colorPalette[color]
            val coloredPixel = listOf(javaColor.red, javaColor.green, javaColor.blue, javaColor.alpha).toIntArray()
            for (pixelY in 0..7) {
                for (pixelX in 0..7) {
                    val source = sourceRaster.getPixel(pixelX, pixelY, pixelArray)
                    if (source[0] != 0) {
                        coloredRaster.setPixel(pixelX, pixelY, coloredPixel)
                    }
                }
            }
            coloredCharacters[Triple(char, color, shifted)] = colored
            cached = colored
        }
        fullscreenG2d.drawImage(cached, x * 8 + ScreenDefs.BORDER_SIZE, y * 8 + ScreenDefs.BORDER_SIZE, null)
    }
}

class MainC64Window(
    title: String,
    chargenData: ByteArray,
    val ram: MemoryComponent,
    val cpu: Cpu6502,
    val keypressCia: Cia
) : JFrame(title), KeyListener {
    private val canvas = BitmapScreenPanel(chargenData, ram)

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        isResizable = false
        isFocusable = true

        add(canvas)
        addKeyListener(this)
        pack()
        setLocationRelativeTo(null)
        location = Point(location.x / 2, location.y)
        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, mutableSetOf())
        setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, mutableSetOf())
        requestFocusInWindow()
    }

    fun start() {
        // repaint the screen's back buffer ~60 times per second
        val repaintTimer = Timer(1000 / 60) {
            repaint()
        }
        repaintTimer.initialDelay = 0
        repaintTimer.start()
    }

    // keyboard events:
    override fun keyTyped(event: KeyEvent) {}

    override fun keyPressed(event: KeyEvent) {
        // '\' is mapped as RESTORE, this causes a NMI on the cpu
        if (event.keyChar == '\\') {
            cpu.nmi()
        } else {
            keypressCia.hostKeyPressed(event)
        }
    }

    override fun keyReleased(event: KeyEvent) {
        keypressCia.hostKeyPressed(event)
    }
}
