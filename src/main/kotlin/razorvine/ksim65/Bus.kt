package razorvine.ksim65

import razorvine.ksim65.components.Address
import razorvine.ksim65.components.BusComponent
import razorvine.ksim65.components.MemMappedComponent
import razorvine.ksim65.components.UByte

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
            if (address >= it.startAddress && address <= it.endAddress)
                return it[address]
        }
        return 0xff
    }

    /**
     * Write a data byte to the given address.
     * Any memory mapped component that listens to the address, will receive the data.
     */
    fun write(address: Address, data: UByte) {
        memComponents.forEach {
            if (address >= it.startAddress && address <= it.endAddress)
                it[address] = data
        }
    }
}
