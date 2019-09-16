package razorvine.examplemachine

import razorvine.ksim65.Cpu6502
import java.awt.*
import java.awt.image.BufferedImage
import java.util.ArrayDeque
import javax.imageio.ImageIO
import javax.swing.event.MouseInputListener
import razorvine.ksim65.IHostInterface
import java.awt.event.*
import javax.swing.*


/**
 * Define a monochrome screen that can display 640x480 pixels
 * and/or 80x30 characters (these are 8x16 pixels).
 */
object ScreenDefs {
    const val SCREEN_WIDTH_CHARS = 80
    const val SCREEN_HEIGHT_CHARS = 30
    const val SCREEN_WIDTH = SCREEN_WIDTH_CHARS * 8
    const val SCREEN_HEIGHT = SCREEN_HEIGHT_CHARS * 16
    const val DISPLAY_PIXEL_SCALING: Double = 1.5
    val BG_COLOR = Color(0, 10, 20)
    val FG_COLOR = Color(200, 255, 230)
    val BORDER_COLOR = Color(20, 30, 40)
    val Characters = loadCharacters()

    private fun loadCharacters(): Array<BufferedImage> {
        val img = ImageIO.read(javaClass.getResource("/charset/unscii8x16.png"))
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
    }

    fun clearScreen() {
        g2d.background = ScreenDefs.BG_COLOR
        g2d.clearRect(0, 0, ScreenDefs.SCREEN_WIDTH, ScreenDefs.SCREEN_HEIGHT)
        cursorX = 0
        cursorY = 0
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
}

class DebugWindow(val vm: VirtualMachine) : JFrame("debugger"), ActionListener {
    val cyclesTf = JTextField("00000000000000")
    val regAtf = JTextField("000")
    val regXtf = JTextField("000")
    val regYtf = JTextField("000")
    val regPCtf = JTextField("00000")
    val regSPtf = JTextField("000")
    val regPtf = JTextField("000000000")
    val opcodeTf = JTextField("000")
    val mnemonicTf = JTextField("brk ")
    private val pauseBt = JButton("Pause").also { it.actionCommand = "pause" }

    init {
        isFocusable = true
        val cpuPanel = JPanel(GridBagLayout())
        cpuPanel.border = BorderFactory.createTitledBorder("CPU: ${vm.cpu.name}")
        val gc = GridBagConstraints()
        gc.insets = Insets(4, 4, 4, 4)
        gc.anchor = GridBagConstraints.EAST
        gc.gridx = 0
        gc.gridy = 0
        val cyclesLb = JLabel("cycles")
        val regAlb = JLabel("A")
        val regXlb = JLabel("X")
        val regYlb = JLabel("Y")
        val regSPlb = JLabel("SP")
        val regPClb = JLabel("PC")
        val dummyLb = JLabel("")
        val regPlb = JLabel("Status")
        val opcodeLb = JLabel("opcode")
        val mnemonicLb = JLabel("mnemonic")
        listOf(cyclesLb, regAlb, regXlb, regYlb, regSPlb, regPClb, dummyLb, regPlb, opcodeLb, mnemonicLb).forEach {
            cpuPanel.add(it, gc)
            gc.gridy++
        }
        gc.anchor = GridBagConstraints.WEST
        gc.gridx = 1
        gc.gridy = 0
        val bitsLb = JTextField("NV-BDIZC")
        listOf(cyclesTf, regAtf, regXtf, regYtf, regSPtf, regPCtf, bitsLb, regPtf, opcodeTf, mnemonicTf).forEach {
            it.font = Font(Font.MONOSPACED, Font.PLAIN, 16)
            it.isEditable = false
            it.columns = it.text.length
            cpuPanel.add(it, gc)
            gc.gridy++
        }
        add(cpuPanel, BorderLayout.NORTH)

        val buttonPanel = JPanel(FlowLayout())
        buttonPanel.border = BorderFactory.createTitledBorder("Control")

        val resetBt = JButton("Reset").also { it.actionCommand = "reset" }
        val cycleBt = JButton("Step").also { it.actionCommand = "step" }
        listOf(resetBt, cycleBt, pauseBt).forEach {
            it.addActionListener(this)
            buttonPanel.add(it)
        }

        add(buttonPanel, BorderLayout.CENTER)

        pack()
    }

