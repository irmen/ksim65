package razorvine.examplemachines

import razorvine.ksim65.Bus
import razorvine.ksim65.Cpu6502
import razorvine.ksim65.Version
import razorvine.ksim65.components.Display
import razorvine.ksim65.components.Keyboard
import razorvine.ksim65.components.Ram
import razorvine.ksim65.components.Rom
import javax.swing.ImageIcon
import javax.swing.JOptionPane

/**
 * A virtual computer constructed from the various virtual components,
 * running the 6502 Enhanced Basic ROM.
 */
class EhBasicMachine(title: String) {
    val bus = Bus()
    val cpu = Cpu6502()
    val ram = Ram(0x0000, 0xbfff)
    val rom = Rom(0xc000, 0xffff).also { it.load(javaClass.getResourceAsStream("/ehbasic_C000.bin").readBytes()) }

    private val hostDisplay = MainWindow(title)
    private val display = Display(0xd000, 0xd00a, hostDisplay, ScreenDefs.SCREEN_WIDTH_CHARS, ScreenDefs.SCREEN_HEIGHT_CHARS)
    private val keyboard = Keyboard(0xd400, 0xd400, hostDisplay)
    private var paused = false

    init {
        bus += display
        bus += keyboard
        bus += rom
        bus += ram
        bus += cpu
        bus.reset()

        hostDisplay.iconImage = ImageIcon(javaClass.getResource("/icon.png")).image
        hostDisplay.isVisible = true
        hostDisplay.start(30)
    }

    private fun step() {
        // step a full single instruction
        while (cpu.instrCycles > 0) bus.clock()
        bus.clock()
        while (cpu.instrCycles > 0) bus.clock()
    }

    fun start() {
        // a simple loop to run the cpu, not targeting a specific desired clock frequency
        // (but with a small sleep to not run flat out)
        while(true) {
            if(paused) {
                Thread.sleep(200)
            } else {
                try {
                    repeat(400) { step() }
                } catch (rx: RuntimeException) {
                    JOptionPane.showMessageDialog(hostDisplay, "Run time error: $rx", "Error during execution", JOptionPane.ERROR_MESSAGE)
                    break
                } catch (ex: Error) {
                    JOptionPane.showMessageDialog(hostDisplay, "Run time error: $ex", "Error during execution", JOptionPane.ERROR_MESSAGE)
                    break
                }
                Thread.sleep(1)
            }
        }
    }
}

fun main(args: Array<String>) {
    val machine = EhBasicMachine("KSim65 demo virtual machine - using ksim65 v${Version.version}")
    machine.start()
}
