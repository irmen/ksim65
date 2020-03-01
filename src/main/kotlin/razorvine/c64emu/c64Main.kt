package razorvine.c64emu

import razorvine.examplemachines.DebugWindow
import razorvine.ksim65.*
import razorvine.ksim65.components.Address
import razorvine.ksim65.components.Ram
import razorvine.ksim65.components.Rom
import razorvine.ksim65.components.UByte
import java.io.File
import java.io.FileFilter
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.ImageIcon
import javax.swing.JOptionPane
import kotlin.concurrent.scheduleAtFixedRate

/**
 * The virtual representation of the Commodore-64
 *
 * It simulates text-mode video.
 * It hooks into the LOAD and SAVE kernal routines so you can actually
 * load and save your basic programs to the host filesystem.
 */
class C64Machine(title: String) : IVirtualMachine {
    private val romsPath = determineRomPath()

    private val chargenRom = Rom(0xd000, 0xdfff).also {
        val chargenData = romsPath.resolve("chargen").toFile().readBytes()
        it.load(chargenData)
    }
    private val basicRom = Rom(0xa000, 0xbfff).also {
        val basicData = romsPath.resolve("basic").toFile().readBytes()
        it.load(basicData)
    }
    private val kernalRom = Rom(0xe000, 0xffff).also {
        val kernalData = romsPath.resolve("kernal").toFile().readBytes()
        it.load(kernalData)
    }

    val cpuIoPort = CpuIoPort()

    // This bus contains "mmu" logic to control the memory bank switching controlled by the 6510's io port in $00/$01.
    // Therefore we provide it the various roms directly and not "connect" these to the bus in the default way.
    override val bus = Bus6510(cpuIoPort, chargenRom, basicRom, kernalRom)
    override val cpu = Cpu6502()

    // the C64 has 64KB of RAM.  Some of it may be banked out and replaced by ROM.
    val ram = Ram(0x0000, 0xffff)

    val vic = VicII(0xd000, 0xd3ff, cpu)
    val cia1 = Cia(1, 0xdc00, 0xdcff, cpu)
    val cia2 = Cia(2, 0xdd00, 0xddff, cpu)

    private val monitor = Monitor(bus, cpu)
    private val debugWindow = DebugWindow(this)
    private val hostDisplay = MainC64Window(title, chargenRom, ram, cpu, cia1)
    private var paused = false

    init {
        cpu.addBreakpoint(0xffd5, ::breakpointKernelLoad)       // intercept LOAD subroutine in the kernal
        cpu.addBreakpoint(0xffd8, ::breakpointKernelSave)       // intercept SAVE subroutine in the kernal
        cpu.breakpointForBRK = ::breakpointBRK

        bus += vic
        bus += cia1
        bus += cia2
        bus += cpuIoPort
        bus += ram      // note: the ROMs are mapped depending on the cpu's io port
        bus += cpu
        bus.reset()

        hostDisplay.iconImage = ImageIcon(javaClass.getResource("/icon.png")).image
        debugWindow.iconImage = hostDisplay.iconImage
        debugWindow.setLocation(hostDisplay.location.x+hostDisplay.width, hostDisplay.location.y)
        debugWindow.isVisible = true
        hostDisplay.isVisible = true
        //hostDisplay.start(30)
    }

    private fun breakpointKernelLoad(cpu: Cpu6502, pc: Address): Cpu6502.BreakpointResultAction {
        if (cpu.regA == 0) {
            val fnlen = ram[0xb7]   // file name length
            val fa = ram[0xba]      // device number
            val sa = ram[0xb9]      // secondary address
            val txttab = ram[0x2b]+256*ram[0x2c]  // basic load address ($0801 usually)
            val fnaddr = ram[0xbb]+256*ram[0xbc]  // file name address
            return if (fnlen > 0) {
                val filename = (0 until fnlen).map { ram[fnaddr+it].toChar() }.joinToString("")
                val loadEndAddress = searchAndLoadFile(filename, fa, sa, txttab)
                if (loadEndAddress != null) {
                    ram[0x90] = 0  // status OK
                    ram[0xae] = (loadEndAddress and 0xff).toShort()
                    ram[0xaf] = (loadEndAddress ushr 8).toShort()
                    Cpu6502.BreakpointResultAction(changePC = 0xf5a9)  // success!
                } else Cpu6502.BreakpointResultAction(changePC = 0xf704)   // 'file not found'
            } else Cpu6502.BreakpointResultAction(changePC = 0xf710)  // 'missing file name'
        } else return Cpu6502.BreakpointResultAction(changePC = 0xf707)   // 'device not present' (VERIFY command not supported)
    }