    override fun actionPerformed(e: ActionEvent) {
        when {
            e.actionCommand == "reset" -> {
                vm.bus.reset()
                updateCpu(vm.cpu)
            }
            e.actionCommand == "step" -> {
                vm.stepInstruction()
                updateCpu(vm.cpu)
            }
            e.actionCommand == "pause" -> {
                vm.paused = true
                pauseBt.actionCommand = "continue"
                pauseBt.text = "Cont."
            }
            e.actionCommand == "continue" -> {
                vm.paused = false
                pauseBt.actionCommand = "pause"
                pauseBt.text = "Pause"
            }
        }
    }

    fun updateCpu(cpu: Cpu6502) {
        cyclesTf.text = cpu.totalCycles.toString()
        regAtf.text = cpu.hexB(cpu.regA)
        regXtf.text = cpu.hexB(cpu.regX)
        regYtf.text = cpu.hexB(cpu.regY)
        regPtf.text = cpu.regP.asByte().toString(2).padStart(8, '0')
        regPCtf.text = cpu.hexW(cpu.regPC)
        regSPtf.text = cpu.hexB(cpu.regSP)
        opcodeTf.text = cpu.hexB(cpu.currentOpcode)
        mnemonicTf.text = cpu.currentMnemonic
    }
}


class MainWindow(title: String) : JFrame(title), KeyListener, MouseInputListener, IHostInterface {
    private val canvas = BitmapScreenPanel()
    val keyboardBuffer = ArrayDeque<Char>()
    var mousePos = Point(0, 0)
    var leftButton = false
    var rightButton = false
    var middleButton = false

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
        pack()
        requestFocusInWindow()
        setLocationRelativeTo(null)
        isVisible = true
    }

    fun start() {
        // repaint the screen's back buffer ~60 times per second
        val repaintTimer = Timer(1000 / 60) { repaint() }
        repaintTimer.initialDelay = 0
        repaintTimer.start()
    }

    // keyboard events:
    override fun keyTyped(p0: KeyEvent) {
        println(p0)
        println("[${p0.keyChar}]")
        keyboardBuffer.add(p0.keyChar)
        while (keyboardBuffer.size > 8)
            keyboardBuffer.pop()
    }

    override fun keyPressed(p0: KeyEvent) {}
    override fun keyReleased(p0: KeyEvent) {}

    // mouse events:
    override fun mousePressed(p0: MouseEvent) {}

    override fun mouseReleased(p0: MouseEvent) {}
    override fun mouseEntered(p0: MouseEvent) {}
    override fun mouseExited(p0: MouseEvent) {}
    override fun mouseDragged(p0: MouseEvent) {}
    override fun mouseClicked(p0: MouseEvent) {
        val pos = canvas.mousePixelPosition()
        if (pos == null)
            return
        else {
            mousePos = pos
            leftButton = SwingUtilities.isLeftMouseButton(p0)
            rightButton = SwingUtilities.isRightMouseButton(p0)
            middleButton = SwingUtilities.isMiddleMouseButton(p0)
        }
    }

    override fun mouseMoved(p0: MouseEvent) {
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
    override fun scrollUp() = canvas.scrollUp()
    override fun mouse(): IHostInterface.MouseInfo {
        return IHostInterface.MouseInfo(mousePos.x, mousePos.y, leftButton, rightButton, middleButton)
    }

    override fun keyboard(): Char? {
        return if (keyboardBuffer.isEmpty())
            null
        else
            keyboardBuffer.pop()
    }
}
