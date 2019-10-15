package razorvine.examplemachines

import razorvine.ksim65.*
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Integer.parseInt
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.event.MouseInputListener


/**
 * Define a monochrome screen that can display 640x480 pixels
 * and/or 80x30 characters (these are 8x16 pixels).
 */
object ScreenDefs {
    const val SCREEN_WIDTH_CHARS = 80
    const val SCREEN_HEIGHT_CHARS = 30
    const val SCREEN_WIDTH = SCREEN_WIDTH_CHARS*8
    const val SCREEN_HEIGHT = SCREEN_HEIGHT_CHARS*16
    const val DISPLAY_PIXEL_SCALING: Double = 1.5
    const val BORDER_SIZE = 32

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
                if (col == black) charactersImage.setRGB(x, y, nopixel)
                else charactersImage.setRGB(x, y, foreground)
            }
        }

        val numColumns = charactersImage.width/8
        val charImages = (0..255).map {
            val charX = it%numColumns
            val charY = it/numColumns
            charactersImage.getSubimage(charX*8, charY*16, 8, 16)
        }

        return charImages.toTypedArray()
    }
}

private class BitmapScreenPanel : JPanel() {

    private val image: BufferedImage
    private val g2d: Graphics2D
    private var cursorX: Int = 0
    private var cursorY: Int = 0
    private var cursorState: Boolean = false

    init {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val gd = ge.defaultScreenDevice.defaultConfiguration
        image = gd.createCompatibleImage(ScreenDefs.SCREEN_WIDTH, ScreenDefs.SCREEN_HEIGHT, Transparency.OPAQUE)
        g2d = image.graphics as Graphics2D

        val size = Dimension((image.width*ScreenDefs.DISPLAY_PIXEL_SCALING).toInt(),
                             (image.height*ScreenDefs.DISPLAY_PIXEL_SCALING).toInt())
        minimumSize = size
        maximumSize = size
        preferredSize = size
        isFocusable = true
        isDoubleBuffered = false
        requestFocusInWindow()
        clearScreen()
    }

    override fun paint(graphics: Graphics) {
        val g2d = graphics as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.drawImage(image, 0, 0, (image.width*ScreenDefs.DISPLAY_PIXEL_SCALING).toInt(),
                      (image.height*ScreenDefs.DISPLAY_PIXEL_SCALING).toInt(), null)
        if (cursorState) {
            val scx = (cursorX*ScreenDefs.DISPLAY_PIXEL_SCALING*8).toInt()
            val scy = (cursorY*ScreenDefs.DISPLAY_PIXEL_SCALING*16).toInt()
            val scw = (8*ScreenDefs.DISPLAY_PIXEL_SCALING).toInt()
            val sch = (16*ScreenDefs.DISPLAY_PIXEL_SCALING).toInt()
            g2d.setXORMode(Color.CYAN)
            g2d.fillRect(scx, scy, scw, sch)
            g2d.setPaintMode()
        }
        Toolkit.getDefaultToolkit().sync()
    }

    fun clearScreen() {
        g2d.background = ScreenDefs.BG_COLOR
        g2d.clearRect(0, 0, ScreenDefs.SCREEN_WIDTH, ScreenDefs.SCREEN_HEIGHT)
        cursorPos(0, 0)
    }

    fun setPixel(x: Int, y: Int, onOff: Boolean) {
        if (onOff) image.setRGB(x, y, ScreenDefs.FG_COLOR.rgb)
        else image.setRGB(x, y, ScreenDefs.BG_COLOR.rgb)
    }

    fun getPixel(x: Int, y: Int) = image.getRGB(x, y) != ScreenDefs.BG_COLOR.rgb

    fun setChar(x: Int, y: Int, character: Char) {
        g2d.clearRect(8*x, 16*y, 8, 16)
        val coloredImage = ScreenDefs.Characters[character.toInt()]
        g2d.drawImage(coloredImage, 8*x, 16*y, null)
    }

    fun scrollUp() {
        g2d.copyArea(0, 16, ScreenDefs.SCREEN_WIDTH, ScreenDefs.SCREEN_HEIGHT-16, 0, -16)
        g2d.background = ScreenDefs.BG_COLOR
        g2d.clearRect(0, ScreenDefs.SCREEN_HEIGHT-16, ScreenDefs.SCREEN_WIDTH, 16)
    }

