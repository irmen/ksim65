package razorvine.ksim65.components

import razorvine.ksim65.IHostInterface

/**
 * Simple keyboard for text entry.
 * The keyboard device itself takes care of decoding the keys,
 * this device simply produces the actual keys pressed.
 * There's NO support right now to detect keydown/keyup events or the
 * state of the shift/control/function keys.
 *
 * reg.   value
 * ----   ---------
 *  00    character from keyboard, 0 = no character/key pressed
 */
class Keyboard(startAddress: Address, endAddress: Address, private val host: IHostInterface) :
        MemMappedComponent(startAddress, endAddress) {

    init {
        require(endAddress-startAddress+1 == 1) { "keyboard needs exactly 1 memory byte" }
    }

    override fun clock() {}
    override fun reset() {}

    override operator fun get(offset: Int): UByte {
        return when (offset) {
            0x00 -> host.keyboard()?.toShort() ?: 0
            else -> 0xff
        }
    }

    override operator fun set(offset: Int, data: UByte) { /* read-only device */ }
}
