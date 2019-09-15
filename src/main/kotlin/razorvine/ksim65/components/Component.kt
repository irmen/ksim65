package razorvine.ksim65.components

import razorvine.ksim65.Bus

typealias UByte = Short
typealias Address = Int


/**
 * Base class for any component connected to the system bus.
 */
abstract class BusComponent {
    lateinit var bus: Bus

    /**
     * One clock cycle on the bus
     */
    abstract fun clock()

    /**
     * Reset all devices on the bus
     */
    abstract fun reset()
}

/**
 * Base class for components that have registers mapped into the cpu's address space.
 * Most I/O components fall into this category.
 */
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

/**
 * Base class for components that actually contain memory (RAM or ROM chips).
 */
abstract class MemoryComponent(startAddress: Address, endAddress: Address) :
    MemMappedComponent(startAddress, endAddress) {
    abstract fun copyOfMem(): Array<UByte>

    init {
        require(startAddress and 0xff == 0 && endAddress and 0xff == 0xff) {"address range must span complete page(s)"}
    }
}
