package razorvine.ksim65

import razorvine.ksim65.components.Address
import razorvine.ksim65.components.BusComponent
import razorvine.ksim65.components.UByte


/**
 * 6502 cpu simulation (the NMOS version) including the 'illegal' opcodes.
 * TODO: actually implement the illegal opcodes, see http://www.ffd2.com/fridge/docs/6502-NMOS.extra.opcodes or https://sourceforge.net/p/moarnes/code/ci/master/tree/src/6502.c
 */
open class Cpu6502 : BusComponent() {
    open val name = "6502"
    var tracing: ((state: String) -> Unit)? = null
    var totalCycles = 0L
        protected set
    private var resetTime = System.nanoTime()

    var breakpointForBRK: BreakpointHandler? = null

    class InstructionError(msg: String) : RuntimeException(msg)

    companion object {
        const val NMI_vector = 0xfffa
        const val RESET_vector = 0xfffc
        const val IRQ_vector = 0xfffe
    }

    class StatusRegister(var C: Boolean = false, var Z: Boolean = false, var I: Boolean = false, var D: Boolean = false,
                         var B: Boolean = false, var V: Boolean = false, var N: Boolean = false) {
        fun asInt(): Int {
            return (0b00100000
                    or (if (N) 0b10000000 else 0)
                    or (if (V) 0b01000000 else 0)
                    or (if (B) 0b00010000 else 0)
                    or (if (D) 0b00001000 else 0)
                    or (if (I) 0b00000100 else 0)
                    or (if (Z) 0b00000010 else 0)
                    or (if (C) 0b00000001 else 0))
        }

        fun fromInt(byte: Int) {
            N = (byte and 0b10000000) != 0
            V = (byte and 0b01000000) != 0
            B = (byte and 0b00010000) != 0
            D = (byte and 0b00001000) != 0
            I = (byte and 0b00000100) != 0
            Z = (byte and 0b00000010) != 0
            C = (byte and 0b00000001) != 0
        }

        override fun toString(): String {
            return asInt().toString(2).padStart(8, '0')
        }

        override fun hashCode(): Int = asInt()

        override fun equals(other: Any?): Boolean {
            if (other !is StatusRegister) return false
            return asInt() == other.asInt()
        }
    }

    /**
     * Breakpoint handlers have to return this to specify to the CPU simulator
     * what should happen after the breakpoint code has executed.
     *
     * Setting changePC will continue execution from a different memory location.
     * Setting changeOpcode will execute a different opcode in place of the one
     *    that's actually on the location of the breakpoint.
     *    (it's a bit limited; you can only use one-byte instructions for this)
     * Setting causeBRK will simulate a software interrupt via BRK,
     *    without having to actually have a BRK in the breakpoint's memory location
     *    (this is the same as changeOpcode=0x00)
     */
    class BreakpointResultAction(val changePC: Address? = null, val changeOpcode: Int? = null, val causeBRK: Boolean = false)

    class State(val A: UByte, val X: UByte, val Y: UByte, val SP: Address, val P: StatusRegister, val PC: Address, val cycles: Long) {
        override fun toString(): String {
            return "cycle:$cycles - pc=${hexW(PC)} "+"A=${hexB(A)} "+"X=${hexB(X)} "+"Y=${hexB(Y)} "+
                   "SP=${hexB(SP)} "+" n="+(if (P.N) "1" else "0")+" v="+(if (P.V) "1" else "0")+
                   " b="+(if (P.B) "1" else "0")+" d="+(if (P.D) "1" else "0")+" i="+(if (P.I) "1" else "0")+
                   " z="+(if (P.Z) "1" else "0")+" c="+(if (P.C) "1" else "0")
        }
    }

    enum class AddrMode {
        Imp, Acc, Imm, Zp, ZpX, ZpY, Rel, Abs, AbsX, AbsY, Ind, IzX, IzY,
        // modes used only by the 65C02:
        Zpr, Izp, IaX
    }

    class Instruction(val mnemonic: String, val mode: AddrMode, val cycles: Int)

    var regA: Int = 0
    var regX: Int = 0
    var regY: Int = 0
    var regSP: Int = 0
    var regPC: Address = 0
    val regP = StatusRegister()
    var irqAsserted = false
    var nmiAsserted = false
    var currentOpcode: Int = 0
        protected set
    var currentOpcodeAddress: Address = 0    // the PC can be changed already depending on the addressing mode
        protected set
    var instrCycles: Int = 0
        protected set
    val isLooping: Boolean get() {
        // jump loop detection
        return (previousOpcodeAddress == currentOpcodeAddress) && !(nmiAsserted || irqAsserted)
    }
    private var previousOpcodeAddress: Address = 0xffff

    lateinit var currentInstruction: Instruction

    val averageSpeedKhzSinceReset: Double
        get() = totalCycles.toDouble()/(System.nanoTime()-resetTime)*1_000_000

    @Synchronized
    fun snapshot(): State {
        val status = StatusRegister().also { it.fromInt(regP.asInt()) }
        return State(regA.toShort(), regX.toShort(), regY.toShort(), regSP, status, regPC, totalCycles)
    }

    // data byte from the instruction (only set when addr.mode is Accumulator, Immediate or Implied)
    protected var fetchedData: Int = 0

    // all other addressing modes yield a fetched memory address
    protected var fetchedAddress: Address = 0

    private val breakpoints = mutableMapOf<Address, BreakpointHandler>()

    fun addBreakpoint(address: Address, handler: BreakpointHandler) { breakpoints[address] = handler }

    fun removeBreakpoint(address: Address) = breakpoints.remove(address)

    /**
     * Reset the cpu
     */
    override fun reset() {
        // TODO don't perform all of the reset logic immediately, handle the reset 'pin' in the regular clock() instead (much like a NMI)
        regP.I = true
        regP.C = false
        regP.Z = false
        regP.D = false
        regP.B = false
        regP.V = false
        regP.N = false
        regSP = 0xfd
        regPC = readWord(RESET_vector)
        regA = 0
        regX = 0
        regY = 0
        instrCycles = 8       // a reset takes 8 clock cycles
        currentOpcode = 0
        currentInstruction = instructions[0]
        totalCycles = 0
        resetTime = System.nanoTime()
    }

    /**
     * Process once clock cycle in the cpu.
     * Use this if goal is cycle-perfect emulation.
     */
    override fun clock() {
        if (instrCycles == 0) {
            if(nmiAsserted || (irqAsserted && !regP.I)) {
                handleInterrupt()
                return
            }

            // no interrupt, fetch next instruction from memory
            previousOpcodeAddress = currentOpcodeAddress
            currentOpcodeAddress = regPC
            currentOpcode = read(regPC)
            currentInstruction = instructions[currentOpcode]

            // tracing and breakpoint handling
            tracing?.invoke(snapshot().toString())
            breakpoints[regPC]?.let {
                if (breakpoint(it)) return
            }

            if (currentOpcode == 0x00) breakpointForBRK?.let {
                if (breakpoint(it)) return
            }

            regPC++
            instrCycles = currentInstruction.cycles
            val extraCycleFromAddr = applyAddressingMode(currentInstruction.mode)
            val extraCycleFromInstr = dispatchOpcode(currentOpcode)
            if(extraCycleFromAddr and extraCycleFromInstr)
                instrCycles++
        }

        instrCycles--
        totalCycles++
    }

