package razorvine.c64emu

import razorvine.ksim65.Cpu6502
import razorvine.ksim65.components.Address
import razorvine.ksim65.components.MemMappedComponent
import razorvine.ksim65.components.UByte
import java.awt.event.KeyEvent

/**
 * Minimal simulation of the MOS 6526 CIA chip.
 * Depending on what CIA it is (1 or 2), some registers do different things on the C64.
 * This implementation provides a working keyboard matrix, TOD clock, and the essentials of the timer A and B.
 */
class Cia(val number: Int, startAddress: Address, endAddress: Address, val cpu: Cpu6502) : MemMappedComponent(startAddress, endAddress) {
    private var ramBuffer = Array<UByte>(endAddress-startAddress+1) { 0 }
    private var regPRA = 0xff

    class TimeOfDay {
        private var updatedAt = 0L
        private var startedAt = 0L
        private var userStartTime = 0.0
        private var running = false
        var hours = 0
        var minutes = 0
        var seconds = 0
        var tenths = 0
        private var latchedTime = 0L
        private var latched = false

        fun start() {
            if (!running) {
                updatedAt = System.currentTimeMillis()
                startedAt = updatedAt
                userStartTime = hours*3600+minutes*60+seconds+tenths/10.0
                running = true
            }
            latch(false)
        }

        fun stop() {
            running = false
        }

        fun latch(latched: Boolean) {
            this.latched = latched
            update()
        }

        fun update() {
            if (!running || latched) return
            latchedTime = System.currentTimeMillis()
            if (updatedAt != latchedTime) {
                updatedAt = latchedTime
                var elapsedSeconds = (latchedTime.toDouble()-startedAt)/1000.0+userStartTime
                hours = (elapsedSeconds/3600).toInt()
                elapsedSeconds -= hours*3600
                minutes = (elapsedSeconds/60).toInt()
                elapsedSeconds -= minutes*60
                seconds = (elapsedSeconds).toInt()
                elapsedSeconds -= seconds
                tenths = (elapsedSeconds*10).toInt()
            }
        }
    }

    private var totalCycles = 0
    private var tod = TimeOfDay()
    private var timerAset = 0
    private var timerBset = 0
    private var timerAactual = 0
    private var timerBactual = 0
    private var timerAinterruptEnabled = false
    private var timerBinterruptEnabled = false

    private data class HostKeyPress(val code: Int, val rightSide: Boolean = false, val numpad: Boolean = false)

    private val hostKeyPresses = mutableSetOf<HostKeyPress>()

    init {
        require(endAddress-startAddress+1 == 256) { "cia requires exactly 256 memory bytes (16*16 mirrored)" }
    }

    override fun clock() {
        totalCycles++

        if (totalCycles%20000 == 0) {
            // TOD resolution is 0.1 second, no need to update it in every cycle
            tod.update()
        }

        if (ramBuffer[0x0e].toInt() and 1 != 0) {
            // timer A is enabled, assume system cycles counting for now
            timerAactual--
            if (timerAactual == 0 && timerAinterruptEnabled) {
                if (number == 1) cpu.irq()
                else if (number == 2) cpu.nmi()
            }
            if (timerAactual < 0) timerAactual = if (ramBuffer[0x0e].toInt() and 0b00001000 != 0) 0 else timerAset
        }
        if (ramBuffer[0x0f].toInt() and 1 != 0) {
            // timer B is enabled
            val regCRB = ramBuffer[0x0f].toInt()
            if (regCRB and 0b01000000 != 0) {
                // timer B counts timer A underruns
                if (timerAactual == 0) timerBactual--
            } else {
                // timer B counts just the system cycles
                timerBactual--
            }
            if (timerBactual == 0 && timerBinterruptEnabled) {
                if (number == 1) cpu.irq()
                else if (number == 2) cpu.nmi()
            }
            if (timerBactual < 0) timerBactual = if (regCRB and 0b00001000 != 0) 0 else timerBset
        }
    }

    override fun reset() {
        hostKeyPresses.clear()
        tod.stop()
        ramBuffer.fill(0)
        timerAactual = 0
        timerAset = 0
        timerBactual = 0
        timerBset = 0
    }

