import razorvine.ksim65.Assembler
import razorvine.ksim65.Bus
import razorvine.ksim65.Cpu6502
import razorvine.ksim65.Cpu6502Core
import razorvine.ksim65.components.Ram
import kotlin.test.*

/*
Wolfgang Lorenz's 6502 test suite
See http://www.softwolves.com/arkiv/cbm-hackers/7/7114.html
 */
abstract class FunctionalTestsBase {

    val cpu = Cpu6502()
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
        val assembler = Assembler(cpu, ram, 0xff48)
        val result = assembler.assemble("""
   pha
   txa
   pha
   tya
   pha
   tsx
   lda $0104,x
   and #$10
   beq *+5
   jmp ($0316)
   jmp ($0314)            
""".lines())
        assertTrue(result.success)

        ram.loadPrg("src/test/kotlin/6502testsuite/$testprogram", null)
        ram[0x02] = 0
        ram[0xa002] = 0
        ram[0xa003] = 0x80
        ram[Cpu6502Core.IRQ_VECTOR] = 0x48
        ram[Cpu6502Core.IRQ_VECTOR+1] = 0xff
        ram[Cpu6502Core.RESET_VECTOR] = 0x01
        ram[Cpu6502Core.RESET_VECTOR+1] = 0x08
        ram[0x01fe] = 0xff
        ram[0x01ff] = 0x7f
        cpu.regP.fromInt(4)
        cpu.regPC = 0x0801
        try {
            while (cpu.totalCycles < 40000000L) {
                bus.clock()
            }
            fail("test hangs: " + cpu.snapshot())
        } catch (e: Cpu6502Core.InstructionError) {
            println(">>> INSTRUCTION ERROR: ${e.message}")
        } catch (_: KernalLoadNextPart) {
            return  // test ok
        } catch (_: KernalInputRequired) {
            fail("test failed")
        }
        fail("test failed")
    }
}
