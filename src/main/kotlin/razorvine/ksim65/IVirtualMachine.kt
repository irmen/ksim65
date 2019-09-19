package razorvine.ksim65

interface IVirtualMachine {
    fun stepInstruction()

    var paused: Boolean
    val cpu: Cpu6502
    val bus: Bus
}
