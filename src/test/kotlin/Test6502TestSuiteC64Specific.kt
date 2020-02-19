import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import razorvine.c64emu.*
import razorvine.ksim65.Assembler
import razorvine.ksim65.Bus
import razorvine.ksim65.Cpu6502
import razorvine.ksim65.components.Ram
import razorvine.ksim65.components.Rom
import kotlin.test.*

@Execution(ExecutionMode.CONCURRENT)
class Test6502TestSuiteC64Specific {

    val cpu: Cpu6502 = Cpu6502()
    val ioPort = CpuIoPort()
    val ram = Ram(0, 0xffff)
    val bus: Bus
    val kernalStubs = C64KernalStubs(ram)

    init {
        val romsPath = determineRomPath()
        val chargenRom = Rom(0xd000, 0xdfff).also {
            val chargenData = romsPath.resolve("chargen").toFile().readBytes()
            it.load(chargenData)
        }
        val basicRom = Rom(0xa000, 0xbfff).also {
            val basicData = romsPath.resolve("basic").toFile().readBytes()
            it.load(basicData)
        }
        val kernalRom = Rom(0xe000, 0xffff).also {
            val kernalData = romsPath.resolve("kernal").toFile().readBytes()
            it.load(kernalData)
        }

        cpu.tracing = null
        cpu.addBreakpoint(0xffd2) { cpu, pc -> kernalStubs.handleBreakpoint(cpu, pc) }
        cpu.addBreakpoint(0xffe4) { cpu, pc -> kernalStubs.handleBreakpoint(cpu, pc) }
        cpu.addBreakpoint(0xe16f) { cpu, pc -> kernalStubs.handleBreakpoint(cpu, pc) }
        cpu.addBreakpoint(0x8000) { cpu, pc -> kernalStubs.handleBreakpoint(cpu, pc) }
        cpu.addBreakpoint(0xa474) { cpu, pc -> kernalStubs.handleBreakpoint(cpu, pc) }

        bus = Bus6510(ioPort, chargenRom, basicRom, kernalRom)
        bus += VicII(0xd000, 0xd3ff, cpu)
        bus += Cia(1, 0xdc00, 0xdcff, cpu)
        bus += Cia(2, 0xdd00, 0xddff, cpu)
        bus += ioPort
        bus += cpu
        bus += ram      // note: the ROMs are mapped depending on the cpu's io port
        bus.reset()
    }

    private fun runTest(testprogram: String) {
        // setup the irq/brk routine and other stubbing
        // http://www.softwolves.com/arkiv/cbm-hackers/7/7114.html
        val assembler = Assembler(cpu, ram, 0xff48)
        val result = assembler.assemble("""
   pha
   txa
   pha
   tya
   pha
   tsx
   lda ${'$'}0104,x
   and #${'$'}10
   beq *+5
   jmp (${'$'}0316)
   jmp (${'$'}0314)            
""".lines())
        assertTrue(result.success)

        ram.loadPrg("src/test/kotlin/6502testsuite/$testprogram", null)
        ram[0x02] = 0
        ram[0xa002] = 0
        ram[0xa003] = 0x80
        ram[Cpu6502.IRQ_vector] = 0x48
        ram[Cpu6502.IRQ_vector + 1] = 0xff
        ram[Cpu6502.RESET_vector] = 0x01
        ram[Cpu6502.RESET_vector + 1] = 0x08
        ram[0x01fe] = 0xff
        ram[0x01ff] = 0x7f
        bus[0] = 47
        bus[1] = 55
        cpu.regPC = 0x0801
        cpu.regP.fromInt(4)
        try {
            while (cpu.totalCycles < 40000000L) {
                bus.clock()
            }
            fail("test hangs: " + cpu.snapshot())
        } catch (e: Cpu6502.InstructionError) {
            println(">>> INSTRUCTION ERROR: ${e.message}")
        } catch (le: KernalLoadNextPart) {
            return  // test ok
        } catch (ie: KernalInputRequired) {
            fail("test failed")
        }
        fail("test failed")
    }

    @Test
    fun testRegularShouldSucceed() {
        // as long as this one doesn't succeed, there's something wrong with the test setup
        runTest("adca")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testCia1pb6() {
        runTest("cia1pb6")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testCia1pb7() {
        runTest("cia1pb7")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testCia1ta() {
        runTest("cia1ta")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testCia1tab() {
        runTest("cia1tab")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testCia1tb() {
        runTest("cia1tb")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testCia1tb123() {
        runTest("cia1tb123")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testCia2pb6() {
        runTest("cia2pb6")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testCia2pb7() {
        runTest("cia2pb7")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testCia2ta() {
        runTest("cia2ta")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testCia2tb() {
        runTest("cia2tb")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testCia2tb123() {
        runTest("cia2tb123")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testCntdef() {
        runTest("cntdef")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testCnto2() {
        runTest("cnto2")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testCputiming() {
        runTest("cputiming")        // TODO fix this test once the cycle times are correct?
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testFlipos() {
        runTest("flipos")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testIcr01() {
        runTest("icr01")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testImr() {
        runTest("imr")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testIrq() {
        runTest("irq")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testLoadth() {
        runTest("loadth")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testNmi() {
        runTest("nmi")
    }

    @Test
    @Disabled("tests c64 specific hardware")
    fun testOneshot() {
        runTest("oneshot")
    }

    @Test
    fun testCpuport() {
        runTest("cpuport")
    }

    @Test
    fun testMmu() {
        runTest("mmu")
    }

    @Test
    fun testMmufetch() {
        runTest("mmufetch")
    }
}
