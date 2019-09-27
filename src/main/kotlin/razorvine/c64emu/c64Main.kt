package razorvine.c64emu

import razorvine.examplemachines.DebugWindow
import razorvine.ksim65.Bus
import razorvine.ksim65.Cpu6502
import razorvine.ksim65.IVirtualMachine
import razorvine.ksim65.Version
import razorvine.ksim65.components.*
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.ImageIcon

/**
 * The virtual representation of the Commodore-64
 */
class C64Machine(title: String) : IVirtualMachine {
    private val romsPath = determineRomPath()
    private val chargenData = romsPath.resolve("chargen").toFile().readBytes()
    private val basicData = romsPath.resolve("basic").toFile().readBytes()
    private val kernalData = romsPath.resolve("kernal").toFile().readBytes()

    override val bus = Bus()
    override val cpu = Cpu6502(false)
    val ram = Ram(0x0000, 0xffff)
    val vic = VicII(0xd000, 0xd3ff)
    val cia1 = Cia(1, 0xdc00, 0xdcff)
    val cia2 = Cia(2, 0xdd00, 0xddff)
    val basicRom = Rom(0xa000, 0xbfff).also { it.load(basicData) }
    val kernalRom = Rom(0xe000, 0xffff).also { it.load(kernalData) }

    private val debugWindow = DebugWindow(this)
    private val hostDisplay = MainC64Window(title, chargenData, ram, cpu, cia1)
    private var paused = false

    init {
        hostDisplay.iconImage = ImageIcon(javaClass.getResource("/icon.png")).image
        debugWindow.iconImage = hostDisplay.iconImage
        debugWindow.setLocation(hostDisplay.location.x+hostDisplay.width, hostDisplay.location.y)

        bus += basicRom
        bus += kernalRom
        bus += vic
        bus += cia1
        bus += cia2
        bus += ram
        bus += cpu
        bus.reset()

        debugWindow.isVisible = true
        hostDisplay.isVisible = true
        hostDisplay.start()
    }

    private fun determineRomPath(): Path {
        val candidates = listOf("./roms", "~/roms/c64", "~/roms", "~/.vice/C64")
        candidates.forEach {
            val path = Paths.get(expandUser(it))
            if(path.toFile().isDirectory)
                return path
        }
        throw FileNotFoundException("no roms directory found, tried: $candidates")
    }

    private fun expandUser(path: String): String {
        return when {
            path.startsWith("~/") -> System.getProperty("user.home") + path.substring(1)
            path.startsWith("~" + File.separatorChar) -> System.getProperty("user.home") + path.substring(1)
            path.startsWith("~") -> throw UnsupportedOperationException("home dir expansion not implemented for other users")
            else -> path
        }
    }

    override fun loadFileInRam(file: File, loadAddress: Address?) {
        if(file.extension=="prg" && loadAddress==null)
            ram.loadPrg(file.inputStream())
        else
            ram.load(file.readBytes(), loadAddress!!)
    }

    override fun getZeroAndStackPages(): Array<UByte> = ram.getPages(0, 2)

    override fun pause(paused: Boolean) {
        this.paused = paused
    }

    override fun step() {
        // step a single full instruction
        while (cpu.instrCycles > 0) bus.clock()
        bus.clock()
        while (cpu.instrCycles > 0) bus.clock()
    }

    fun start() {
        javax.swing.Timer(10) {
            debugWindow.updateCpu(cpu, bus)
        }.start()

        // busy waiting loop, averaging cpu speed to ~1 Mhz:
        var numInstructionsteps = 600
        val targetSpeedKhz = 1000
        while (true) {
            if (paused) {
                Thread.sleep(100)
            } else {
                cpu.startSpeedMeasureInterval()
                Thread.sleep(0, 1000)
                repeat(numInstructionsteps) {
                    step()
                    if(vic.currentRasterLine == 255) {
                        // we force an irq here ourselves rather than fully emulating the VIC-II's raster IRQ
                        // or the CIA timer IRQ/NMI.
                        cpu.irq()
                    }
                }
                val speed = cpu.measureAvgIntervalSpeedKhz()
                if (speed < targetSpeedKhz - 50)
                    numInstructionsteps++
                else if (speed > targetSpeedKhz + 50)
                    numInstructionsteps--
            }
        }
    }
}

fun main(args: Array<String>) {
    val machine = C64Machine("virtual Commodore-64 - using KSim65 v${Version.version}")
    machine.start()
}
