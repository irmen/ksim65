package razorvine.ksim65.components

import razorvine.ksim65.IHostInterface

/**
 * A mouse or tablet absolute position input device, with 2 buttons.
 *
 * reg.   value
 * ----   ---------
 *  00    mouse pixel pos X (lsb)
 *  01    mouse pixel pos X (msb)
 *  02    mouse pixel pos Y (lsb)
 *  03    mouse pixel pos Y (msb)
 *  04    buttons, bit 0 = left button, bit 1 = right button, bit 2 = middle button
 *  05    latch: when written, samples the current mouse position and button state.
 */
class Mouse(startAddress: Address, endAddress: Address, private val host: IHostInterface) : MemMappedComponent(startAddress, endAddress) {

    init {
        require(endAddress-startAddress+1 == 6) { "mouse needs exactly 6 memory bytes" }
    }

    override fun clock() {}
    override fun reset() {}

    private var mouse = host.mouse()

    override operator fun get(offset: Int): UByte {
        return when (offset) {
            0x00 -> (mouse.x and 0xff).toShort()
            0x01 -> (mouse.x ushr 8).toShort()
            0x02 -> (mouse.y and 0xff).toShort()
            0x03 -> (mouse.y ushr 8).toShort()
            0x04 -> {
                val b1 = if (mouse.left) 0b00000001 else 0
                val b2 = if (mouse.right) 0b00000010 else 0
                val b3 = if (mouse.middle) 0b00000100 else 0
                return (b1 or b2 or b3).toShort()
            }
            else -> 0xff
        }
    }

    override operator fun set(offset: Int, data: UByte) {
        if (offset == 0x05) mouse = host.mouse()
    }
}
