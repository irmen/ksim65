package razorvine.ksim65

import razorvine.ksim65.components.Address

fun hexW(number: Address, allowSingleByte: Boolean = false): String {
    val msb = number ushr 8
    val lsb = number and 0xff
    return if (msb == 0 && allowSingleByte)
        hexB(lsb)
    else
        hexB(msb) + hexB(lsb)
}

fun hexB(number: Short): String = hexB(number.toInt())

fun hexB(number: Int): String {
    val hexdigits = "0123456789abcdef"
    val loNibble = number and 15
    val hiNibble = number ushr 4
    return hexdigits[hiNibble].toString() + hexdigits[loNibble]
}

typealias BreakpointHandler = (cpu: Cpu6502, pc: Address) -> Cpu6502.BreakpointResultAction
