package razorvine.ksim65.components

import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * A RAM chip with read/write memory.
 */
class Ram(startAddress: Address, endAddress: Address) : MemoryComponent(startAddress, endAddress) {
    override val data = Array<UByte>(endAddress-startAddress+1) { 0 }

    override operator fun get(offset: Int): UByte = data[offset]

    override operator fun set(offset: Int, data: UByte) {
        this.data[offset] = data
    }

    override fun clock() {}

    override fun reset() {
        // contents of RAM doesn't change on a reset
    }

    fun fill(data: UByte) = this.data.fill(data)

    /**
     * Load a c64-style prg program. This file type has the load address as the first two bytes.
     */
    fun loadPrg(filename: String, overrideLoadAddress: Address?) = loadPrg(File(filename).inputStream(), overrideLoadAddress)

    /**
     * Load a c64-style prg program. This file type has the load address as the first two bytes.
     */
    fun loadPrg(stream: InputStream, overrideLoadAddress: Address?): Pair<Address, Int> {
        val bytes = stream.readBytes()
        if (bytes.size > 0xffff) throw IOException("file too large")
        val loadAddress = overrideLoadAddress ?: bytes[0]+256*bytes[1]
        val baseAddress = loadAddress-startAddress
        bytes.drop(2).forEachIndexed { index, byte ->
            data[baseAddress+index] = if (byte >= 0) byte.toShort()
            else (256+byte).toShort()
        }
        return Pair(baseAddress, bytes.size-2)
    }

    /**
     * load a binary data file at the given address
     */
    fun load(filename: String, address: Address) {
        val bytes = File(filename).readBytes()
        load(bytes, address)
    }

    fun load(data: Array<UByte>, address: Address) = data.forEachIndexed { index, byte ->
        val baseAddress = address-startAddress
        this.data[baseAddress+index] = byte
    }

    fun load(data: ByteArray, address: Address) = data.forEachIndexed { index, byte ->
        val baseAddress = address-startAddress
        this.data[baseAddress+index] = if (byte >= 0) byte.toShort()
        else (256+byte).toShort()
    }
}
