package razorvine.c64emu

import razorvine.ksim65.Cpu6502
import razorvine.ksim65.components.MemoryComponent
import razorvine.ksim65.components.UByte
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import javax.swing.JFrame
import javax.swing.JPanel
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

    class Palette {
        // this is Pepto's Commodore-64 palette  http://www.pepto.de/projects/colorvic/
        private val sixteenColors = listOf(
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
        operator fun get(i: Int): Color = sixteenColors[i and 15]
        operator fun get(i: UByte): Color = sixteenColors[i.toInt() and 15]
    }

    val colorPalette = Palette()
}

private class BitmapScreenPanel(val chargenData: ByteArray, val ram: MemoryComponent) : JPanel() {

    private val fullscreenImage: BufferedImage
    private val fullscreenG2d: Graphics2D
    private val normalCharacters = loadCharacters(false)
    private val shiftedCharacters = loadCharacters(true)

    init {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val gd = ge.defaultScreenDevice.defaultConfiguration
        fullscreenImage = gd.createCompatibleImage(ScreenDefs.SCREEN_WIDTH + 2*ScreenDefs.BORDER_SIZE,
            ScreenDefs.SCREEN_HEIGHT + 2*ScreenDefs.BORDER_SIZE, Transparency.OPAQUE)
        fullscreenImage.accelerationPriority = 1.0f
        fullscreenG2d = fullscreenImage.graphics as Graphics2D

        val size = Dimension(
            fullscreenImage.width * ScreenDefs.DISPLAY_PIXEL_SCALING.toInt(),
            fullscreenImage.height*ScreenDefs.DISPLAY_PIXEL_SCALING.toInt()
        )
        minimumSize = size
        maximumSize = size
        preferredSize = size
        isFocusable = true
        isDoubleBuffered = false
        requestFocusInWindow()
    }

