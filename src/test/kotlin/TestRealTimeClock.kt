import razorvine.ksim65.components.RealTimeClock
import kotlin.test.*
import java.time.LocalDateTime

class TestRealTimeClock {
    @Test
    fun testReadCurrentTime() {
        val rtc = RealTimeClock(0x1000)
        val now = LocalDateTime.now()
        
        val yearLsb = rtc[0].toInt()
        val yearMsb = rtc[1].toInt()
        val rtcYear = yearLsb or (yearMsb shl 8)
        assertEquals(now.year, rtcYear)
        assertEquals(now.monthValue.toUByte(), rtc[2])
        assertEquals(now.dayOfMonth.toUByte(), rtc[3])
        assertEquals(now.hour.toUByte(), rtc[4])
        assertEquals(now.minute.toUByte(), rtc[5])
        // second might have ticked over, so we don't strictly assert equality but it should be close
        val rtcSec = rtc[6].toInt()
        assertTrue(rtcSec == now.second || rtcSec == (now.second + 1) % 60)
    }

    @Test
    fun testSetTime() {
        val rtc = RealTimeClock(0x1000)
        
        // Set to 2000-01-01 12:00:00.500
        rtc[0] = 0xd0.toUByte() // 2000 LSB (2000 = 0x07d0)
        rtc[1] = 0x07.toUByte() // 2000 MSB
        rtc[2] = 1.toUByte()    // Jan
        rtc[3] = 1.toUByte()    // 1st
        rtc[4] = 12.toUByte()   // 12:xx
        rtc[5] = 0.toUByte()    // xx:00
        rtc[6] = 0.toUByte()    // xx:xx:00
        rtc[7] = 0xf4.toUByte() // 500 ms LSB (500 = 0x01f4)
        rtc[8] = 0x01.toUByte() // 500 ms MSB
        
        rtc[9] = 1.toUByte()    // TRIGGER
        
        assertEquals(2000, rtc[0].toInt() or (rtc[1].toInt() shl 8))
        assertEquals(1.toUByte(), rtc[2])
        assertEquals(1.toUByte(), rtc[3])
        assertEquals(12.toUByte(), rtc[4])
        assertEquals(0.toUByte(), rtc[5])
        // second might have passed, but it should still be 0 or 1
        assertTrue(rtc[6].toInt() in 0..1)
        
        val ms = rtc[7].toInt() or (rtc[8].toInt() shl 8)
        // allow for some execution time drift
        assertTrue(ms in 500..999)
    }
}
