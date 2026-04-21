package razorvine.ksim65

import razorvine.ksim65.components.*

/**
 * Factory for creating a configured 65xx computer system for automated testing purposes.
 */
object TestMachineFactory {

    enum class CpuType {
        CPU6502,    // NMOS 6502 with illegal opcodes
        CPU65C02    // CMOS 65C02 (WDC)
    }

    /**
     * Simple I/O device for testing.
     * Provides read from input callback and write to output callback.
     */
    class SimpleIO(
        startAddress: Address,
        val input: () -> Short,
        val output: (Short) -> Unit
    ) : MemMappedComponent(startAddress, startAddress) {

        companion object {
            const val DEFAULT_ADDRESS = 0xf030
        }

        override fun clock() {}
        override fun reset() {}

        override operator fun get(offset: Int): UByte {
            return if (offset == 0x00) input() else 0.toShort()
        }

        override operator fun set(offset: Int, data: UByte) {
            if (offset == 0x01) output(data)
        }
    }

    /**
     * Result of creating a machine.
     */
    data class Machine(
        val bus: Bus,
        val cpu: Cpu6502Core,
        val ram: Ram,
        val cpuType: CpuType
    )

    /**
     * Create a generic 65C02 system with configurable RAM.
     */
    fun create(
        cpuType: CpuType = CpuType.CPU65C02,
        ramSize: Int = 65536,
        ramStart: Int = 0x0000,
        resetAddress: Int = 0x1000,
        timerAddress: Int? = null,
        rtcAddress: Int? = null,
        ioAddress: Int? = null,
        input: (() -> UByte)? = null,
        output: ((UByte) -> Unit)? = null
    ): Machine {
        val cpu: Cpu6502Core = when (cpuType) {
            CpuType.CPU6502 -> Cpu6502()
            CpuType.CPU65C02 -> Cpu65C02()
        }

        val ram = Ram(ramStart, ramStart + ramSize - 1)
        val bus = Bus()

        // Set up reset vector
        ram[0xfffa] = 0
        ram[0xfffb] = 0
        ram[0xfffc] = (resetAddress and 0xff).toShort()
        ram[0xfffd] = ((resetAddress shr 8) and 0xff).toShort()
        ram[0xfffe] = 0
        ram[0xffff] = 0

        // Add optional components
        if (timerAddress != null) {
            val timer = Timer(timerAddress, cpu)
            bus += timer
        }
        if (rtcAddress != null) {
            val rtc = RealTimeClock(rtcAddress)
            bus += rtc
        }
        if (ioAddress != null && input != null && output != null) {
            val io = SimpleIO(ioAddress, input, output)
            bus += io
        }

        // Connect RAM and CPU
        bus += ram
        bus += cpu

        bus.reset()
        require(cpu.regPC == resetAddress) { "CPU PC should be set to reset address after reset" }

        return Machine(bus, cpu, ram, cpuType)
    }

    /**
     * Create a generic 65C02 system with full 64KB RAM.
     */
    fun createGeneric(): Machine = create()

    /**
     * Create a 65C02 system with a Timer.
     */
    fun createWithTimer(timerAddress: Int = 0xf000): Machine =
        create(timerAddress = timerAddress)

    /**
     * Create a 65C02 system with an RTC.
     */
    fun createWithRTC(rtcAddress: Int = 0xf020): Machine =
        create(rtcAddress = rtcAddress)

    /**
     * Create a 65C02 system with both Timer and RTC.
     */
    fun createWithTimerAndRTC(timerAddress: Int = 0xf000, rtcAddress: Int = 0xf020): Machine =
        create(timerAddress = timerAddress, rtcAddress = rtcAddress)

    /**
     * Create a 65C02 system with I/O for stdin/stdout simulation.
     * Input returns -1 when no data available.
     */
    fun createWithIO(
        ioAddress: Int = SimpleIO.DEFAULT_ADDRESS,
        input: () -> UByte = { -1 },
        output: (UByte) -> Unit = {}
    ): Machine = create(ioAddress = ioAddress, input = input, output = output)
}