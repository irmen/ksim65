package razorvine.ksim65.testing

import razorvine.ksim65.components.Address
import razorvine.ksim65.components.MemMappedComponent

/**
 * Simple I/O device for testing.
 * Provides read from input callback and write to output callback.
 */
class SerialInputOutput(
        startAddress: Address,
        input: Sequence<UByte>,
        val output: (UByte) -> Unit
) : MemMappedComponent(startAddress, startAddress) {

    val inputSeq = input.iterator()
    override fun clock() {}
    override fun reset() {}

    override operator fun get(offset: Int): UByte {
        return if (offset == 0x00) inputSeq.next() else 0.toUByte()
    }

    override operator fun set(offset: Int, data: UByte) {
        if (offset == 0x01) output(data)
    }
}