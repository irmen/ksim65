package razorvine.examplemachines

import kotlin.concurrent.scheduleAtFixedRate
import razorvine.ksim65.Bus
import razorvine.ksim65.Cpu6502
import razorvine.ksim65.IVirtualMachine
import razorvine.ksim65.Version
import razorvine.ksim65.components.*
import razorvine.ksim65.components.Timer
import javax.swing.ImageIcon

/**
 * A virtual computer constructed from the various virtual components
 */
class VirtualMachine(title: String) : IVirtualMachine {
    override val bus = Bus()
    override val cpu = Cpu6502(false)
    val ram = Ram(0x0000, 0xffff)
    private val rtc = RealTimeClock(0xd100, 0xd108)
    private val timer = Timer(0xd200, 0xd203, cpu)

    private val debugWindow = DebugWindow(this)
    private val hostDisplay = MainWindow(title)
    private val display = Display(0xd000, 0xd00a, hostDisplay,
        ScreenDefs.SCREEN_WIDTH_CHARS, ScreenDefs.SCREEN_HEIGHT_CHARS,
        ScreenDefs.SCREEN_WIDTH, ScreenDefs.SCREEN_HEIGHT)
    private val mouse = Mouse(0xd300, 0xd305, hostDisplay)
    private val keyboard = Keyboard(0xd400, 0xd400, hostDisplay)
    private var paused = false

    init {
        hostDisplay.iconImage = ImageIcon(javaClass.getResource("/icon.png")).image
        debugWindow.iconImage = hostDisplay.iconImage
        debugWindow.setLocation(hostDisplay.location.x+hostDisplay.width, hostDisplay.location.y)

        ram[Cpu6502.RESET_vector] = 0x00
        ram[Cpu6502.RESET_vector + 1] = 0x10
        ram.loadPrg(javaClass.getResourceAsStream("/vmdemo.prg"))

        bus += rtc
        bus += timer
        bus += display
        bus += mouse
        bus += keyboard
        bus += ram
        bus += cpu
        bus.reset()

        debugWindow.isVisible = true
        hostDisplay.isVisible = true
        hostDisplay.start()
    }

    override fun pause(paused: Boolean) {
        this.paused = paused
    }

    override fun step() {
        // step a full single instruction
        while (cpu.instrCycles > 0) bus.clock()
        bus.clock()
        while (cpu.instrCycles > 0) bus.clock()
    }

    fun start() {
        val timer = java.util.Timer("clock", true)
        val startTime = System.currentTimeMillis()
        timer.scheduleAtFixedRate(1, 1) {
            if(!paused) {
                repeat(50) {
                    step()
                }
                debugWindow.updateCpu(cpu, bus)
                val duration = System.currentTimeMillis() - startTime
                val speedKhz = cpu.totalCycles.toDouble() / duration
                debugWindow.speedKhzTf.text = "%.1f".format(speedKhz)
            }
        }
    }
}

fun main(args: Array<String>) {
    val machine = VirtualMachine("KSim65 demo virtual machine - using ksim65 v${Version.version}")
    machine.start()
}