    private fun breakpoint(handler: BreakpointHandler): Boolean {
        val oldPC = regPC
        val result = handler(this, regPC)

        when {
            result.changePC != null -> regPC = result.changePC
            result.changeOpcode != null -> {
                currentOpcode = result.changeOpcode
                currentInstruction = instructions[currentOpcode]
            }
            result.causeBRK -> {
                currentOpcode = 0x00
                currentInstruction = instructions[0x00]
            }
        }

        return if (regPC != oldPC) {
            clock()
            true
        } else false
    }

    /**
     * Execute one single complete instruction.
     * Use this when the goal is emulation performance and not a cycle perfect system.
     */
    open fun step() {
        totalCycles += instrCycles
        instrCycles = 0
        clock()
        totalCycles += instrCycles
        instrCycles = 0
    }

    protected fun getFetched() =
            if (currentInstruction.mode == AddrMode.Imm || currentInstruction.mode == AddrMode.Acc || currentInstruction.mode == AddrMode.Imp) fetchedData
            else read(fetchedAddress)

    protected fun readPc(): Int = bus.read(regPC++).toInt()

    protected fun pushStackAddr(address: Address) {
        val lo = address and 0xff
        val hi = (address ushr 8)
        pushStack(hi)
        pushStack(lo)
    }

    protected fun pushStack(status: StatusRegister) {
        pushStack(status.asInt())
    }

    protected fun pushStack(data: Int) {
        write(regSP or 0x0100, data)
        regSP = (regSP-1) and 0xff
    }

    protected fun popStack(): Int {
        regSP = (regSP+1) and 0xff
        return read(regSP or 0x0100)
    }

    protected fun popStackAddr(): Address {
        val lo = popStack()
        val hi = popStack()
        return lo or (hi shl 8)
    }

    protected fun read(address: Address): Int = bus.read(address).toInt()
    protected fun readWord(address: Address): Int = bus.read(address).toInt() or (bus.read(address+1).toInt() shl 8)
    protected fun write(address: Address, data: Int) = bus.write(address, data.toShort())

