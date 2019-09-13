import razorvine.ksim65.components.Cpu6502
import razorvine.ksim65.components.Cpu65C02
import razorvine.ksim65.components.Ram
import kotlin.test.*


class TestDisassembler {

    @Test
    fun testDisassembleAll6502Opcodes() {
        val cpu = Cpu6502()
        val memory = Ram(0, 0xffff)
        val binfile = javaClass.classLoader.getResourceAsStream("disassem_instr_test.prg")?.readAllBytes()!!
        memory.load(binfile, 0x1000-2)
        val result = cpu.disassemble(memory, 0x1000, 0x1221)
        assertEquals(256, result.size)
        assertEquals("\$1000  69 01       adc  #\$01", result[0])

        val reference = javaClass.classLoader.getResource("disassem_ref_output.txt")?.readText()!!.trim().lines()
        assertEquals(256, reference.size)
        for (line in result.zip(reference)) {
            if (line.first != line.second) {
                fail("disassembled instruction mismatch: '${line.first}', expected '${line.second}'")
            }
        }
    }

    @Test
    fun testDisassembleRockwell65C02() {
        val cpu = Cpu65C02()
        val memory = Ram(0, 0x1000)
        val source = javaClass.classLoader.getResource("disassem_r65c02.bin")!!
        memory.load(source, 0)
        val resultLines = cpu.disassemble(memory, 0x0000, 0x0050)
        val result = resultLines.joinToString("\n")
        assertEquals("""${'$'}0000  07 12       rmb0  ${'$'}12
${'$'}0002  17 12       rmb1  ${'$'}12
${'$'}0004  27 12       rmb2  ${'$'}12
${'$'}0006  37 12       rmb3  ${'$'}12
${'$'}0008  47 12       rmb4  ${'$'}12
${'$'}000a  57 12       rmb5  ${'$'}12
${'$'}000c  67 12       rmb6  ${'$'}12
${'$'}000e  77 12       rmb7  ${'$'}12
${'$'}0010  87 12       smb0  ${'$'}12
${'$'}0012  97 12       smb1  ${'$'}12
${'$'}0014  a7 12       smb2  ${'$'}12
${'$'}0016  b7 12       smb3  ${'$'}12
${'$'}0018  c7 12       smb4  ${'$'}12
${'$'}001a  d7 12       smb5  ${'$'}12
${'$'}001c  e7 12       smb6  ${'$'}12
${'$'}001e  f7 12       smb7  ${'$'}12
${'$'}0020  0f 12 2a    bbr0  ${'$'}12, ${'$'}4d
${'$'}0023  1f 12 27    bbr1  ${'$'}12, ${'$'}4d
${'$'}0026  2f 12 24    bbr2  ${'$'}12, ${'$'}4d
${'$'}0029  3f 12 21    bbr3  ${'$'}12, ${'$'}4d
${'$'}002c  4f 12 1e    bbr4  ${'$'}12, ${'$'}4d
${'$'}002f  5f 12 1b    bbr5  ${'$'}12, ${'$'}4d
${'$'}0032  6f 12 18    bbr6  ${'$'}12, ${'$'}4d
${'$'}0035  8f 12 15    bbs0  ${'$'}12, ${'$'}4d
${'$'}0038  9f 12 12    bbs1  ${'$'}12, ${'$'}4d
${'$'}003b  af 12 0f    bbs2  ${'$'}12, ${'$'}4d
${'$'}003e  bf 12 0c    bbs3  ${'$'}12, ${'$'}4d
${'$'}0041  cf 12 09    bbs4  ${'$'}12, ${'$'}4d
${'$'}0044  df 12 06    bbs5  ${'$'}12, ${'$'}4d
${'$'}0047  ef 12 03    bbs6  ${'$'}12, ${'$'}4d
${'$'}004a  ff 12 00    bbs7  ${'$'}12, ${'$'}4d
${'$'}004d  00          brk
${'$'}004e  00          brk
${'$'}004f  00          brk
${'$'}0050  00          brk""", result)
    }

    @Test
    fun testDisassembleWDC65C02() {
        val cpu = Cpu65C02()
        val memory = Ram(0, 0x1000)
        val source = javaClass.classLoader.getResource("disassem_wdc65c02.bin")!!
        memory.load(source, 0)
        val resultLines = cpu.disassemble(memory, 0x0000, 0x0015)
        val result = resultLines.joinToString("\n")
        assertEquals("""${'$'}0000  cb          wai
${'$'}0001  db          stp
${'$'}0002  3a          dec  a
${'$'}0003  1a          inc  a
${'$'}0004  64 12       stz  ${'$'}12
${'$'}0006  14 12       trb  ${'$'}12
${'$'}0008  04 12       tsb  ${'$'}12
${'$'}000a  72 12       adc  ${'$'}(12)
${'$'}000c  b2 12       lda  ${'$'}(12)
${'$'}000e  92 12       sta  ${'$'}(12)
${'$'}0010  7c 00 20    jmp  ${'$'}(2000,x)
${'$'}0013  00          brk
${'$'}0014  00          brk
${'$'}0015  00          brk""", result)
    }

}


