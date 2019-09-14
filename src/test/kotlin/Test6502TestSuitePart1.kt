import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.TestInstance
import kotlin.test.*


@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@Disabled("this test suite takes a long time to complete")
class Test6502TestSuitePart1: FunctionalTestsBase() {

    @Test
    fun test0start() {
        runTest("0start")
    }

    @Test
    fun testAdca() {
        runTest("adca")
    }

    @Test
    fun testAdcax() {
        runTest("adcax")
    }

    @Test
    fun testAdcay() {
        runTest("adcay")
    }

    @Test
    fun testAdcb() {
        runTest("adcb")
    }

    @Test
    fun testAdcix() {
        runTest("adcix")
    }

    @Test
    fun testAdciy() {
        runTest("adciy")
    }

    @Test
    fun testAdcz() {
        runTest("adcz")
    }

    @Test
    fun testAdczx() {
        runTest("adczx")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testAlrb() {
        runTest("alrb")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testAncb() {
        runTest("ancb")
    }

    @Test
    fun testAnda() {
        runTest("anda")
    }

    @Test
    fun testAndax() {
        runTest("andax")
    }

    @Test
    fun testAnday() {
        runTest("anday")
    }

    @Test
    fun testAndb() {
        runTest("andb")
    }

    @Test
    fun testAndix() {
        runTest("andix")
    }

    @Test
    fun testAndiy() {
        runTest("andiy")
    }

    @Test
    fun testAndz() {
        runTest("andz")
    }

    @Test
    fun testAndzx() {
        runTest("andzx")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testAneb() {
        runTest("aneb")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testArrb() {
        runTest("arrb")
    }

    @Test
    fun testAsla() {
        runTest("asla")
    }

    @Test
    fun testAslax() {
        runTest("aslax")
    }

    @Test
    fun testAsln() {
        runTest("asln")
    }

    @Test
    fun testAslz() {
        runTest("aslz")
    }

    @Test
    fun testAslzx() {
        runTest("aslzx")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testAsoa() {
        runTest("asoa")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testAsoax() {
        runTest("asoax")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testAsoay() {
        runTest("asoay")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testAsoix() {
        runTest("asoix")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testAsoiy() {
        runTest("asoiy")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testAsoz() {
        runTest("asoz")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testAsozx() {
        runTest("asozx")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testAxsa() {
        runTest("axsa")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testAxsix() {
        runTest("axsix")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testAxsz() {
        runTest("axsz")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testAxszy() {
        runTest("axszy")
    }

    @Test
    fun testBccr() {
        runTest("bccr")
    }

    @Test
    fun testBcsr() {
        runTest("bcsr")
    }

    @Test
    fun testBeqr() {
        runTest("beqr")
    }

    @Test
    fun testBita() {
        runTest("bita")
    }

    @Test
    fun testBitz() {
        runTest("bitz")
    }

    @Test
    fun testBmir() {
        runTest("bmir")
    }

    @Test
    fun testBner() {
        runTest("bner")
    }

    @Test
    fun testBplr() {
        runTest("bplr")
    }

    @Test
    fun testBranchwrap() {
        runTest("branchwrap")
    }

    @Test
    fun testBrkn() {
        runTest("brkn")
    }

    @Test
    fun testBvcr() {
        runTest("bvcr")
    }

    @Test
    fun testBvsr() {
        runTest("bvsr")
    }

    @Test
    @Disabled("c64 specific component")
    fun testCia1pb6() {
        runTest("cia1pb6")
    }

    @Test
    @Disabled("c64 specific component")
    fun testCia1pb7() {
        runTest("cia1pb7")
    }

    @Test
    @Disabled("c64 specific component")
    fun testCia1ta() {
        runTest("cia1ta")
    }

    @Test
    @Disabled("c64 specific component")
    fun testCia1tab() {
        runTest("cia1tab")
    }

    @Test
    @Disabled("c64 specific component")
    fun testCia1tb() {
        runTest("cia1tb")
    }

    @Test
    @Disabled("c64 specific component")
    fun testCia1tb123() {
        runTest("cia1tb123")
    }

    @Test
    @Disabled("c64 specific component")
    fun testCia2pb6() {
        runTest("cia2pb6")
    }

    @Test
    @Disabled("c64 specific component")
    fun testCia2pb7() {
        runTest("cia2pb7")
    }

    @Test
    @Disabled("c64 specific component")
    fun testCia2ta() {
        runTest("cia2ta")
    }

    @Test
    @Disabled("c64 specific component")
    fun testCia2tb() {
        runTest("cia2tb")
    }

    @Test
    @Disabled("c64 specific component")
    fun testCia2tb123() {
        runTest("cia2tb123")
    }

    @Test
    fun testClcn() {
        runTest("clcn")
    }

    @Test
    fun testCldn() {
        runTest("cldn")
    }

    @Test
    fun testClin() {
        runTest("clin")
    }

    @Test
    fun testClvn() {
        runTest("clvn")
    }

    @Test
    fun testCmpa() {
        runTest("cmpa")
    }

    @Test
    fun testCmpax() {
        runTest("cmpax")
    }

    @Test
    fun testCmpay() {
        runTest("cmpay")
    }

    @Test
    fun testCmpb() {
        runTest("cmpb")
    }

    @Test
    fun testCmpix() {
        runTest("cmpix")
    }

    @Test
    fun testCmpiy() {
        runTest("cmpiy")
    }

    @Test
    fun testCmpz() {
        runTest("cmpz")
    }

    @Test
    fun testCmpzx() {
        runTest("cmpzx")
    }

    @Test
    @Disabled("c64 6510 specific component")
    fun testCntdef() {
        runTest("cntdef")
    }

    @Test
    @Disabled("c64 6510 specific component")
    fun testCnto2() {
        runTest("cnto2")
    }

    @Test
    @Disabled("c64 6510 specific component")
    fun testCpuport() {
        runTest("cpuport")
    }

    @Test
    @Disabled("todo: get all cycle times right, and uses c64 specific timing hardware")
    fun testCputiming() {
        runTest("cputiming")
    }

    @Test
    fun testCpxa() {
        runTest("cpxa")
    }

    @Test
    fun testCpxb() {
        runTest("cpxb")
    }

    @Test
    fun testCpxz() {
        runTest("cpxz")
    }

    @Test
    fun testCpya() {
        runTest("cpya")
    }

    @Test
    fun testCpyb() {
        runTest("cpyb")
    }

    @Test
    fun testCpyz() {
        runTest("cpyz")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testDcma() {
        runTest("dcma")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testDcmax() {
        runTest("dcmax")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testDcmay() {
        runTest("dcmay")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testDcmix() {
        runTest("dcmix")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testDcmiy() {
        runTest("dcmiy")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testDcmz() {
        runTest("dcmz")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testDcmzx() {
        runTest("dcmzx")
    }

    @Test
    fun testDeca() {
        runTest("deca")
    }

    @Test
    fun testDecax() {
        runTest("decax")
    }

    @Test
    fun testDecz() {
        runTest("decz")
    }

    @Test
    fun testDeczx() {
        runTest("deczx")
    }

    @Test
    fun testDexn() {
        runTest("dexn")
    }

    @Test
    fun testDeyn() {
        runTest("deyn")
    }

    @Test
    fun testEora() {
        runTest("eora")
    }

    @Test
    fun testEorax() {
        runTest("eorax")
    }

    @Test
    fun testEoray() {
        runTest("eoray")
    }

    @Test
    fun testEorb() {
        runTest("eorb")
    }

    @Test
    fun testEorix() {
        runTest("eorix")
    }

    @Test
    fun testEoriy() {
        runTest("eoriy")
    }

    @Test
    fun testEorz() {
        runTest("eorz")
    }

    @Test
    fun testEorzx() {
        runTest("eorzx")
    }

    @Test
    @Disabled("c64 specific component")
    fun testFlipos() {
        runTest("flipos")
    }

    @Test
    @Disabled("c64 specific component")
    fun testIcr01() {
        runTest("icr01")
    }

    @Test
    @Disabled("c64 specific component")
    fun testImr() {
        runTest("imr")
    }

    @Test
    fun testInca() {
        runTest("inca")
    }

    @Test
    fun testIncax() {
        runTest("incax")
    }

    @Test
    fun testIncz() {
        runTest("incz")
    }

    @Test
    fun testInczx() {
        runTest("inczx")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testInsa() {
        runTest("insa")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testInsax() {
        runTest("insax")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testInsay() {
        runTest("insay")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testInsix() {
        runTest("insix")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testInsiy() {
        runTest("insiy")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testInsz() {
        runTest("insz")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testInszx() {
        runTest("inszx")
    }

    @Test
    fun testInxn() {
        runTest("inxn")
    }

    @Test
    fun testInyn() {
        runTest("inyn")
    }

    @Test
    @Disabled("c64 specific component")
    fun testIrq() {
        runTest("irq")
    }

    @Test
    fun testJmpi() {
        runTest("jmpi")
    }

    @Test
    fun testJmpw() {
        runTest("jmpw")
    }

    @Test
    fun testJsrw() {
        runTest("jsrw")
    }
}