    // opcodes table from  http://www.oxyron.de/html/opcodes02.html
    open val instructions: Array<Instruction> = listOf(
            /* 00 */  Instruction("brk", AddrMode.Imp, 7),
            /* 01 */  Instruction("ora", AddrMode.IzX, 6),
            /* 02 */  Instruction("???", AddrMode.Imp, 2),
            /* 03 */  Instruction("slo", AddrMode.IzX, 8),
            /* 04 */  Instruction("nop", AddrMode.Zp, 3),
            /* 05 */  Instruction("ora", AddrMode.Zp, 3),
            /* 06 */  Instruction("asl", AddrMode.Zp, 5),
            /* 07 */  Instruction("slo", AddrMode.Zp, 5),
            /* 08 */  Instruction("php", AddrMode.Imp, 3),
            /* 09 */  Instruction("ora", AddrMode.Imm, 2),
            /* 0a */  Instruction("asl", AddrMode.Acc, 2),
            /* 0b */  Instruction("anc", AddrMode.Imm, 2),
            /* 0c */  Instruction("nop", AddrMode.Abs, 4),
            /* 0d */  Instruction("ora", AddrMode.Abs, 4),
            /* 0e */  Instruction("asl", AddrMode.Abs, 6),
            /* 0f */  Instruction("slo", AddrMode.Abs, 6),
            /* 10 */  Instruction("bpl", AddrMode.Rel, 2),
            /* 11 */  Instruction("ora", AddrMode.IzY, 5),
            /* 12 */  Instruction("???", AddrMode.Imp, 2),
            /* 13 */  Instruction("slo", AddrMode.IzY, 8),
            /* 14 */  Instruction("nop", AddrMode.ZpX, 4),
            /* 15 */  Instruction("ora", AddrMode.ZpX, 4),
            /* 16 */  Instruction("asl", AddrMode.ZpX, 6),
            /* 17 */  Instruction("slo", AddrMode.ZpX, 6),
            /* 18 */  Instruction("clc", AddrMode.Imp, 2),
            /* 19 */  Instruction("ora", AddrMode.AbsY, 4),
            /* 1a */  Instruction("nop", AddrMode.Imp, 2),
            /* 1b */  Instruction("slo", AddrMode.AbsY, 7),
            /* 1c */  Instruction("nop", AddrMode.AbsX, 4),
            /* 1d */  Instruction("ora", AddrMode.AbsX, 4),
            /* 1e */  Instruction("asl", AddrMode.AbsX, 7),
            /* 1f */  Instruction("slo", AddrMode.AbsX, 7),
            /* 20 */  Instruction("jsr", AddrMode.Abs, 6),
            /* 21 */  Instruction("and", AddrMode.IzX, 6),
            /* 22 */  Instruction("???", AddrMode.Imp, 2),
            /* 23 */  Instruction("rla", AddrMode.IzX, 8),
            /* 24 */  Instruction("bit", AddrMode.Zp, 3),
            /* 25 */  Instruction("and", AddrMode.Zp, 3),
            /* 26 */  Instruction("rol", AddrMode.Zp, 5),
            /* 27 */  Instruction("rla", AddrMode.Zp, 5),
            /* 28 */  Instruction("plp", AddrMode.Imp, 4),
            /* 29 */  Instruction("and", AddrMode.Imm, 2),
            /* 2a */  Instruction("rol", AddrMode.Acc, 2),
            /* 2b */  Instruction("anc", AddrMode.Imm, 2),
            /* 2c */  Instruction("bit", AddrMode.Abs, 4),
            /* 2d */  Instruction("and", AddrMode.Abs, 4),
            /* 2e */  Instruction("rol", AddrMode.Abs, 6),
            /* 2f */  Instruction("rla", AddrMode.Abs, 6),
            /* 30 */  Instruction("bmi", AddrMode.Rel, 2),
            /* 31 */  Instruction("and", AddrMode.IzY, 5),
            /* 32 */  Instruction("???", AddrMode.Imp, 2),
            /* 33 */  Instruction("rla", AddrMode.IzY, 8),
            /* 34 */  Instruction("nop", AddrMode.ZpX, 4),
            /* 35 */  Instruction("and", AddrMode.ZpX, 4),
            /* 36 */  Instruction("rol", AddrMode.ZpX, 6),
            /* 37 */  Instruction("rla", AddrMode.ZpX, 6),
            /* 38 */  Instruction("sec", AddrMode.Imp, 2),
            /* 39 */  Instruction("and", AddrMode.AbsY, 4),
            /* 3a */  Instruction("nop", AddrMode.Imp, 2),
            /* 3b */  Instruction("rla", AddrMode.AbsY, 7),
            /* 3c */  Instruction("nop", AddrMode.AbsX, 4),
            /* 3d */  Instruction("and", AddrMode.AbsX, 4),
            /* 3e */  Instruction("rol", AddrMode.AbsX, 7),
            /* 3f */  Instruction("rla", AddrMode.AbsX, 7),
            /* 40 */  Instruction("rti", AddrMode.Imp, 6),
            /* 41 */  Instruction("eor", AddrMode.IzX, 6),
            /* 42 */  Instruction("???", AddrMode.Imp, 2),
            /* 43 */  Instruction("sre", AddrMode.IzX, 8),
            /* 44 */  Instruction("nop", AddrMode.Zp, 3),
            /* 45 */  Instruction("eor", AddrMode.Zp, 3),
            /* 46 */  Instruction("lsr", AddrMode.Zp, 5),
            /* 47 */  Instruction("sre", AddrMode.Zp, 5),
            /* 48 */  Instruction("pha", AddrMode.Imp, 3),
            /* 49 */  Instruction("eor", AddrMode.Imm, 2),
            /* 4a */  Instruction("lsr", AddrMode.Acc, 2),
            /* 4b */  Instruction("alr", AddrMode.Imm, 2),
            /* 4c */  Instruction("jmp", AddrMode.Abs, 3),
            /* 4d */  Instruction("eor", AddrMode.Abs, 4),
            /* 4e */  Instruction("lsr", AddrMode.Abs, 6),
            /* 4f */  Instruction("sre", AddrMode.Abs, 6),
            /* 50 */  Instruction("bvc", AddrMode.Rel, 2),
            /* 51 */  Instruction("eor", AddrMode.IzY, 5),
            /* 52 */  Instruction("???", AddrMode.Imp, 2),
            /* 53 */  Instruction("sre", AddrMode.IzY, 8),
            /* 54 */  Instruction("nop", AddrMode.ZpX, 4),
            /* 55 */  Instruction("eor", AddrMode.ZpX, 4),
            /* 56 */  Instruction("lsr", AddrMode.ZpX, 6),
            /* 57 */  Instruction("sre", AddrMode.ZpX, 6),
            /* 58 */  Instruction("cli", AddrMode.Imp, 2),
            /* 59 */  Instruction("eor", AddrMode.AbsY, 4),
            /* 5a */  Instruction("nop", AddrMode.Imp, 2),
            /* 5b */  Instruction("sre", AddrMode.AbsY, 7),
            /* 5c */  Instruction("nop", AddrMode.AbsX, 4),
            /* 5d */  Instruction("eor", AddrMode.AbsX, 4),
            /* 5e */  Instruction("lsr", AddrMode.AbsX, 7),
            /* 5f */  Instruction("sre", AddrMode.AbsX, 7),
            /* 60 */  Instruction("rts", AddrMode.Imp, 6),
            /* 61 */  Instruction("adc", AddrMode.IzX, 6),
            /* 62 */  Instruction("???", AddrMode.Imp, 2),
            /* 63 */  Instruction("rra", AddrMode.IzX, 8),
            /* 64 */  Instruction("nop", AddrMode.Zp, 3),
            /* 65 */  Instruction("adc", AddrMode.Zp, 3),
            /* 66 */  Instruction("ror", AddrMode.Zp, 5),
            /* 67 */  Instruction("rra", AddrMode.Zp, 5),
            /* 68 */  Instruction("pla", AddrMode.Imp, 4),
            /* 69 */  Instruction("adc", AddrMode.Imm, 2),
            /* 6a */  Instruction("ror", AddrMode.Acc, 2),
            /* 6b */  Instruction("arr", AddrMode.Imm, 2),
            /* 6c */  Instruction("jmp", AddrMode.Ind, 5),
            /* 6d */  Instruction("adc", AddrMode.Abs, 4),
            /* 6e */  Instruction("ror", AddrMode.Abs, 6),
            /* 6f */  Instruction("rra", AddrMode.Abs, 6),
            /* 70 */  Instruction("bvs", AddrMode.Rel, 2),
            /* 71 */  Instruction("adc", AddrMode.IzY, 5),
            /* 72 */  Instruction("???", AddrMode.Imp, 2),
            /* 73 */  Instruction("rra", AddrMode.IzY, 8),
            /* 74 */  Instruction("nop", AddrMode.ZpX, 4),
            /* 75 */  Instruction("adc", AddrMode.ZpX, 4),
            /* 76 */  Instruction("ror", AddrMode.ZpX, 6),
            /* 77 */  Instruction("rra", AddrMode.ZpX, 6),
            /* 78 */  Instruction("sei", AddrMode.Imp, 2),
            /* 79 */  Instruction("adc", AddrMode.AbsY, 4),
            /* 7a */  Instruction("nop", AddrMode.Imp, 2),
            /* 7b */  Instruction("rra", AddrMode.AbsY, 7),
            /* 7c */  Instruction("nop", AddrMode.AbsX, 4),
            /* 7d */  Instruction("adc", AddrMode.AbsX, 4),
            /* 7e */  Instruction("ror", AddrMode.AbsX, 7),
            /* 7f */  Instruction("rra", AddrMode.AbsX, 7),
            /* 80 */  Instruction("nop", AddrMode.Imm, 2),
            /* 81 */  Instruction("sta", AddrMode.IzX, 6),
            /* 82 */  Instruction("nop", AddrMode.Imm, 2),
            /* 83 */  Instruction("sax", AddrMode.IzX, 6),
            /* 84 */  Instruction("sty", AddrMode.Zp, 3),
            /* 85 */  Instruction("sta", AddrMode.Zp, 3),
            /* 86 */  Instruction("stx", AddrMode.Zp, 3),
            /* 87 */  Instruction("sax", AddrMode.Zp, 3),
            /* 88 */  Instruction("dey", AddrMode.Imp, 2),
            /* 89 */  Instruction("nop", AddrMode.Imm, 2),
            /* 8a */  Instruction("txa", AddrMode.Imp, 2),
            /* 8b */  Instruction("xaa", AddrMode.Imm, 2),
            /* 8c */  Instruction("sty", AddrMode.Abs, 4),
            /* 8d */  Instruction("sta", AddrMode.Abs, 4),
            /* 8e */  Instruction("stx", AddrMode.Abs, 4),
            /* 8f */  Instruction("sax", AddrMode.Abs, 4),
            /* 90 */  Instruction("bcc", AddrMode.Rel, 2),
            /* 91 */  Instruction("sta", AddrMode.IzY, 6),
            /* 92 */  Instruction("???", AddrMode.Imp, 2),
            /* 93 */  Instruction("ahx", AddrMode.IzY, 6),
            /* 94 */  Instruction("sty", AddrMode.ZpX, 4),
            /* 95 */  Instruction("sta", AddrMode.ZpX, 4),
            /* 96 */  Instruction("stx", AddrMode.ZpY, 4),
            /* 97 */  Instruction("sax", AddrMode.ZpY, 4),
            /* 98 */  Instruction("tya", AddrMode.Imp, 2),
            /* 99 */  Instruction("sta", AddrMode.AbsY, 5),
            /* 9a */  Instruction("txs", AddrMode.Imp, 2),
            /* 9b */  Instruction("tas", AddrMode.AbsY, 5),
            /* 9c */  Instruction("shy", AddrMode.AbsX, 5),
            /* 9d */  Instruction("sta", AddrMode.AbsX, 5),
            /* 9e */  Instruction("shx", AddrMode.AbsY, 5),
            /* 9f */  Instruction("ahx", AddrMode.AbsY, 5),
            /* a0 */  Instruction("ldy", AddrMode.Imm, 2),
            /* a1 */  Instruction("lda", AddrMode.IzX, 6),
            /* a2 */  Instruction("ldx", AddrMode.Imm, 2),
            /* a3 */  Instruction("lax", AddrMode.IzX, 6),
            /* a4 */  Instruction("ldy", AddrMode.Zp, 3),
            /* a5 */  Instruction("lda", AddrMode.Zp, 3),
            /* a6 */  Instruction("ldx", AddrMode.Zp, 3),
            /* a7 */  Instruction("lax", AddrMode.Zp, 3),
            /* a8 */  Instruction("tay", AddrMode.Imp, 2),
            /* a9 */  Instruction("lda", AddrMode.Imm, 2),
            /* aa */  Instruction("tax", AddrMode.Imp, 2),
            /* ab */  Instruction("lax", AddrMode.Imm, 2),
            /* ac */  Instruction("ldy", AddrMode.Abs, 4),
            /* ad */  Instruction("lda", AddrMode.Abs, 4),
            /* ae */  Instruction("ldx", AddrMode.Abs, 4),
            /* af */  Instruction("lax", AddrMode.Abs, 4),
            /* b0 */  Instruction("bcs", AddrMode.Rel, 2),
            /* b1 */  Instruction("lda", AddrMode.IzY, 5),
            /* b2 */  Instruction("???", AddrMode.Imp, 2),
            /* b3 */  Instruction("lax", AddrMode.IzY, 5),
            /* b4 */  Instruction("ldy", AddrMode.ZpX, 4),
            /* b5 */  Instruction("lda", AddrMode.ZpX, 4),
            /* b6 */  Instruction("ldx", AddrMode.ZpY, 4),
            /* b7 */  Instruction("lax", AddrMode.ZpY, 4),
            /* b8 */  Instruction("clv", AddrMode.Imp, 2),
            /* b9 */  Instruction("lda", AddrMode.AbsY, 4),
            /* ba */  Instruction("tsx", AddrMode.Imp, 2),
            /* bb */  Instruction("las", AddrMode.AbsY, 4),
            /* bc */  Instruction("ldy", AddrMode.AbsX, 4),
            /* bd */  Instruction("lda", AddrMode.AbsX, 4),
            /* be */  Instruction("ldx", AddrMode.AbsY, 4),
            /* bf */  Instruction("lax", AddrMode.AbsY, 4),
            /* c0 */  Instruction("cpy", AddrMode.Imm, 2),
            /* c1 */  Instruction("cmp", AddrMode.IzX, 6),
            /* c2 */  Instruction("nop", AddrMode.Imm, 2),
            /* c3 */  Instruction("dcp", AddrMode.IzX, 8),
            /* c4 */  Instruction("cpy", AddrMode.Zp, 3),
            /* c5 */  Instruction("cmp", AddrMode.Zp, 3),
            /* c6 */  Instruction("dec", AddrMode.Zp, 5),
            /* c7 */  Instruction("dcp", AddrMode.Zp, 5),
            /* c8 */  Instruction("iny", AddrMode.Imp, 2),
            /* c9 */  Instruction("cmp", AddrMode.Imm, 2),
            /* ca */  Instruction("dex", AddrMode.Imp, 2),
            /* cb */  Instruction("axs", AddrMode.Imm, 2),
            /* cc */  Instruction("cpy", AddrMode.Abs, 4),
            /* cd */  Instruction("cmp", AddrMode.Abs, 4),
            /* ce */  Instruction("dec", AddrMode.Abs, 6),
            /* cf */  Instruction("dcp", AddrMode.Abs, 6),
            /* d0 */  Instruction("bne", AddrMode.Rel, 2),
            /* d1 */  Instruction("cmp", AddrMode.IzY, 5),
            /* d2 */  Instruction("???", AddrMode.Imp, 2),
            /* d3 */  Instruction("dcp", AddrMode.IzY, 8),
            /* d4 */  Instruction("nop", AddrMode.ZpX, 4),
            /* d5 */  Instruction("cmp", AddrMode.ZpX, 4),
            /* d6 */  Instruction("dec", AddrMode.ZpX, 6),
            /* d7 */  Instruction("dcp", AddrMode.ZpX, 6),
            /* d8 */  Instruction("cld", AddrMode.Imp, 2),
            /* d9 */  Instruction("cmp", AddrMode.AbsY, 4),
            /* da */  Instruction("nop", AddrMode.Imp, 2),
            /* db */  Instruction("dcp", AddrMode.AbsY, 7),
            /* dc */  Instruction("nop", AddrMode.AbsX, 4),
            /* dd */  Instruction("cmp", AddrMode.AbsX, 4),
            /* de */  Instruction("dec", AddrMode.AbsX, 7),
            /* df */  Instruction("dcp", AddrMode.AbsX, 7),
            /* e0 */  Instruction("cpx", AddrMode.Imm, 2),
            /* e1 */  Instruction("sbc", AddrMode.IzX, 6),
            /* e2 */  Instruction("nop", AddrMode.Imm, 2),
            /* e3 */  Instruction("isc", AddrMode.IzX, 8),
            /* e4 */  Instruction("cpx", AddrMode.Zp, 3),
            /* e5 */  Instruction("sbc", AddrMode.Zp, 3),
            /* e6 */  Instruction("inc", AddrMode.Zp, 5),
            /* e7 */  Instruction("isc", AddrMode.Zp, 5),
            /* e8 */  Instruction("inx", AddrMode.Imp, 2),
            /* e9 */  Instruction("sbc", AddrMode.Imm, 2),
            /* ea */  Instruction("nop", AddrMode.Imp, 2),
            /* eb */  Instruction("sbc", AddrMode.Imm, 2),
            /* ec */  Instruction("cpx", AddrMode.Abs, 4),
            /* ed */  Instruction("sbc", AddrMode.Abs, 4),
            /* ee */  Instruction("inc", AddrMode.Abs, 6),
            /* ef */  Instruction("isc", AddrMode.Abs, 6),
            /* f0 */  Instruction("beq", AddrMode.Rel, 2),
            /* f1 */  Instruction("sbc", AddrMode.IzY, 5),
            /* f2 */  Instruction("???", AddrMode.Imp, 2),
            /* f3 */  Instruction("isc", AddrMode.IzY, 8),
            /* f4 */  Instruction("nop", AddrMode.ZpX, 4),
            /* f5 */  Instruction("sbc", AddrMode.ZpX, 4),
            /* f6 */  Instruction("inc", AddrMode.ZpX, 6),
            /* f7 */  Instruction("isc", AddrMode.ZpX, 6),
            /* f8 */  Instruction("sed", AddrMode.Imp, 2),
            /* f9 */  Instruction("sbc", AddrMode.AbsY, 4),
            /* fa */  Instruction("nop", AddrMode.Imp, 2),
            /* fb */  Instruction("isc", AddrMode.AbsY, 7),
            /* fc */  Instruction("nop", AddrMode.AbsX, 4),
            /* fd */  Instruction("sbc", AddrMode.AbsX, 4),
            /* fe */  Instruction("inc", AddrMode.AbsX, 7),
            /* ff */  Instruction("isc", AddrMode.AbsX, 7)).toTypedArray()

