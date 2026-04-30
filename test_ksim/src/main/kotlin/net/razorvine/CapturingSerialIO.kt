package net.razorvine

import razorvine.ksim65.testing.IHostSerialAndPowerIO
import java.util.ArrayDeque

class CapturingSerialIO(
    private val printToStdout: Boolean = true,
    initialInput: List<UByte> = emptyList()
) : IHostSerialAndPowerIO {
    private val buffer = StringBuilder()
    private val inputBuffer = ArrayDeque<UByte>(initialInput.toList())

    override fun write(byte: UByte) {
        val char = byte.toInt().toChar()
        buffer.append(char)
        if (printToStdout) {
            print(char)
        }
    }

    override fun read(): UByte {
        return if (inputBuffer.isEmpty()) {
            throw IllegalStateException("No input available in serial buffer")
        } else {
            inputBuffer.removeFirst()
        }
    }

    override fun reset() {
        throw ResetMachine()
    }

    override fun poweroff() {
        throw PoweroffMachine()
    }

    fun getOutput(): String = buffer.toString()

    fun assertOutputEquals(expected: String) {
        check(buffer.toString() == expected) { "Expected: '$expected', but was: '$buffer'" }
    }

    class ResetMachine : Exception()
    class PoweroffMachine : Exception()
}
