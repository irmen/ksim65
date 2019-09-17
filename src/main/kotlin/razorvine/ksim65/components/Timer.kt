package razorvine.ksim65.components

import razorvine.ksim65.Cpu6502

/**
 * A programmable timer. Causes an IRQ or NMI at specified 24-bits clock cycle intervals.
 *
 * reg.   value
 * ----   --------------
 *  00    control register  bit 0=enable  bit 1=nmi (instead of irq)
 *  01    24 bits interval value, bits 0-7 (lo)
 *  02    24 bits interval value, bits 8-15 (mid)
 *  03    24 bits interval value, bits 16-23  (hi)
 */
class Timer(startAddress: Address, endAddress: Address, val cpu: Cpu6502) : MemMappedComponent(startAddress, endAddress) {
    private var counter: Int = 0
    private var interval: Int = 0
    private var nmi = false
    private var enabled = false
        set(value) {
            if (value && !field) {
                // timer is set to enabled (was disabled) - reset the counter
                counter = 0
            }
            field = value
        }

    init {
        require(endAddress - startAddress + 1 == 4) { "timer needs exactly 4 memory bytes" }
    }

    override fun clock() {
        if (enabled && interval > 0) {
            counter++
            if (counter == interval) {
                if (nmi)
                    cpu.nmi()
                else
                    cpu.irq()
                counter = 0
            }
        }
    }

    override fun reset() {
        counter = 0
        interval = 0
        enabled = false
        nmi = false
    }

    override operator fun get(address: Address): UByte {
        return when (address - startAddress) {
            0x00 -> {
                var data = 0
                if (enabled) data = data or 0b00000001
                if (nmi) data = data or 0b00000010
                data.toShort()
            }
            0x01 -> (counter and 0xff).toShort()
            0x02 -> ((counter ushr 8) and 0xff).toShort()
            0x03 -> ((counter ushr 16) and 0xff).toShort()
            else -> 0xff
        }
    }

    override operator fun set(address: Address, data: UByte) {
        when (address - startAddress) {
            0x00 -> {
                val i = data.toInt()
                enabled = (i and 0b00000001) != 0
                nmi = (i and 0b00000010) != 0
            }
            0x01 -> {
                interval = (interval and 0x7fffff00) or data.toInt()
            }
            0x02 -> {
                interval = (interval and 0x7fff00ff) or (data.toInt() shl 8)
            }
            0x03 -> {
                interval = (interval and 0x7f00ffff) or (data.toInt() shl 16)
            }
        }
    }
}