    protected open fun applyAddressingMode(addrMode: AddrMode): Boolean {
        // an addressing mode can cause an extra clock cycle on certain instructions
        return when (addrMode) {
            AddrMode.Imp, AddrMode.Acc -> {
                fetchedData = regA
                false
            }
            AddrMode.Imm -> {
                fetchedData = readPc()
                false
            }
            AddrMode.Zp -> {
                fetchedAddress = readPc()
                false
            }
            AddrMode.ZpX -> {
                // note: zeropage index will not leave Zp when page boundary is crossed
                fetchedAddress = (readPc()+regX) and 0xff
                false
            }
            AddrMode.ZpY -> {
                // note: zeropage index will not leave Zp when page boundary is crossed
                fetchedAddress = (readPc()+regY) and 0xff
                false
            }
            AddrMode.Rel -> {
                val relative = readPc()
                fetchedAddress = if (relative >= 0x80) {
                    regPC-(256-relative) and 0xffff
                } else regPC+relative and 0xffff
                false
            }
            AddrMode.Abs -> {
                val lo = readPc()
                val hi = readPc()
                fetchedAddress = lo or (hi shl 8)
                false
            }
            AddrMode.AbsX -> {
                val lo = readPc()
                val hi = readPc()
                fetchedAddress = regX+(lo or (hi shl 8)) and 0xffff
                // if this address is a different page, extra clock cycle:
                (fetchedAddress and 0xff00) != hi shl 8
            }
            AddrMode.AbsY -> {
                val lo = readPc()
                val hi = readPc()
                fetchedAddress = regY+(lo or (hi shl 8)) and 0xffff
                // if this address is a different page, extra clock cycle:
                (fetchedAddress and 0xff00) != hi shl 8
            }
            AddrMode.Ind -> {
                var lo = readPc()
                var hi = readPc()
                fetchedAddress = lo or (hi shl 8)
                if (lo == 0xff) {
                    // emulate 6502 bug (fixed in 65C02):
                    // not able to fetch an address which crosses the page boundary.
                    lo = read(fetchedAddress)
                    hi = read(fetchedAddress and 0xff00)
                } else {
                    // normal behavior
                    lo = read(fetchedAddress)
                    hi = read(fetchedAddress+1)
                }
                fetchedAddress = lo or (hi shl 8)
                false
            }
            AddrMode.IzX -> {
                // note: not able to fetch an address which crosses the (zero)page boundary
                fetchedAddress = readPc()
                val lo = read((fetchedAddress+regX) and 0xff)
                val hi = read((fetchedAddress+regX+1) and 0xff)
                fetchedAddress = lo or (hi shl 8)
                false
            }
            AddrMode.IzY -> {
                // note: not able to fetch an address which crosses the (zero)page boundary
                fetchedAddress = readPc()
                val lo = read(fetchedAddress)
                val hi = read((fetchedAddress+1) and 0xff)
                fetchedAddress = regY+(lo or (hi shl 8)) and 0xffff
                false
            }
            AddrMode.Zpr, AddrMode.Izp, AddrMode.IaX -> {
                // addressing mode used by the 65C02 only
                throw InstructionError("65c02 addressing mode not implemented on 6502")
            }
        }
    }

