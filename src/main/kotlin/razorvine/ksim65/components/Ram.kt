package razorvine.ksim65.components

import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Paths

/**
 * A RAM chip with read/write memory.
 */
class Ram(startAddress: Address, endAddress: Address) : MemoryComponent(startAddress, endAddress) {
    private val memory = ShortArray(endAddress - startAddress + 1)

    override operator fun get(address: Address): UByte = memory[address - startAddress]

    override operator fun set(address: Address, data: UByte) {
        memory[address - startAddress] = data
    }

    override fun copyOfMem(): Array<UByte> = memory.toTypedArray()

    override fun clock() {}

    override fun reset() {
        // contents of RAM doesn't change on a reset
    }

    fun fill(data: UByte) {
        memory.fill(data)
    }

    /**
     * Load a c64-style prg program. This file type has the load address as the first two bytes.
     */
    fun loadPrg(filename: String) {
        loadPrg(Paths.get(filename).toUri())
    }

    /**
     * Load a c64-style prg program. This file type has the load address as the first two bytes.
     */
    fun loadPrg(file: URI) {
        val bytes = File(file).readBytes()
        val loadAddress = (bytes[0].toInt() or (bytes[1].toInt() shl 8)) and 65535
        val baseAddress = loadAddress - startAddress
        bytes.drop(2).forEachIndexed { index, byte ->
            memory[baseAddress + index] =
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
            memory[baseAddress + index] = byte
        }

    fun load(data: ByteArray, address: Address) =
        data.forEachIndexed { index, byte ->
            val baseAddress = address - startAddress
            memory[baseAddress + index] =
                if (byte >= 0)
                    byte.toShort()
                else
                    (256 + byte).toShort()
        }
}
