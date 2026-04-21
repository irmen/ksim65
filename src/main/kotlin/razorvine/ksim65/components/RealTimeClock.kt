package razorvine.ksim65.components

import java.time.Duration
import java.time.LocalDateTime


/**
 * A real-time time of day clock. Takes 10 I/O registers.
 * Returns hosts's localdate/time by default but can be set to a custom timestamp as well.
 * (System timers are elsewhere)
 *
 * reg.   value
 * ----   ----------
 *  00    year (lsb)
 *  01    year (msb)
 *  02    month, 1-12
 *  03    day, 1-31
 *  04    hour, 0-23
 *  05    minute, 0-59
 *  06    second, 0-59
 *  07    millisecond, 0-999 (lsb)
 *  08    millisecond, 0-999 (msb)
 *  09    set trigger: writing any value here latches the values written to 00-08
 *        as the new clock time and starts counting from that.
 */
class RealTimeClock(startAddress: Address) : MemMappedComponent(startAddress, startAddress+9) {

    private var timeOffset: Duration = Duration.ZERO
    private var latchedYearLsb = 0
    private var latchedYearMsb = 0
    private var latchedMonth = 1
    private var latchedDay = 1
    private var latchedHour = 0
    private var latchedMinute = 0
    private var latchedSecond = 0
    private var latchedMsLsb = 0
    private var latchedMsMsb = 0

    override fun clock() {
        /* not updated on clock pulse */
    }

    override fun reset() {
        /* never reset */
    }

    override operator fun get(offset: Int): UByte {
        val now = LocalDateTime.now().plus(timeOffset)
        return when (offset) {
            0x00 -> (now.year and 255).toUByte()
            0x01 -> (now.year ushr 8).toUByte()
            0x02 -> now.monthValue.toUByte()
            0x03 -> now.dayOfMonth.toUByte()
            0x04 -> now.hour.toUByte()
            0x05 -> now.minute.toUByte()
            0x06 -> now.second.toUByte()
            0x07 -> {
                val ms = now.nano/1000000
                (ms and 255).toUByte()
            }
            0x08 -> {
                val ms = now.nano/1000000
                (ms ushr 8).toUByte()
            }
            else -> 0xff.toUByte()
        }
    }

    override operator fun set(offset: Int, data: UByte) {
        when (offset) {
            0x00 -> latchedYearLsb = data.toInt()
            0x01 -> latchedYearMsb = data.toInt()
            0x02 -> latchedMonth = data.toInt()
            0x03 -> latchedDay = data.toInt()
            0x04 -> latchedHour = data.toInt()
            0x05 -> latchedMinute = data.toInt()
            0x06 -> latchedSecond = data.toInt()
            0x07 -> latchedMsLsb = data.toInt()
            0x08 -> latchedMsMsb = data.toInt()
            0x09 -> {
                val year = latchedYearLsb or (latchedYearMsb shl 8)
                val ms = latchedMsLsb or (latchedMsMsb shl 8)
                try {
                    val newTime = LocalDateTime.of(
                        year, latchedMonth, latchedDay,
                        latchedHour, latchedMinute, latchedSecond, ms * 1000000
                    )
                    timeOffset = Duration.between(LocalDateTime.now(), newTime)
                } catch (e: Exception) {
                    // ignore invalid time settings
                }
            }
        }
    }
}
