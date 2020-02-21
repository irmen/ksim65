import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.test.*

// TODO: implement the still missing illegal instructions and replace these tests with the 'real' runTest

@Execution(ExecutionMode.CONCURRENT)
class Test6502TestSuiteIllegalInstructions: FunctionalTestsBase() {

    @Test
    fun testAncb() {
        runTest("ancb")
    }

    @Test
    fun testAsoa() {
        runTest("asoa")
    }

    @Test
    fun testAsoax() {
        runTest("asoax")
    }

    @Test
    fun testAsoay() {
        runTest("asoay")
    }

    @Test
    fun testAsoix() {
        runTest("asoix")
    }

    @Test
    fun testAsoiy() {
        runTest("asoiy")
    }

    @Test
    fun testAsoz() {
        runTest("asoz")
    }

    @Test
    fun testAsozx() {
        runTest("asozx")
    }

    @Test
    fun testAxsa() {
        runTest("axsa")
    }

    @Test
    fun testAxsix() {
        runTest("axsix")
    }

    @Test
    fun testAxsz() {
        runTest("axsz")
    }

    @Test
    fun testAxszy() {
        runTest("axszy")
    }

    @Test
    fun testDcma() {
        runTest("dcma")
    }

    @Test
    fun testDcmax() {
        runTest("dcmax")
    }

    @Test
    fun testDcmay() {
        runTest("dcmay")
    }

    @Test
    fun testDcmix() {
        runTest("dcmix")
    }

    @Test
    fun testDcmiy() {
        runTest("dcmiy")
    }

    @Test
    fun testDcmz() {
        runTest("dcmz")
    }

    @Test
    fun testDcmzx() {
        runTest("dcmzx")
    }

    @Test
    fun testInsa() {
        runTest("insa")
    }

    @Test
    fun testInsax() {
        runTest("insax")
    }

    @Test
    fun testInsay() {
        runTest("insay")
    }

    @Test
    fun testInsix() {
        runTest("insix")
    }

    @Test
    fun testInsiy() {
        runTest("insiy")
    }

    @Test
    fun testInsz() {
        runTest("insz")
    }

    @Test
    fun testInszx() {
        runTest("inszx")
    }

    @Test
    fun testLasay() {
        runTest("lasay")
    }

    @Test
    fun testLaxa() {
        runTest("laxa")
    }

    @Test
    fun testLaxay() {
        runTest("laxay")
    }

    @Test
    fun testLaxix() {
        runTest("laxix")
    }

    @Test
    fun testLaxiy() {
        runTest("laxiy")
    }

    @Test
    fun testLaxz() {
        runTest("laxz")
    }

    @Test
    fun testLaxzy() {
        runTest("laxzy")
    }

    @Test
    fun testLsea() {
        runTest("lsea")
    }

    @Test
    fun testLseax() {
        runTest("lseax")
    }

    @Test
    fun testLseay() {
        runTest("lseay")
    }

    @Test
    fun testLseix() {
        runTest("lseix")
    }

    @Test
    fun testLseiy() {
        runTest("lseiy")
    }

    @Test
    fun testLsez() {
        runTest("lsez")
    }

    @Test
    fun testLsezx() {
        runTest("lsezx")
    }

    @Test
    fun testRlaa() {
        runTest("rlaa")
    }

    @Test
    fun testRlaax() {
        runTest("rlaax")
    }

    @Test
    fun testRlaay() {
        runTest("rlaay")
    }

    @Test
    fun testRlaix() {
        runTest("rlaix")
    }

    @Test
    fun testRlaiy() {
        runTest("rlaiy")
    }

    @Test
    fun testRlaz() {
        runTest("rlaz")
    }

    @Test
    fun testRlazx() {
        runTest("rlazx")
    }

    @Test
    fun testRraa() {
        runTest("rraa")
    }

    @Test
    fun testRraax() {
        runTest("rraax")
    }

    @Test
    fun testRraay() {
        runTest("rraay")
    }

    @Test
    fun testRraix() {
        runTest("rraix")
    }

    @Test
    fun testRraiy() {
        runTest("rraiy")
    }

    @Test
    fun testRraz() {
        runTest("rraz")
    }

    @Test
    fun testRrazx() {
        runTest("rrazx")
    }

    @Test
    @Disabled("this illegal instruction is probablyt not implemented correctly yet")
    fun testAlrb() {
        runTest("alrb") // TODO fix?
    }

    @Test
    @Disabled("this illegal instruction is probablyt not implemented correctly yet")
    fun testAneb() {
        runTest("aneb") // TODO fix?
    }

    @Test
    @Disabled("this illegal instruction is probablyt not implemented correctly yet")
    fun testArrb() {
        runTest("arrb") // TODO fix?
    }

    @Test
    @Disabled("this illegal instruction is probablyt not implemented correctly yet")
    fun testLxab() {
        runTest("lxab")         // TODO fix something?
    }

    @Test
    @Disabled("this illegal instruction is probablyt not implemented correctly yet")
    fun testSbxb() {
        runTest("sbxb") // TODO fix?
    }

    @Test
    @Disabled("this illegal instruction is probablyt not implemented correctly yet")
    fun testShaay() {
        runTest("shaay")    // TODO fix?
    }

    @Test
    @Disabled("this illegal instruction is probablyt not implemented correctly yet")
    fun testShaiy() {
        runTest("shaiy")    // TODO fix?
    }

    @Test
    @Disabled("this illegal instruction is probablyt not implemented correctly yet")
    fun testShsay() {
        runTest("shsay")    // TODO fix?
    }

    @Test
    @Disabled("this illegal instruction is probablyt not implemented correctly yet")
    fun testShxay() {
        runTest("shxay")    // TODO fix?
    }

    @Test
    @Disabled("this illegal instruction is probablyt not implemented correctly yet")
    fun testShyax() {
        runTest("shyax")    // TODO fix?
    }
}
