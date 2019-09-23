package razorvine.c64emu

import razorvine.ksim65.components.MemoryComponent
import razorvine.ksim65.components.UByte
import java.awt.*
import java.awt.image.BufferedImage
import java.awt.event.*
import java.io.CharConversionException
import java.util.*
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

class MainC64Window(title: String, chargenData: ByteArray, val ram: MemoryComponent) : JFrame(title), KeyListener {
    private val canvas = BitmapScreenPanel(chargenData, ram)
    private val keyboardBuffer = ArrayDeque<KeyEvent>()
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

            if(keyboardBuffer.isNotEmpty()) {
                // inject keystrokes directly into the c64's keyboard buffer (translate to petscii first)
                var kbbLen = ram[0xc6]
                while(kbbLen<=10 && keyboardBuffer.isNotEmpty()) {
                    try {
                        val petscii = keyEventToPetscii(keyboardBuffer.pop())
                        if(petscii > 0) {
                            ram[0x277 + kbbLen] = petscii
                            kbbLen++
                        }
                    } catch(ccx: CharConversionException) {
                        // ignore character
                    }
                }
                ram[0xc6] = kbbLen
            }
        }
        repaintTimer.initialDelay = 0
        repaintTimer.start()
    }

    /**
     * Map a key to the corresponding PETSCII character,
     * that is inserted directly into the C64's character buffer.
     * This avoids having to deal with the 'real' keyboard matrix,
     * but it can't map keys like RUN/STOP and RESTORE properly.
     *
     * TODO: replace this by the real keyboard matrix.
     */
    private fun keyEventToPetscii(ke: KeyEvent): UByte {
        if(ke.isActionKey) {
            // function keys, cursor keys etc.
            if(ke.id == KeyEvent.KEY_PRESSED) {
                return when (ke.keyCode) {
                    KeyEvent.VK_F1 -> 0x85
                    KeyEvent.VK_F2 -> 0x86
                    KeyEvent.VK_F3 -> 0x87
                    KeyEvent.VK_F4 -> 0x88
                    KeyEvent.VK_F5 -> 0x89
                    KeyEvent.VK_F6 -> 0x8a
                    KeyEvent.VK_F7 -> 0x8b
                    KeyEvent.VK_F8 -> 0x8c
                    KeyEvent.VK_UP -> 0x91
                    KeyEvent.VK_DOWN -> 0x11
                    KeyEvent.VK_LEFT -> 0x9d
                    KeyEvent.VK_RIGHT -> 0x1d
                    KeyEvent.VK_HOME -> {
                        if(ke.modifiersEx and KeyEvent.SHIFT_DOWN_MASK != 0)
                            0x93  // clear
                        else
                            0x13  // home
                    }
                    KeyEvent.VK_INSERT -> 0x94  //insert
                    else -> 0   // no mapped key
                }
            }
        } else {
            if(ke.id == KeyEvent.KEY_PRESSED) {
                return when(ke.keyChar) {
                    '\u001b' -> {
                        if(ke.isShiftDown) 0x83
                        else 0x03
                    }        // break
                    '\n' -> 0x0d      // enter
                    '\b' -> 0x14      // backspace ('delete')
                    '1' -> {
                        when {
                            ke.isControlDown -> 0x90    // black
                            ke.isAltDown -> 0x81        // orange
                            else -> '1'.toShort()
                        }
                    }
                    '2' -> {
                        when {
                            ke.isControlDown -> 0x05    // white
                            ke.isAltDown -> 0x95        // brown
                            else -> '2'.toShort()
                        }
                    }
                    '3' -> {
                        when {
                            ke.isControlDown -> 0x1c    // red
                            ke.isAltDown -> 0x96        // pink
                            else -> '3'.toShort()
                        }
                    }
                    '4' -> {
                        when {
                            ke.isControlDown -> 0x9f    // cyan
                            ke.isAltDown -> 0x97        // dark grey
                            else -> '4'.toShort()
                        }
                    }
                    '5' -> {
                        when {
                            ke.isControlDown -> 0x9c    // purple
                            ke.isAltDown -> 0x98        // grey
                            else -> '5'.toShort()
                        }
                    }
                    '6' -> {
                        when {
                            ke.isControlDown -> 0x1e    // green
                            ke.isAltDown -> 0x99        // light green
                            else -> '6'.toShort()
                        }
                    }
                    '7' -> {
                        when {
                            ke.isControlDown -> 0x1f    // blue
                            ke.isAltDown -> 0x9a        // light blue
                            else -> '7'.toShort()
                        }
                    }
                    '8' -> {
                        when {
                            ke.isControlDown -> 0x9e    // yellow
                            ke.isAltDown -> 0x9b        // light grey
                            else -> '8'.toShort()
                        }
                    }
                    '9' -> {
                        if (ke.isControlDown) 0x12    // reverse on
                        else '9'.toShort()
                    }
                    '0' -> {
                        if (ke.isControlDown) 0x92    // reverse off
                        else '0'.toShort()
                    }
                    '`' -> 0x5f    // left arrow
                    '\\' -> 0x5e  // up arrow
                    '|' -> 0x7e  // pi
                    '}' -> 0x5c  // pound
                    else -> {
                        if(ke.isAltDown) {
                            // commodore+key petscii symbol
                            return when(ke.keyCode) {
                                KeyEvent.VK_A -> 0xb0
                                KeyEvent.VK_B -> 0xbf
                                KeyEvent.VK_C -> 0xbc
                                KeyEvent.VK_D -> 0xac
                                KeyEvent.VK_E -> 0xb1
                                KeyEvent.VK_F -> 0xbb
                                KeyEvent.VK_G -> 0xa5
                                KeyEvent.VK_H -> 0xb4
                                KeyEvent.VK_I -> 0xa2
                                KeyEvent.VK_J -> 0xb5
                                KeyEvent.VK_K -> 0xa1
                                KeyEvent.VK_L -> 0xb6
                                KeyEvent.VK_M -> 0xa7
                                KeyEvent.VK_N -> 0xaa
                                KeyEvent.VK_O -> 0xb9
                                KeyEvent.VK_P -> 0xaf
                                KeyEvent.VK_Q -> 0xab
                                KeyEvent.VK_R -> 0xb2
                                KeyEvent.VK_S -> 0xae
                                KeyEvent.VK_T -> 0xa3
                                KeyEvent.VK_U -> 0xb8
                                KeyEvent.VK_V -> 0xbe
                                KeyEvent.VK_W -> 0xb3
                                KeyEvent.VK_X -> 0xbd
                                KeyEvent.VK_Y -> 0xb7
                                KeyEvent.VK_Z -> 0xad
                                else -> 0 // not mapped
                            }
                        } else {
                            Petscii.encodePetscii(ke.keyChar.toString(), true)[0]
                        }
                    }
                }
            }
            else if(ke.id == KeyEvent.KEY_RELEASED) {
                if((ke.keyCode==KeyEvent.VK_SHIFT && ke.modifiersEx and KeyEvent.ALT_DOWN_MASK != 0) ||
                   (ke.keyCode==KeyEvent.VK_ALT && ke.modifiersEx and KeyEvent.SHIFT_DOWN_MASK != 0)) {
                    // shift+alt is mapped to shift+commodore key, to toggle charsets
                    val charSet = ram[0xd018].toInt() and 0b00000010
                    return if(charSet==0)
                        0x0e        // lo/up charset
                    else
                        0x8e        // up/gfx charset
                }
            }
        }
        return 0  // no mapped key
    }

    // keyboard events:
    override fun keyTyped(event: KeyEvent) {}

    override fun keyPressed(event: KeyEvent) {
        keyboardBuffer += event
    }
    override fun keyReleased(event: KeyEvent) {
        keyboardBuffer += event
    }
}