    protected open fun dispatchOpcode(opcode: Int): Boolean {
        when (opcode) {
            0x00 -> iBrk()
            0x01 -> iOra()
            0x02 -> iInvalid()
            0x03 -> iSlo()
            0x04 -> iNop()
            0x05 -> iOra()
            0x06 -> iAsl()
            0x07 -> iSlo()
            0x08 -> iPhp()
            0x09 -> iOra()
            0x0a -> iAsl()
            0x0b -> iAnc()
            0x0c -> iNop()
            0x0d -> iOra()
            0x0e -> iAsl()
            0x0f -> iSlo()
            0x10 -> iBpl()
            0x11 -> iOra()
            0x12 -> iInvalid()
            0x13 -> iSlo()
            0x14 -> iNop()
            0x15 -> iOra()
            0x16 -> iAsl()
            0x17 -> iSlo()
            0x18 -> iClc()
            0x19 -> iOra()
            0x1a -> iNop()
            0x1b -> iSlo()
            0x1c -> iNop()
            0x1d -> iOra()
            0x1e -> iAsl()
            0x1f -> iSlo()
            0x20 -> iJsr()
            0x21 -> iAnd()
            0x22 -> iInvalid()
            0x23 -> iRla()
            0x24 -> iBit()
            0x25 -> iAnd()
            0x26 -> iRol()
            0x27 -> iRla()
            0x28 -> iPlp()
            0x29 -> iAnd()
            0x2a -> iRol()
            0x2b -> iAnc()
            0x2c -> iBit()
            0x2d -> iAnd()
            0x2e -> iRol()
            0x2f -> iRla()
            0x30 -> iBmi()
            0x31 -> iAnd()
            0x32 -> iInvalid()
            0x33 -> iRla()
            0x34 -> iNop()
            0x35 -> iAnd()
            0x36 -> iRol()
            0x37 -> iRla()
            0x38 -> iSec()
            0x39 -> iAnd()
            0x3a -> iNop()
            0x3b -> iRla()
            0x3c -> iNop()
            0x3d -> iAnd()
            0x3e -> iRol()
            0x3f -> iRla()
            0x40 -> iRti()
            0x41 -> iEor()
            0x42 -> iInvalid()
            0x43 -> iSre()
            0x44 -> iNop()
            0x45 -> iEor()
            0x46 -> iLsr()
            0x47 -> iSre()
            0x48 -> iPha()
            0x49 -> iEor()
            0x4a -> iLsr()
            0x4b -> iAlr()
            0x4c -> iJmp()
            0x4d -> iEor()
            0x4e -> iLsr()
            0x4f -> iSre()
            0x50 -> iBvc()
            0x51 -> iEor()
            0x52 -> iInvalid()
            0x53 -> iSre()
            0x54 -> iNop()
            0x55 -> iEor()
            0x56 -> iLsr()
            0x57 -> iSre()
            0x58 -> iCli()
            0x59 -> iEor()
            0x5a -> iNop()
            0x5b -> iSre()
            0x5c -> iNop()
            0x5d -> iEor()
            0x5e -> iLsr()
            0x5f -> iSre()
            0x60 -> iRts()
            0x61 -> iAdc()
            0x62 -> iInvalid()
            0x63 -> iRra()
            0x64 -> iNop()
            0x65 -> iAdc()
            0x66 -> iRor()
            0x67 -> iRra()
            0x68 -> iPla()
            0x69 -> iAdc()
            0x6a -> iRor()
            0x6b -> iArr()
            0x6c -> iJmp()
            0x6d -> iAdc()
            0x6e -> iRor()
            0x6f -> iRra()
            0x70 -> iBvs()
            0x71 -> iAdc()
            0x72 -> iInvalid()
            0x73 -> iRra()
            0x74 -> iNop()
            0x75 -> iAdc()
            0x76 -> iRor()
            0x77 -> iRra()
            0x78 -> iSei()
            0x79 -> iAdc()
            0x7a -> iNop()
            0x7b -> iRra()
            0x7c -> iNop()
            0x7d -> iAdc()
            0x7e -> iRor()
            0x7f -> iRra()
            0x80 -> iNop()
            0x81 -> iSta()
            0x82 -> iNop()
            0x83 -> iSax()
            0x84 -> iSty()
            0x85 -> iSta()
            0x86 -> iStx()
            0x87 -> iSax()
            0x88 -> iDey()
            0x89 -> iNop()
            0x8a -> iTxa()
            0x8b -> iXaa()
            0x8c -> iSty()
            0x8d -> iSta()
            0x8e -> iStx()
            0x8f -> iSax()
            0x90 -> iBcc()
            0x91 -> iSta()
            0x92 -> iInvalid()
            0x93 -> iAhx()
            0x94 -> iSty()
            0x95 -> iSta()
            0x96 -> iStx()
            0x97 -> iSax()
            0x98 -> iTya()
            0x99 -> iSta()
            0x9a -> iTxs()
            0x9b -> iTas()
            0x9c -> iShy()
            0x9d -> iSta()
            0x9e -> iShx()
            0x9f -> iAhx()
            0xa0 -> iLdy()
            0xa1 -> iLda()
            0xa2 -> iLdx()
            0xa3 -> iLax()
            0xa4 -> iLdy()
            0xa5 -> iLda()
            0xa6 -> iLdx()
            0xa7 -> iLax()
            0xa8 -> iTay()
            0xa9 -> iLda()
            0xaa -> iTax()
            0xab -> iLax()
            0xac -> iLdy()
            0xad -> iLda()
            0xae -> iLdx()
            0xaf -> iLax()
            0xb0 -> iBcs()
            0xb1 -> iLda()
            0xb2 -> iInvalid()
            0xb3 -> iLax()
            0xb4 -> iLdy()
            0xb5 -> iLda()
            0xb6 -> iLdx()
            0xb7 -> iLax()
            0xb8 -> iClv()
            0xb9 -> iLda()
            0xba -> iTsx()
            0xbb -> iLas()
            0xbc -> iLdy()
            0xbd -> iLda()
            0xbe -> iLdx()
            0xbf -> iLax()
            0xc0 -> iCpy()
            0xc1 -> iCmp()
            0xc2 -> iNop()
            0xc3 -> iDcp()
            0xc4 -> iCpy()
            0xc5 -> iCmp()
            0xc6 -> iDec()
            0xc7 -> iDcp()
            0xc8 -> iIny()
            0xc9 -> iCmp()
            0xca -> iDex()
            0xcb -> iAxs()
            0xcc -> iCpy()
            0xcd -> iCmp()
            0xce -> iDec()
            0xcf -> iDcp()
            0xd0 -> iBne()
            0xd1 -> iCmp()
            0xd2 -> iInvalid()
            0xd3 -> iDcp()
            0xd4 -> iNop()
            0xd5 -> iCmp()
            0xd6 -> iDec()
            0xd7 -> iDcp()
            0xd8 -> iCld()
            0xd9 -> iCmp()
            0xda -> iNop()
            0xdb -> iDcp()
            0xdc -> iNop()
            0xdd -> iCmp()
            0xde -> iDec()
            0xdf -> iDcp()
            0xe0 -> iCpx()
            0xe1 -> iSbc()
            0xe2 -> iNop()
            0xe3 -> iIsc()
            0xe4 -> iCpx()
            0xe5 -> iSbc()
            0xe6 -> iInc()
            0xe7 -> iIsc()
            0xe8 -> iInx()
            0xe9 -> iSbc()
            0xea -> iNop()
            0xeb -> iSbc()
            0xec -> iCpx()
            0xed -> iSbc()
            0xee -> iInc()
            0xef -> iIsc()
            0xf0 -> iBeq()
            0xf1 -> iSbc()
            0xf2 -> iInvalid()
            0xf3 -> iIsc()
            0xf4 -> iNop()
            0xf5 -> iSbc()
            0xf6 -> iInc()
            0xf7 -> iIsc()
            0xf8 -> iSed()
            0xf9 -> iSbc()
            0xfa -> iNop()
            0xfb -> iIsc()
            0xfc -> iNop()
            0xfd -> iSbc()
            0xfe -> iInc()
            0xff -> iIsc()
            else -> { /* can't occur */ }
        }
        return false        //  TODO determine if instructions can cause extra clock cycle
    }


