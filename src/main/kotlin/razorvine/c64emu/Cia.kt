package razorvine.c64emu

import razorvine.ksim65.components.Address
import razorvine.ksim65.components.MemMappedComponent
import razorvine.ksim65.components.UByte
import java.awt.event.KeyEvent

/**
 * Minimal simulation of the MOS 6526 CIA chip.
 * Depending on what CIA it is (1 or 2), some registers do different things on the C64.
 * TODO implement at least Timer A, so that RND(0) starts working.
 */
class Cia(val number: Int, startAddress: Address, endAddress: Address) : MemMappedComponent(startAddress, endAddress) {
    private var ramBuffer = Array<UByte>(endAddress - startAddress + 1) { 0x00 }
    private var pra = 0xff

    class TimeOfDay {
        private var updatedAt = 0L
        private var startedAt = 0L
        private var userStartTime = 0.0
        private var running = false
        var hours = 0
        var minutes = 0
        var seconds = 0
        var tenths = 0

        fun start() {
            if (!running) {
                updatedAt = System.currentTimeMillis()
                startedAt = updatedAt
                userStartTime = hours * 3600 + minutes * 60 + seconds + tenths / 10.0
                running = true
            }
        }

        fun stop() {
            running = false
        }

        fun update() {
            val currentTime = System.currentTimeMillis()
            if (running && updatedAt != currentTime) {
                updatedAt = currentTime
                var elapsedSeconds = (currentTime.toDouble() - startedAt) / 1000.0 + userStartTime
                hours = (elapsedSeconds / 3600).toInt()
                elapsedSeconds -= hours * 3600
                minutes = (elapsedSeconds / 60).toInt()
                elapsedSeconds -= minutes * 60
                seconds = (elapsedSeconds).toInt()
                elapsedSeconds -= seconds
                tenths = (elapsedSeconds * 10).toInt()
            }
        }
    }

    private var tod = TimeOfDay()

    private data class HostKeyPress(val code: Int, val rightSide: Boolean=false, val numpad: Boolean=false)

    private val hostKeyPresses = mutableSetOf<HostKeyPress>()

    init {
        require(endAddress - startAddress + 1 == 256) { "cia requires exactly 256 memory bytes (16*16 mirrored)" }
    }

    override fun clock() {
        // TODO: timer A and B countdowns, IRQ triggering.
        tod.update()
    }