    private fun loadCharacters(shifted: Boolean): Array<BufferedImage> {
        val chars = Array(256) { BufferedImage(8, 8, BufferedImage.TYPE_BYTE_BINARY).also { it.accelerationPriority=1.0f } }
        val offset = if (shifted) 256 * 8 else 0
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

    override fun paint(graphics: Graphics) {
        val windowG2d = graphics as Graphics2D
        val vicSCROLY = ram[0xd011].toInt()
        val vicVMCSB = ram[0xd018].toInt()
        if(vicSCROLY and 0b10000 == 0) {
            // screen blanked, only display border
            fullscreenG2d.background = ScreenDefs.colorPalette[ram[0xd020]]
            fullscreenG2d.clearRect(
                0, 0,
                ScreenDefs.SCREEN_WIDTH + 2 * ScreenDefs.BORDER_SIZE, ScreenDefs.SCREEN_HEIGHT+ 2*ScreenDefs.BORDER_SIZE)
        } else {
            // draw the screen border
            fullscreenG2d.background = ScreenDefs.colorPalette[ram[0xd020]]
            fullscreenG2d.clearRect(0, 0, ScreenDefs.SCREEN_WIDTH + 2 * ScreenDefs.BORDER_SIZE, ScreenDefs.BORDER_SIZE)
            fullscreenG2d.clearRect(
                0, ScreenDefs.SCREEN_HEIGHT + ScreenDefs.BORDER_SIZE,
                ScreenDefs.SCREEN_WIDTH + 2 * ScreenDefs.BORDER_SIZE, ScreenDefs.BORDER_SIZE
            )
            fullscreenG2d.clearRect(0, ScreenDefs.BORDER_SIZE, ScreenDefs.BORDER_SIZE, ScreenDefs.SCREEN_HEIGHT)
            fullscreenG2d.clearRect(
                ScreenDefs.SCREEN_WIDTH + ScreenDefs.BORDER_SIZE, ScreenDefs.BORDER_SIZE,
                ScreenDefs.BORDER_SIZE, ScreenDefs.SCREEN_HEIGHT
            )

            if(vicSCROLY and 0b100000 != 0) {
                // bitmap mode 320x200
                fullscreenG2d.background = ScreenDefs.colorPalette[ram[0xd021]]
                fullscreenG2d.clearRect(ScreenDefs.BORDER_SIZE, ScreenDefs.BORDER_SIZE, ScreenDefs.SCREEN_WIDTH, ScreenDefs.SCREEN_HEIGHT)
                // TODO vic address offset in memory, so that medusa.prg will work
                val bitmap = ram.getPages(if(vicVMCSB and 0b00001000 != 0) 32 else 0, 32)
                val colorBytes = ram.getPages((vicVMCSB ushr 4) shl 2, 4)
                val pixels: IntArray = (fullscreenImage.raster.dataBuffer as DataBufferInt).data
                for(y in 0 until ScreenDefs.SCREEN_HEIGHT) {
                    for(x in 0 until ScreenDefs.SCREEN_WIDTH step 8) {
                        val colorbyte = ScreenDefs.SCREEN_WIDTH_CHARS*(y ushr 3) + (x ushr 3)
                        val bgColor = ScreenDefs.colorPalette[colorBytes[colorbyte].toInt() and 15].rgb
                        val fgColor = ScreenDefs.colorPalette[colorBytes[colorbyte].toInt() ushr 4].rgb
                        draw8Pixels(pixels, x, y, bitmap, fgColor, bgColor)
                    }
                }
            } else {
                // normal character mode
                fullscreenG2d.background = ScreenDefs.colorPalette[ram[0xd021]]
                fullscreenG2d.clearRect(ScreenDefs.BORDER_SIZE, ScreenDefs.BORDER_SIZE, ScreenDefs.SCREEN_WIDTH, ScreenDefs.SCREEN_HEIGHT)
                redrawCharacters()
            }
        }

        // scale and draw the image to the window, and simulate a slight scanline effect
        windowG2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        windowG2d.drawImage(
            fullscreenImage, 0, 0, (fullscreenImage.width * ScreenDefs.DISPLAY_PIXEL_SCALING).toInt(),
            (fullscreenImage.height * ScreenDefs.DISPLAY_PIXEL_SCALING).toInt(), null
        )
        windowG2d.color = Color(0, 0, 0, 40)
        windowG2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        val width = fullscreenImage.width * ScreenDefs.DISPLAY_PIXEL_SCALING.toInt()
        val height = fullscreenImage.height * ScreenDefs.DISPLAY_PIXEL_SCALING.toInt()
        for (y in 0 until height step ScreenDefs.DISPLAY_PIXEL_SCALING.toInt()) {
            windowG2d.drawLine(0, y, width, y)
        }
        Toolkit.getDefaultToolkit().sync()
    }

    private fun draw8Pixels(pixels: IntArray, xstart: Int, y: Int,
                            bitmap: Array<UByte>, fgColorRgb: Int, bgColorRgb: Int) {
        val offset = ScreenDefs.BORDER_SIZE + ScreenDefs.BORDER_SIZE*fullscreenImage.width +
                xstart + y * fullscreenImage.width
        val byte = bitmap[ScreenDefs.SCREEN_WIDTH_CHARS*(y and 248) + (y and 7) + (xstart and 504)].toInt()
        pixels[offset + 0] = if(byte and 0b10000000 != 0) fgColorRgb else bgColorRgb
        pixels[offset + 1] = if(byte and 0b01000000 != 0) fgColorRgb else bgColorRgb
        pixels[offset + 2] = if(byte and 0b00100000 != 0) fgColorRgb else bgColorRgb
        pixels[offset + 3] = if(byte and 0b00010000 != 0) fgColorRgb else bgColorRgb
        pixels[offset + 4] = if(byte and 0b00001000 != 0) fgColorRgb else bgColorRgb
        pixels[offset + 5] = if(byte and 0b00000100 != 0) fgColorRgb else bgColorRgb
        pixels[offset + 6] = if(byte and 0b00000010 != 0) fgColorRgb else bgColorRgb
        pixels[offset + 7] = if(byte and 0b00000001 != 0) fgColorRgb else bgColorRgb
    }

    private fun redrawCharacters() {
        val screen = 0x0400
        val colors = 0xd800
        val shifted = (ram[0xd018].toInt() and 0b00000010) != 0
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

    fun start(updateRate: Int) {
        // repaint the screen's back buffer
        val repaintTimer = Timer(1000 / updateRate) {
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
