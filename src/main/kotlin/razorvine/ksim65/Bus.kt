package razorvine.ksim65

import razorvine.ksim65.components.*

/**
 * The system bus that connects all other components together.
 * Usually, there is just a single Bus present.
 *
 * It distributes reset and clock signals to every connected component.
 * Data bytes can be read from the bus or written to the bus. It's distributed to the corresponding component(s).
 *
 * NOTE: currently the bus address mapping is STATIC: there is no possibility for Bank-switching.
 *       (such as what the C-64 has; the ability to swap ROMs in and out of the address space).
 */
open class Bus {

    private val allComponents = mutableListOf<BusComponent>()
    private val memComponents = mutableListOf<MemMappedComponent>()

    fun reset() = allComponents.forEach { it.reset() }
    fun clock() = allComponents.forEach { it.clock() }

    operator fun plusAssign(memComponent: MemMappedComponent) = add(memComponent)
    operator fun plusAssign(component: BusComponent) = add(component)
    operator fun get(address: Address): UByte = read(address)
    operator fun set(address: Address, data: UByte) = write(address, data)


    fun add(component: BusComponent) {
        allComponents.add(component)
        component.bus = this
    }

    fun add(memComponent: MemMappedComponent) {
        memComponents.add(memComponent)
        allComponents.add(memComponent)
        memComponent.bus = this
    }

    /**
     * Read a data byte at the given address.
     * The first registered memory mapped component that listens to that address, will respond.
     * If no component is available, some CPUs generate a BUS ERROR but we return 0xff instead.
     */
    open fun read(address: Address): UByte {
        memComponents.forEach {
            if (address >= it.startAddress && address <= it.endAddress) {
                val data = it[address - it.startAddress]
                require(data in 0..255) { "data at address $address must be a byte 0..255, but is $data" }
                return data
            }
        }
        return 0xff
    }

    /**
     * Write a data byte to the given address.
     * All memory mapped components that are mapped to the address, will receive the data.
     */
    open fun write(address: Address, data: UByte) {
        require(data in 0..255) { "data written to address $address must be a byte 0..255" }
        memComponents.forEach {
            if (address >= it.startAddress && address <= it.endAddress)
                it[address-it.startAddress] = data
        }
    }
}
