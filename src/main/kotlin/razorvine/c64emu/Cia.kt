package razorvine.c64emu

import razorvine.ksim65.components.Address
import razorvine.ksim65.components.MemMappedComponent
import razorvine.ksim65.components.UByte
import java.awt.event.KeyEvent

/**
 * Minimal simulation of the MOS 6526 CIA chip.
 * Depending on what CIA it is (1 or 2), some registers do different things on the C64.
 */
class Cia(val number: Int, startAddress: Address, endAddress: Address): MemMappedComponent(startAddress, endAddress) {
    private var ramBuffer = Array<UByte>(endAddress - startAddress + 1) { 0x00 }
    private var pra = 0xff

    private val hostKeyPresses = mutableSetOf<Int>()

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
                // register 1 on CIA#1 is the keyboard data port
                // if bit is cleared in PRA, contains keys pressed in that column of the matrix
                // TODO tweak the keyboard mapping so that keys like '=' can be pressed,
                //   RESTORE works (this is not a normal key but connected to the cpu's NMI line),
                //   Cursor up/down/left/right all work and left/right shift are different.
                when(pra) {
                    0b00000000 -> {
                        // check if any keys are pressed at all (by checking all columns at once)
                        if(hostKeyPresses.isEmpty()) 0xff.toShort() else 0x00.toShort()
                    }
                    0b11111110 -> {
                        // read column 0
                        val presses =
                            (if (KeyEvent.VK_DOWN in hostKeyPresses) 0b10000000 else 0) or
                            (if (KeyEvent.VK_F5 in hostKeyPresses) 0b01000000 else 0) or
                            (if (KeyEvent.VK_F3 in hostKeyPresses) 0b00100000 else 0) or
                            (if (KeyEvent.VK_F1 in hostKeyPresses) 0b00010000 else 0) or
                            (if (KeyEvent.VK_F7 in hostKeyPresses) 0b00001000 else 0) or
                            (if (KeyEvent.VK_RIGHT in hostKeyPresses) 0b00000100 else 0) or
                            (if (KeyEvent.VK_ENTER in hostKeyPresses) 0b00000010 else 0) or
                            (if (KeyEvent.VK_BACK_SPACE in hostKeyPresses) 0b00000001 else 0)
                        (presses.inv() and 255).toShort()
                    }
                    0b11111101 -> {
                        // read column 1
                        val presses =
                            (if(KeyEvent.VK_SHIFT in hostKeyPresses) 0b10000000 else 0) or      // TODO make it LEFT shift only
                            (if(KeyEvent.VK_E in hostKeyPresses) 0b01000000 else 0) or
                            (if(KeyEvent.VK_S in hostKeyPresses) 0b00100000 else 0) or
                            (if(KeyEvent.VK_Z in hostKeyPresses) 0b00010000 else 0) or
                            (if(KeyEvent.VK_4 in hostKeyPresses) 0b00001000 else 0) or
                            (if(KeyEvent.VK_A in hostKeyPresses) 0b00000100 else 0) or
                            (if(KeyEvent.VK_W in hostKeyPresses) 0b00000010 else 0) or
                            (if(KeyEvent.VK_3 in hostKeyPresses) 0b00000001 else 0)
                        (presses.inv() and 255).toShort()
                    }
                    0b11111011 -> {
                        // read column 2
                        val presses =
                            (if (KeyEvent.VK_X in hostKeyPresses) 0b10000000 else 0) or
                            (if (KeyEvent.VK_T in hostKeyPresses) 0b01000000 else 0) or
                            (if (KeyEvent.VK_F in hostKeyPresses) 0b00100000 else 0) or
                            (if (KeyEvent.VK_C in hostKeyPresses) 0b00010000 else 0) or
                            (if (KeyEvent.VK_6 in hostKeyPresses) 0b00001000 else 0) or
                            (if (KeyEvent.VK_D in hostKeyPresses) 0b00000100 else 0) or
                            (if (KeyEvent.VK_R in hostKeyPresses) 0b00000010 else 0) or
                            (if (KeyEvent.VK_5 in hostKeyPresses) 0b00000001 else 0)
                        (presses.inv() and 255).toShort()
                    }
                    0b11110111 -> {
                        // read column 3
                        val presses =
                            (if (KeyEvent.VK_V in hostKeyPresses) 0b10000000 else 0) or
                            (if (KeyEvent.VK_U in hostKeyPresses) 0b01000000 else 0) or
                            (if (KeyEvent.VK_H in hostKeyPresses) 0b00100000 else 0) or
                            (if (KeyEvent.VK_B in hostKeyPresses) 0b00010000 else 0) or
                            (if (KeyEvent.VK_8 in hostKeyPresses) 0b00001000 else 0) or
                            (if (KeyEvent.VK_G in hostKeyPresses) 0b00000100 else 0) or
                            (if (KeyEvent.VK_Y in hostKeyPresses) 0b00000010 else 0) or
                            (if (KeyEvent.VK_7 in hostKeyPresses) 0b00000001 else 0)
                        (presses.inv() and 255).toShort()
                    }
                    0b11101111 -> {
                        // read column 4
                        val presses =
                            (if (KeyEvent.VK_N in hostKeyPresses) 0b10000000 else 0) or
                            (if (KeyEvent.VK_O in hostKeyPresses) 0b01000000 else 0) or
                            (if (KeyEvent.VK_K in hostKeyPresses) 0b00100000 else 0) or
                            (if (KeyEvent.VK_M in hostKeyPresses) 0b00010000 else 0) or
                            (if (KeyEvent.VK_0 in hostKeyPresses) 0b00001000 else 0) or
                            (if (KeyEvent.VK_J in hostKeyPresses) 0b00000100 else 0) or
                            (if (KeyEvent.VK_I in hostKeyPresses) 0b00000010 else 0) or
                            (if (KeyEvent.VK_9 in hostKeyPresses) 0b00000001 else 0)
                        (presses.inv() and 255).toShort()
                    }
                    0b11011111 -> {
                        // read column 5
                        val presses =
                            (if (KeyEvent.VK_COMMA in hostKeyPresses) 0b10000000 else 0) or
                            (if (KeyEvent.VK_OPEN_BRACKET in hostKeyPresses) 0b01000000 else 0) or     // '[' = @
                            (if (KeyEvent.VK_SEMICOLON in hostKeyPresses) 0b00100000 else 0) or     // ';' -> :
                            (if (KeyEvent.VK_PERIOD in hostKeyPresses) 0b00010000 else 0) or
                            (if (KeyEvent.VK_MINUS in hostKeyPresses) 0b00001000 else 0) or
                            (if (KeyEvent.VK_L in hostKeyPresses) 0b00000100 else 0) or
                            (if (KeyEvent.VK_P in hostKeyPresses) 0b00000010 else 0) or
                            (if (KeyEvent.VK_PLUS in hostKeyPresses) 0b00000001 else 0)
                        (presses.inv() and 255).toShort()
                    }
                    0b10111111 -> {
                        // read column 6
                        val presses =
                            (if (KeyEvent.VK_SLASH in hostKeyPresses) 0b10000000 else 0) or
                            (if (KeyEvent.VK_BACK_SLASH in hostKeyPresses) 0b01000000 else 0) or    // '\' -> up arrow
                            (if (KeyEvent.VK_EQUALS in hostKeyPresses) 0b00100000 else 0) or
                            (if (KeyEvent.VK_SHIFT in hostKeyPresses) 0b00010000 else 0) or         // TODO RIGHT shift only
                            (if (KeyEvent.VK_HOME in hostKeyPresses) 0b00001000 else 0) or
                            (if (KeyEvent.VK_QUOTE in hostKeyPresses) 0b00000100 else 0) or          //  ' -> ;
                            (if (KeyEvent.VK_CLOSE_BRACKET in hostKeyPresses) 0b00000010 else 0) or  // ']' = *
                            (if (KeyEvent.VK_END in hostKeyPresses) 0b00000001 else 0)        // END -> pound key
                        (presses.inv() and 255).toShort()
                    }
                    0b01111111 -> {
                        // read column 7
                        val presses =
                            (if (KeyEvent.VK_ESCAPE in hostKeyPresses) 0b10000000 else 0) or        // esc -> STOP
                            (if (KeyEvent.VK_Q in hostKeyPresses) 0b01000000 else 0) or
                            (if (KeyEvent.VK_ALT in hostKeyPresses) 0b00100000 else 0) or           // alt -> Commodore
                            (if (KeyEvent.VK_SPACE in hostKeyPresses) 0b00010000 else 0) or
                            (if (KeyEvent.VK_2 in hostKeyPresses) 0b00001000 else 0) or
                            (if (KeyEvent.VK_CONTROL in hostKeyPresses) 0b00000100 else 0) or
                            (if (KeyEvent.VK_BACK_QUOTE in hostKeyPresses) 0b00000010 else 0) or        // '`' -> left arrow
                            (if (KeyEvent.VK_1 in hostKeyPresses) 0b00000001 else 0)
                        (presses.inv() and 255).toShort()
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

    fun hostKeyPressed(event: KeyEvent) {
        if(event.id==KeyEvent.KEY_PRESSED)
            hostKeyPresses.add(event.keyCode)
        else if(event.id==KeyEvent.KEY_RELEASED)
            hostKeyPresses.remove(event.keyCode)
    }
}
