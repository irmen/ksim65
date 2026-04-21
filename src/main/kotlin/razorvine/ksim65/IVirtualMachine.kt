package razorvine.ksim65

import razorvine.ksim65.components.Address
import java.io.File

interface IVirtualMachine {
    fun step()
    fun pause(paused: Boolean)
    fun reset()
    fun getZeroAndStackPages(): Array<UByte>
    fun loadFileInRam(file: File, loadAddress: Address?)

    class MonitorCmdResult(val output: String, val prompt: String, val echo: Boolean)

    fun executeMonitorCommand(command: String): MonitorCmdResult

    val cpu: Cpu6502Core
    val bus: Bus
}
