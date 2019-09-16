package razorvine.ksim65.components

import java.io.File
import java.io.InputStream
import java.net.URL

/**
 * A RAM chip with read/write memory.
 */
class Ram(startAddress: Address, endAddress: Address) : MemoryComponent(startAddress, endAddress) {
    override val data = Array<UByte>(endAddress - startAddress + 1) { 0 }

    override operator fun get(address: Address): UByte = data[address - startAddress]

    override operator fun set(address: Address, data: UByte) {
        this.data[address - startAddress] = data
    }

    override fun clock() {}

    override fun reset() {
        // contents of RAM doesn't change on a reset
    }

    fun fill(data: UByte) = this.data.fill(data)

    /**
     * Load a c64-style prg program. This file type has the load address as the first two bytes.
     */
    fun loadPrg(filename: String) = loadPrg(File(filename).inputStream())

    /**
     * Load a c64-style prg program. This file type has the load address as the first two bytes.
     */
    fun loadPrg(stream: InputStream) {
        val bytes = stream.readAllBytes()
        val loadAddress = (bytes[0].toInt() or (bytes[1].toInt() shl 8)) and 65535
        val baseAddress = loadAddress - startAddress
        bytes.drop(2).forEachIndexed { index, byte ->
            data[baseAddress + index] =
                if (byte >= 0)
                    byte.toShort()
                else
                    (256 + byte).toShort()
        }
    }

    /**
     * load a binary program at the given address
     */
    fun load(filename: String, address: Address) {
        val bytes = File(filename).readBytes()
        load(bytes, address)
    }

    fun load(source: URL, address: Address) {
        val bytes = source.readBytes()
        load(bytes, address)
    }

    fun load(data: Array<UByte>, address: Address) =
        data.forEachIndexed { index, byte ->
            val baseAddress = address - startAddress
            this.data[baseAddress + index] = byte
        }

    fun load(data: ByteArray, address: Address) =
        data.forEachIndexed { index, byte ->
            val baseAddress = address - startAddress
            this.data[baseAddress + index] =
                if (byte >= 0)
                    byte.toShort()
                else
                    (256 + byte).toShort()
        }
}
