package razorvine.c64emu

import razorvine.ksim65.Cpu6502
import razorvine.ksim65.components.MemoryComponent
import razorvine.ksim65.components.UByte
import java.awt.Color
import java.awt.KeyboardFocusManager
import java.awt.Point
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JFrame
import javax.swing.Timer


/**
 * Define the C64 character screen matrix: 320x200 pixels,
 * 40x25 characters (of 8x8 pixels), and a colored border.
 */
object ScreenDefs {
    const val SCREEN_WIDTH_CHARS = 40
    const val SCREEN_HEIGHT_CHARS = 25
    const val SCREEN_WIDTH = SCREEN_WIDTH_CHARS*8
    const val SCREEN_HEIGHT = SCREEN_HEIGHT_CHARS*8
    const val PIXEL_SCALING = 3.0
    const val ASPECT_RATIO = 1.06       // c64 PAL pixels are slightly taller than wide
    const val BORDER_SIZE = 24

    class Palette {
        // this is Pepto's Commodore-64 palette  http://www.pepto.de/projects/colorvic/
        private val sixteenColors = listOf(Color(0x000000),  // 0 = black
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

class MainC64Window(title: String, chargenData: ByteArray, val ram: MemoryComponent, val cpu: Cpu6502, val keypressCia: Cia) :
        JFrame(title), KeyListener {
    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        isResizable = false
        isFocusable = true

        add(Screen(chargenData, ram))
        addKeyListener(this)
        pack()
        setLocationRelativeTo(null)
        location = Point(location.x/2, location.y)
        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, mutableSetOf())
        setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, mutableSetOf())
        requestFocusInWindow()
    }

    fun start(updateRate: Int) {
        // repaint the screen's back buffer
        val repaintTimer = Timer(1000/updateRate) {
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
