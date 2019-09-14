import razorvine.ksim65.Bus
import razorvine.ksim65.components.Ram
import razorvine.ksim65.Cpu6502
import razorvine.ksim65.Cpu65C02
import java.lang.Exception
import kotlin.test.*


class Test6502Functional {

    private class SuccessfulTestResult: Exception()

    @Test
    fun testFunctional6502() {
        val cpu = Cpu6502(false)
        val bus = Bus()
        val ram = Ram(0, 0xffff)
        ram.load("src/test/kotlin/6502_functional_tests/bin_files/6502_functional_test.bin", 0)
        bus.add(cpu)
        bus.add(ram)
        cpu.reset()
        cpu.regPC = 0x0400
        cpu.addBreakpoint(0x3469) { _, _ ->
            // reaching this address means successful test result
            if(cpu.currentOpcode==0x4c)
                throw SuccessfulTestResult()
            Cpu6502.BreakpointResult(null, null)
        }

        try {
            while (cpu.totalCycles < 100000000) {
                cpu.clock()
            }
        } catch (sx: SuccessfulTestResult) {
            println("test successful  ${cpu.totalCycles}")
            return
        }

        println(cpu.logState())
        val d = cpu.disassemble(ram, cpu.regPC-20, cpu.regPC+20)
        println(d.joinToString ("\n"))
        fail("test failed")
    }

    @Test
    fun testFunctional65C02() {
        val cpu = Cpu65C02(false)
        val bus = Bus()
        val ram = Ram(0, 0xffff)
        ram.load("src/test/kotlin/6502_functional_tests/bin_files/65C02_extended_opcodes_test.bin", 0)
        bus.add(cpu)
        bus.add(ram)
        cpu.reset()
        cpu.regPC = 0x0400
        cpu.addBreakpoint(0x24f1) { _, _ ->
            // reaching this address means successful test result
            if(cpu.currentOpcode==0x4c)
                throw SuccessfulTestResult()
            Cpu6502.BreakpointResult(null, null)
        }

        try {
            while (cpu.totalCycles < 100000000) {
                cpu.clock()
            }
        } catch (sx: SuccessfulTestResult) {
            println("test successful")
            return
        }

        println(cpu.logState())
        val d = cpu.disassemble(ram, cpu.regPC-20, cpu.regPC+20)
        println(d.joinToString ("\n"))
        fail("test failed")
    }

}
