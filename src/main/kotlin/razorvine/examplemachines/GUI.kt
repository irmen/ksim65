package razorvine.examplemachines

import razorvine.fonts.PsfFont
import razorvine.ksim65.*
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.util.*
import javax.swing.*
import javax.swing.event.MouseInputListener


/**
 * Define a monochrome screen that can display 80x30 charaacters
 * (usually equivalent to 640x480 pixels, but depends on the font size)
 */
object ScreenDefs {
    const val COLUMNS = 80
    const val ROWS = 30
    const val BORDER_SIZE = 32

    val BG_COLOR = Color(0, 10, 20)
    val FG_COLOR = Color(200, 255, 230)
    val BORDER_COLOR = Color(20, 30, 40)
}

private class BitmapScreenPanel : JPanel() {

    private val image: BufferedImage
    private val g2d: Graphics2D
    private var cursorX: Int = 0
    private var cursorY: Int = 0
    private var cursorState: Boolean = false
    private val screenFont = PsfFont("spleen-12x24")   // nice fonts:  sun12x22, iso01-12x22, ter-124b, spleen-12x24, default8x16
    private val PIXEL_SCALING: Double = if(screenFont.width <= 8) 1.5 else 1.0
    private val screenFontImage: BufferedImage

    init {
        println("SCREENFONT WIDTH: ${screenFont.width}")

        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val gd = ge.defaultScreenDevice.defaultConfiguration
        image = gd.createCompatibleImage(ScreenDefs.COLUMNS*screenFont.width, ScreenDefs.ROWS*screenFont.height, Transparency.OPAQUE)
        g2d = image.graphics as Graphics2D
        screenFontImage = screenFont.convertToImage(g2d, ScreenDefs.FG_COLOR)

        val size = Dimension((image.width*PIXEL_SCALING).toInt(),
                             (image.height*PIXEL_SCALING).toInt())
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
        g2d.drawImage(image, 0, 0, (image.width*PIXEL_SCALING).toInt(),
                      (image.height*PIXEL_SCALING).toInt(), null)
        if (cursorState) {
            val scx = (cursorX*PIXEL_SCALING*screenFont.width).toInt()
            val scy = (cursorY*PIXEL_SCALING*screenFont.height).toInt()
            val scw = (screenFont.width*PIXEL_SCALING).toInt()
            val sch = (screenFont.height*PIXEL_SCALING).toInt()
            g2d.setXORMode(Color.CYAN)
            g2d.fillRect(scx, scy, scw, sch)
            g2d.setPaintMode()
        }
        Toolkit.getDefaultToolkit().sync()
    }

    fun clearScreen() {
        g2d.background = ScreenDefs.BG_COLOR
        g2d.clearRect(0, 0, ScreenDefs.COLUMNS*screenFont.width, ScreenDefs.ROWS*screenFont.height)
        cursorPos(0, 0)
    }

    fun setPixel(x: Int, y: Int, onOff: Boolean) {
        if (onOff) image.setRGB(x, y, ScreenDefs.FG_COLOR.rgb)
        else image.setRGB(x, y, ScreenDefs.BG_COLOR.rgb)
    }

    fun getPixel(x: Int, y: Int) = image.getRGB(x, y) != ScreenDefs.BG_COLOR.rgb

    fun setChar(x: Int, y: Int, character: Char) {
        val charnum = character.toInt()
        val cx = charnum % (screenFontImage.width/screenFont.width)
        val cy = charnum / (screenFontImage.width/screenFont.width)
        g2d.clearRect(x*screenFont.width, y*screenFont.height, screenFont.width, screenFont.height)
        g2d.drawImage(screenFontImage, x*screenFont.width, y*screenFont.height, (x+1)*screenFont.width,
                      (y+1)*screenFont.height, cx*screenFont.width, cy*screenFont.height,
                      (cx+1)*screenFont.width, (cy+1)*screenFont.height, null)
    }

    fun scrollUp() {
        g2d.copyArea(0, screenFont.height,
                     ScreenDefs.COLUMNS*screenFont.width, (ScreenDefs.ROWS-1)*screenFont.height,
                     0, -screenFont.height)
        g2d.background = ScreenDefs.BG_COLOR
        g2d.clearRect(0, (ScreenDefs.ROWS-1)*screenFont.height, ScreenDefs.COLUMNS*screenFont.width, screenFont.height)
    }

    fun mousePixelPosition(): Point? {
        val pos = mousePosition ?: return null
        return Point((pos.x/PIXEL_SCALING).toInt(), (pos.y/PIXEL_SCALING).toInt())
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

    fun reset() {
        // when the reset button is pressed
    }

}