    private fun breakpointKernelSave(cpu: Cpu6502, pc: Address): Cpu6502.BreakpointResultAction {
        val fnlen = ram[0xb7]   // file name length
        //        val fa = ram[0xba]      // device number
        //        val sa = ram[0xb9]      // secondary address
        val fnaddr = ram[0xbb]+256*ram[0xbc]  // file name address
        return if (fnlen > 0) {
            val fromAddr = ram[cpu.regA]+256*ram[cpu.regA+1]
            val endAddr = cpu.regX+256*cpu.regY
            val data = (fromAddr..endAddr).map { ram[it].toByte() }.toByteArray()
            var filename = (0 until fnlen).map { ram[fnaddr+it].toChar() }.joinToString("").toLowerCase()
            if (!filename.endsWith(".prg")) filename += ".prg"
            File(filename).outputStream().use {
                it.write(fromAddr and 0xff)
                it.write(fromAddr ushr 8)
                it.write(data)
            }
            ram[0x90] = 0  // status OK
            Cpu6502.BreakpointResultAction(changePC = 0xf5a9)  // success!
        } else Cpu6502.BreakpointResultAction(changePC = 0xf710)  // 'missing file name'
    }

    private fun breakpointBRK(cpu: Cpu6502, pc: Address): Cpu6502.BreakpointResultAction {
        throw Cpu6502.InstructionError("BRK instruction hit at $${hexW(pc)}")
    }

    private fun searchAndLoadFile(filename: String, device: UByte, secondary: UByte, basicLoadAddress: Address): Address? {
        when (filename) {
            "*" -> {
                // load the first file in the directory
                return searchAndLoadFile(File(".").listFiles()?.firstOrNull()?.name ?: "", device, secondary, basicLoadAddress)
            }
            "$" -> {
                // load the directory
                val files = File(".").listFiles(FileFilter { it.isFile })!!.associate {
                    val name = it.nameWithoutExtension.toUpperCase()
                    val ext = it.extension.toUpperCase()
                    val fileAndSize = Pair(it, it.length())
                    if (name.isEmpty()) Pair(".$ext", "") to fileAndSize
                    else Pair(name, ext) to fileAndSize
                }
                val dirname = File(".").canonicalPath.substringAfterLast(File.separator).toUpperCase()
                val dirlisting = makeDirListing(dirname, files, basicLoadAddress)
                ram.load(dirlisting, basicLoadAddress)
                return basicLoadAddress+dirlisting.size-1
            }
            else -> {
                fun findHostFile(filename: String): String? {
                    val file = File(".").listFiles(FileFilter { it.isFile })?.firstOrNull {
                        it.name.toUpperCase() == filename
                    }
                    return file?.name
                }

                val hostFileName = findHostFile(filename) ?: findHostFile("$filename.PRG") ?: return null
                return try {
                    return if (secondary == 1.toShort()) {
                        val (loadAddress, size) = ram.loadPrg(hostFileName, null)
                        loadAddress+size-1
                    } else {
                        val (loadAddress, size) = ram.loadPrg(hostFileName, basicLoadAddress)
                        loadAddress+size-1
                    }
                } catch (iox: IOException) {
                    println("LOAD ERROR $iox")
                    null
                }
            }
        }
    }

