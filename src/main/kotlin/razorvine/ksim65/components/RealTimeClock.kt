package razorvine.ksim65.components

import java.time.LocalDate
import java.time.LocalTime


/**
 * A real-time time of day clock. Takes 9 I/O registers.
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
 */
class RealTimeClock(startAddress: Address) : MemMappedComponent(startAddress, startAddress+8) {

    override fun clock() {
        /* not updated on clock pulse */
    }

    override fun reset() {
        /* never reset */
    }

    override operator fun get(offset: Int): UByte {
        return when (offset) {
            0x00 -> {
                val year = LocalDate.now().year
                (year and 255).toUByte()
            }
            0x01 -> {
                val year = LocalDate.now().year
                (year ushr 8).toUByte()
            }
            0x02 -> LocalDate.now().monthValue.toUByte()
            0x03 -> LocalDate.now().dayOfMonth.toUByte()
            0x04 -> LocalTime.now().hour.toUByte()
            0x05 -> LocalTime.now().minute.toUByte()
            0x06 -> LocalTime.now().second.toUByte()
            0x07 -> {
                val ms = LocalTime.now().nano/1000000
                (ms and 255).toUByte()
            }
            0x08 -> {
                val ms = LocalTime.now().nano/1000000
                (ms ushr 8).toUByte()
            }
            else -> 0xff.toUByte()
        }
    }

    override operator fun set(offset: Int, data: UByte) { /* real time clock can't be set */ }
}
