package razorvine.ksim65.components

/**
 * A simple parallel output device (basically, prints bytes as characters to the console)
 *
 * reg.   value
 * ----   ---------
 *  00    data (the 8 parallel bits)
 *  01    control latch (set bit 0 to write the data byte)
 */
class ParallelPort(startAddress: Address, endAddress: Address) : MemMappedComponent(startAddress, endAddress) {
    private var dataByte: UByte = 0

    init {
        require(endAddress-startAddress+1 == 2) { "parallel needs exactly 2 memory bytes (data + control)" }
    }

    override fun clock() {}
    override fun reset() {}

    override operator fun get(offset: Int): UByte {
        return if (offset == 0) dataByte
        else 0xff
    }

    override operator fun set(offset: Int, data: UByte) {
        if (offset == 0) dataByte = data
        else if (offset == 1) {
            if ((data.toInt() and 1) == 1) {
                val char = dataByte.toChar()
                println("PARALLEL WRITE: '$char'")
            }
        }
    }
}
