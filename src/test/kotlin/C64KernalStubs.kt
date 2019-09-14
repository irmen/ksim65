import razorvine.ksim65.components.Address
import razorvine.ksim65.Cpu6502
import razorvine.ksim65.components.Ram


class C64KernalStubs(private val ram: Ram) {

    fun handleBreakpoint(cpu: Cpu6502, pc: Address): Cpu6502.BreakpointResult {
        when(pc) {
            0xffd2 -> {
                // CHROUT
                ram[0x030c] = 0
                val char = cpu.regA.toChar()
                if(char==13.toChar())
                    println()
                else if(char in ' '..'~')
                    print(char)
                return Cpu6502.BreakpointResult(null, 0x60)     // perform an RTS to exit this subroutine
            }
            0xffe4 -> {
                // GETIN
                throw KernalInputRequired()
//                print("[Input required:] ")
//                val s = readLine()
//                if(s.isNullOrEmpty())
//                    cpu.A = 3
//                else
//                    cpu.A = Petscii.encodePetscii(s, true).first().toInt()
//                cpu.currentOpcode = 0x60    // rts to end the stub
            }
            0xe16f -> {
                throw KernalLoadNextPart()
                // LOAD/VERIFY
//                val loc = ram[0xbb].toInt() or (ram[0xbc].toInt() shl 8)
//                val len = ram[0xb7].toInt()
//                val filename = Petscii.decodePetscii((loc until loc + len).map { ram[it] }.toList(), true).toLowerCase()
//                ram.loadPrg("test/6502testsuite/$filename")
//                cpu.popStackAddr()
//                cpu.PC = 0x0816     // continue in next module
            }
        }

        return Cpu6502.BreakpointResult(null, null)
    }
}

internal class KernalLoadNextPart: Exception()
internal class KernalInputRequired: Exception()

