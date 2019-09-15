package razorvine.ksim65.components

import java.time.LocalDate
import java.time.LocalTime


/**
 * A real-time time of day clock.
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
class RealTimeClock(startAddress: Address, endAddress: Address) : MemMappedComponent(startAddress, endAddress) {

    init {
        require(endAddress - startAddress + 1 == 9) { "rtc needs exactly 9 memory bytes" }
    }

    override fun clock() {
        /* not updated on clock pulse */
    }

    override fun reset() {
        /* never reset */
    }

    override operator fun get(address: Address): UByte {
        return when (address - startAddress) {
            0x00 -> {
                val year = LocalDate.now().year
                (year and 255).toShort()
            }
            0x01 -> {
                val year = LocalDate.now().year
                (year ushr 8).toShort()
            }
            0x02 -> LocalDate.now().monthValue.toShort()
            0x03 -> LocalDate.now().dayOfMonth.toShort()
            0x04 -> LocalTime.now().hour.toShort()
            0x05 -> LocalTime.now().minute.toShort()
            0x06 -> LocalTime.now().second.toShort()
            0x07 -> {
                val ms = LocalTime.now().nano / 1000
                (ms and 255).toShort()
            }
            0x08 -> {
                val ms = LocalTime.now().nano / 1000
                (ms ushr 8).toShort()
            }
            else -> 0xff
        }
    }

    override operator fun set(address: Address, data: UByte) {
        /* real time clock can't be set */
    }
}
