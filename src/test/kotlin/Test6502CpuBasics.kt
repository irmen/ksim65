import razorvine.ksim65.Bus
import razorvine.ksim65.Cpu6502
import razorvine.ksim65.Cpu65C02
import razorvine.ksim65.components.Ram
import kotlin.test.*
import kotlin.system.measureNanoTime
import kotlin.test.assertEquals


class Test6502CpuBasics {

    @Test
    fun testCpuFlagsAfterReset6502() {
        val cpu = Cpu6502(true)
        val bus = Bus()
        bus.add(cpu)
        cpu.reset()
        assertEquals(0xfd, cpu.regSP)
        assertEquals(0xffff, cpu.regPC)
        assertEquals(0, cpu.totalCycles)
        assertEquals(8, cpu.instrCycles)
        assertEquals(0, cpu.regA)
        assertEquals(0, cpu.regX)
        assertEquals(0, cpu.regY)
        assertEquals(0, cpu.currentOpcode)
        assertEquals(Cpu6502.StatusRegister(C = false, Z = false, I = true, D = false, B = false, V = false, N = false), cpu.regP)
        assertEquals(0b00100100, cpu.regP.asInt())
    }

    @Test
    fun testCpuFlagsAfterReset65c02() {
        val cpu = Cpu65C02(true)
        val bus = Bus()
        bus.add(cpu)
        cpu.reset()
        assertEquals(0xfd, cpu.regSP)
        assertEquals(0xffff, cpu.regPC)
        assertEquals(0, cpu.totalCycles)
        assertEquals(8, cpu.instrCycles)
        assertEquals(0, cpu.regA)
        assertEquals(0, cpu.regX)
        assertEquals(0, cpu.regY)
        assertEquals(0, cpu.currentOpcode)
        assertEquals(Cpu6502.StatusRegister(C = false, Z = false, I = true, D = false, B = false, V = false, N = false), cpu.regP)
        assertEquals(0b00100100, cpu.regP.asInt())
    }

    @Test
    fun testCpuPerformance6502() {
        val cpu = Cpu6502(true)
        val ram = Ram(0x1000, 0x1fff)
        // load a simple program that loops a few instructions
        for(b in listOf(0xa9, 0x63, 0xaa, 0x86, 0x22, 0x8e, 0x22, 0x22, 0x91, 0x22, 0x6d, 0x33, 0x33, 0xcd, 0x55, 0x55, 0xd0, 0xee, 0xf0, 0xec).withIndex()) {
            ram[0x1000+b.index] = b.value.toShort()
        }

        val bus = Bus()
        bus.add(cpu)
        bus.add(ram)
        cpu.reset()
        cpu.regPC = 0x1000

        // warmup
        while(cpu.totalCycles<5000000)
            cpu.clock()

        // timing
        val cycles = 100000000
        val duration = measureNanoTime {
            while (cpu.totalCycles < cycles)
                cpu.clock()
        }
        val seconds = duration.toDouble() / 1e9
        val mhz = (cycles.toDouble() / seconds) / 1e6
        println("duration $seconds sec  for $cycles = $mhz Mhz")

    }

    @Test
    fun testCpuPerformance65C02() {
        val cpu = Cpu65C02(true)
        val ram = Ram(0x0000, 0x1fff)
        // load a simple program that loops a few instructions
        for(b in listOf(0xa9, 0x63, 0xaa, 0x86, 0x22, 0x8e, 0x22, 0x22, 0x91, 0x22, 0x6d, 0x33, 0x33, 0xcd, 0x55, 0x55,
            0xff, 0xff, 0x79, 0x9e, 0x56, 0x34, 0xd0, 0xe8, 0xf0, 0xe6).withIndex()) {
            ram[0x1000+b.index] = b.value.toShort()
        }

        val bus = Bus()
        bus.add(cpu)
        bus.add(ram)
        cpu.reset()
        cpu.regPC = 0x1000

        // warmup
        while(cpu.totalCycles<5000000)
            cpu.clock()

        // timing
        val cycles = 100000000
        val duration = measureNanoTime {
            while (cpu.totalCycles < cycles)
                cpu.clock()
        }
        val seconds = duration.toDouble() / 1e9
        val mhz = (cycles.toDouble() / seconds) / 1e6
        println("duration $seconds sec  for $cycles = $mhz Mhz")

    }

    @Test
    fun testBCD6502() {
        val cpu = Cpu6502(true)
        val bus = Bus()
        bus.add(cpu)
        val ram = Ram(0, 0xffff)
        ram[Cpu6502.RESET_vector] = 0x00
        ram[Cpu6502.RESET_vector +1] = 0x10
        val bytes = javaClass.getResource("bcdtest6502.bin").readBytes()      // only works on 6502, not on the 65c02
        ram.load(bytes, 0x1000)
        bus.add(ram)
        bus.reset()

        try {
            while (true) {
                bus.clock()
            }
        } catch(e: Cpu6502.InstructionError) {
            // do nothing
        }

        if(ram[0x0400] ==0.toShort()) {
            println("BCD TEST for 6502: OK!")
        }
        else {
            val code = ram[0x0400]
            val v1 = ram[0x0401]
            val v2 = ram[0x0402]
            val predictedA = ram[0x00fc]
            val actualA = ram[0x00fd]
            val predictedF = ram[0x00fe]
            val actualF = ram[0x00ff]
            println("BCD TEST: FAIL!! code=${cpu.hexB(code)} value1=${cpu.hexB(v1)} value2=${cpu.hexB(v2)}")
            println("  predictedA=${cpu.hexB(predictedA)}")
            println("  actualA=${cpu.hexB(actualA)}")
            println("  predictedF=${predictedF.toString(2).padStart(8,'0')}")
            println("  actualF=${actualF.toString(2).padStart(8,'0')}")
            fail("BCD test failed")
        }
    }

}
