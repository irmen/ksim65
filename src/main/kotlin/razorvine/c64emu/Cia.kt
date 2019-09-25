package razorvine.c64emu

import razorvine.ksim65.components.Address
import razorvine.ksim65.components.MemMappedComponent
import razorvine.ksim65.components.UByte
import java.awt.event.KeyEvent

/**
 * Minimal simulation of the MOS 6526 CIA chip.
 * Depending on what CIA it is (1 or 2), some registers do different things on the C64.
 */
class Cia(val number: Int, startAddress: Address, endAddress: Address) : MemMappedComponent(startAddress, endAddress) {
    private var ramBuffer = Array<UByte>(endAddress - startAddress + 1) { 0x00 }
    private var pra = 0xff

    private data class HostKeyPress(val code: Int, val rightSide: Boolean, val numpad: Boolean)

    private val hostKeyPresses = mutableSetOf<HostKeyPress>()

    init {
        require(endAddress - startAddress + 1 == 256) { "cia requires exactly 256 memory bytes (16*16 mirrored)" }
    }

    override fun clock() {
        // TODO: TOD timer, timer A and B countdowns, IRQ triggering.
    }

    override fun reset() {
        hostKeyPresses.clear()
        // TODO: reset TOD timer, timer A and B
    }

    override fun get(address: Address): UByte {
        fun scanColumn(keys: List<HostKeyPress>): UByte {
            var bits = 0b10000000
            var presses = 0
            for (key in keys) {
                if (key in hostKeyPresses) presses = presses or bits
                bits = bits ushr 1
            }
            return (presses.inv() and 255).toShort()
        }

        val register = (address - startAddress) and 15
        if (number == 1) {
            return if (register == 0x01) {
                // register 1 on CIA#1 is the keyboard data port
                // if bit is cleared in PRA, contains keys pressed in that column of the matrix
                when (pra) {
                    0b00000000 -> {
                        // check if any keys are pressed at all (by checking all columns at once)
                        if (hostKeyPresses.isEmpty()) 0xff.toShort() else 0x00.toShort()
                    }
                    0b11111110 -> {
                        // read column 0
                        scanColumn(
                            listOf(
                                HostKeyPress(KeyEvent.VK_DOWN, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_F5, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_F3, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_F1, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_F7, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_RIGHT, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_ENTER, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_BACK_SPACE, rightSide = false, numpad = false)
                            )
                        )
                    }
                    0b11111101 -> {
                        // read column 1
                        scanColumn(
                            listOf(
                                HostKeyPress(KeyEvent.VK_SHIFT, rightSide = false, numpad = false),     // left shift
                                HostKeyPress(KeyEvent.VK_E, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_S, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_Z, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_4, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_A, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_W, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_3, rightSide = false, numpad = false)
                            )
                        )
                    }
                    0b11111011 -> {
                        // read column 2
                        scanColumn(
                            listOf(
                                HostKeyPress(KeyEvent.VK_X, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_T, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_F, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_C, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_6, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_D, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_R, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_5, rightSide = false, numpad = false)
                            )
                        )
                    }
                    0b11110111 -> {
                        // read column 3
                        scanColumn(
                            listOf(
                                HostKeyPress(KeyEvent.VK_V, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_U, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_H, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_B, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_8, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_G, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_Y, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_7, rightSide = false, numpad = false)
                            )
                        )
                    }
                    0b11101111 -> {
                        // read column 4
                        scanColumn(
                            listOf(
                                HostKeyPress(KeyEvent.VK_N, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_O, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_K, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_M, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_0, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_J, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_I, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_9, rightSide = false, numpad = false)
                            )
                        )
                    }
                    0b11011111 -> {
                        // read column 5
                        scanColumn(
                            listOf(
                                HostKeyPress(KeyEvent.VK_COMMA, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_AT, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_COLON, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_PERIOD, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_MINUS, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_L, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_P, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_PLUS, rightSide = false, numpad = false)
                            )
                        )
                    }
                    0b10111111 -> {
                        // read column 6
                        scanColumn(
                            listOf(
                                HostKeyPress(KeyEvent.VK_SLASH, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_CIRCUMFLEX, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_EQUALS, rightSide = false, numpad = false),
                                HostKeyPress(
                                    KeyEvent.VK_SHIFT,
                                    rightSide = true,
                                    numpad = false
                                ),              // right shift
                                HostKeyPress(KeyEvent.VK_HOME, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_SEMICOLON, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_ASTERISK, rightSide = false, numpad = false),
                                HostKeyPress(
                                    KeyEvent.VK_DEAD_TILDE,
                                    rightSide = false,
                                    numpad = false
                                )     // pound sign
                            )
                        )
                    }
                    0b01111111 -> {
                        // read column 7
                        scanColumn(
                            listOf(
                                HostKeyPress(KeyEvent.VK_ESCAPE, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_Q, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_ALT, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_SPACE, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_2, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_CONTROL, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_BACK_QUOTE, rightSide = false, numpad = false),
                                HostKeyPress(KeyEvent.VK_1, rightSide = false, numpad = false)
                            )
                        )
                    }
                    else -> {
                        // invalid column selection
                        0xff
                    }
                }
            } else ramBuffer[register]
        }

        // CIA #2 is not emulated yet
        return ramBuffer[register]
    }

