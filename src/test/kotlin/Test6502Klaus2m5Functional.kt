import razorvine.ksim65.Bus
import razorvine.ksim65.components.Ram
import razorvine.ksim65.Cpu6502
import razorvine.ksim65.Cpu65C02
import razorvine.ksim65.components.Address
import razorvine.ksim65.components.BusComponent
import razorvine.ksim65.components.MemMappedComponent
import razorvine.ksim65.components.UByte
import razorvine.ksim65.hexW
import java.lang.Exception
import kotlin.math.max
import kotlin.math.min
import kotlin.test.*


/**
 * This runs the 'functional 6502/65c02' tests from Klaus2m5
 * (Klaus Dormann, sourced from here https://github.com/Klaus2m5/6502_65C02_functional_tests)
 */
class Test6502Klaus2m5Functional {

    private class SuccessfulTestResult: Exception()

    @Test
    fun testFunctional6502() {
        val cpu = Cpu6502()
        val bus = Bus()
        val ram = Ram(0, 0xffff)
        ram.load("src/test/kotlin/6502_functional_tests/bin_files/6502_functional_test.bin", 0)
        bus.add(cpu)
        bus.add(ram)
        cpu.reset()
        cpu.regPC = 0x0400
        try {
            do {
                val previousPC = cpu.regPC
                cpu.step()
            } while(cpu.regPC!=previousPC)
        } catch(nx: NotImplementedError) {
            println("encountered a not yet implemented feature: ${nx.message}")
        }

        // the test is successful if address 0x3469 is reached ("success" label in source code)
        val testnum = bus[0x200].toInt()
        if(cpu.regPC!=0x3469 || testnum!=0xf0) {
            println(cpu.snapshot())
            val d = cpu.disassemble(ram, max(0, cpu.regPC-20), min(65535, cpu.regPC+20))
            println(d.first.joinToString("\n"))
            fail("test failed")
        }
    }

    @Test
    fun testExtendedOpcodes65C02() {
        val cpu = Cpu65C02()
        val bus = Bus()
        val ram = Ram(0, 0xffff)
        ram.load("src/test/kotlin/6502_functional_tests/bin_files/65C02_extended_opcodes_test.bin", 0)
        bus.add(cpu)
        bus.add(ram)
        cpu.reset()
        cpu.regPC = 0x0400
        try {
            do {
                val previousPC = cpu.regPC
                cpu.step()
            } while(cpu.regPC!=previousPC)
        } catch(nx: NotImplementedError) {
            println("encountered a not yet implemented feature: ${nx.message}")
        }

        // the test is successful if address 0x24f1 is reached ("success" label in source code)
        val testnum = bus[0x202].toInt()
        println(testnum)
        if(cpu.regPC!=0x24f1 || testnum!=0xf0) {
            println(cpu.snapshot())
            val d = cpu.disassemble(ram, max(0, cpu.regPC-20), min(65535, cpu.regPC+20))
            println(d.first.joinToString("\n"))
            fail("test failed")
        }
    }