    // official instructions

    protected open fun iAdc() {
        val operand = getFetched()
        if (regP.D) {
            // BCD add
            // see http://www.6502.org/tutorials/decimal_mode.html
            // and http://nesdev.com/6502.txt
            // and https://sourceforge.net/p/vice-emu/code/HEAD/tree/trunk/vice/src/6510core.c#l598
            // (the implementation below is based on the code used by Vice)
            var tmp = (regA and 0xf)+(operand and 0xf)+(if (regP.C) 1 else 0)
            if (tmp > 9) tmp += 6
            tmp = if (tmp <= 0x0f) {
                (tmp and 0xf)+(regA and 0xf0)+(operand and 0xf0)
            } else {
                (tmp and 0xf)+(regA and 0xf0)+(operand and 0xf0)+0x10
            }
            regP.Z = regA+operand+(if (regP.C) 1 else 0) and 0xff == 0
            regP.N = tmp and 0b10000000 != 0
            regP.V = (regA xor tmp) and 0x80 != 0 && (regA xor operand) and 0b10000000 == 0
            if (tmp and 0x1f0 > 0x90) tmp += 0x60
            regP.C = tmp > 0xf0     // original: (tmp and 0xff0) > 0xf0
            regA = tmp and 0xff
        } else {
            // normal add
            val tmp = operand+regA+if (regP.C) 1 else 0
            regP.N = (tmp and 0b10000000) != 0
            regP.Z = (tmp and 0xff) == 0
            regP.V = (regA xor operand).inv() and (regA xor tmp) and 0b10000000 != 0
            regP.C = tmp > 0xff
            regA = tmp and 0xff
        }
    }

    protected fun iAnd() {
        regA = regA and getFetched()
        regP.Z = regA == 0
        regP.N = (regA and 0b10000000) != 0
    }

