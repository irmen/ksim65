package net.razorvine.ksim65.components

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

    fun read(address: Address): UByte {
        memComponents.forEach {
            if (address >= it.startAddress && address <= it.endAddress)
                return it[address]
        }
        return 0xff
    }

    fun write(address: Address, data: UByte) {
        memComponents.forEach {
            if (address >= it.startAddress && address <= it.endAddress)
                it[address] = data
        }
    }
}
