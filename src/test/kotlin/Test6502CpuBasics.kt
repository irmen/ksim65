import razorvine.ksim65.*
import razorvine.ksim65.components.Ram
import kotlin.test.*
import kotlin.system.measureNanoTime
import kotlin.test.assertEquals


class Test6502CpuBasics {

    @Test
    fun testCpuFlagsAfterReset6502() {
        val cpu = Cpu6502()
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
        val cpu = Cpu65C02()
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
        val cpu = Cpu6502()
        val ram = Ram(0x1000, 0x1fff)
        // load a simple program that loops a few instructions
        for(b in listOf(0xa9, 0x63, 0xaa, 0x86, 0x22, 0x8e, 0x22, 0x22, 0x91, 0x22, 0x6d, 0x33, 0x33, 0xcd, 0x55, 0x55, 0xd0, 0xee, 0xf0, 0xec).withIndex()) {
            ram[b.index] = b.value.toShort()
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
        val cpu = Cpu65C02()
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
        // this test only works on 6502, not on the 65c02
        val cpu = Cpu6502()
        val bus = Bus()
        bus.add(cpu)
        val ram = Ram(0, 0xffff)
        ram[Cpu6502.RESET_vector] = 0x00
        ram[Cpu6502.RESET_vector +1] = 0x10
        val bytes = javaClass.getResource("bcdtest6502.bin").readBytes()
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
            println("BCD TEST: FAIL!! code=${hexB(code)} value1=${hexB(v1)} value2=${hexB(v2)}")
            println("  predictedA=${hexB(predictedA)}")
            println("  actualA=${hexB(actualA)}")
            println("  predictedF=${predictedF.toString(2).padStart(8,'0')}")
            println("  actualF=${actualF.toString(2).padStart(8,'0')}")
            fail("BCD test failed")
        }
    }

    fun runBCDbeebTest(cpu: Cpu6502, testChoice: Char) {
        // bcd test code from https://github.com/hoglet67/AtomSoftwareArchive/tree/master/tests/clark
        val bus = Bus()
        bus.add(cpu)
        cpu.breakpointForBRK = { _, pc -> fail("brk instruction at \$${hexW(pc)}") }
        cpu.addBreakpoint(0xffee) { cpu, pc ->
            // OSWRCH write character
            print("${cpu.regA.toChar()}")
            Cpu6502.BreakpointResultAction()
        }
        cpu.addBreakpoint(0xffe0) { cpu, pc ->
            // OSRDCH read character
            cpu.regA = testChoice.toInt()
            Cpu6502.BreakpointResultAction()
        }
        val ram = Ram(0, 0xffff)
        val bytes = javaClass.getResource("BCDTEST_beeb.bin").readBytes()
        ram.load(bytes, 0x2900)
        ram[0x0200] = 0x20  // jsr $2900
        ram[0x0201] = 0x00
        ram[0x0202] = 0x29
        ram[0x0203] = 0x00  // brk
        ram[0xffe0] = 0x60  // rts
        ram[0xffee] = 0x60  // rts
        bus.add(ram)
        bus.reset()
        cpu.regPC = 0x0200

        while (cpu.regPC!=0x203 && cpu.totalCycles < 200000000L) {
            bus.clock()
        }

        assertEquals(0x0203, cpu.regPC, "test hangs: "+cpu.snapshot())
        assertEquals(0, ram[0x84], "test failed- check the console output for diag message")
    }

    @Test
    fun testBCDbeeb6502() {
        runBCDbeebTest(Cpu6502(), 'D')
    }

    @Test
    fun testBCDbeeb65c02() {
        runBCDbeebTest(Cpu65C02(), 'H')
    }

