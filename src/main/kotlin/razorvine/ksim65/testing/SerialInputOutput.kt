package razorvine.ksim65.testing

import razorvine.ksim65.components.Address
import razorvine.ksim65.components.MemMappedComponent

/**
 * Simple I/O device for testing.
 * Provides read from input callback and write to output callback.
 * Register 0 = read serial byte
 * Register 1 = write serial byte
 * Register 2 = when written, reset the system
 * Register 3 = when written, poweroff the system
 */
class SerialInputOutput(startAddress: Address, val host: IHostSerialAndPowerIO) : MemMappedComponent(startAddress, startAddress+3) {

    override fun clock() {}
    override fun reset() {}

    override operator fun get(offset: Int): UByte {
        return if (offset == 0x00) host.read() else 0.toUByte()
    }

    override operator fun set(offset: Int, data: UByte) {
        when (offset) {
            0x01 -> host.write(data)
            0x02 -> host.reset()
            0x03 -> host.poweroff()
        }
    }
}