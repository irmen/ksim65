package razorvine.examplemachines

import razorvine.ksim65.Bus
import razorvine.ksim65.Cpu6502
import java.awt.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.event.MouseInputListener
import razorvine.ksim65.IHostInterface
import razorvine.ksim65.IVirtualMachine
import java.awt.event.*
import java.util.*
import javax.swing.*
import javax.swing.Timer


/**
 * Define a monochrome screen that can display 640x480 pixels
 * and/or 80x30 characters (these are 8x16 pixels).
 */
object ScreenDefs {
    const val SCREEN_WIDTH_CHARS = 80
    const val SCREEN_HEIGHT_CHARS = 30
    const val SCREEN_WIDTH = SCREEN_WIDTH_CHARS * 8
    const val SCREEN_HEIGHT = SCREEN_HEIGHT_CHARS * 16
    const val DISPLAY_PIXEL_SCALING: Double = 1.25
    val BG_COLOR = Color(0, 10, 20)
    val FG_COLOR = Color(200, 255, 230)
    val BORDER_COLOR = Color(20, 30, 40)
    val Characters = loadCharacters()

    private fun loadCharacters(): Array<BufferedImage> {
        val img = ImageIO.read(javaClass.getResourceAsStream("/charset/unscii8x16.png"))
        val charactersImage = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
        charactersImage.createGraphics().drawImage(img, 0, 0, null)

        val black = Color(0, 0, 0).rgb
        val foreground = FG_COLOR.rgb
        val nopixel = Color(0, 0, 0, 0).rgb
        for (y in 0 until charactersImage.height) {
            for (x in 0 until charactersImage.width) {
                val col = charactersImage.getRGB(x, y)
                if (col == black)
                    charactersImage.setRGB(x, y, nopixel)
                else
                    charactersImage.setRGB(x, y, foreground)
            }
        }

        val numColumns = charactersImage.width / 8
        val charImages = (0..255).map {
            val charX = it % numColumns
            val charY = it / numColumns
            charactersImage.getSubimage(charX * 8, charY * 16, 8, 16)
        }

        return charImages.toTypedArray()
    }
}

private class BitmapScreenPanel : JPanel() {

    private val image = BufferedImage(ScreenDefs.SCREEN_WIDTH, ScreenDefs.SCREEN_HEIGHT, BufferedImage.TYPE_INT_ARGB)
    private val g2d = image.graphics as Graphics2D
    private var cursorX: Int = 0
    private var cursorY: Int = 0
    private var cursorState: Boolean = false

    init {
        val size = Dimension(
            (image.width * ScreenDefs.DISPLAY_PIXEL_SCALING).toInt(),
            (image.height * ScreenDefs.DISPLAY_PIXEL_SCALING).toInt()
        )
        minimumSize = size
        maximumSize = size
        preferredSize = size
        clearScreen()
        isFocusable = true
        requestFocusInWindow()
    }

    override fun paint(graphics: Graphics?) {
        val g2d = graphics as Graphics2D?
        g2d!!.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE)
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g2d.drawImage(
            image, 0, 0, (image.width * ScreenDefs.DISPLAY_PIXEL_SCALING).toInt(),
            (image.height * ScreenDefs.DISPLAY_PIXEL_SCALING).toInt(), null
        )
        if(cursorState) {
            val scx = (cursorX * ScreenDefs.DISPLAY_PIXEL_SCALING * 8).toInt()
            val scy = (cursorY * ScreenDefs.DISPLAY_PIXEL_SCALING * 16).toInt()
            val scw = (8 * ScreenDefs.DISPLAY_PIXEL_SCALING).toInt()
            val sch = (16 * ScreenDefs.DISPLAY_PIXEL_SCALING).toInt()
            g2d.setXORMode(Color.CYAN)
            g2d.fillRect(scx, scy, scw, sch)
            g2d.setPaintMode()
        }

    }

    fun clearScreen() {
        g2d.background = ScreenDefs.BG_COLOR
        g2d.clearRect(0, 0, ScreenDefs.SCREEN_WIDTH, ScreenDefs.SCREEN_HEIGHT)
        cursorPos(0, 0)
    }

    fun setPixel(x: Int, y: Int, onOff: Boolean) {
        if (onOff)
            image.setRGB(x, y, ScreenDefs.FG_COLOR.rgb)
        else
            image.setRGB(x, y, ScreenDefs.BG_COLOR.rgb)
    }

    fun getPixel(x: Int, y: Int) = image.getRGB(x, y) != ScreenDefs.BG_COLOR.rgb

    fun setChar(x: Int, y: Int, character: Char) {
        g2d.clearRect(8 * x, 16 * y, 8, 16)
        val coloredImage = ScreenDefs.Characters[character.toInt()]
        g2d.drawImage(coloredImage, 8 * x, 16 * y, null)
    }

    fun scrollUp() {
        g2d.copyArea(0, 16, ScreenDefs.SCREEN_WIDTH, ScreenDefs.SCREEN_HEIGHT - 16, 0, -16)
        g2d.background = ScreenDefs.BG_COLOR
        g2d.clearRect(0, ScreenDefs.SCREEN_HEIGHT - 16, ScreenDefs.SCREEN_WIDTH, 16)
    }

    fun mousePixelPosition(): Point? {
        val pos = mousePosition ?: return null
        return Point(
            (pos.x / ScreenDefs.DISPLAY_PIXEL_SCALING).toInt(),
            (pos.y / ScreenDefs.DISPLAY_PIXEL_SCALING).toInt()
        )
    }

    fun cursorPos(x: Int, y: Int) {
        if(x!=cursorX || y!=cursorY)
            cursorState=true
        cursorX = x
        cursorY = y
    }

    fun blinkCursor() {
        cursorState = !cursorState
    }
}

