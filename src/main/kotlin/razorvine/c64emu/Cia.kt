package razorvine.c64emu

import razorvine.ksim65.components.Address
import razorvine.ksim65.components.MemMappedComponent
import razorvine.ksim65.components.UByte

/**
 * Minimal simulation of the MOS 6526 CIA chip.
 * Depending on what CIA it is (1 or 2), some registers do different things on the C64.
 */
class Cia(val number: Int, startAddress: Address, endAddress: Address): MemMappedComponent(startAddress, endAddress) {
    private var ramBuffer = Array<UByte>(endAddress - startAddress + 1) { 0x00 }
    private var pra = 0xff

    init {
        require(endAddress - startAddress + 1 == 256) { "cia requires exactly 256 memory bytes (16*16 mirrored)" }
    }

    override fun clock() {
        // TODO: TOD timer, timer A and B countdowns, IRQ triggering.
    }

    override fun reset() {
        // TODO: reset TOD timer, timer A and B
    }

    override fun get(address: Address): UByte {
        val register = (address - startAddress) and 15
        if(number==1) {
            return if (register == 0x01) {
                // PRB data port B (if bit is cleared in PRA, contains keys pressed in that column of the matrix)
                println("read PRB, pra=${pra.toString(2)}")
                when(pra) {
                    0b00000000 -> {
                        // check if any keys are pressed at all (by checking all columns at once)
                        // TODO zero if there's any key pressed, 0xff otherwise
                        0xff
                    }
                    0b11111110 -> {
                        // read column 0
                        0xff
                    }
                    0b11111101 -> {
                        // read column 1
                        0xff
                    }
                    0b11111011 -> {
                        0b11011111.toShort()    // 'F'
                    }
                    0b11110111 -> {
                        // read column 3
                        0xff
                    }
                    0b11101111 -> {
                        // read column 4
                        0xff
                    }
                    0b11011111 -> {
                        // read column 5
                        0xff
                    }
                    0b10111111 -> {
                        // read column 6
                        0xff
                    }
                    0b01111111 -> {
                        // read column 7
                        0xff
                    }
                    else ->  {
                        // invalid column selection
                        0xff
                    }
                }
            }
            else ramBuffer[register]
        }

        // CIA #2 is not emulated yet
        return ramBuffer[register]
    }

    override fun set(address: Address, data: UByte) {
        val register = (address - startAddress) and 15
        ramBuffer[register] = data
        if(number==1) {
            when (register) {
                0x00 -> {
                    // PRA data port A (select keyboard matrix column)
                    pra = data.toInt()
                }
            }
        }
        // CIA #2 is not emulated yet
    }
}
