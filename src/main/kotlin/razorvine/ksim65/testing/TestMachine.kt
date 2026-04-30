package razorvine.ksim65.testing

import razorvine.ksim65.Bus
import razorvine.ksim65.Cpu6502
import razorvine.ksim65.Cpu6502Core
import razorvine.ksim65.Cpu65C02
import razorvine.ksim65.IVirtualMachine
import razorvine.ksim65.components.*
import razorvine.ksim65.hexB
import razorvine.ksim65.hexW
import java.io.File


enum class CpuType {
    CPU6502,    // NMOS 6502 with illegal opcodes
    CPU65C02    // CMOS 65C02 (WDC)
}

const val BEGIN_OF_IO_SPACE = 0xf000
const val DEFAULT_TIMER_ADDRESS = 0xf000
const val DEFAULT_RTC_ADDRESS = 0xf100
const val DEFAULT_SERIAL_ADDRESS = 0xf200


interface IHostSerialAndPowerIO {
    fun write(byte: UByte)
    fun read(): UByte

    fun reset()
    fun poweroff()
}

/**
 * A virtual 6502 machine for automated testing purposes, with a few essential components.
 */
class TestMachine(
        cpuType: CpuType = CpuType.CPU65C02,
        ramSize: Int = 0xf000,
        ramStart: Int = 0x0000,
        resetAddress: Int = 0x1000,
        timerAddress: Int = DEFAULT_TIMER_ADDRESS,
        rtcAddress: Int = DEFAULT_RTC_ADDRESS,
        serialAndPower: IHostSerialAndPowerIO? = null,
        serialAddress: Int = DEFAULT_SERIAL_ADDRESS
) : IVirtualMachine {

    override val cpu: Cpu6502Core
    override val bus: Bus
    val ram: Ram
    val highram: Ram

    init {
        require(ramSize > 0) { "RAM size must be > 0" }
        require(ramStart + ramSize - 1 < BEGIN_OF_IO_SPACE) { "RAM should stop below the IO space" }
        require(timerAddress != rtcAddress) { "timer and rtc addresses must be different" }
        if(serialAndPower!=null) {
            require(serialAddress>0)
            require(serialAddress != timerAddress) { "serial and timer addresses must be different" }
            require(serialAddress != rtcAddress) { "serial and rtc addresses must be different" }
        }

        cpu = when (cpuType) {
            CpuType.CPU6502 -> Cpu6502()
            CpuType.CPU65C02 -> Cpu65C02()
        }
        if (cpu is Cpu65C02)
            cpu.hostSerialAndPower = serialAndPower

        ram = Ram(ramStart, ramStart + ramSize - 1)
        highram = Ram(0xff00, 0xffff)
        bus = Bus()

        // Set up reset vectors
        highram[0xfa] = 0.toUByte()
        highram[0xfb] = 0.toUByte()
        highram[0xfc] = (resetAddress and 0xff).toUByte()
        highram[0xfd] = ((resetAddress shr 8) and 0xff).toUByte()
        highram[0xfe] = 0.toUByte()
        highram[0xff] = 0.toUByte()

        // Add optional components
        if (timerAddress>=0) {
            val timer = Timer(timerAddress, cpu)
            bus += timer
        }
        if (rtcAddress>=0) {
            val rtc = RealTimeClock(rtcAddress)
            bus += rtc
        }
        if(serialAndPower!=null) {
            val io = SerialInputOutput(serialAddress, serialAndPower)
            bus += io
        }

        // Connect RAM and CPU
        bus += ram
        bus += highram
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
        if (file.extension == "prg" && (loadAddress == null || loadAddress == 0x0801)) ram.loadPrg(file.inputStream(), null)
        else ram.load(file.readBytes(), loadAddress!!)
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

    // Test assertion helpers:
    
    fun assertRegA(expected: Int) =
        assertEqual(expected, cpu.regA, "A")

    fun assertRegX(expected: Int) =
        assertEqual(expected, cpu.regX, "X")

    fun assertRegY(expected: Int) =
        assertEqual(expected, cpu.regY, "Y")

    fun assertRegPC(expected: Int) =
        assertEqual(expected, cpu.regPC, "PC")

    fun assertRegSP(expected: Int) =
        assertEqual(expected, cpu.regSP, "SP")

    fun assertMemory(address: Int, expected: Int) =
        assertEqual(expected, ram[address].toInt(), "memory[$${hexW(address)}]")

    fun assertFlagZ(set: Boolean) =
        assertFlag(cpu.regP.Z, set, "Z")

    fun assertFlagN(set: Boolean) =
        assertFlag(cpu.regP.N, set, "N")

    fun assertFlagC(set: Boolean) =
        assertFlag(cpu.regP.C, set, "C")

    fun assertFlagV(set: Boolean) =
        assertFlag(cpu.regP.V, set, "V")

    fun assertFlagD(set: Boolean) =
        assertFlag(cpu.regP.D, set, "D")

    fun assertFlagI(set: Boolean) =
        assertFlag(cpu.regP.I, set, "I")

    fun assertFlagB(set: Boolean) =
        assertFlag(cpu.regP.B, set, "B")

    fun assertCycles(expected: Long) =
        assertEqualCycles(expected, cpu.totalCycles)

    private fun assertEqual(expected: Int, actual: Int, name: String) {
        if (expected != actual)
            throw AssertionError("$name expected=\$${hexB(expected)} but was=\$${hexB(actual)}")
    }

    private fun assertEqualCycles(expected: Long, actual: Long) {
        if (expected != actual)
            throw AssertionError("cycles expected=$expected but was=$actual")
    }

    private fun assertFlag(actual: Boolean, expected: Boolean, name: String) {
        if (expected != actual)
            throw AssertionError("flag $name expected=$expected but was=$actual")
    }
}


private object HostSerialAndPowerIO : IHostSerialAndPowerIO {
    override fun write(byte: UByte) {
        print(byte.toInt().toChar())
    }

    override fun read(): UByte {
        TODO("serial read not yet implemented")
    }

    override fun reset() {
        throw ResetMachine()
    }

    override fun poweroff() {
        throw PoweroffMachine()
    }

    class ResetMachine : Exception()
    class PoweroffMachine : Exception()
}



fun main() {
    val machine = TestMachine(CpuType.CPU65C02, serialAndPower = HostSerialAndPowerIO)
    println("Machine created with CPU type: ${machine.cpu.name}")

    machine.loadFileInRam(File("testprogram/test.prg"), null)

    machine.ram.hexDump(0x1000, 0x103f)

    println("\n~~Machine powered on~~\n")

    try {
        repeat(1_000_000) {
            machine.step()
        }
        println("\n~~Machine execution loop finished, final state:~~")
    } catch(_: HostSerialAndPowerIO.PoweroffMachine) {
        println("\n~~Machine powered off, final state:~~")
    }

    println(machine.cpu.snapshot())

}
