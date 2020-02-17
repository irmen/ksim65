import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.test.*


@Execution(ExecutionMode.CONCURRENT)
@Disabled("this test suite is quite intensive and for regular test runs, the other tests are sufficient")
class Test6502TestSuitePart2: FunctionalTestsBase() {

    @Test
    fun testLdaa() {
        runTest("ldaa")
    }

    @Test
    fun testLdaax() {
        runTest("ldaax")
    }

    @Test
    fun testLdaay() {
        runTest("ldaay")
    }

    @Test
    fun testLdab() {
        runTest("ldab")
    }

    @Test
    fun testLdaix() {
        runTest("ldaix")
    }

    @Test
    fun testLdaiy() {
        runTest("ldaiy")
    }

    @Test
    fun testLdaz() {
        runTest("ldaz")
    }

    @Test
    fun testLdazx() {
        runTest("ldazx")
    }

    @Test
    fun testLdxa() {
        runTest("ldxa")
    }

    @Test
    fun testLdxay() {
        runTest("ldxay")
    }

    @Test
    fun testLdxb() {
        runTest("ldxb")
    }

    @Test
    fun testLdxz() {
        runTest("ldxz")
    }

    @Test
    fun testLdxzy() {
        runTest("ldxzy")
    }

    @Test
    fun testLdya() {
        runTest("ldya")
    }

    @Test
    fun testLdyax() {
        runTest("ldyax")
    }

    @Test
    fun testLdyb() {
        runTest("ldyb")
    }

    @Test
    fun testLdyz() {
        runTest("ldyz")
    }

    @Test
    fun testLdyzx() {
        runTest("ldyzx")
    }

    @Test
    fun testLsra() {
        runTest("lsra")
    }

    @Test
    fun testLsrax() {
        runTest("lsrax")
    }

    @Test
    fun testLsrn() {
        runTest("lsrn")
    }

    @Test
    fun testLsrz() {
        runTest("lsrz")
    }

    @Test
    fun testLsrzx() {
        runTest("lsrzx")
    }

    @Test
    fun testNopa() {
        runTest("nopa")
    }

    @Test
    fun testNopax() {
        runTest("nopax")
    }

    @Test
    fun testNopb() {
        runTest("nopb")
    }

    @Test
    fun testNopn() {
        runTest("nopn")
    }

    @Test
    fun testNopz() {
        runTest("nopz")
    }

    @Test
    fun testNopzx() {
        runTest("nopzx")
    }

    @Test
    fun testOraa() {
        runTest("oraa")
    }

    @Test
    fun testOraax() {
        runTest("oraax")
    }

    @Test
    fun testOraay() {
        runTest("oraay")
    }

    @Test
    fun testOrab() {
        runTest("orab")
    }

    @Test
    fun testOraix() {
        runTest("oraix")
    }

    @Test
    fun testOraiy() {
        runTest("oraiy")
    }

    @Test
    fun testOraz() {
        runTest("oraz")
    }

    @Test
    fun testOrazx() {
        runTest("orazx")
    }

    @Test
    fun testPhan() {
        runTest("phan")
    }

    @Test
    fun testPhpn() {
        runTest("phpn")
    }

    @Test
    fun testPlan() {
        runTest("plan")
    }

    @Test
    fun testPlpn() {
        runTest("plpn")
    }

    @Test
    fun testRola() {
        runTest("rola")
    }

    @Test
    fun testRolax() {
        runTest("rolax")
    }

    @Test
    fun testRoln() {
        runTest("roln")
    }

    @Test
    fun testRolz() {
        runTest("rolz")
    }

    @Test
    fun testRolzx() {
        runTest("rolzx")
    }

    @Test
    fun testRora() {
        runTest("rora")
    }

    @Test
    fun testRorax() {
        runTest("rorax")
    }

    @Test
    fun testRorn() {
        runTest("rorn")
    }

    @Test
    fun testRorz() {
        runTest("rorz")
    }

    @Test
    fun testRorzx() {
        runTest("rorzx")
    }

    @Test
    fun testRtin() {
        runTest("rtin")
    }

    @Test
    fun testRtsn() {
        runTest("rtsn")
    }

    @Test
    fun testSbca() {
        runTest("sbca")
    }

    @Test
    fun testSbcax() {
        runTest("sbcax")
    }

    @Test
    fun testSbcay() {
        runTest("sbcay")
    }

    @Test
    fun testSbcb() {
        runTest("sbcb")
    }

    @Test
    fun testSbcb_eb() {
        runTest("sbcb(eb)")
    }

    @Test
    fun testSbcix() {
        runTest("sbcix")
    }

    @Test
    fun testSbciy() {
        runTest("sbciy")
    }

    @Test
    fun testSbcz() {
        runTest("sbcz")
    }

    @Test
    fun testSbczx() {
        runTest("sbczx")
    }

    @Test
    fun testSecn() {
        runTest("secn")
    }

    @Test
    fun testSedn() {
        runTest("sedn")
    }

    @Test
    fun testSein() {
        runTest("sein")
    }

    @Test
    fun testStaa() {
        runTest("staa")
    }

    @Test
    fun testStaax() {
        runTest("staax")
    }

    @Test
    fun testStaay() {
        runTest("staay")
    }

    @Test
    fun testStaix() {
        runTest("staix")
    }

    @Test
    fun testStaiy() {
        runTest("staiy")
    }

    @Test
    fun testStaz() {
        runTest("staz")
    }

    @Test
    fun testStazx() {
        runTest("stazx")
    }

    @Test
    fun testStxa() {
        runTest("stxa")
    }

    @Test
    fun testStxz() {
        runTest("stxz")
    }

    @Test
    fun testStxzy() {
        runTest("stxzy")
    }

    @Test
    fun testStya() {
        runTest("stya")
    }

    @Test
    fun testStyz() {
        runTest("styz")
    }

    @Test
    fun testStyzx() {
        runTest("styzx")
    }

    @Test
    fun testTaxn() {
        runTest("taxn")
    }

    @Test
    fun testTayn() {
        runTest("tayn")
    }

    @Test
    fun testTsxn() {
        runTest("tsxn")
    }

    @Test
    fun testTxan() {
        runTest("txan")
    }

    @Test
    fun testTxsn() {
        runTest("txsn")
    }

    @Test
    fun testTyan() {
        runTest("tyan")
    }

}
