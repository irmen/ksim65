package razorvine.ksim65

interface IVirtualMachine {
    fun step()
    fun pause(paused: Boolean)

    val cpu: Cpu6502
    val bus: Bus
}
