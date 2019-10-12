import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.test.*


@Execution(ExecutionMode.CONCURRENT)
@Disabled("this test suite is quite intensive and for regular test runs, the other tests are sufficient")
class Test6502TestSuitePart2: FunctionalTestsBase() {

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testLasay() {
        runTest("lasay")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testLaxa() {
        runTest("laxa")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testLaxay() {
        runTest("laxay")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testLaxix() {
        runTest("laxix")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testLaxiy() {
        runTest("laxiy")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testLaxz() {
        runTest("laxz")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testLaxzy() {
        runTest("laxzy")
    }

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
    @Disabled("c64 specific component")
    fun testLoadth() {
        runTest("loadth")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testLsea() {
        runTest("lsea")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testLseax() {
        runTest("lseax")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testLseay() {
        runTest("lseay")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testLseix() {
        runTest("lseix")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testLseiy() {
        runTest("lseiy")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testLsez() {
        runTest("lsez")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testLsezx() {
        runTest("lsezx")
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
    @Disabled("not yet implemented- illegal instruction")
    fun testLxab() {
        runTest("lxab")
    }

    @Test
    @Disabled("c64 6510 specific component")
    fun testMmu() {
        runTest("mmu")
    }

    @Test
    @Disabled("c64 6510 specific component")
    fun testMmufetch() {
        runTest("mmufetch")
    }

    @Test
    @Disabled("c64 specific component")
    fun testNmi() {
        runTest("nmi")
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
    @Disabled("c64 specific component")
    fun testOneshot() {
        runTest("oneshot")
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
    @Disabled("not yet implemented- illegal instruction")
    fun testRlaa() {
        runTest("rlaa")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testRlaax() {
        runTest("rlaax")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testRlaay() {
        runTest("rlaay")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testRlaix() {
        runTest("rlaix")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testRlaiy() {
        runTest("rlaiy")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testRlaz() {
        runTest("rlaz")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testRlazx() {
        runTest("rlazx")
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
    @Disabled("not yet implemented- illegal instruction")
    fun testRraa() {
        runTest("rraa")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testRraax() {
        runTest("rraax")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testRraay() {
        runTest("rraay")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testRraix() {
        runTest("rraix")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testRraiy() {
        runTest("rraiy")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testRraz() {
        runTest("rraz")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testRrazx() {
        runTest("rrazx")
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
    @Disabled("not yet implemented- illegal instruction")
    fun testSbxb() {
        runTest("sbxb")
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
    @Disabled("not yet implemented- illegal instruction sha/ahx")
    fun testShaay() {
        runTest("shaay")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction sha/ahx")
    fun testShaiy() {
        runTest("shaiy")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction shs/tas")
    fun testShsay() {
        runTest("shsay")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testShxay() {
        runTest("shxay")
    }

    @Test
    @Disabled("not yet implemented- illegal instruction")
    fun testShyax() {
        runTest("shyax")
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
