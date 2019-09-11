import razorvine.ksim65.components.Cpu6502
import razorvine.ksim65.components.Ram
import java.io.File
import kotlin.test.*


class TestDisassembler {

    @Test
    fun testDisassembleAllOpcodes() {
        val cpu = Cpu6502(true)
        val memory = Ram(0, 0xffff)
        memory.load("src/test/kotlin/testfiles/disassem_instr_test.prg", 0x1000 - 2)
        val result = cpu.disassemble(memory, 0x1000, 0x1221)
        assertEquals(256, result.size)
        assertEquals("\$1000  69 01       adc  #\$01", result[0])

        val reference = File("src/test/kotlin/testfiles/disassem_ref_output.txt").readLines()
        assertEquals(256, reference.size)
        for (line in result.zip(reference)) {
            if (line.first != line.second) {
                fail("disassembled instruction mismatch: '${line.first}', expected '${line.second}'")
            }
        }
    }

}