class DebugWindow(private val vm: IVirtualMachine) : JFrame("debugger"), ActionListener {
    private val cyclesTf = JTextField("00000000000000")
    private val speedKhzTf = JTextField("0000000")
    private val regAtf = JTextField("000")
    private val regXtf = JTextField("000")
    private val regYtf = JTextField("000")
    private val regPCtf = JTextField("00000")
    private val regSPtf = JTextField("000")
    private val regPtf = JTextArea("NV-BDIZC\n000000000")
    private val disassemTf = JTextField("00 00 00   lda   (fffff),x")
    private val pauseBt = JButton("Pause").also { it.actionCommand = "pause" }
    private val zeropageTf = JTextArea(8, 102).also {
        it.border = BorderFactory.createEtchedBorder()
        it.isEnabled = false
        it.disabledTextColor = Color.DARK_GRAY
        it.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
    }
    private val stackpageTf = JTextArea(8, 102).also {
        it.border = BorderFactory.createEtchedBorder()
        it.isEnabled=false
        it.disabledTextColor = Color.DARK_GRAY
        it.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
    }
    private val startTime = System.currentTimeMillis()

    init {
        contentPane.layout = GridBagLayout()
        defaultCloseOperation = EXIT_ON_CLOSE
        val cpuPanel = JPanel(GridBagLayout())
        cpuPanel.border = BorderFactory.createTitledBorder("CPU: ${vm.cpu.name}")
        val gc = GridBagConstraints()
        gc.insets = Insets(2, 2, 2, 2)
        gc.anchor = GridBagConstraints.EAST
        gc.gridx = 0
        gc.gridy = 0
        val cyclesLb = JLabel("cycles")
        val speedKhzLb = JLabel("speed (kHz)")
        val regAlb = JLabel("A")
        val regXlb = JLabel("X")
        val regYlb = JLabel("Y")
        val regSPlb = JLabel("SP")
        val regPClb = JLabel("PC")
        val regPlb = JLabel("Status")
        val disassemLb = JLabel("Instruction")
        listOf(cyclesLb, speedKhzLb, regAlb, regXlb, regYlb, regSPlb, regPClb, disassemLb, regPlb).forEach {
            cpuPanel.add(it, gc)
            gc.gridy++
        }
        gc.anchor = GridBagConstraints.WEST
        gc.gridx = 1
        gc.gridy = 0
        listOf(cyclesTf, speedKhzTf, regAtf, regXtf, regYtf, regSPtf, regPCtf, disassemTf, regPtf).forEach {
            it.font = Font(Font.MONOSPACED, Font.PLAIN, 14)
            it.disabledTextColor = Color.DARK_GRAY
            it.isEnabled = false
            if(it is JTextField) {
                it.columns = it.text.length
            } else if(it is JTextArea) {
                it.border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(2,2,2,2))
            }
            cpuPanel.add(it, gc)
            gc.gridy++
        }

        val buttonPanel = JPanel(FlowLayout())
        buttonPanel.border = BorderFactory.createTitledBorder("Control")

