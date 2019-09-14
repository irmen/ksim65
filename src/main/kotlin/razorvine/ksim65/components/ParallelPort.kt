package razorvine.ksim65.components

/**
 * A simple parallel output device (basically, prints bytes as characters to the console)
 *
 * byte   value
 * ----   ---------
 *  00    data (the 8 parallel bits)
 *  01    control latch (set bit 0 to write the data byte)
 */
class ParallelPort(startAddress: Address, endAddress: Address) : MemMappedComponent(startAddress, endAddress) {
    private var dataByte: UByte = 0

    init {
        require(endAddress - startAddress + 1 == 2) { "parallel needs exactly 2 memory bytes (data + control)" }
    }

    override fun clock() {}
    override fun reset() {}

    override operator fun get(address: Address): UByte {
        return if (address == startAddress)
            dataByte
        else
            0
    }

    override operator fun set(address: Address, data: UByte) {
        if (address == startAddress)
            dataByte = data
        else if (address == endAddress) {
            if ((data.toInt() and 1) == 1) {
                val char = dataByte.toChar()
                println("PARALLEL WRITE: '$char'")
            }
        }
    }
}
