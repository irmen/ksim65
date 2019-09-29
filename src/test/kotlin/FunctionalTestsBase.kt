import razorvine.ksim65.Bus
import razorvine.ksim65.Cpu6502
import razorvine.ksim65.components.Ram
import kotlin.test.*


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

        // create the system bus and add device to it.
        // note that the order is relevant w.r.t. where reads and writes are going.
        ram[0x02] = 0
        ram[0xa002] = 0
        ram[0xa003] = 0x80
        ram[Cpu6502.IRQ_vector] = 0x48
        ram[Cpu6502.IRQ_vector + 1] = 0xff
        ram[Cpu6502.RESET_vector] = 0x01
        ram[Cpu6502.RESET_vector + 1] = 0x08
        ram[0x01fe] = 0xff
        ram[0x01ff] = 0x7f
        ram[0x8000] = 2
        ram[0xa474] = 2

        // setup the irq/brk routine
        for(b in listOf(0x48, 0x8A, 0x48, 0x98, 0x48, 0xBA, 0xBD, 0x04,
                0x01, 0x29, 0x10, 0xF0, 0x03, 0x6C, 0x16, 0x03,
                0x6C, 0x14, 0x03).withIndex()) {
            ram[0xff48+b.index] = b.value.toShort()
        }
        bus.add(cpu)
        bus.add(ram)
    }

    protected fun runTest(testprogram: String) {
        ram.loadPrg("src/test/kotlin/6502testsuite/$testprogram", null)
        bus.reset()
        cpu.regSP = 0xfd
        cpu.regP.fromInt(0b00100100)
        try {
            while (cpu.totalCycles < 50000000L) {
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
}
