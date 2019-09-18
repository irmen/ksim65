package razorvine.ksim65

import razorvine.ksim65.components.*

/**
 * The system bus that connects all other components together.
 * Usually, there is just a single Bus present.
 *
 * It distributes reset and clock signals to every connected component.
 * Data bytes can be read from the bus or written to the bus. It's distributed to the corresponding component(s).
 */
class Bus {

    private val components = mutableListOf<BusComponent>()
    private val memComponents = mutableListOf<MemMappedComponent>()

    fun reset() {
        components.forEach { it.reset() }
        memComponents.forEach { it.reset() }
    }

    fun clock() {
        components.forEach { it.clock() }
        memComponents.forEach { it.clock() }
    }

    operator fun plusAssign(memcomponent: MemMappedComponent) = add(memcomponent)
    operator fun plusAssign(component: BusComponent) = add(component)
    operator fun get(address: Address): UByte = read(address)
    operator fun set(address: Address, data: UByte) = write(address, data)


    fun add(component: BusComponent) {
        components.add(component)
        component.bus = this
    }

    fun add(component: MemMappedComponent) {
        memComponents.add(component)
        component.bus = this
    }

    /**
     * Read a data byte at the given address.
     * The first memory mapped component that listens to that address, will respond.
     */
    fun read(address: Address): UByte {
        memComponents.forEach {
            if (address >= it.startAddress && address <= it.endAddress) {
                val data = it[address]
                require(data in 0..255) {
                    "data must be a byte 0..255"
                }
                return data
            }
        }
        return 0xff
    }

    /**
     * Write a data byte to the given address.
     * Any memory mapped component that listens to the address, will receive the data.
     */
    fun write(address: Address, data: UByte) {
        require(data in 0..255) {
            "data must be a byte 0..255"
        }
        memComponents.forEach {
            if (address >= it.startAddress && address <= it.endAddress)
                it[address] = data
        }
    }

    fun memoryComponentFor(address: Address): MemoryComponent {
        memComponents.forEach {
            if (it is MemoryComponent && address >= it.startAddress && address <= it.endAddress) {
                return it
            }
        }
        throw NoSuchElementException()
    }
}
