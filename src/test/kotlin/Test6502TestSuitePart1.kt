import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.test.*

@Execution(ExecutionMode.CONCURRENT)
@Disabled("this test suite is quite intensive and for regular test runs, the other tests are sufficient")
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
    fun testInxn() {
        runTest("inxn")
    }

    @Test
    fun testInyn() {
        runTest("inyn")
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
