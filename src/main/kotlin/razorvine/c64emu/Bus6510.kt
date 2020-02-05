package razorvine.c64emu

import razorvine.ksim65.components.Address
import razorvine.ksim65.components.Rom
import razorvine.ksim65.components.UByte

/**
 * The C64's bus is a bit peculiar:
 * appearance of RAM or ROM in certain address ranges can be dynamically controlled
 * via the 6510's IO port register in $00/$01, "bank switching".
 * More info here https://www.c64-wiki.com/wiki/Bank_Switching
 *
 * Note: we don't implement the expansion port's _EXROM and _GAME lines that are used
 * for mapping in cartridge ROMs into the address space.
 */
class Bus6510(private val ioPort: CpuIoPort,
              private val chargen: Rom,
              private val basic: Rom,
              private val kernal: Rom): razorvine.ksim65.Bus() {

    override fun read(address: Address): UByte {
        return when(address) {
            in 0x0000..0x9fff -> super.read(address)  // always RAM
            in 0xa000..0xbfff -> {
                // BASIC or RAM
                if(ioPort.loram && ioPort.hiram)
                    basic[address - 0xa000]
                else
                    super.read(address)
            }
            in 0xc000..0xcfff -> super.read(address)  // always RAM
            in 0xd000..0xdfff -> {
                // IO or CHAR ROM
                if(ioPort.charen)
                    super.read(address)
                else
                    chargen[address - 0xd000]
            }
            else -> {
                // 0xe000..0xffff, KERNAL or RAM
                if(ioPort.hiram)
                    kernal[address - 0xe000]
                else
                    super.read(address)
            }
        }
    }
}
