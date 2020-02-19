import razorvine.c64emu.CpuIoPort
import kotlin.test.*


class TestC64Components {

    @Test
    fun testCpuIoPort() {
        val io = CpuIoPort()
        io.reset()

        assertEquals(0xef, io[0])
        assertEquals(0x37, io[1])

        io[0] = 0b11111111
        io[1] = 0b11111111
        assertEquals(0b11111111,io[0])
        assertEquals(0b11111111,io[1])
        io[0] = 0
        assertEquals(0b11011111,io[1], "bit 5 (cassette motor) is drawn low if input")
        io[0] = 0b11111111
        assertEquals(0b11111111,io[1], "bit 5 (cassette motor) is high if output")

        io[1] = 0x37
        io[0] = 0b00100000
        assertEquals(0b00100000, io[0])
        assertEquals(0x37, io[1])

        io[0] = 0xff
        assertEquals(0xff, io[0])
        assertEquals(0x37, io[1])

        io[1] = 0b11110000
        assertEquals(0b11110000, io[1])

        io[0] = 0b00100000
        io[1] = 0b01010101
        assertEquals(0b11010000, io[1])
        io[0] = 0b00100001
        assertEquals(0b11010000, io[1])
        io[1] = 0b01010101
        assertEquals(0b11010001, io[1])

        io[0] = 0b11000011
        assertEquals(0b11010001, io[1])
        io[1] = 0b00100000
        assertEquals(0b00010000, io[1])
        io[1] = 0b11111111
        assertEquals(0b11010011, io[1])
    }

    @Test
    fun testRoms() {
        val io = CpuIoPort()
        io.reset()

        assertTrue(io.loram)
        assertTrue(io.hiram)
        assertTrue(io.charen)

        io[0] = 0
        io[1] = 0
        assertTrue(io.loram)
        assertTrue(io.hiram)
        assertTrue(io.charen)

        io[0] = 255
        assertTrue(io.loram)
        assertTrue(io.hiram)
        assertTrue(io.charen)
        io[1] = 0b1111110
        assertFalse(io.loram)
        assertTrue(io.hiram)
        assertTrue(io.charen)
        io[1] = 0b1111101
        assertTrue(io.loram)
        assertFalse(io.hiram)
        assertTrue(io.charen)
        io[1] = 0b1111011
        assertTrue(io.loram)
        assertTrue(io.hiram)
        assertFalse(io.charen)

    }

}


