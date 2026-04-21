package razorvine.ksim65.testing

import razorvine.ksim65.Bus
import razorvine.ksim65.Cpu6502
import razorvine.ksim65.Cpu6502Core
import razorvine.ksim65.Cpu65C02
import razorvine.ksim65.IVirtualMachine
import razorvine.ksim65.components.*
import java.io.File


enum class CpuType {
    CPU6502,    // NMOS 6502 with illegal opcodes
    CPU65C02    // CMOS 65C02 (WDC)
}

const val DEFAULT_TIMER_ADDRESS = 0xf000
const val DEFAULT_RTC_ADDRESS = 0xf020
const val DEFAULT_IO_ADDRESS = 0xf040


/**
 * A virtual 6502 machine for automated testing purposes, with a few essential components.
 */
class TestMachine(
        cpuType: CpuType = CpuType.CPU65C02,
        ramSize: Int = 65536,
        ramStart: Int = 0x0000,
        resetAddress: Int = 0x1000,
        timerAddress: Int = DEFAULT_TIMER_ADDRESS,
        rtcAddress: Int = DEFAULT_RTC_ADDRESS,
        ioAddress: Int = DEFAULT_IO_ADDRESS,
        input: Sequence<UByte> = emptySequence(),
        output: (UByte) -> Unit = {}
) : IVirtualMachine {

    override val cpu: Cpu6502Core
    override val bus: Bus
    val ram: Ram

    init {
        require(ramSize > 0) { "RAM size must be > 0" }
        require(timerAddress != rtcAddress) { "timer and rtc addresses must be different" }
        require(ioAddress != timerAddress) { "io and timer addresses must be different" }
        require(ioAddress != rtcAddress) { "io and rtc addresses must be different" }

        cpu = when (cpuType) {
            CpuType.CPU6502 -> Cpu6502()
            CpuType.CPU65C02 -> Cpu65C02()
        }

        ram = Ram(ramStart, ramStart + ramSize - 1)
        bus = Bus()

        // Set up reset vector
        ram[0xfffa] = 0.toUByte()
        ram[0xfffb] = 0.toUByte()
        ram[0xfffc] = (resetAddress and 0xff).toUByte()
        ram[0xfffd] = ((resetAddress shr 8) and 0xff).toUByte()
        ram[0xfffe] = 0.toUByte()
        ram[0xffff] = 0.toUByte()

        // Add optional components
        if (timerAddress>=0) {
            val timer = Timer(timerAddress, cpu)
            bus += timer
        }
        if (rtcAddress>=0) {
            val rtc = RealTimeClock(rtcAddress)
            bus += rtc
        }
        if(ioAddress>=0) {
            val io = SerialInputOutput(ioAddress, input, output)
            bus += io
        }

        // Connect RAM and CPU
        bus += ram
        bus += cpu

        bus.reset()
        require(cpu.regPC == resetAddress) { "CPU PC should be set to reset address after reset" }
    }

    override fun step() {
        // step a full single instruction
        while (cpu.instrCycles > 0) bus.clock()
        bus.clock()
        while (cpu.instrCycles > 0) bus.clock()
    }

    override fun reset() {
        bus.reset()
    }

    override fun getZeroAndStackPages(): Array<UByte> = ram.getBlock(0, 512)

    override fun loadFileInRam(file: File, loadAddress: Address?) {
        require(loadAddress!=null) { "loadAddress must be specified for test machine" }
        ram.load(file.readBytes(), loadAddress)
    }

    fun loadInRam(data: Array<UByte>, loadAddress: Address) {
        require(data.size <= ram.endAddress-loadAddress+1) { "data size exceeds available RAM" }
        ram.load(data, loadAddress)
    }

    override fun pause(paused: Boolean) {
        throw UnsupportedOperationException("pause not implemented for test machine")
    }

    override fun executeMonitorCommand(command: String): IVirtualMachine.MonitorCmdResult {
        throw UnsupportedOperationException("monitor commands not implemented on test machine")
    }
}



fun main() {
    val serialInput = (0..100).asSequence().map { it.toUByte() }
    val machine = TestMachine(CpuType.CPU65C02, input = serialInput, output = ::println)
    println("Machine created with CPU type: ${machine.cpu.name}")

    println(machine.cpu.snapshot())

    repeat(1000) {
        machine.step()
    }

    println(machine.cpu.snapshot())
}