    override operator fun get(offset: Int): UByte {
        fun scanColumn(vararg keys: HostKeyPress): UByte {
            var bits = 0b10000000
            var presses = 0
            for (key in keys) {
                if (key in hostKeyPresses) presses = presses or bits
                bits = bits ushr 1
            }
            return (presses.inv() and 255).toShort()
        }

        val register = offset and 15
        if (number == 1 && register == 0x01) {
            // register 1 on CIA#1 is the keyboard data port
            // if bit is cleared in PRA, contains keys pressed in that column of the matrix
            return when (regPRA) {
                0b00000000 -> {
                    // check if any keys are pressed at all (by checking all columns at once)
                    if (hostKeyPresses.isEmpty()) 0xff.toShort() else 0x00.toShort()
                }
                0b11111110 -> {
                    // read column 0
                    scanColumn(HostKeyPress(KeyEvent.VK_DOWN), HostKeyPress(KeyEvent.VK_F5), HostKeyPress(KeyEvent.VK_F3),
                               HostKeyPress(KeyEvent.VK_F1), HostKeyPress(KeyEvent.VK_F7), HostKeyPress(KeyEvent.VK_RIGHT),
                               HostKeyPress(KeyEvent.VK_ENTER), HostKeyPress(KeyEvent.VK_BACK_SPACE))
                }
                0b11111101 -> {
                    // read column 1
                    scanColumn(HostKeyPress(KeyEvent.VK_SHIFT),     // left shift
                               HostKeyPress(KeyEvent.VK_E), HostKeyPress(KeyEvent.VK_S), HostKeyPress(KeyEvent.VK_Z),
                               HostKeyPress(KeyEvent.VK_4), HostKeyPress(KeyEvent.VK_A), HostKeyPress(KeyEvent.VK_W),
                               HostKeyPress(KeyEvent.VK_3))
                }
                0b11111011 -> {
                    // read column 2
                    scanColumn(HostKeyPress(KeyEvent.VK_X), HostKeyPress(KeyEvent.VK_T), HostKeyPress(KeyEvent.VK_F),
                               HostKeyPress(KeyEvent.VK_C), HostKeyPress(KeyEvent.VK_6), HostKeyPress(KeyEvent.VK_D),
                               HostKeyPress(KeyEvent.VK_R), HostKeyPress(KeyEvent.VK_5))
                }
                0b11110111 -> {
                    // read column 3
                    scanColumn(HostKeyPress(KeyEvent.VK_V), HostKeyPress(KeyEvent.VK_U), HostKeyPress(KeyEvent.VK_H),
                               HostKeyPress(KeyEvent.VK_B), HostKeyPress(KeyEvent.VK_8), HostKeyPress(KeyEvent.VK_G),
                               HostKeyPress(KeyEvent.VK_Y), HostKeyPress(KeyEvent.VK_7))
                }
                0b11101111 -> {
                    // read column 4
                    scanColumn(HostKeyPress(KeyEvent.VK_N), HostKeyPress(KeyEvent.VK_O), HostKeyPress(KeyEvent.VK_K),
                               HostKeyPress(KeyEvent.VK_M), HostKeyPress(KeyEvent.VK_0), HostKeyPress(KeyEvent.VK_J),
                               HostKeyPress(KeyEvent.VK_I), HostKeyPress(KeyEvent.VK_9))
                }
                0b11011111 -> {
                    // read column 5
                    scanColumn(HostKeyPress(KeyEvent.VK_COMMA), HostKeyPress(KeyEvent.VK_AT), HostKeyPress(KeyEvent.VK_COLON),
                               HostKeyPress(KeyEvent.VK_PERIOD), HostKeyPress(KeyEvent.VK_MINUS), HostKeyPress(KeyEvent.VK_L),
                               HostKeyPress(KeyEvent.VK_P), HostKeyPress(KeyEvent.VK_PLUS))
                }
                0b10111111 -> {
                    // read column 6
                    scanColumn(HostKeyPress(KeyEvent.VK_SLASH), HostKeyPress(KeyEvent.VK_CIRCUMFLEX), HostKeyPress(KeyEvent.VK_EQUALS),
                               HostKeyPress(KeyEvent.VK_SHIFT, rightSide = true), // right shift
                               HostKeyPress(KeyEvent.VK_HOME), HostKeyPress(KeyEvent.VK_SEMICOLON), HostKeyPress(KeyEvent.VK_ASTERISK),
                               HostKeyPress(KeyEvent.VK_DEAD_TILDE)     // pound sign
                    )
                }
                0b01111111 -> {
                    // read column 7
                    scanColumn(HostKeyPress(KeyEvent.VK_ESCAPE), HostKeyPress(KeyEvent.VK_Q), HostKeyPress(KeyEvent.VK_ALT),
                               HostKeyPress(KeyEvent.VK_SPACE), HostKeyPress(KeyEvent.VK_2), HostKeyPress(KeyEvent.VK_CONTROL),
                               HostKeyPress(KeyEvent.VK_BACK_QUOTE), HostKeyPress(KeyEvent.VK_1))
                }
                else -> {
                    // invalid column selection
                    0xff
                }
            }
        } else ramBuffer[register]

        return when (register) {
            0x04 -> (timerAactual and 0xff).toShort()
            0x05 -> (timerAactual ushr 8).toShort()
            0x06 -> (timerBactual and 0xff).toShort()
            0x07 -> (timerBactual ushr 8).toShort()
            0x08 -> {
                tod.start()
                (tod.tenths and 0x0f).toShort()
            }
            0x09 -> toBCD(tod.seconds)
            0x0a -> toBCD(tod.minutes)
            0x0b -> {
                tod.latch(true)
                toBCD(tod.hours)
            }
            else -> ramBuffer[register]
        }
    }

