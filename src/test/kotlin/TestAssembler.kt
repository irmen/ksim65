import razorvine.ksim65.*
import razorvine.ksim65.components.Ram
import kotlin.test.*


class TestAssembler {

    @Test
    fun testAssembleSingleInstruction() {
        val cpu = Cpu6502()
        val ram = Ram(0, 0xffff)
        val assembler = Assembler(cpu, ram)

        val result = assembler.assemble($$"$c000 jmp $ea31")
        assertTrue(result.success)
        assertEquals("", result.error)
        assertEquals(0xc000, result.startAddress)
        assertEquals(3, result.numBytes)
        assertEquals(0x4c, ram[0xc000])
        assertEquals(0x31, ram[0xc001])
        assertEquals(0xea, ram[0xc002])
    }

    @Test
    fun testAssembleMulti() {
        val cpu = Cpu6502()
        val ram = Ram(0, 0xffff)
        val assembler = Assembler(cpu, ram)
        val result = assembler.assemble($$"""
*=$a2b3 
  nop
  jmp $ea31
  bne *-2
""".lines())
        assertEquals("", result.error)
        assertTrue(result.success)
        assertEquals(0xa2b3, result.startAddress)
        assertEquals(6, result.numBytes)
    }

}


