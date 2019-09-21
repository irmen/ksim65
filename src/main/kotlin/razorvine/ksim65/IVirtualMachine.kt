package razorvine.ksim65

import razorvine.ksim65.components.UByte

interface IVirtualMachine {
    fun step()
    fun pause(paused: Boolean)
    fun getZeroAndStackPages(): Array<UByte>

    val cpu: Cpu6502
    val bus: Bus
}