    private fun makeDirListing(dirname: String, files: Map<Pair<String, String>, Pair<File, Long>>,
                               basicLoadAddress: Address): Array<UByte> {
        var address = basicLoadAddress
        val listing = mutableListOf<UByte>()
        fun addLine(lineNumber: Int, line: String) {
            address += line.length+3
            listing.add((address and 0xff).toShort())
            listing.add((address ushr 8).toShort())
            listing.add((lineNumber and 0xff).toShort())
            listing.add((lineNumber ushr 8).toShort())
            listing.addAll(line.map { it.toShort() })
            listing.add(0)
        }
        addLine(0, "\u0012\"${dirname.take(16).padEnd(16)}\" 00 2A")
        var totalBlocks = 0
        files.forEach {
            val blocksize = (it.value.second/256).toInt()
            totalBlocks += blocksize
            val filename = it.key.first.take(16)
            val padding1 = "   ".substring(blocksize.toString().length)
            val padding2 = "                ".substring(filename.length)
            addLine(blocksize, "$padding1 \"$filename\" $padding2${it.key.second.take(3).padEnd(3)}")
        }
        addLine(kotlin.math.max(0, 664-totalBlocks), "BLOCKS FREE.")
        listing.add(0)
        listing.add(0)
        return listing.toTypedArray()
    }

    override fun loadFileInRam(file: File, loadAddress: Address?) {
        if (file.extension == "prg" && (loadAddress == null || loadAddress == 0x0801)) ram.loadPrg(file.inputStream(), null)
        else ram.load(file.readBytes(), loadAddress!!)
    }

    override fun getZeroAndStackPages(): Array<UByte> = ram.getBlock(0, 512)

    override fun pause(paused: Boolean) {
        this.paused = paused
    }

    override fun step() {
        // step a single full instruction
        while (cpu.instrCycles > 0) bus.clock()
        bus.clock()
        while (cpu.instrCycles > 0) bus.clock()
    }

    override fun reset() {
        bus.reset()
        hostDisplay.reset()
    }

    override fun executeMonitorCommand(command: String) = monitor.command(command)

    fun start() {
        javax.swing.Timer(50) {
            debugWindow.updateCpu(cpu, bus)
        }.start()

        // we synchronise cpu cycles to the vertical blank of the Vic chip
        // this should result in ~1 Mhz cpu speed
        val timer = java.util.Timer("cpu-cycle", true)
        timer.scheduleAtFixedRate(0, 1000L/VicII.framerate) {
//            if(cpu.isLooping) {
//                // cpu is jump looping, could do some sleeping here perhaps
//                // but should still consider irqs occurring in the meantime...
//            }
            if (!paused) {
                try {
                    while (vic.vsync) step()
                    while (!vic.vsync) step()
                    hostDisplay.repaint()           // repaint synced with VIC vertical blank
                } catch (rx: RuntimeException) {
                    JOptionPane.showMessageDialog(hostDisplay, "Run time error: $rx", "Error during execution", JOptionPane.ERROR_MESSAGE)
                    this.cancel()
                    throw rx
                } catch (ex: Error) {
                    JOptionPane.showMessageDialog(hostDisplay, "Run time error: $ex", "Error during execution", JOptionPane.ERROR_MESSAGE)
                    this.cancel()
                    throw ex
                }
            }
        }
    }
}


fun determineRomPath(): Path {
    val candidates = listOf("./roms", "~/roms/c64", "~/roms", "~/.vice/C64")
    candidates.forEach {
        val path = Paths.get(expandUser(it))
        if (path.toFile().isDirectory) return path
    }
    throw FileNotFoundException("no roms directory found, tried: $candidates")
}

fun expandUser(path: String): String {
    return when {
        path.startsWith("~/") -> System.getProperty("user.home")+path.substring(1)
        path.startsWith("~"+File.separatorChar) -> System.getProperty("user.home")+path.substring(1)
        path.startsWith("~") -> throw UnsupportedOperationException("home dir expansion not implemented for other users")
        else -> path
    }
}


fun main() {
    val machine = C64Machine("virtual Commodore-64 - using KSim65 v${Version.version}")
    machine.start()
}
