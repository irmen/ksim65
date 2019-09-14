package razorvine.ksim65.components

typealias UByte = Short
typealias Address = Int


abstract class BusComponent {
    lateinit var bus: Bus

    abstract fun clock()
    abstract fun reset()
}

abstract class MemMappedComponent(val startAddress: Address, val endAddress: Address) : BusComponent() {
    abstract operator fun get(address: Address): UByte
    abstract operator fun set(address: Address, data: UByte)

    init {
        require(endAddress >= startAddress)
        require(startAddress >= 0 && endAddress <= 0xffff) { "can only have 16-bit address space" }
    }

    fun hexDump(from: Address, to: Address, charmapper: ((Short)->Char)? = null) {
        (from..to).chunked(16).forEach {
            print("\$${it.first().toString(16).padStart(4, '0')}  ")
            val bytes = it.map { address -> get(address) }
            bytes.forEach { byte ->
                print(byte.toString(16).padStart(2, '0') + " ")
            }
            print("  ")
            val chars =
                if(charmapper!=null)
                    bytes.map { b -> charmapper(b) }
                else
                    bytes.map { b -> if(b in 32..255) b.toChar() else '.' }
            println(chars.joinToString(""))
        }
    }
}

abstract class MemoryComponent(startAddress: Address, endAddress: Address) :
    MemMappedComponent(startAddress, endAddress) {
    abstract fun cloneContents(): Array<UByte>
}