    protected fun iAsl() {
        if (currentInstruction.mode == AddrMode.Acc) {
            regP.C = (regA and 0b10000000) != 0
            regA = (regA shl 1) and 0xff
            regP.Z = regA == 0
            regP.N = (regA and 0b10000000) != 0
        } else {
            val data = read(fetchedAddress)
            regP.C = (data and 0b10000000) != 0
            val shifted = (data shl 1) and 0xff
            write(fetchedAddress, shifted)
            regP.Z = shifted == 0
            regP.N = (shifted and 0b10000000) != 0
        }
    }

    protected fun iBcc() {
        if (!regP.C) regPC = fetchedAddress
    }

    protected fun iBcs() {
        if (regP.C) regPC = fetchedAddress
    }

    protected fun iBeq() {
        if (regP.Z) regPC = fetchedAddress
    }

    protected open fun iBit() {
        val operand = getFetched()
        regP.Z = (regA and operand) == 0
        regP.V = (operand and 0b01000000) != 0
        regP.N = (operand and 0b10000000) != 0
    }

    protected fun iBmi() {
        if (regP.N) regPC = fetchedAddress
    }

    protected fun iBne() {
        if (!regP.Z) regPC = fetchedAddress
    }

    protected fun iBpl() {
        if (!regP.N) regPC = fetchedAddress
    }

    protected open fun iBrk() {
        // handle BRK ('software interrupt')
        regPC++
        if(nmiAsserted)
            return      // if an NMI occurs during BRK, the BRK won't get executed on 6502 (65C02 fixes this)
        pushStackAddr(regPC)
        regP.B = true
        pushStack(regP)
        regP.I = true     // interrupts are now disabled
        // NMOS 6502 doesn't clear the D flag (CMOS 65C02 version does...)
        regPC = readWord(IRQ_vector)

        // TODO prevent NMI from triggering immediately after IRQ/BRK... how does that work exactly?
    }

    protected open fun handleInterrupt() {
        // handle NMI or IRQ -- very similar to the BRK opcode above
        pushStackAddr(regPC)
        regPC++
        regP.B = false
        pushStack(regP)
        regP.I = true     // interrupts are now disabled
        // NMOS 6502 doesn't clear the D flag (CMOS 65C02 version does...)

        // jump to the appropriate irq vector and clear the assertion status of the irq
        // (hmm... should the cpu do that? or is this the peripheral's job?)
        if(nmiAsserted) {
            regPC = readWord(NMI_vector)
            nmiAsserted = false
        } else {
            regPC = readWord(IRQ_vector)
            irqAsserted = false
        }

        // TODO prevent NMI from triggering immediately after IRQ/BRK... how does that work exactly?
    }

    protected fun iBvc() {
        if (!regP.V) regPC = fetchedAddress
    }

    protected fun iBvs() {
        if (regP.V) regPC = fetchedAddress
    }

    protected fun iClc() {
        regP.C = false
    }

    protected fun iCld() {
        regP.D = false
    }

    protected fun iCli() {
        regP.I = false
    }

    protected fun iClv() {
        regP.V = false
    }

    protected fun iCmp() {
        val fetched = getFetched()
        regP.C = regA >= fetched
        regP.Z = regA == fetched
        regP.N = ((regA-fetched) and 0b10000000) != 0
    }

    protected fun iCpx() {
        val fetched = getFetched()
        regP.C = regX >= fetched
        regP.Z = regX == fetched
        regP.N = ((regX-fetched) and 0b10000000) != 0
    }

    protected fun iCpy() {
        val fetched = getFetched()
        regP.C = regY >= fetched
        regP.Z = regY == fetched
        regP.N = ((regY-fetched) and 0b10000000) != 0
    }

    protected open fun iDec() {
        val data = (read(fetchedAddress)-1) and 0xff
        write(fetchedAddress, data)
        regP.Z = data == 0
        regP.N = (data and 0b10000000) != 0
    }

    protected fun iDex() {
        regX = (regX-1) and 0xff
        regP.Z = regX == 0
        regP.N = (regX and 0b10000000) != 0
    }

    protected fun iDey() {
        regY = (regY-1) and 0xff
        regP.Z = regY == 0
        regP.N = (regY and 0b10000000) != 0
    }

    protected fun iEor() {
        regA = regA xor getFetched()
        regP.Z = regA == 0
        regP.N = (regA and 0b10000000) != 0
    }

    protected open fun iInc() {
        val data = (read(fetchedAddress)+1) and 0xff
        write(fetchedAddress, data)
        regP.Z = data == 0
        regP.N = (data and 0b10000000) != 0
    }

    protected fun iInx() {
        regX = (regX+1) and 0xff
        regP.Z = regX == 0
        regP.N = (regX and 0b10000000) != 0
    }

    protected fun iIny() {
        regY = (regY+1) and 0xff
        regP.Z = regY == 0
        regP.N = (regY and 0b10000000) != 0
    }

    protected fun iJmp() {
        regPC = fetchedAddress
    }

    protected fun iJsr() {
        pushStackAddr(regPC-1)
        regPC = fetchedAddress
    }

    protected fun iLda() {
        regA = getFetched()
        regP.Z = regA == 0
        regP.N = (regA and 0b10000000) != 0
    }

    protected fun iLdx() {
        regX = getFetched()
        regP.Z = regX == 0
        regP.N = (regX and 0b10000000) != 0
    }

    protected fun iLdy() {
        regY = getFetched()
        regP.Z = regY == 0
        regP.N = (regY and 0b10000000) != 0
    }

    protected fun iLsr() {
        if (currentInstruction.mode == AddrMode.Acc) {
            regP.C = (regA and 1) == 1
            regA = regA ushr 1
            regP.Z = regA == 0
            regP.N = (regA and 0b10000000) != 0
        } else {
            val data = read(fetchedAddress)
            regP.C = (data and 1) == 1
            val shifted = data ushr 1
            write(fetchedAddress, shifted)
            regP.Z = shifted == 0
            regP.N = (shifted and 0b10000000) != 0
        }
    }

    protected fun iNop() {}

    protected fun iOra() {
        regA = regA or getFetched()
        regP.Z = regA == 0
        regP.N = (regA and 0b10000000) != 0
    }

    protected fun iPha() {
        pushStack(regA)
    }

    protected fun iPhp() {
        val origBreakflag = regP.B
        regP.B = true
        pushStack(regP)
        regP.B = origBreakflag
    }

    protected fun iPla() {
        regA = popStack()
        regP.Z = regA == 0
        regP.N = (regA and 0b10000000) != 0
    }

    protected fun iPlp() {
        regP.fromInt(popStack())
        regP.B = true  // break is always 1 except when pushing on stack
    }

    protected fun iRol() {
        val oldCarry = regP.C
        if (currentInstruction.mode == AddrMode.Acc) {
            regP.C = (regA and 0b10000000) != 0
            regA = (regA shl 1 and 0xff) or (if (oldCarry) 1 else 0)
            regP.Z = regA == 0
            regP.N = (regA and 0b10000000) != 0
        } else {
            val data = read(fetchedAddress)
            regP.C = (data and 0b10000000) != 0
            val shifted = (data shl 1 and 0xff) or (if (oldCarry) 1 else 0)
            write(fetchedAddress, shifted)
            regP.Z = shifted == 0
            regP.N = (shifted and 0b10000000) != 0
        }
    }