    @Test
    fun testInterrupts6502() {
        val cpu = Cpu6502()
        class Trigger(startAddress: Address, endAddress: Address) : MemMappedComponent(startAddress, endAddress) {
            var value: UByte = 0
            var lastIRQpc: Address = 0
            var lastNMIpc: Address = 0

            override fun get(offset: Int): UByte  = value

            override fun set(offset: Int, data: UByte) {
                value = data
                when(value.toInt()) {
                    1 -> {
                        // println("IRQ at pc ${hexW(cpu.regPC)}")
                        lastIRQpc = cpu.regPC
                        cpu.irqAsserted = true
                    }
                    2 -> {
                        // println("NMI at pc ${hexW(cpu.regPC)}")
                        lastNMIpc = cpu.regPC
                        cpu.nmiAsserted = true
                    }
                    3 -> {
                        // println("IRQ+NMI at pc ${hexW(cpu.regPC)}")
                        lastIRQpc = cpu.regPC
                        lastNMIpc = cpu.regPC
                        cpu.nmiAsserted = true
                        cpu.irqAsserted = true
                    }
                }
            }

            override fun clock() {}
            override fun reset() {}
        }
        val bus = Bus()
        val ram = Ram(0, 0xffff)
        val irqtrigger = Trigger(0xbffc, 0xbffc)
        ram.load("src/test/kotlin/6502_functional_tests/bin_files/6502_interrupt_test.bin", 0)
        bus.add(cpu)
        bus.add(irqtrigger)
        bus.add(ram)
        cpu.reset()
        cpu.regPC = 0x0400
        try {
            do {
                val previousPC = cpu.regPC
                cpu.step()
            } while(cpu.regPC!=previousPC)
        } catch(nx: NotImplementedError) {
            println("encountered a not yet implemented feature: ${nx.message}")
        }

        // the test is successful if address 0x06f5 is reached ("success" label in source code)
        if(cpu.regPC!=0x06f5) {
            println("Last IRQ triggered at ${hexW(irqtrigger.lastIRQpc)} last NMI at ${hexW(irqtrigger.lastNMIpc)}")
            println(cpu.snapshot())
            val d = cpu.disassemble(ram, max(0, cpu.regPC-20), min(65535, cpu.regPC+20))
            println(d.first.joinToString("\n"))
            fail("test failed")
        }
    }

    @Test
    fun testDecimal6502() {
        val cpu = Cpu6502()
        val bus = Bus()
        val ram = Ram(0, 0xffff)
        ram.load("src/test/kotlin/6502_functional_tests/bin_files/6502_decimal_test.bin", 0x0200)
        bus.add(cpu)
        bus.add(ram)
        cpu.reset()
        cpu.regPC = 0x0200
        cpu.breakpointForBRK = { _, address ->
            if(address==0x024b) {    // test end address
                val error=bus.read(0x000b)      // the 'ERROR' variable is stored here
                if(error==0.toShort())
                    throw SuccessfulTestResult()
            }
            Cpu6502.BreakpointResultAction(null, null)
        }
        try {
            while (cpu.totalCycles < 100000000) {
                cpu.clock()
            }
        } catch (sx: SuccessfulTestResult) {
            println("test successful  ${cpu.totalCycles}")
            return
        } catch(nx: NotImplementedError) {
            println("encountered a not yet implemented feature: ${nx.message}")
        }

        println(cpu.snapshot())
        val d = cpu.disassemble(ram, max(0, cpu.regPC-20), min(65535, cpu.regPC+20))
        println(d.first.joinToString ("\n"))
        fail("test failed")
    }

    @Test
    fun testDecimal65C02() {
        val cpu = Cpu65C02()
        val bus = Bus()
        val ram = Ram(0, 0xffff)
        ram.load("src/test/kotlin/6502_functional_tests/bin_files/65C02_decimal_test.bin", 0x0200)
        bus.add(cpu)
        bus.add(ram)
        cpu.reset()
        cpu.regPC = 0x0200
        cpu.breakpointForBRK = { _, address ->
            if(address==0x024b) {   // test end address
                val error=bus.read(0x000b)      // the 'ERROR' variable is stored here
                if(error==0.toShort())
                    throw SuccessfulTestResult()
            }
            Cpu6502.BreakpointResultAction(null, null)
        }
        try {
            while (cpu.totalCycles < 100000000) {
                cpu.clock()
            }
        } catch (sx: SuccessfulTestResult) {
            println("test successful  ${cpu.totalCycles}")
            return
        } catch(nx: NotImplementedError) {
            println("encountered a not yet implemented feature: ${nx.message}")
        }

        println(cpu.snapshot())
        val d = cpu.disassemble(ram, max(0, cpu.regPC-20), min(65535, cpu.regPC+20))
        println(d.first.joinToString ("\n"))
        fail("test failed")
    }
}
