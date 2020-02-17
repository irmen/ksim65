import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.test.*

// TODO: run these tests by using the C64 machine emulation components

@Execution(ExecutionMode.CONCURRENT)
@Disabled("test code is not using C64 specific components yet")
class Test6502TestSuiteC64Specific: FunctionalTestsBase() {

    @Test
    fun testCia1pb6() {
        runTest("cia1pb6")
    }

    @Test
    fun testCia1pb7() {
        runTest("cia1pb7")
    }

    @Test
    fun testCia1ta() {
        runTest("cia1ta")
    }

    @Test
    fun testCia1tab() {
        runTest("cia1tab")
    }

    @Test
    fun testCia1tb() {
        runTest("cia1tb")
    }

    @Test
    fun testCia1tb123() {
        runTest("cia1tb123")
    }

    @Test
    fun testCia2pb6() {
        runTest("cia2pb6")
    }

    @Test
    fun testCia2pb7() {
        runTest("cia2pb7")
    }

    @Test
    fun testCia2ta() {
        runTest("cia2ta")
    }

    @Test
    fun testCia2tb() {
        runTest("cia2tb")
    }

    @Test
    fun testCia2tb123() {
        runTest("cia2tb123")
    }

    @Test
    fun testCntdef() {
        runTest("cntdef")
    }

    @Test
    fun testCnto2() {
        runTest("cnto2")
    }

    @Test
    fun testCpuport() {
        runTest("cpuport")
    }

    @Test
    fun testCputiming() {
        runTest("cputiming")
    }

    @Test
    fun testFlipos() {
        runTest("flipos")
    }

    @Test
    fun testIcr01() {
        runTest("icr01")
    }

    @Test
    fun testImr() {
        runTest("imr")
    }

    @Test
    fun testIrq() {
        runTest("irq")
    }

    @Test
    fun testLoadth() {
        runTest("loadth")
    }

    @Test
    fun testMmu() {
        runTest("mmu")
    }

    @Test
    fun testMmufetch() {
        runTest("mmufetch")
    }

    @Test
    fun testNmi() {
        runTest("nmi")
    }

    @Test
    fun testOneshot() {
        runTest("oneshot")
    }
}