    protected fun iRor() {
        val oldCarry = regP.C
        if (currentInstruction.mode == AddrMode.Acc) {
            regP.C = (regA and 1) == 1
            regA = (regA ushr 1) or (if (oldCarry) 0b10000000 else 0)
            regP.Z = regA == 0
            regP.N = (regA and 0b10000000) != 0
        } else {
            val data = read(fetchedAddress)
            regP.C = (data and 1) == 1
            val shifted = (data ushr 1) or (if (oldCarry) 0b10000000 else 0)
            write(fetchedAddress, shifted)
            regP.Z = shifted == 0
            regP.N = (shifted and 0b10000000) != 0
        }
    }

    protected fun iRti() {
        regP.fromInt(popStack())
        regP.B = true  // break is always 1 except when pushing on stack
        regPC = popStackAddr()
    }

    protected fun iRts() {
        regPC = popStackAddr()
        regPC = (regPC+1) and 0xffff
    }

    protected open fun iSbc() {
        val operand = getFetched()
        val tmp = (regA-operand-if (regP.C) 0 else 1) and 0xffff
        regP.V = (regA xor operand) and (regA xor tmp) and 0b10000000 != 0
        if (regP.D) {
            // BCD subtract
            // see http://www.6502.org/tutorials/decimal_mode.html
            // and http://nesdev.com/6502.txt
            // and https://sourceforge.net/p/vice-emu/code/HEAD/tree/trunk/vice/src/6510core.c#l1396
            // (the implementation below is based on the code used by Vice)
            var tmpA = ((regA and 0xf)-(operand and 0xf)-if (regP.C) 0 else 1) and 0xffff
            tmpA = if ((tmpA and 0x10) != 0) {
                ((tmpA-6) and 0xf) or (regA and 0xf0)-(operand and 0xf0)-0x10
            } else {
                (tmpA and 0xf) or (regA and 0xf0)-(operand and 0xf0)
            }
            if ((tmpA and 0x100) != 0) tmpA -= 0x60
            regA = tmpA and 0xff
        } else {
            // normal subtract
            regA = tmp and 0xff
        }
        regP.C = tmp < 0x100
        regP.Z = (tmp and 0xff) == 0
        regP.N = (tmp and 0b10000000) != 0
    }

    protected fun iSec() {
        regP.C = true
    }

    protected fun iSed() {
        regP.D = true
    }

    protected fun iSei() {
        regP.I = true
    }

    protected fun iSta() {
        write(fetchedAddress, regA)
    }

    protected fun iStx() {
        write(fetchedAddress, regX)
    }

    protected fun iSty() {
        write(fetchedAddress, regY)
    }

    protected fun iTax() {
        regX = regA
        regP.Z = regX == 0
        regP.N = (regX and 0b10000000) != 0
    }

    protected fun iTay() {
        regY = regA
        regP.Z = regY == 0
        regP.N = (regY and 0b10000000) != 0
    }

    protected fun iTsx() {
        regX = regSP
        regP.Z = regX == 0
        regP.N = (regX and 0b10000000) != 0
    }

    protected fun iTxa() {
        regA = regX
        regP.Z = regA == 0
        regP.N = (regA and 0b10000000) != 0
    }

    protected fun iTxs() {
        regSP = regX
    }

    protected fun iTya() {
        regA = regY
        regP.Z = regA == 0
        regP.N = (regA and 0b10000000) != 0
    }

    // unofficial/illegal 6502 instructions

    private fun iAhx() {
        TODO("\$${hexB(currentOpcode)} - ahx - ('illegal' instruction) @ \$${hexW(currentOpcodeAddress)}")
    }

    private fun iAlr() {
        TODO("\$${hexB(currentOpcode)} - alr=asr - ('illegal' instruction) @ \$${hexW(currentOpcodeAddress)}")
    }

    private fun iAnc() {
        TODO("\$${hexB(currentOpcode)} - anc - ('illegal' instruction) @ \$${hexW(currentOpcodeAddress)}")
    }

    private fun iArr() {
        TODO("\$${hexB(currentOpcode)} - arr - ('illegal' instruction) @ \$${hexW(currentOpcodeAddress)}")
    }

    private fun iAxs() {
        TODO("\$${hexB(currentOpcode)} - axs - ('illegal' instruction) @ \$${hexW(currentOpcodeAddress)}")
    }

    private fun iDcp() {
        TODO("\$${hexB(currentOpcode)} - dcp - ('illegal' instruction) @ \$${hexW(currentOpcodeAddress)}")
    }

    private fun iIsc() {
        TODO("\$${hexB(currentOpcode)} - isc=isb - ('illegal' instruction) @ \$${hexW(currentOpcodeAddress)}")
    }

    private fun iLas() {
        TODO("\$${hexB(currentOpcode)} - las=lar - ('illegal' instruction) @ \$${hexW(currentOpcodeAddress)}")
    }

    private fun iLax() {
        TODO("\$${hexB(currentOpcode)} - lax - ('illegal' instruction) @ \$${hexW(currentOpcodeAddress)}")
    }

    private fun iRla() {
        TODO("\$${hexB(currentOpcode)} - rla - ('illegal' instruction) @ \$${hexW(currentOpcodeAddress)}")
    }

    private fun iRra() {
        TODO("\$${hexB(currentOpcode)} - rra - ('illegal' instruction) @ \$${hexW(currentOpcodeAddress)}")
    }

    private fun iSax() {
        TODO("\$${hexB(currentOpcode)} - sax - ('illegal' instruction) @ \$${hexW(currentOpcodeAddress)}")
    }

    private fun iShx() {
        TODO("\$${hexB(currentOpcode)} - shx - ('illegal' instruction) @ \$${hexW(currentOpcodeAddress)}")
    }

    private fun iShy() {
        TODO("\$${hexB(currentOpcode)} - shy - ('illegal' instruction) @ \$${hexW(currentOpcodeAddress)}")
    }

    private fun iSlo() {
        TODO("\$${hexB(currentOpcode)} - slo=aso - ('illegal' instruction) @ \$${hexW(currentOpcodeAddress)}")
    }

    private fun iSre() {
        TODO("\$${hexB(currentOpcode)} - sre=lse - ('illegal' instruction) @ \$${hexW(currentOpcodeAddress)}")
    }

    private fun iTas() {
        TODO("\$${hexB(currentOpcode)} - tas - ('illegal' instruction) @ \$${hexW(currentOpcodeAddress)}")
    }

    private fun iXaa() {
        TODO("\$${hexB(currentOpcode)} - xaa - ('illegal' instruction) @ \$${hexW(currentOpcodeAddress)}")
    }

    // invalid instruction (JAM / KIL / HLT)
    private fun iInvalid() {
        throw InstructionError("invalid instruction encountered: opcode=${hexB(currentOpcode)} instr=${currentInstruction.mnemonic} @ ${hexW(currentOpcodeAddress)}")
    }
}