        val resetBt = JButton("Reset").also { it.actionCommand = "reset" }
        val stepBt = JButton("Step").also { it.actionCommand = "step" }
        val irqBt = JButton("IRQ").also { it.actionCommand = "irq" }
        val nmiBt = JButton("NMI").also { it.actionCommand = "nmi" }
        val quitBt = JButton("Quit").also { it.actionCommand = "quit" }
        listOf(resetBt, irqBt, nmiBt, pauseBt, stepBt, quitBt).forEach {
            it.addActionListener(this)
            buttonPanel.add(it)
        }

        val zeropagePanel = JPanel()
        zeropagePanel.layout = BoxLayout(zeropagePanel, BoxLayout.Y_AXIS)
        zeropagePanel.border = BorderFactory.createTitledBorder("Zeropage and Stack")
        zeropagePanel.add(zeropageTf)
        zeropagePanel.add(stackpageTf)

        gc.gridx=0
        gc.gridy=0
        gc.fill=GridBagConstraints.BOTH
        contentPane.add(cpuPanel, gc)
        gc.gridy++
        contentPane.add(zeropagePanel, gc)
        gc.gridy++
        contentPane.add(buttonPanel, gc)

        pack()
    }

    override fun actionPerformed(e: ActionEvent) {
        when(e.actionCommand) {
            "reset" -> {
                vm.bus.reset()
                updateCpu(vm.cpu, vm.bus)
            }
            "step" -> {
                vm.step()
                updateCpu(vm.cpu, vm.bus)
            }
            "pause" -> {
                vm.pause(true)
                pauseBt.actionCommand = "continue"
                pauseBt.text = "Continue"
            }
            "continue" -> {
                vm.pause(false)
                pauseBt.actionCommand = "pause"
                pauseBt.text = "Pause"
            }
            "irq" -> vm.cpu.irq()
            "nmi" -> vm.cpu.nmi()
            "quit" -> {
                dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
            }
        }
    }

    fun updateCpu(cpu2: Cpu6502, bus: Bus) {
        val state = cpu2.snapshot()
        cyclesTf.text = state.cycles.toString()
        regAtf.text = cpu2.hexB(state.A)
        regXtf.text = cpu2.hexB(state.X)
        regYtf.text = cpu2.hexB(state.Y)
        regPtf.text = "NV-BDIZC\n" + state.P.asInt().toString(2).padStart(8, '0')
        regPCtf.text = cpu2.hexW(state.PC)
        regSPtf.text = cpu2.hexB(state.SP)
        val memory = bus.memoryComponentFor(state.PC)
        disassemTf.text = cpu2.disassembleOneInstruction(memory.data, state.PC, memory.startAddress).first.substringAfter(' ').trim()
        val pages = vm.getZeroAndStackPages()
        if(pages.isNotEmpty()) {
            val zpLines = (0..0xff step 32).map { location ->
                " ${'$'}${location.toString(16).padStart(2, '0')}  " + (
                        (0..31).joinToString(" ") { lineoffset ->
                            pages[location + lineoffset].toString(16).padStart(2, '0')
                        })
            }
            val stackLines = (0x100..0x1ff step 32).map { location ->
                "${'$'}${location.toString(16).padStart(2, '0')}  " + (
                        (0..31).joinToString(" ") { lineoffset ->
                            pages[location + lineoffset].toString(16).padStart(2, '0')
                        })
            }
            zeropageTf.text = zpLines.joinToString ("\n")
            stackpageTf.text = stackLines.joinToString ("\n")
        }

        val spentTime = System.currentTimeMillis() - startTime
        val speedKhz = state.cycles.toDouble() / spentTime
        speedKhzTf.text = "%.1f".format(speedKhz)
    }
}


class MainWindow(title: String) : JFrame(title), KeyListener, MouseInputListener, IHostInterface {
    private val canvas = BitmapScreenPanel()
    private val keyboardBuffer = ArrayDeque<Char>()
    private var mousePos = Point(0, 0)
    private var leftButton = false
    private var rightButton = false
    private var middleButton = false