    fun mousePixelPosition(): Point? {
        val pos = mousePosition ?: return null
        return Point((pos.x/ScreenDefs.DISPLAY_PIXEL_SCALING).toInt(), (pos.y/ScreenDefs.DISPLAY_PIXEL_SCALING).toInt())
    }

    fun cursorPos(x: Int, y: Int) {
        if (x != cursorX || y != cursorY) cursorState = true
        cursorX = x
        cursorY = y
    }

    fun blinkCursor() {
        cursorState = !cursorState
    }
}

private class UnaliasedTextBox(rows: Int, columns: Int) : JTextArea(rows, columns) {
    override fun paintComponent(g: Graphics) {
        g as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        super.paintComponent(g)
    }
}

class DebugWindow(private val vm: IVirtualMachine) : JFrame("Debugger - ksim65 v${Version.version}"), ActionListener {
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
    private val zeropageTf = UnaliasedTextBox(8, 102).also {
        it.border = BorderFactory.createEtchedBorder()
        it.isEnabled = false
        it.disabledTextColor = Color.DARK_GRAY
        it.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
    }
    private val stackpageTf = UnaliasedTextBox(8, 102).also {
        it.border = BorderFactory.createEtchedBorder()
        it.isEnabled = false
        it.disabledTextColor = Color.DARK_GRAY
        it.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
    }

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
            if (it is JTextField) {
                it.columns = it.text.length
            } else if (it is JTextArea) {
                it.border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                                                               BorderFactory.createEmptyBorder(2, 2, 2, 2))
            }
            cpuPanel.add(it, gc)
            gc.gridy++
        }

        val buttonPanel = JPanel(FlowLayout())
        buttonPanel.border = BorderFactory.createTitledBorder("Control")

        val loadBt = JButton("Inject program").also { it.actionCommand = "inject" }
        val resetBt = JButton("Reset").also { it.actionCommand = "reset" }
        val stepBt = JButton("Step").also { it.actionCommand = "step" }
        val irqBt = JButton("IRQ").also { it.actionCommand = "irq" }
        val nmiBt = JButton("NMI").also { it.actionCommand = "nmi" }
        val quitBt = JButton("Quit").also { it.actionCommand = "quit" }
        listOf(loadBt, resetBt, irqBt, nmiBt, pauseBt, stepBt, quitBt).forEach {
            it.addActionListener(this)
            buttonPanel.add(it)
        }

        val zeropagePanel = JPanel()
        zeropagePanel.layout = BoxLayout(zeropagePanel, BoxLayout.Y_AXIS)
        zeropagePanel.border = BorderFactory.createTitledBorder("Zeropage and Stack")
        val showZp = JCheckBox("show Zero page and Stack dumps", true)
        showZp.addActionListener {
            val visible = (it.source as JCheckBox).isSelected
            zeropageTf.isVisible = visible
            stackpageTf.isVisible = visible
        }
        zeropagePanel.add(showZp)
        zeropagePanel.add(zeropageTf)
        zeropagePanel.add(stackpageTf)

        val monitorPanel = JPanel()
        monitorPanel.layout = BoxLayout(monitorPanel, BoxLayout.Y_AXIS)
        monitorPanel.border = BorderFactory.createTitledBorder("Built-in Monitor")
        val output = JTextArea(6, 80)
        output.font = Font(Font.MONOSPACED, Font.PLAIN, 14)
        output.isEditable = false
        val outputScroll = JScrollPane(output)
        monitorPanel.add(outputScroll)
        outputScroll.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        val input = JTextField(50)
        input.border = BorderFactory.createLineBorder(Color.LIGHT_GRAY)
        input.font = Font(Font.MONOSPACED, Font.PLAIN, 14)
        input.addActionListener {
            output.append("\n")
            val command = input.text.trim()
            val result = vm.executeMonitorCommand(command)
            if (result.echo) output.append("> $command\n")
            output.append(result.output)
            input.text = result.prompt
        }
        monitorPanel.add(input)

        gc.gridx = 0
        gc.gridy = 0
        gc.fill = GridBagConstraints.BOTH
        contentPane.add(cpuPanel, gc)
        gc.gridy++
        contentPane.add(zeropagePanel, gc)
        gc.gridy++
        contentPane.add(monitorPanel, gc)
        gc.gridy++
        contentPane.add(buttonPanel, gc)
        pack()
    }

    override fun actionPerformed(e: ActionEvent) {
        when (e.actionCommand) {
            "inject" -> {
                val chooser = JFileChooser()
                chooser.dialogTitle = "Choose binary program or .prg to load"
                chooser.currentDirectory = File(".")
                chooser.isMultiSelectionEnabled = false
                val result = chooser.showOpenDialog(this)
                if (result == JFileChooser.APPROVE_OPTION) {
                    if (chooser.selectedFile.extension == "prg") {
                        vm.loadFileInRam(chooser.selectedFile, null)
                    } else {
                        val addressStr = JOptionPane.showInputDialog(this,
                                                                     "The selected file isn't a .prg.\nSpecify memory load address (hexadecimal) manually.",
                                                                     "Load address", JOptionPane.QUESTION_MESSAGE, null, null,
                                                                     "$") as String

                        val loadAddress = parseInt(addressStr.removePrefix("$"), 16)
                        vm.loadFileInRam(chooser.selectedFile, loadAddress)
                    }
                }

            }
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

    fun updateCpu(cpu: Cpu6502, bus: Bus) {
        val state = cpu.snapshot()
        cyclesTf.text = state.cycles.toString()
        regAtf.text = hexB(state.A)
        regXtf.text = hexB(state.X)
        regYtf.text = hexB(state.Y)
        regPtf.text = "NV-BDIZC\n"+state.P.asInt().toString(2).padStart(8, '0')
        regPCtf.text = hexW(state.PC)
        regSPtf.text = hexB(state.SP)
        val memory = bus.memoryComponentFor(state.PC)
        disassemTf.text = cpu.disassembleOneInstruction(memory.data, state.PC, memory.startAddress).first.substringAfter(' ').trim()
        val pages = vm.getZeroAndStackPages()
        if (pages.isNotEmpty()) {
            val zpLines = (0..0xff step 32).map { location ->
                " ${'$'}${location.toString(16).padStart(2, '0')}  "+((0..31).joinToString(" ") { lineoffset ->
                    pages[location+lineoffset].toString(16).padStart(2, '0')
                })
            }
            val stackLines = (0x100..0x1ff step 32).map { location ->
                "${'$'}${location.toString(16).padStart(2, '0')}  "+((0..31).joinToString(" ") { lineoffset ->
                    pages[location+lineoffset].toString(16).padStart(2, '0')
                })
            }
            zeropageTf.text = zpLines.joinToString("\n")
            stackpageTf.text = stackLines.joinToString("\n")
        }

        speedKhzTf.text = "%.1f".format(cpu.averageSpeedKhzSinceReset)
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
        contentPane.layout = GridBagLayout()
        defaultCloseOperation = EXIT_ON_CLOSE
        isResizable = false
        isFocusable = true
        contentPane.background = ScreenDefs.BORDER_COLOR
        val gc = GridBagConstraints()
        gc.fill = GridBagConstraints.BOTH
        gc.gridx = 1
        gc.gridy = 1
        gc.insets = Insets(ScreenDefs.BORDER_SIZE, ScreenDefs.BORDER_SIZE, ScreenDefs.BORDER_SIZE, ScreenDefs.BORDER_SIZE)
        contentPane.add(canvas, gc)
        addKeyListener(this)
        addMouseMotionListener(this)
        addMouseListener(this)
        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, mutableSetOf())
        pack()
        setLocationRelativeTo(null)
        location = Point(location.x/2, location.y)
        requestFocusInWindow()
    }

    fun start(updateRate: Int) {
        // repaint the screen's back buffer
        var cursorBlink = 0L
        val repaintTimer = javax.swing.Timer(1000/updateRate) {
            repaint()
            if (it.`when`-cursorBlink > 200L) {
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
        while (keyboardBuffer.size > 8) keyboardBuffer.pop()
    }

    override fun keyPressed(event: KeyEvent) {}
    override fun keyReleased(event: KeyEvent) {}

    // mouse events:
    override fun mousePressed(event: MouseEvent) {
        val pos = canvas.mousePixelPosition()
        if (pos == null) return
        else {
            mousePos = pos
            leftButton = leftButton or SwingUtilities.isLeftMouseButton(event)
            rightButton = rightButton or SwingUtilities.isRightMouseButton(event)
            middleButton = middleButton or SwingUtilities.isMiddleMouseButton(event)
        }
    }

    override fun mouseReleased(event: MouseEvent) {
        val pos = canvas.mousePixelPosition()
        if (pos == null) return
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
        if (pos == null) return
        else mousePos = pos
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
        return if (keyboardBuffer.isEmpty()) null
        else keyboardBuffer.pop()
    }

}
