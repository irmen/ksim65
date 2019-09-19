package razorvine.c64emu

import razorvine.examplemachine.DebugWindow
import kotlin.concurrent.scheduleAtFixedRate
import razorvine.ksim65.Bus
import razorvine.ksim65.Cpu6502
import razorvine.ksim65.IVirtualMachine
import razorvine.ksim65.Version
import razorvine.ksim65.components.*
import java.io.File
import java.nio.file.Paths
import javax.swing.ImageIcon

/**
 * The virtual representation of the Commodore-64
 */
class C64Machine(title: String) : IVirtualMachine {

    private val romsPath = Paths.get(expandUser("~/.vice/C64"))
    private val chargenData = romsPath.resolve("chargen").toFile().readBytes()
    private val basicData = romsPath.resolve("basic").toFile().readBytes()
    private val kernalData = romsPath.resolve("kernal").toFile().readBytes()

    override val bus = Bus()
    override val cpu = Cpu6502(false)
    val ram = Ram(0x0000, 0xffff)
    val vic = VicII(0xd000, 0xd3ff)
    val basicRom = Rom(0xa000, 0xbfff).also { it.load(basicData) }
    val kernalRom = Rom(0xe000, 0xffff).also { it.load(kernalData) }

    private val debugWindow = DebugWindow(this)
    private val hostDisplay = MainWindow(title, chargenData, ram)

    init {
        hostDisplay.iconImage = ImageIcon(javaClass.getResource("/icon.png")).image
        debugWindow.iconImage = hostDisplay.iconImage
        debugWindow.setLocation(hostDisplay.location.x+hostDisplay.width, hostDisplay.location.y)

        bus += basicRom
        bus += kernalRom
        bus += vic
        bus += ram
        bus += cpu
        bus.reset()

        debugWindow.isVisible = true
        hostDisplay.isVisible = true
        hostDisplay.start()
    }

    private fun expandUser(path: String): String {
        if(path.startsWith("~" + File.separator)) {
            return System.getProperty("user.home") + path.substring(1)
        } else {
            throw UnsupportedOperationException("home dir expansion not implemented for other users")
        }
    }

    override var paused = false

    override fun stepInstruction() {
        while (cpu.instrCycles > 0) bus.clock()
        bus.clock()
        while (cpu.instrCycles > 0) bus.clock()
    }

    fun start() {
        val timer = java.util.Timer("clock", true)
        val startTime = System.currentTimeMillis()
        timer.scheduleAtFixedRate(1, 1) {
            if(!paused) {
                repeat(400) {
                    stepInstruction()
                    if(vic.currentRasterLine == 255) {
                        // we force an irq here ourselves rather than fully emulating the VIC-II's raster IRQ
                        // or the CIA timer IRQ/NMI.
                        cpu.irq()
                    }
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
    val machine = C64Machine("virtual Commodore-64 - using KSim65 v${Version.version}")
    machine.start()
}