    init {
        val borderWidth = 16
        layout = GridBagLayout()
        defaultCloseOperation = EXIT_ON_CLOSE
        isResizable = false
        isFocusable = true

        // the borders (top, left, right, bottom)
        val borderTop = JPanel().apply {
            preferredSize = Dimension(
                (ScreenDefs.DISPLAY_PIXEL_SCALING * (ScreenDefs.SCREEN_WIDTH + 2 * borderWidth)).toInt(),
                (ScreenDefs.DISPLAY_PIXEL_SCALING * borderWidth).toInt()
            )
            background = ScreenDefs.BORDER_COLOR
        }
        val borderBottom = JPanel().apply {
            preferredSize = Dimension(
                (ScreenDefs.DISPLAY_PIXEL_SCALING * (ScreenDefs.SCREEN_WIDTH + 2 * borderWidth)).toInt(),
                (ScreenDefs.DISPLAY_PIXEL_SCALING * borderWidth).toInt()
            )
            background = ScreenDefs.BORDER_COLOR
        }
        val borderLeft = JPanel().apply {
            preferredSize = Dimension(
                (ScreenDefs.DISPLAY_PIXEL_SCALING * borderWidth).toInt(),
                (ScreenDefs.DISPLAY_PIXEL_SCALING * ScreenDefs.SCREEN_HEIGHT).toInt()
            )
            background = ScreenDefs.BORDER_COLOR
        }
        val borderRight = JPanel().apply {
            preferredSize = Dimension(
                (ScreenDefs.DISPLAY_PIXEL_SCALING * borderWidth).toInt(),
                (ScreenDefs.DISPLAY_PIXEL_SCALING * ScreenDefs.SCREEN_HEIGHT).toInt()
            )
            background = ScreenDefs.BORDER_COLOR
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
        addMouseMotionListener(this)
        addMouseListener(this)
        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, mutableSetOf())
        pack()
        setLocationRelativeTo(null)
        location = Point(location.x/2, location.y)
        requestFocusInWindow()
    }

    fun start() {
        // repaint the screen's back buffer ~60 times per second
        var cursorBlink = 0L
        val repaintTimer = Timer(1000 / 60) {
            repaint()
            if(it.`when` - cursorBlink > 200L) {
                cursorBlink = it.`when`
                canvas.blinkCursor()
            }
        }
        repaintTimer.initialDelay = 0
        repaintTimer.start()
    }

    // keyboard events:
    override fun keyTyped(event: KeyEvent) {
        keyboardBuffer.add(event.keyChar)
        while (keyboardBuffer.size > 8)
            keyboardBuffer.pop()
    }

    override fun keyPressed(event: KeyEvent) {}
    override fun keyReleased(event: KeyEvent) {}

    // mouse events:
    override fun mousePressed(event: MouseEvent) {
        val pos = canvas.mousePixelPosition()
        if (pos == null)
            return
        else {
            mousePos = pos
            leftButton = leftButton or SwingUtilities.isLeftMouseButton(event)
            rightButton = rightButton or SwingUtilities.isRightMouseButton(event)
            middleButton = middleButton or SwingUtilities.isMiddleMouseButton(event)
        }
    }
    override fun mouseReleased(event: MouseEvent) {
        val pos = canvas.mousePixelPosition()
        if (pos == null)
            return
        else {
            mousePos = pos
            leftButton = leftButton xor SwingUtilities.isLeftMouseButton(event)
            rightButton = rightButton xor SwingUtilities.isRightMouseButton(event)
            middleButton = middleButton xor SwingUtilities.isMiddleMouseButton(event)
        }
    }
    override fun mouseEntered(event: MouseEvent) {}
    override fun mouseExited(event: MouseEvent) {}
    override fun mouseDragged(event: MouseEvent) = mouseMoved(event)
    override fun mouseClicked(event: MouseEvent) {}

    override fun mouseMoved(event: MouseEvent) {
        val pos = canvas.mousePixelPosition()
        if (pos == null)
            return
        else
            mousePos = pos
    }


    // the overrides required for IHostDisplay:
    override fun clearScreen() = canvas.clearScreen()
    override fun setPixel(x: Int, y: Int) = canvas.setPixel(x, y, true)
    override fun clearPixel(x: Int, y: Int) = canvas.setPixel(x, y, false)
    override fun getPixel(x: Int, y: Int) = canvas.getPixel(x, y)
    override fun setChar(x: Int, y: Int, character: Char) = canvas.setChar(x, y, character)
    override fun cursor(x: Int, y: Int) = canvas.cursorPos(x, y)
    override fun scrollUp() = canvas.scrollUp()
    override fun mouse() = IHostInterface.MouseInfo(mousePos.x, mousePos.y, leftButton, rightButton, middleButton)

    override fun keyboard(): Char? {
        return if (keyboardBuffer.isEmpty())
            null
        else
            keyboardBuffer.pop()
    }

}