    private fun toBCD(data: Int): UByte {
        val tens = data/10
        val ones = data-tens*10
        return ((tens shl 4) or ones).toShort()
    }

    private fun fromBCD(bcd: UByte): Int {
        val ibcd = bcd.toInt()
        val tens = ibcd ushr 4
        val ones = ibcd and 0x0f
        return tens*10+ones
    }

    override operator fun set(offset: Int, data: UByte) {
        val register = offset and 15
        if (number == 1 && register == 0x00) {
            // PRA data port A (select keyboard matrix column)
            regPRA = data.toInt()
        }

        if (register != 0x0d) ramBuffer[register] = data

        when (register) {
            0x04 -> {
                if (ramBuffer[0x0e].toInt() and 0b10000000 == 0) {
                    timerAset = (timerAset and 0xff00) or data.toInt()
                    timerAactual = timerAset
                }
            }
            0x05 -> {
                if (ramBuffer[0x0e].toInt() and 0b10000000 == 0) {
                    timerAset = (timerAset and 0x00ff) or (data.toInt() shl 8)
                    timerAactual = timerAset
                }
            }
            0x06 -> {
                if (ramBuffer[0x0f].toInt() and 0b10000000 == 0) {
                    timerBset = (timerBset and 0xff00) or data.toInt()
                    timerBactual = timerBset
                }
            }
            0x07 -> {
                if (ramBuffer[0x0f].toInt() and 0b10000000 == 0) {
                    timerBset = (timerBset and 0x00ff) or (data.toInt() shl 8)
                    timerBactual = timerBset
                }
            }
            0x08 -> tod.tenths = data.toInt() and 0x0f
            0x09 -> tod.seconds = fromBCD(data)
            0x0a -> tod.minutes = fromBCD(data)
            0x0b -> {
                tod.stop()
                tod.hours = fromBCD(data)
            }
            0x0d -> {
                if (data.toInt() and 0b10000000 != 0) {
                    // set ICR bits
                    val newICR = ramBuffer[0x0d].toInt() or (data.toInt() and 0b01111111)
                    timerAinterruptEnabled = newICR and 1 != 0
                    timerBinterruptEnabled = newICR and 2 != 0
                    ramBuffer[0x0d] = newICR.toShort()
                } else {
                    // clear ICR bits
                    val newICR = ramBuffer[0x0d].toInt() and (data.toInt() and 0b01111111).inv()
                    timerAinterruptEnabled = newICR and 1 != 0
                    timerBinterruptEnabled = newICR and 2 != 0
                    ramBuffer[0x0d] = newICR.toShort()
                }
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
        // (this can happen because we're changing the key code for some pressed keys below,
        // and a released key doesn't always match the pressed key code anymore then)
        if (event.id == KeyEvent.KEY_RELEASED && event.keyCode in listOf(KeyEvent.VK_SHIFT, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT,
                                                                         KeyEvent.VK_ALT_GRAPH)
        ) hostKeyPresses.clear()

        // try to remap the keys a bit so a modern PC keyboard maps better to the keys of the C64
        when (event.keyChar) {
            '@' -> {
                unregister(KeyEvent.VK_SHIFT)
                register(event.id, HostKeyPress(KeyEvent.VK_AT))
            }
            '^' -> {
                unregister(KeyEvent.VK_SHIFT)
                register(event.id, HostKeyPress(KeyEvent.VK_CIRCUMFLEX))
            }
            '*' -> {
                unregister(KeyEvent.VK_SHIFT)
                register(event.id, HostKeyPress(KeyEvent.VK_ASTERISK))
            }
            '+' -> {
                unregister(KeyEvent.VK_SHIFT)
                register(event.id, HostKeyPress(KeyEvent.VK_PLUS))
            }
            ':' -> {
                unregister(KeyEvent.VK_SHIFT)
                register(event.id, HostKeyPress(KeyEvent.VK_COLON))
            }
            '~' -> {
                unregister(KeyEvent.VK_SHIFT)
                register(event.id, HostKeyPress(KeyEvent.VK_DEAD_TILDE))
            }
            '&' -> register(event.id, shift, HostKeyPress(KeyEvent.VK_6))
            '(' -> register(event.id, shift, HostKeyPress(KeyEvent.VK_8))
            ')' -> register(event.id, shift, HostKeyPress(KeyEvent.VK_9))
            '[' -> register(event.id, shift, HostKeyPress(KeyEvent.VK_COLON))
            ']' -> register(event.id, shift, HostKeyPress(KeyEvent.VK_SEMICOLON))
            '"' -> register(event.id, HostKeyPress(KeyEvent.VK_2))
            '\'' -> register(event.id, shift, HostKeyPress(KeyEvent.VK_7))
            else -> // F2 = shift+F1
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