    override fun reset() {
        hostKeyPresses.clear()
        tod.stop()
        // TODO: reset timer A and B
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
            if (register == 0x01) {
                // register 1 on CIA#1 is the keyboard data port
                // if bit is cleared in PRA, contains keys pressed in that column of the matrix
                return when (pra) {
                    0b00000000 -> {
                        // check if any keys are pressed at all (by checking all columns at once)
                        if (hostKeyPresses.isEmpty()) 0xff.toShort() else 0x00.toShort()
                    }
                    0b11111110 -> {
                        // read column 0
                        scanColumn(
                            listOf(
                                HostKeyPress(KeyEvent.VK_DOWN),
                                HostKeyPress(KeyEvent.VK_F5),
                                HostKeyPress(KeyEvent.VK_F3),
                                HostKeyPress(KeyEvent.VK_F1),
                                HostKeyPress(KeyEvent.VK_F7),
                                HostKeyPress(KeyEvent.VK_RIGHT),
                                HostKeyPress(KeyEvent.VK_ENTER),
                                HostKeyPress(KeyEvent.VK_BACK_SPACE)
                            )
                        )
                    }
                    0b11111101 -> {
                        // read column 1
                        scanColumn(
                            listOf(
                                HostKeyPress(KeyEvent.VK_SHIFT),     // left shift
                                HostKeyPress(KeyEvent.VK_E),
                                HostKeyPress(KeyEvent.VK_S),
                                HostKeyPress(KeyEvent.VK_Z),
                                HostKeyPress(KeyEvent.VK_4),
                                HostKeyPress(KeyEvent.VK_A),
                                HostKeyPress(KeyEvent.VK_W),
                                HostKeyPress(KeyEvent.VK_3)
                            )
                        )
                    }
                    0b11111011 -> {
                        // read column 2
                        scanColumn(
                            listOf(
                                HostKeyPress(KeyEvent.VK_X),
                                HostKeyPress(KeyEvent.VK_T),
                                HostKeyPress(KeyEvent.VK_F),
                                HostKeyPress(KeyEvent.VK_C),
                                HostKeyPress(KeyEvent.VK_6),
                                HostKeyPress(KeyEvent.VK_D),
                                HostKeyPress(KeyEvent.VK_R),
                                HostKeyPress(KeyEvent.VK_5)
                            )
                        )
                    }
                    0b11110111 -> {
                        // read column 3
                        scanColumn(
                            listOf(
                                HostKeyPress(KeyEvent.VK_V),
                                HostKeyPress(KeyEvent.VK_U),
                                HostKeyPress(KeyEvent.VK_H),
                                HostKeyPress(KeyEvent.VK_B),
                                HostKeyPress(KeyEvent.VK_8),
                                HostKeyPress(KeyEvent.VK_G),
                                HostKeyPress(KeyEvent.VK_Y),
                                HostKeyPress(KeyEvent.VK_7)
                            )
                        )
                    }
                    0b11101111 -> {
                        // read column 4
                        scanColumn(
                            listOf(
                                HostKeyPress(KeyEvent.VK_N),
                                HostKeyPress(KeyEvent.VK_O),
                                HostKeyPress(KeyEvent.VK_K),
                                HostKeyPress(KeyEvent.VK_M),
                                HostKeyPress(KeyEvent.VK_0),
                                HostKeyPress(KeyEvent.VK_J),
                                HostKeyPress(KeyEvent.VK_I),
                                HostKeyPress(KeyEvent.VK_9)
                            )
                        )
                    }
                    0b11011111 -> {
                        // read column 5
                        scanColumn(
                            listOf(
                                HostKeyPress(KeyEvent.VK_COMMA),
                                HostKeyPress(KeyEvent.VK_AT),
                                HostKeyPress(KeyEvent.VK_COLON),
                                HostKeyPress(KeyEvent.VK_PERIOD),
                                HostKeyPress(KeyEvent.VK_MINUS),
                                HostKeyPress(KeyEvent.VK_L),
                                HostKeyPress(KeyEvent.VK_P),
                                HostKeyPress(KeyEvent.VK_PLUS)
                            )
                        )
                    }
                    0b10111111 -> {
                        // read column 6
                        scanColumn(
                            listOf(
                                HostKeyPress(KeyEvent.VK_SLASH),
                                HostKeyPress(KeyEvent.VK_CIRCUMFLEX),
                                HostKeyPress(KeyEvent.VK_EQUALS),
                                HostKeyPress(KeyEvent.VK_SHIFT, rightSide = true), // right shift
                                HostKeyPress(KeyEvent.VK_HOME),
                                HostKeyPress(KeyEvent.VK_SEMICOLON),
                                HostKeyPress(KeyEvent.VK_ASTERISK),
                                HostKeyPress(KeyEvent.VK_DEAD_TILDE)     // pound sign
                            )
                        )
                    }
                    0b01111111 -> {
                        // read column 7
                        scanColumn(
                            listOf(
                                HostKeyPress(KeyEvent.VK_ESCAPE),
                                HostKeyPress(KeyEvent.VK_Q),
                                HostKeyPress(KeyEvent.VK_ALT),
                                HostKeyPress(KeyEvent.VK_SPACE),
                                HostKeyPress(KeyEvent.VK_2),
                                HostKeyPress(KeyEvent.VK_CONTROL),
                                HostKeyPress(KeyEvent.VK_BACK_QUOTE),
                                HostKeyPress(KeyEvent.VK_1)
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


        return when (register) {
            0x08 -> {
                tod.start()
                (tod.tenths and 0x0f).toShort()
            }
            0x09 -> {
                toBCD(tod.seconds)
            }
            0x0a -> {
                toBCD(tod.minutes)
            }
            0x0b -> {
                val hours = toBCD(tod.hours)
                if (tod.hours >= 12)
                    (hours.toInt() or 0x10000000).toShort()
                else
                    hours
            }
            else -> ramBuffer[register]
        }
    }

    private fun toBCD(data: Int): UByte {
        val tens = data / 10
        val ones = data - tens * 10
        return ((tens shl 4) or ones).toShort()
    }

    private fun fromBCD(bcd: UByte): Int {
        val ibcd = bcd.toInt()
        val tens = ibcd ushr 4
        val ones = ibcd and 0x0f
        return tens * 10 + ones
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

        when (register) {
            0x08 -> {
                tod.tenths = data.toInt() and 0x0f
            }
            0x09 -> {
                tod.seconds = fromBCD(data)
            }
            0x0a -> {
                tod.minutes = fromBCD(data)
            }
            0x0b -> {
                tod.stop()
                tod.hours = fromBCD(data)
                if (data >= 12)
                    tod.hours = tod.hours or 0b10000000
            }
        }
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
                register(event.id, HostKeyPress(KeyEvent.VK_AT))
            }
            event.keyChar == '^' -> {
                unregister(KeyEvent.VK_SHIFT)
                register(event.id, HostKeyPress(KeyEvent.VK_CIRCUMFLEX))
            }
            event.keyChar == '*' -> {
                unregister(KeyEvent.VK_SHIFT)
                register(event.id, HostKeyPress(KeyEvent.VK_ASTERISK))
            }
            event.keyChar == '+' -> {
                unregister(KeyEvent.VK_SHIFT)
                register(event.id, HostKeyPress(KeyEvent.VK_PLUS))
            }
            event.keyChar == ':' -> {
                unregister(KeyEvent.VK_SHIFT)
                register(event.id, HostKeyPress(KeyEvent.VK_COLON))
            }
            event.keyChar == '~' -> {
                unregister(KeyEvent.VK_SHIFT)
                register(event.id, HostKeyPress(KeyEvent.VK_DEAD_TILDE))
            }
            event.keyChar == '&' -> register(event.id, shift, HostKeyPress(KeyEvent.VK_6))
            event.keyChar == '(' -> register(event.id, shift, HostKeyPress(KeyEvent.VK_8))
            event.keyChar == ')' -> register(event.id, shift, HostKeyPress(KeyEvent.VK_9))
            event.keyChar == '[' -> register(event.id, shift, HostKeyPress(KeyEvent.VK_COLON))
            event.keyChar == ']' -> register(event.id, shift, HostKeyPress(KeyEvent.VK_SEMICOLON))
            event.keyChar == '"' -> register(event.id, HostKeyPress(KeyEvent.VK_2))
            event.keyChar == '\'' -> register(event.id, shift, HostKeyPress(KeyEvent.VK_7))
            else ->
                // F2 = shift+F1
                // F4 = shift+F3
                // F6 = shift+F5
                // F8 = shift+F7
                // insert = shift+backspace(del)
                // up = shift+down
                // left = shift+right
                // just map the key as usual
                // both alts map to the 'commodore key' (same left alt)
                // both controls and the tab key map to the 'single left control'
                when (event.keyCode) {
                    KeyEvent.VK_CONTROL, KeyEvent.VK_TAB -> register(event.id, HostKeyPress(KeyEvent.VK_CONTROL))
                    KeyEvent.VK_ALT, KeyEvent.VK_ALT_GRAPH -> register(event.id, HostKeyPress(KeyEvent.VK_ALT))
                    KeyEvent.VK_F2 -> register(event.id, shift, HostKeyPress(KeyEvent.VK_F1))
                    KeyEvent.VK_F4 -> register(event.id, shift, HostKeyPress(KeyEvent.VK_F3))
                    KeyEvent.VK_F6 -> register(event.id, shift, HostKeyPress(KeyEvent.VK_F5))
                    KeyEvent.VK_F8 -> register(event.id, shift, HostKeyPress(KeyEvent.VK_F7))
                    KeyEvent.VK_INSERT -> register(event.id, shift, HostKeyPress(KeyEvent.VK_BACK_SPACE))
                    KeyEvent.VK_UP -> register(event.id, shift, HostKeyPress(KeyEvent.VK_DOWN))
                    KeyEvent.VK_LEFT -> register(event.id, shift, HostKeyPress(KeyEvent.VK_RIGHT))
                    else -> register(event.id, HostKeyPress(event.keyCode, rightSide, numpad))
                }
        }
    }
}