    @Test
    fun testBRKbreakpoint() {
        val cpu = Cpu6502()
        val bus = Bus()
        val ram = Ram(0, 0x0ffff)
        ram[0xfffe] = 0x00
        ram[0xffff] = 0xc0
        ram[0x3333] = 0xea
        ram[0x3334] = 0xea
        ram[0x200] = 0x00
        bus.add(cpu)
        bus.add(ram)
        bus.reset()
        cpu.regPC = 0x200
        cpu.breakpointForBRK = null
        cpu.step()
        assertEquals(0xc000, cpu.regPC)
        cpu.regPC = 0x200
        cpu.breakpointForBRK = { theCpu, _ ->
            theCpu.regA = 123
            Cpu6502.BreakpointResultAction(changePC = 0x3333)
        }
        cpu.step()
        assertEquals(123, cpu.regA)
        assertEquals(0x3334, cpu.regPC)
    }

    @Test
    fun testNesTest() {
        // http://www.qmtpro.com/~nes/misc/nestest.txt

        class NesCpu: Cpu6502() {
            fun resetTotalCycles(cycles: Long) {
                totalCycles = cycles
                instrCycles = 0
            }

            override fun iAdc(): Boolean {
                // NES cpu doesn't have BCD mode
                val decimal = regP.D
                regP.D = false
                val result = super.iAdc()
                regP.D = decimal
                return result
            }

            override fun iSbc(operandOverride: Int?): Boolean {
                // NES cpu doesn't have BCD mode
                val decimal = regP.D
                regP.D = false
                val result = super.iSbc(operandOverride)
                regP.D = decimal
                return result
            }
        }

        val cpu = NesCpu()
        val ram = Ram(0, 0xffff)
        val disassembler = Disassembler(cpu)

        val bytes = javaClass.getResource("nestest.nes").readBytes().drop(0x10).take(0x4000).toByteArray()
        ram.load(bytes, 0x8000)
        ram.load(bytes, 0xc000)
        val bus = Bus()
        bus.add(cpu)
        bus.add(ram)
        bus.reset()
        cpu.resetTotalCycles(7)     // that is what the nes rom starts with
        cpu.regPC = 0xc000
        var tracingSnapshot = cpu.snapshot()
        cpu.tracing = { tracingSnapshot=it }

        val neslog = javaClass.getResource("nestest.log").readText().lineSequence()
        for(logline in neslog) {
            if(logline.isEmpty())
                break

            cpu.step()

            val nesAddressHex = logline.substring(0, 4).toInt(16)
            assertEquals(nesAddressHex, tracingSnapshot.PC)

//            println("NES: $logline")
//            val disassem = disassembler.disassembleOneInstruction(ram.data, tracingSnapshot.PC, 0).first.substring(1)
//            val spaces = "                                             ".substring(disassem.length-1)
//            println("EMU: $disassem $spaces A:${hexB(tracingSnapshot.A)} X:${hexB(tracingSnapshot.X)} Y:${hexB(tracingSnapshot.Y)} P:${hexB(tracingSnapshot.P.asInt())} SP:${hexB(tracingSnapshot.SP)} PPU:  0,  0 CYC:${tracingSnapshot.cycles}")

            val nesRegsLog = logline.substring(48).split(':')
            val nesA = nesRegsLog[1].substring(0, 2).toShort(16)
            val nesX = nesRegsLog[2].substring(0, 2).toShort(16)
            val nesY = nesRegsLog[3].substring(0, 2).toShort(16)
            val nesP = nesRegsLog[4].substring(0, 2).toInt(16)
            val nesSP = nesRegsLog[5].substring(0, 2).toInt(16)
            val nesCycles = nesRegsLog[7].toLong()
            assertEquals(nesA, tracingSnapshot.A)
            assertEquals(nesX, tracingSnapshot.X)
            assertEquals(nesY, tracingSnapshot.Y)
            assertEquals(nesP, tracingSnapshot.P.asInt())
            assertEquals(nesSP, tracingSnapshot.SP)
            assertEquals(nesCycles, tracingSnapshot.cycles)
        }

        val two = ram[0x02]
        val three = ram[0x03]
        assertEquals(0, two, "test failed, code ${hexB(two)} ${hexB(three)}")
    }
}
