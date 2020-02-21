package razorvine.c64emu

import razorvine.ksim65.components.MemMappedComponent
import razorvine.ksim65.components.UByte

/**
 * The 6510's IO port located at $00/$01
 * Controlling the memory layout, and cassette port (not processed at all).
 * TODO: there are a few functional tests that still fail. Is this not implemented correctly yet?
 */
class CpuIoPort : MemMappedComponent(0x0000, 0x0001) {

    private var dataDirections: Int = 0
    private var ioPort: Int = 0

    var loram: Boolean = false  // Bit 0: LORAM signal.  Selects ROM or RAM at 40960 ($A000).  1=BASIC, 0=RAM
        private set
    var hiram: Boolean = false  // Bit 1: HIRAM signal.  Selects ROM or RAM at 57344 ($E000).  1=Kernal, 0=RAM
        private set
    var charen: Boolean = false // Bit 2: CHAREN signal.  Selects character ROM or I/O devices.  1=I/O, 0=ROM
        private set

    override fun clock() { }
    override fun reset() {
        dataDirections = 0xef
        ioPort = 0x37
        determineRoms()
    }

    override operator fun get(offset: Int): UByte {
        return if(offset==0) {
            dataDirections.toShort()
        } else {
            if(dataDirections and 0b00100000 == 0)
                (ioPort and 0b11011111).toShort()        // bit 5 is low when input
            else
                ioPort.toShort()
        }
    }

    override operator fun set(offset: Int, data: UByte) {
        if(offset==0) {
            dataDirections = data.toInt()
            determineRoms()
        } else {
            ioPort = (ioPort and dataDirections.inv()) or (data.toInt() and dataDirections)
            determineRoms()
        }
    }

    private fun determineRoms() {
        loram = ioPort and 0b00000001 != 0
        hiram = ioPort and 0b00000010 != 0
        charen = ioPort and 0b00000100 != 0
    }
}
