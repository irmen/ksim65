package net.razorvine

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import razorvine.ksim65.Cpu65C02
import razorvine.ksim65.testing.CpuType
import razorvine.ksim65.testing.TestMachine
import java.io.File

class ProgramTests {
    @Test
    fun `test hello world program`() {
        val serial = CapturingSerialIO(printToStdout = false)
        val machine = TestMachine(CpuType.CPU65C02, serialAndPower = serial)

        // The testprogram is in ../testprogram/
        val programPath = System.getProperty("test.program.path") 
            ?: error("test.program.path system property must be set")
        val programFile = File(programPath)
        machine.loadFileInRam(programFile, null)

        // Execute until poweroff
        val maxSteps = 1_000_000
        assertThrows<CapturingSerialIO.PoweroffMachine> {
            repeat(maxSteps) { machine.step() }
        }

        serial.assertOutputEquals("hello, world!\n")
    }

    @Test
    fun `test STP powers off machine`() {
        val serial = CapturingSerialIO(printToStdout = false)
        val machine = TestMachine(CpuType.CPU65C02, serialAndPower = serial)
        (machine.cpu as Cpu65C02).powerOffOnStp = true

        // Load simple STP instruction: 0xdb is STP
        machine.loadInRam(arrayOf(0xdb.toUByte()), 0x1000)

        // STP is 1 byte, so it should power off immediately
        assertThrows<CapturingSerialIO.PoweroffMachine> {
            machine.step()
        }
    }
}
