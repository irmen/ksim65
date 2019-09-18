package razorvine.ksim65.components

import java.io.File

/**
 * A ROM chip (read-only memory).
 */
class Rom(startAddress: Address, endAddress: Address, initialData: Array<UByte>? = null) : MemoryComponent(startAddress, endAddress) {
    override val data: Array<UByte> =
        initialData?.copyOf() ?: Array<UByte>(endAddress - startAddress + 1) { 0 }

    init {
        require(endAddress - startAddress + 1 == data.size) { "rom address range doesn't match size of data bytes" }
    }

    override operator fun get(address: Address): UByte = data[address - startAddress]
    override operator fun set(address: Address, data: UByte) { /* read-only */ }
    override fun clock() {}
    override fun reset() {}

    /**
     * load a binary program at the given address
     */
    fun load(filename: String) {
        val bytes = File(filename).readBytes()
        load(bytes)
    }

    fun load(data: Array<UByte>) =
        data.forEachIndexed { index, byte ->
            this.data[index] = byte
        }

    fun load(data: ByteArray) =
        data.forEachIndexed { index, byte ->
            this.data[index] =
                if (byte >= 0)
                    byte.toShort()
                else
                    (256 + byte).toShort()
        }
}
