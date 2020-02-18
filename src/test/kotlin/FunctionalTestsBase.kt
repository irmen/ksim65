import razorvine.ksim65.Bus
import razorvine.ksim65.Cpu6502
import razorvine.ksim65.components.Ram
import kotlin.test.*

/*
Wolfgang Lorenz's 6502 test suite
See http://www.softwolves.com/arkiv/cbm-hackers/7/7114.html
 */
abstract class FunctionalTestsBase {

    val cpu: Cpu6502 = Cpu6502()
    val ram = Ram(0, 0xffff)
    val bus = Bus()
    val kernalStubs = C64KernalStubs(ram)

    init {
        cpu.tracing = null
        cpu.addBreakpoint(0xffd2) { cpu, pc -> kernalStubs.handleBreakpoint(cpu, pc) }
        cpu.addBreakpoint(0xffe4) { cpu, pc -> kernalStubs.handleBreakpoint(cpu, pc) }
        cpu.addBreakpoint(0xe16f) { cpu, pc -> kernalStubs.handleBreakpoint(cpu, pc) }
        cpu.addBreakpoint(0x8000) { cpu, pc -> kernalStubs.handleBreakpoint(cpu, pc) }
        cpu.addBreakpoint(0xa474) { cpu, pc -> kernalStubs.handleBreakpoint(cpu, pc) }
        bus.add(cpu)
        bus.add(ram)
        bus.reset()
    }

    protected fun runTest(testprogram: String) {
        // setup the irq/brk routine and other stubbing
        // http://www.softwolves.com/arkiv/cbm-hackers/7/7114.html
        for(b in listOf(0x48, 0x8A, 0x48, 0x98, 0x48, 0xBA, 0xBD, 0x04,
                        0x01, 0x29, 0x10, 0xF0, 0x03, 0x6C, 0x16, 0x03,
                        0x6C, 0x14, 0x03).withIndex()) {
            ram[0xff48+b.index] = b.value.toShort()
        }
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
        cpu.regP.fromInt(4)
        cpu.regPC = 0x0801
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

    protected fun runTestExpectNotImplemented(testprogram: String) {
        try {
            runTest(testprogram)
            fail("expected to crash with NotImplementedError")
        } catch(nx: NotImplementedError) {
            // okay!
        }
    }
}
