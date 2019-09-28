package razorvine.c64emu

import razorvine.examplemachines.DebugWindow
import razorvine.ksim65.*
import razorvine.ksim65.components.*
import java.io.File
import java.io.FileFilter
import java.io.FileNotFoundException
import java.io.IOException
import java.io.Serializable
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

    fun breakpointKernelLoad(cpu: Cpu6502, pc: Address): Cpu6502.BreakpointResult {
        if(cpu.regA==0) {
            val fnlen = ram[0xb7]   // file name length
            val fa = ram[0xba]      // device number
            val sa = ram[0xb9]      // secondary address
            val txttab = ram[0x2b] + 256*ram[0x2c]  // basic load address ($0801 usually)
            val fnaddr = ram[0xbb] + 256*ram[0xbc]  // file name address
            return if(fnlen>0) {
                val filename = (0 until fnlen).map { ram[fnaddr+it].toChar() }.joinToString("")
                val loadEndAddress = searchAndLoadFile(filename, fa, sa, txttab)
                if(loadEndAddress!=null) {
                    ram[0x90] = 0  // status OK
                    ram[0xae] = (loadEndAddress and 0xff).toShort()
                    ram[0xaf] = (loadEndAddress ushr 8).toShort()
                    Cpu6502.BreakpointResult(0xf5a9, 0)  // success!
                } else Cpu6502.BreakpointResult(0xf704, null)   // 'file not found'
            } else Cpu6502.BreakpointResult(0xf710, null)  // 'missing file name'
        } else return Cpu6502.BreakpointResult(0xf707, null)   // 'device not present' (VERIFY command not supported)
    }

    // TODO: breakpoint on kernel SAVE

    private fun searchAndLoadFile(filename: String, device: UByte, secondary: UByte, basicLoadAddress: Address): Address? {
        when (filename) {
            "*" ->  {
                // load the first file in the directory
                return searchAndLoadFile(File(".").listFiles()?.firstOrNull()?.name ?: "", device, secondary, basicLoadAddress)
            }
            "$" -> {
                // load the directory
                val files = File(".")
                    .listFiles(FileFilter { it.isFile })!!
                    .associate {
                        val name = it.nameWithoutExtension.toUpperCase()
                        val ext = it.extension.toUpperCase()
                        val fileAndSize = Pair(it, it.length())
                        if(name.isEmpty())
                            Pair("." + ext, "") to fileAndSize
                        else
                            Pair(name, ext) to fileAndSize
                    }
                val dirname = File(".").canonicalPath.substringAfterLast(File.separator).toUpperCase()
                val dirlisting = makeDirListing(dirname, files, basicLoadAddress)
                ram.load(dirlisting, basicLoadAddress)
                return basicLoadAddress + dirlisting.size - 1
            }
            else -> {
                fun findHostFile(filename: String): String? {
                    val file = File(".").listFiles(FileFilter { it.isFile })?.firstOrNull {
                        it.name.toUpperCase()==filename
                    }
                    return file?.name
                }
                val hostFileName = findHostFile(filename) ?: findHostFile("$filename.PRG") ?: return null
                return try {
                    return if (secondary == 1.toShort()) {
                        val (loadAddress, size) = ram.loadPrg(hostFileName, null)
                        loadAddress + size - 1
                    } else {
                        val (loadAddress, size) = ram.loadPrg(hostFileName, basicLoadAddress)
                        loadAddress + size - 1
                    }
                } catch (iox: IOException) {
                    println("LOAD ERROR $iox")
                    null
                }
            }
        }
    }

    private fun makeDirListing(
        dirname: String,
        files: Map<Pair<String, String>, Pair<File, Long>>,
        basicLoadAddress: Address
    ): Array<UByte> {
        var address = basicLoadAddress
        val listing = mutableListOf<UByte>()
        fun addLine(lineNumber: Int, line: String) {
            address += line.length + 3
            listing.add((address and 0xff).toShort())
            listing.add((address ushr 8).toShort())
            listing.add((lineNumber and 0xff).toShort())
            listing.add((lineNumber ushr 8).toShort())
            listing.addAll(line.map{ it.toShort() })
            listing.add(0)
        }
        addLine(0, "\u0012\"${dirname.take(16).padEnd(16)}\" 00 2A")
        var totalBlocks = 0
        files.forEach {
            val blocksize = (it.value.second / 256).toInt()
            totalBlocks += blocksize
            val filename = it.key.first.take(16)
            val padding1 = "   ".substring(blocksize.toString().length)
            val padding2 = "               ".substring(filename.length)
            addLine(blocksize, "$padding1 \"$filename\" $padding2 ${it.key.second.take(3).padEnd(3)}" )
        }
        addLine(kotlin.math.max(0, 664-totalBlocks), "BLOCKS FREE.")
        listing.add(0)
        listing.add(0)
        return listing.toTypedArray()
    }

    init {
        hostDisplay.iconImage = ImageIcon(javaClass.getResource("/icon.png")).image
        debugWindow.iconImage = hostDisplay.iconImage
        debugWindow.setLocation(hostDisplay.location.x+hostDisplay.width, hostDisplay.location.y)
        cpu.addBreakpoint(0xffd5, ::breakpointKernelLoad)

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
        if(file.extension=="prg" && (loadAddress==null || loadAddress==0x0801))
            ram.loadPrg(file.inputStream(), null)
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
