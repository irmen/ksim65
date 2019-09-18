package razorvine.examplemachine

import kotlin.concurrent.scheduleAtFixedRate
import razorvine.ksim65.Bus
import razorvine.ksim65.Cpu6502
import razorvine.ksim65.Version
import razorvine.ksim65.components.*
import javax.swing.ImageIcon

/**
 * A virtual computer constructed from the various virtual components,
 * running the 6502 Enhanced Basic ROM.
 */
class EhBasicMachine(title: String) {
    val bus = Bus()
    val cpu = Cpu6502(false)
    val ram = Ram(0x0000, 0xbfff)
    val rom = Rom(0xc000, 0xffff).also { it.load(javaClass.getResourceAsStream("/ehbasic_C000.bin").readAllBytes()) }

    private val hostDisplay = MainWindow(title)
    private val display = Display(0xd000, 0xd00a, hostDisplay,
        ScreenDefs.SCREEN_WIDTH_CHARS, ScreenDefs.SCREEN_HEIGHT_CHARS,
        ScreenDefs.SCREEN_WIDTH, ScreenDefs.SCREEN_HEIGHT)
    private val keyboard = Keyboard(0xd400, 0xd400, hostDisplay)

    init {
        hostDisplay.iconImage = ImageIcon(javaClass.getResource("/icon.png")).image

        bus += display
        bus += keyboard
        bus += rom
        bus += ram
        bus += cpu
        bus.reset()

        hostDisplay.start()
        hostDisplay.requestFocus()
    }

    var paused = false

    fun stepInstruction() {
        while (cpu.instrCycles > 0) bus.clock()
        bus.clock()
        while (cpu.instrCycles > 0) bus.clock()
    }

    fun start() {
        val timer = java.util.Timer("clock", true)
        timer.scheduleAtFixedRate(1, 1) {
            if(!paused) {
                repeat(500) {
                    stepInstruction()
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    val machine = EhBasicMachine("KSim65 demo virtual machine - using ksim65 v${Version.version}")
    machine.start()
}