    override fun set(address: Address, data: UByte) {
        val register = (address - startAddress) and 15
        ramBuffer[register] = data
        if (number == 1) {
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
        val rightSide = event.keyLocation == KeyEvent.KEY_LOCATION_RIGHT
        val numpad = event.keyLocation == KeyEvent.KEY_LOCATION_NUMPAD
        val shift = HostKeyPress(KeyEvent.VK_SHIFT, rightSide = true, numpad = false)

        fun register(eventId: Int, vararg keys: HostKeyPress) {
            if (eventId == KeyEvent.KEY_PRESSED) keys.forEach { hostKeyPresses.add(it) }
            else keys.forEach { hostKeyPresses.remove(it) }
        }

        fun unregister(keyCode: Int) {
            hostKeyPresses.removeAll { it.code == keyCode }
        }

        // to avoid some 'stuck' keys, if we receive a shift/control/alt RELEASE, we wipe the keyboard buffer
        // (this can happen becase we're changing the keycode for some pressed keys below,
        // and a released key doesn't always match the pressed keycode anymore then)
        if (event.id == KeyEvent.KEY_RELEASED && event.keyCode in listOf(
                KeyEvent.VK_SHIFT,
                KeyEvent.VK_CONTROL,
                KeyEvent.VK_ALT,
                KeyEvent.VK_ALT_GRAPH
            )
        ) hostKeyPresses.clear()

        // try to remap the keys a bit so a modern PC keyboard maps better to the keys of the C64
        when {
            event.keyChar == '@' -> {
                unregister(KeyEvent.VK_SHIFT)
                register(event.id, HostKeyPress(KeyEvent.VK_AT, rightSide = false, numpad = false))
            }
            event.keyChar == '^' -> {
                unregister(KeyEvent.VK_SHIFT)
                register(event.id, HostKeyPress(KeyEvent.VK_CIRCUMFLEX, rightSide = false, numpad = false))
            }
            event.keyChar == '&' -> {
                register(event.id, shift, HostKeyPress(KeyEvent.VK_6, rightSide = false, numpad = false))
            }
            event.keyChar == '*' -> {
                unregister(KeyEvent.VK_SHIFT)
                register(event.id, HostKeyPress(KeyEvent.VK_ASTERISK, rightSide = false, numpad = false))
            }
            event.keyChar == '(' -> {
                register(event.id, shift, HostKeyPress(KeyEvent.VK_8, rightSide = false, numpad = false))
            }
            event.keyChar == ')' -> {
                register(event.id, shift, HostKeyPress(KeyEvent.VK_9, rightSide = false, numpad = false))
            }
            event.keyChar == '[' -> {
                register(event.id, shift, HostKeyPress(KeyEvent.VK_COLON, rightSide = false, numpad = false))
            }
            event.keyChar == ']' -> {
                register(event.id, shift, HostKeyPress(KeyEvent.VK_SEMICOLON, rightSide = false, numpad = false))
            }
            event.keyChar == '+' -> {
                unregister(KeyEvent.VK_SHIFT)
                register(event.id, HostKeyPress(KeyEvent.VK_PLUS, rightSide = false, numpad = false))
            }
            event.keyChar == '"' -> {
                register(event.id, HostKeyPress(KeyEvent.VK_2, rightSide = false, numpad = false))
            }
            event.keyChar == ':' -> {
                unregister(KeyEvent.VK_SHIFT)
                register(event.id, HostKeyPress(KeyEvent.VK_COLON, rightSide = false, numpad = false))
            }
            event.keyChar == '~' -> {
                unregister(KeyEvent.VK_SHIFT)
                register(event.id, HostKeyPress(KeyEvent.VK_DEAD_TILDE, rightSide = false, numpad = false))
            }
            event.keyChar == '\'' -> {
                register(event.id, shift, HostKeyPress(KeyEvent.VK_7, rightSide = false, numpad = false))
            }
            else -> when (event.keyCode) {
                KeyEvent.VK_CONTROL, KeyEvent.VK_TAB -> {
                    // both controls and the tab key map to the 'single left control'
                    register(event.id, HostKeyPress(KeyEvent.VK_CONTROL, rightSide = false, numpad = false))
                }
                KeyEvent.VK_ALT, KeyEvent.VK_ALT_GRAPH -> {
                    // both alts map to the 'commodore key' (same left alt)
                    register(event.id, HostKeyPress(KeyEvent.VK_ALT, rightSide = false, numpad = false))
                }
                KeyEvent.VK_F2 -> {
                    // F2 = shift+F1
                    val func = HostKeyPress(KeyEvent.VK_F1, rightSide = false, numpad = false)
                    register(event.id, shift, func)
                }
                KeyEvent.VK_F4 -> {
                    // F4 = shift+F3
                    val func = HostKeyPress(KeyEvent.VK_F3, rightSide = false, numpad = false)
                    register(event.id, shift, func)
                }
                KeyEvent.VK_F6 -> {
                    // F6 = shift+F5
                    val func = HostKeyPress(KeyEvent.VK_F5, rightSide = false, numpad = false)
                    register(event.id, shift, func)
                }
                KeyEvent.VK_F8 -> {
                    // F8 = shift+F7
                    val func = HostKeyPress(KeyEvent.VK_F7, rightSide = false, numpad = false)
                    register(event.id, shift, func)
                }
                KeyEvent.VK_INSERT -> {
                    // insert = shift+backspace(del)
                    val backspace = HostKeyPress(KeyEvent.VK_BACK_SPACE, rightSide = false, numpad = false)
                    register(event.id, shift, backspace)
                }
                KeyEvent.VK_UP -> {
                    // up = shift+down
                    val cursor = HostKeyPress(KeyEvent.VK_DOWN, rightSide = false, numpad = false)
                    register(event.id, shift, cursor)
                }
                KeyEvent.VK_LEFT -> {
                    // left = shift+right
                    val cursor = HostKeyPress(KeyEvent.VK_RIGHT, rightSide = false, numpad = false)
                    register(event.id, shift, cursor)
                }
                else -> {
                    // just map the key as usual
                    val hostkey = HostKeyPress(event.keyCode, rightSide, numpad)
                    register(event.id, hostkey)
                }
            }
        }
    }
}
