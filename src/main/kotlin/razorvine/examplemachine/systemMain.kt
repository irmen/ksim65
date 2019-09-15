package razorvine.examplemachine

import kotlin.concurrent.scheduleAtFixedRate
import java.time.LocalDateTime
import razorvine.ksim65.Bus
import razorvine.ksim65.Cpu6502
import razorvine.ksim65.Version
import razorvine.ksim65.components.*
import razorvine.ksim65.components.Timer

/**
 * A virtual computer constructed from the various virtual components
 */
class VirtualMachine(title: String) {
    val bus = Bus()
    val cpu = Cpu6502(false)
    val ram = Ram(0x0000, 0xffff)
    private val rtc = RealTimeClock(0xd100, 0xd108)
    private val timer = Timer(0xd200, 0xd203, cpu)

    private val hostDisplay = MainWindow(title)
    private val debugWindow = DebugWindow(this)
    private val display = Display(0xd000, 0xd00a, hostDisplay,
        ScreenDefs.SCREEN_WIDTH_CHARS, ScreenDefs.SCREEN_HEIGHT_CHARS,
        ScreenDefs.SCREEN_WIDTH, ScreenDefs.SCREEN_HEIGHT)
    private val mouse = Mouse(0xd300, 0xd304, hostDisplay)
    private val keyboard = Keyboard(0xd400, 0xd400, hostDisplay)

    init {
        ram[Cpu6502.RESET_vector] = 0x00
        ram[Cpu6502.RESET_vector + 1] = 0x10

        bus += rtc
        bus += timer
        bus += display
        bus += mouse
        bus += keyboard
        bus += ram
        bus += cpu
        bus.reset()

        hostDisplay.start()

        debugWindow.setLocation(hostDisplay.location.x+hostDisplay.width, hostDisplay.location.y)
        debugWindow.isVisible = true
    }

    var paused = false

    fun clock() {
        if(!paused) {
            bus.clock()
            debugWindow.updateCpu(cpu)
        }
    }
}

fun main(args: Array<String>) {
    val machine = VirtualMachine("KSim65 demo virtual machine - using ksim65 v${Version.version}")
    val v = 0xd000

    machine.bus[v + 0x08] = 20
    machine.bus[v + 0x09] = 2
    val text = ">> Hello this is an example text! 1234567890 <<\n" +
            "next line 1\n" +
            "next line 2\n" +
            "next line 3\rnext line 4\rnext line 5\n" +
            "a mistakk\be\n\n\n\n\n\n\n\n\n\n"
    text.forEach {
        machine.bus[v + 0x0a] = it.toShort()
    }

    repeat(20) {
        Thread.sleep(100)
        "time: ${LocalDateTime.now()}\n".forEach { c ->
            machine.bus[v + 0x0a] = c.toShort()
        }
    }

    val timer = java.util.Timer("clock", true)
    timer.scheduleAtFixedRate(1, 1) {
        machine.clock()
    }
}
