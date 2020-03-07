package razorvine.ksim65

import razorvine.ksim65.components.Address

fun hexW(number: Int, allowSingleByte: Boolean = false) =
        if(allowSingleByte && (number and 0xff00 == 0)) {
            number.toString(16).padStart(2, '0')
        } else {
            number.toString(16).padStart(4, '0')
        }


fun hexB(number: Int) = number.toString(16).padStart(2, '0')

fun hexB(number: Short) = hexB(number.toInt())

typealias BreakpointHandler = (cpu: Cpu6502, pc: Address) -> Cpu6502.BreakpointResultAction
