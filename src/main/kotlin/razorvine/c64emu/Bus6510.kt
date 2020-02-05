package razorvine.c64emu

import razorvine.ksim65.components.Address
import razorvine.ksim65.components.Rom
import razorvine.ksim65.components.UByte

/**
 * The C64's bus is a bit peculiar
 * Appearance of RAM or ROM in certain adress ranges can be dynamically controlled
 * via the 6510's IO port register in $00/$01
 * We only implement banking in or out the character rom or I/O space at $d000-$e000 for now.
 * Doing it here in the bus directly is the poor man's mmu solution, I guess
 *
 * TODO: mapping the RAM/ROMs in and out of the other banks in the address space (controlled by loram and hiram).
 */
class Bus6510(private val ioPort: CpuIoPort,
              private val chargen: Rom,
              private val basic: Rom,
              private val kernal: Rom): razorvine.ksim65.Bus() {

    override fun read(address: Address): UByte {
        if(address in 0xd000..0xe000) {
            if(!ioPort.charen) {
                // character rom is enabled in this address range (instead of I/O)
                return chargen[address-chargen.startAddress]
            }
        }

        return super.read(address)
    }
}
