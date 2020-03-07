import razorvine.ksim65.Cpu6502
import razorvine.ksim65.Cpu65C02
import razorvine.ksim65.Disassembler
import razorvine.ksim65.components.Ram
import kotlin.test.*


class TestDisassembler {

    @Test
    fun testDisassembleAll6502Opcodes() {
        val cpu = Cpu6502()
        val disassembler = Disassembler(cpu)
        val memory = Ram(0, 0xffff)
        val binfile = javaClass.classLoader.getResourceAsStream("disassem_instr_test.prg")?.readBytes()!!
        memory.load(binfile, 0x1000-2)
        val result = disassembler.disassemble(memory.data, 0x1000..0x1221, 0)
        assertEquals(256, result.first.size)
        assertEquals(0x1222, result.second)
        assertEquals("\$1000  69 01       adc  #\$01", result.first[0])

        val reference = javaClass.classLoader.getResource("disassem_ref_output.txt")?.readText()!!.trim().lines()
        assertEquals(256, reference.size)
        for (line in result.first.zip(reference)) {
            if (line.first != line.second) {
                fail("disassembled instruction mismatch: '${line.first}', expected '${line.second}'")
            }
        }
    }

    @Test
    fun testDisassembleRockwell65C02() {
        val cpu = Cpu65C02()
        val disassembler = Disassembler(cpu)
        val memory = Ram(0, 0x0fff)
        val source = javaClass.classLoader.getResource("disassem_r65c02.bin")?.readBytes()!!
        memory.load(source, 0x0200)
        val disassem = disassembler.disassemble(memory.data, 0x0200..0x0250, 0)
        assertEquals(0x251, disassem.second)
        val result = disassem.first.joinToString("\n")
        assertEquals("""${'$'}0200  07 12       rmb0  ${'$'}12
${'$'}0202  17 12       rmb1  ${'$'}12
${'$'}0204  27 12       rmb2  ${'$'}12
${'$'}0206  37 12       rmb3  ${'$'}12
${'$'}0208  47 12       rmb4  ${'$'}12
${'$'}020a  57 12       rmb5  ${'$'}12
${'$'}020c  67 12       rmb6  ${'$'}12
${'$'}020e  77 12       rmb7  ${'$'}12
${'$'}0210  87 12       smb0  ${'$'}12
${'$'}0212  97 12       smb1  ${'$'}12
${'$'}0214  a7 12       smb2  ${'$'}12
${'$'}0216  b7 12       smb3  ${'$'}12
${'$'}0218  c7 12       smb4  ${'$'}12
${'$'}021a  d7 12       smb5  ${'$'}12
${'$'}021c  e7 12       smb6  ${'$'}12
${'$'}021e  f7 12       smb7  ${'$'}12
${'$'}0220  0f 12 2a    bbr0  ${'$'}12, ${'$'}024d
${'$'}0223  1f 12 27    bbr1  ${'$'}12, ${'$'}024d
${'$'}0226  2f 12 24    bbr2  ${'$'}12, ${'$'}024d
${'$'}0229  3f 12 21    bbr3  ${'$'}12, ${'$'}024d
${'$'}022c  4f 12 1e    bbr4  ${'$'}12, ${'$'}024d
${'$'}022f  5f 12 1b    bbr5  ${'$'}12, ${'$'}024d
${'$'}0232  6f 12 18    bbr6  ${'$'}12, ${'$'}024d
${'$'}0235  8f 12 15    bbs0  ${'$'}12, ${'$'}024d
${'$'}0238  9f 12 12    bbs1  ${'$'}12, ${'$'}024d
${'$'}023b  af 12 0f    bbs2  ${'$'}12, ${'$'}024d
${'$'}023e  bf 12 0c    bbs3  ${'$'}12, ${'$'}024d
${'$'}0241  cf 12 09    bbs4  ${'$'}12, ${'$'}024d
${'$'}0244  df 12 06    bbs5  ${'$'}12, ${'$'}024d
${'$'}0247  ef 12 03    bbs6  ${'$'}12, ${'$'}024d
${'$'}024a  ff 12 00    bbs7  ${'$'}12, ${'$'}024d
${'$'}024d  00          brk
${'$'}024e  00          brk
${'$'}024f  00          brk
${'$'}0250  00          brk""", result)
    }

    @Test
    fun testDisassembleWDC65C02() {
        val cpu = Cpu65C02()
        val disassembler = Disassembler(cpu)
        val memory = Ram(0, 0x0fff)
        val source = javaClass.classLoader.getResource("disassem_wdc65c02.bin")?.readBytes()!!
        memory.load(source, 0x200)
        val disassem = disassembler.disassemble(memory.data, 0x0200..0x0215, 0)
        assertEquals(0x216, disassem.second)
        val result = disassem.first.joinToString("\n")
        assertEquals("""${'$'}0200  cb          wai
${'$'}0201  db          stp
${'$'}0202  3a          dec  a
${'$'}0203  1a          inc  a
${'$'}0204  64 12       stz  ${'$'}12
${'$'}0206  14 12       trb  ${'$'}12
${'$'}0208  04 12       tsb  ${'$'}12
${'$'}020a  72 12       adc  ${'$'}(12)
${'$'}020c  b2 12       lda  ${'$'}(12)
${'$'}020e  92 12       sta  ${'$'}(12)
${'$'}0210  7c 00 20    jmp  ${'$'}(2000,x)
${'$'}0213  00          brk
${'$'}0214  00          brk
${'$'}0215  00          brk""", result)
    }

}


