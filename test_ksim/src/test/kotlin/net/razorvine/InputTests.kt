package net.razorvine

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class InputTests {
    @Test
    fun `test input reading`() {
        val serial = CapturingSerialIO(
            printToStdout = false, 
            initialInput = "hello".toByteArray().map { it.toUByte() }
        )
        
        assertEquals('h'.code.toUByte(), serial.read())
        assertEquals('e'.code.toUByte(), serial.read())
        assertEquals('l'.code.toUByte(), serial.read())
        assertEquals('l'.code.toUByte(), serial.read())
        assertEquals('o'.code.toUByte(), serial.read())
    }

    @Test
    fun `test input reading empty throws`() {
        val serial = CapturingSerialIO(printToStdout = false)
        kotlin.test.assertFailsWith<IllegalStateException> {
            serial.read()
        }
    }
}
