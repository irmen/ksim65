package razorvine.c64emu

import razorvine.ksim65.Cpu6502
import razorvine.ksim65.components.Address
import razorvine.ksim65.components.MemMappedComponent
import razorvine.ksim65.components.UByte

/**
 * The 6510's IO port located at $00/$01
 * Controlling the memory layout, and cassette port (not processed at all).
 * TODO: actually mapping the roms in and out of the address space. This requires some MMU type logic in the Bus class.
 */
class CpuIoPort(val cpu: Cpu6502) : MemMappedComponent(0x0000, 0x0001) {

    private var dataDirections: Int = 0
    private var ioPort: Int = 0
    private var loram: Boolean = false  // Bit 0: LORAM signal.  Selects ROM or RAM at 40960 ($A000).  1=BASIC, 0=RAM
    private var hiram: Boolean = false  // Bit 1: HIRAM signal.  Selects ROM or RAM at 57344 ($E000).  1=Kernal, 0=RAM
    private var charen: Boolean = false // Bit 2: CHAREN signal.  Selects character ROM or I/O devices.  1=I/O, 0=ROM

    override fun clock() { }
    override fun reset() { }

    override fun get(address: Address): UByte {
        return if(address==0) dataDirections.toShort() else {
            (ioPort or dataDirections.inv() and 0b00111111).toShort()
        }
    }

    override fun set(address: Address, data: UByte) {
        if(address==0) {
            dataDirections = data.toInt()
            determineRoms()
        } else {
            ioPort = data.toInt()
            determineRoms()
        }
    }

    private fun determineRoms() {
        if (dataDirections and 0b00000001 != 0) loram = ioPort and 0b00000001 != 0
        if (dataDirections and 0b00000010 != 0) hiram = ioPort and 0b00000010 != 0
        if (dataDirections and 0b00000100 != 0) charen = ioPort and 0b00000100 != 0
    }
}