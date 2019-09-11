package razorvine.ksim65.components

// TODO: implement the illegal opcodes, see http://www.ffd2.com/fridge/docs/6502-NMOS.extra.opcodes
// TODO: add the optional additional cycles to certain instructions and addressing modes


open class Cpu6502(private val stopOnBrk: Boolean) : BusComponent() {
    var tracing: Boolean = false
    var totalCycles: Long = 0
        private set

    class InstructionError(msg: String) : RuntimeException(msg)

    companion object {
        const val NMI_vector = 0xfffa
        const val RESET_vector = 0xfffc
        const val IRQ_vector = 0xfffe
        const val resetCycles = 8

        fun hexW(number: Address, allowSingleByte: Boolean = false): String {
            val msb = number ushr 8
            val lsb = number and 0xff
            return if (msb == 0 && allowSingleByte)
                hexB(lsb)
            else
                hexB(msb) + hexB(
                    lsb
                )
        }

        private const val hexdigits = "0123456789abcdef"

        fun hexB(number: Short): String = hexB(number.toInt())

        fun hexB(number: Int): String {
            val loNibble = number and 15
            val hiNibble = number ushr 4
            return hexdigits[hiNibble].toString() + hexdigits[loNibble]
        }
    }

    enum class AddrMode {
        Imp,
        Acc,
        Imm,
        Zp,
        ZpX,
        ZpY,
        Rel,
        Abs,
        AbsX,
        AbsY,
        Ind,
        IzX,
        IzY
    }

    class Instruction(val mnemonic: String, val mode: AddrMode, val cycles: Int)

    class StatusRegister(
        var C: Boolean = false,
        var Z: Boolean = false,
        var I: Boolean = false,
        var D: Boolean = false,
        var B: Boolean = false,
        var V: Boolean = false,
        var N: Boolean = false
    ) {
        fun asByte(): UByte {
            return (0b00100000 or
                    (if (N) 0b10000000 else 0) or
                    (if (V) 0b01000000 else 0) or
                    (if (B) 0b00010000 else 0) or
                    (if (D) 0b00001000 else 0) or
                    (if (I) 0b00000100 else 0) or
                    (if (Z) 0b00000010 else 0) or
                    (if (C) 0b00000001 else 0)
                    ).toShort()
        }

        fun fromByte(byte: Int) {
            N = (byte and 0b10000000) != 0
            V = (byte and 0b01000000) != 0
            B = (byte and 0b00010000) != 0
            D = (byte and 0b00001000) != 0
            I = (byte and 0b00000100) != 0
            Z = (byte and 0b00000010) != 0
            C = (byte and 0b00000001) != 0
        }

        override fun toString(): String {
            return asByte().toString(2).padStart(8, '0')
        }

        override fun hashCode(): Int = asByte().toInt()

        override fun equals(other: Any?): Boolean {
            if (other !is StatusRegister)
                return false
            return asByte() == other.asByte()
        }
    }


    var instrCycles: Int = 0
    var A: Int = 0
    var X: Int = 0
    var Y: Int = 0
    var SP: Int = 0
    var PC: Address = 0
    val Status = StatusRegister()
    var currentOpcode: Int = 0
    private lateinit var currentInstruction: Instruction

    // has an interrupt been requested?
    private var pendingInterrupt: Pair<Boolean, BusComponent>? = null

    // data byte from the instruction (only set when addr.mode is Accumulator, Immediate or Implied)
    private var fetchedData: Int = 0

    // all other addressing modes yield a fetched memory address
    private var fetchedAddress: Address = 0

    private val breakpoints = mutableMapOf<Address, (cpu: Cpu6502, pc: Address) -> Unit>()

    fun breakpoint(address: Address, action: (cpu: Cpu6502, pc: Address) -> Unit) {
        breakpoints[address] = action
    }

    fun disassemble(component: MemoryComponent, from: Address, to: Address) =
        disassemble(component.cloneContents(), component.startAddress, from, to)

    fun disassemble(memory: Array<UByte>, baseAddress: Address, from: Address, to: Address): List<String> {
        var address = from - baseAddress
        val spacing1 = "        "
        val spacing2 = "     "
        val spacing3 = "  "
        val result = mutableListOf<String>()

        while (address <= (to - baseAddress)) {
            val byte = memory[address]
            var line = "\$${hexW(address)}  ${hexB(
                byte
            )} "
            address++
            val opcode = instructions[byte.toInt()]
            when (opcode.mode) {
                AddrMode.Acc -> {
                    line += "$spacing1 ${opcode.mnemonic}  a"
                }
                AddrMode.Imp -> {
                    line += "$spacing1 ${opcode.mnemonic}"
                }
                AddrMode.Imm -> {
                    val value = memory[address++]
                    line += "${hexB(value)} $spacing2 ${opcode.mnemonic}  #\$${hexB(
                        value
                    )}"
                }
                AddrMode.Zp -> {
                    val zpAddr = memory[address++]
                    line += "${hexB(zpAddr)} $spacing2 ${opcode.mnemonic}  \$${hexB(
                        zpAddr
                    )}"
                }
                AddrMode.ZpX -> {
                    val zpAddr = memory[address++]
                    line += "${hexB(zpAddr)} $spacing2 ${opcode.mnemonic}  \$${hexB(
                        zpAddr
                    )},x"
                }
                AddrMode.ZpY -> {
                    val zpAddr = memory[address++]
                    line += "${hexB(zpAddr)} $spacing2 ${opcode.mnemonic}  \$${hexB(
                        zpAddr
                    )},y"
                }
                AddrMode.Rel -> {
                    val rel = memory[address++]
                    val target =
                        if (rel <= 0x7f)
                            address + rel
                        else
                            address - (256 - rel)
                    line += "${hexB(rel)} $spacing2 ${opcode.mnemonic}  \$${hexW(
                        target,
                        true
                    )}"
                }
                AddrMode.Abs -> {
                    val lo = memory[address++]
                    val hi = memory[address++]
                    val absAddr = lo.toInt() or (hi.toInt() shl 8)
                    line += "${hexB(lo)} ${hexB(
                        hi
                    )} $spacing3 ${opcode.mnemonic}  \$${hexW(absAddr)}"
                }
                AddrMode.AbsX -> {
                    val lo = memory[address++]
                    val hi = memory[address++]
                    val absAddr = lo.toInt() or (hi.toInt() shl 8)
                    line += "${hexB(lo)} ${hexB(
                        hi
                    )} $spacing3 ${opcode.mnemonic}  \$${hexW(absAddr)},x"
                }
                AddrMode.AbsY -> {
                    val lo = memory[address++]
                    val hi = memory[address++]
                    val absAddr = lo.toInt() or (hi.toInt() shl 8)
                    line += "${hexB(lo)} ${hexB(
                        hi
                    )} $spacing3 ${opcode.mnemonic}  \$${hexW(absAddr)},y"
                }
                AddrMode.Ind -> {
                    val lo = memory[address++]
                    val hi = memory[address++]
                    val indirectAddr = lo.toInt() or (hi.toInt() shl 8)
                    line += "${hexB(lo)} ${hexB(
                        hi
                    )} $spacing3 ${opcode.mnemonic}  (\$${hexW(
                        indirectAddr
                    )})"
                }
                AddrMode.IzX -> {
                    val zpAddr = memory[address++]
                    line += "${hexB(zpAddr)} $spacing2 ${opcode.mnemonic}  (\$${hexB(
                        zpAddr
                    )},x)"
                }
                AddrMode.IzY -> {
                    val zpAddr = memory[address++]
                    line += "${hexB(zpAddr)} $spacing2 ${opcode.mnemonic}  (\$${hexB(
                        zpAddr
                    )}),y"
                }
            }
            result.add(line)
        }

        return result
    }

    override fun reset() {
        SP = 0xfd
        PC = readWord(RESET_vector)
        A = 0
        X = 0
        Y = 0
        Status.C = false
        Status.Z = false
        Status.I = true
        Status.D = false
        Status.B = false
        Status.V = false
        Status.N = false
        instrCycles = resetCycles       // a reset takes time as well
        currentOpcode = 0
        currentInstruction = instructions[0]
    }

    override fun clock() {
        if (instrCycles == 0) {
            if (pendingInterrupt != null) {
                // NMI or IRQ interrupt.
                // handled by the BRK instruction logic.
                currentOpcode = 0
                currentInstruction = instructions[0]
            } else {
                // no interrupt, fetch next instruction from memory
                currentOpcode = read(PC)
                currentInstruction = instructions[currentOpcode]

                if (tracing) printState()

                breakpoints[PC]?.let {
                    val oldPC = PC
                    val oldOpcode = currentOpcode
                    it(this, PC)
                    if (PC != oldPC)
                        return clock()
                    if (oldOpcode != currentOpcode)
                        currentInstruction = instructions[currentOpcode]
                }

                if (stopOnBrk && currentOpcode == 0) {
                    throw InstructionError(
                        "stopped on BRK instruction at ${hexW(
                            PC
                        )}"
                    )
                }
            }

            PC++
            instrCycles = currentInstruction.cycles
            applyAddressingMode(currentInstruction.mode)
            dispatchOpcode(currentOpcode)
        }

        instrCycles--
        totalCycles++
    }

    fun step() {
        // step a whole instruction
        while (instrCycles > 0) clock()        // remaining instruction subcycles from the previous instruction
        clock()   // the actual instruction execution cycle
        while (instrCycles > 0) clock()        // instruction subcycles
    }

    fun nmi(source: BusComponent) {
        pendingInterrupt = Pair(true, source)
    }

    fun irq(source: BusComponent) {
        if (!Status.I)
            pendingInterrupt = Pair(false, source)
    }

    fun printState() {
        println(
            "cycle:$totalCycles - pc=${hexW(PC)} " +
                    "A=${hexB(A)} " +
                    "X=${hexB(X)} " +
                    "Y=${hexB(Y)} " +
                    "SP=${hexB(SP)} " +
                    " n=" + (if (Status.N) "1" else "0") +
                    " v=" + (if (Status.V) "1" else "0") +
                    " b=" + (if (Status.B) "1" else "0") +
                    " d=" + (if (Status.D) "1" else "0") +
                    " i=" + (if (Status.I) "1" else "0") +
                    " z=" + (if (Status.Z) "1" else "0") +
                    " c=" + (if (Status.C) "1" else "0") +
                    "  icycles=$instrCycles  instr=${hexB(currentOpcode)}:${currentInstruction.mnemonic}"
        )
    }

    private fun getFetched() =
        if (currentInstruction.mode == AddrMode.Imm ||
            currentInstruction.mode == AddrMode.Acc ||
            currentInstruction.mode == AddrMode.Imp
        )
            fetchedData
        else
            read(fetchedAddress)

    private fun readPc(): Int = bus.read(PC++).toInt()

    private fun pushStackAddr(address: Address) {
        val lo = address and 0xff
        val hi = (address ushr 8)
        pushStack(hi)
        pushStack(lo)
    }

    private fun pushStack(status: StatusRegister) {
        pushStack(status.asByte().toInt())
    }

    private fun pushStack(data: Int) {
        write(SP or 0x0100, data)
        SP = (SP - 1) and 0xff
    }

    private fun popStack(): Int {
        SP = (SP + 1) and 0xff
        return read(SP or 0x0100)
    }

    private fun popStackAddr(): Address {
        val lo = popStack()
        val hi = popStack()
        return lo or (hi shl 8)
    }

    private fun read(address: Address): Int = bus.read(address).toInt()
    private fun readWord(address: Address): Int = bus.read(address).toInt() or (bus.read(address + 1).toInt() shl 8)
    private fun write(address: Address, data: Int) = bus.write(address, data.toShort())

    // opcodes table from  http://www.oxyron.de/html/opcodes02.html
    private val instructions = listOf(
        Instruction("brk", AddrMode.Imp, 7),
        Instruction("ora", AddrMode.IzX, 6),
        Instruction("???", AddrMode.Imp, 0),
        Instruction("slo", AddrMode.IzX, 8),
        Instruction("nop", AddrMode.Zp, 3),
        Instruction("ora", AddrMode.Zp, 3),
        Instruction("asl", AddrMode.Zp, 5),
        Instruction("slo", AddrMode.Zp, 5),
        Instruction("php", AddrMode.Imp, 3),
        Instruction("ora", AddrMode.Imm, 2),
        Instruction("asl", AddrMode.Acc, 2),
        Instruction("anc", AddrMode.Imm, 2),
        Instruction("nop", AddrMode.Abs, 4),
        Instruction("ora", AddrMode.Abs, 4),
        Instruction("asl", AddrMode.Abs, 6),
        Instruction("slo", AddrMode.Abs, 6),
        Instruction("bpl", AddrMode.Rel, 2),
        Instruction("ora", AddrMode.IzY, 5),
        Instruction("???", AddrMode.Imp, 0),
        Instruction("slo", AddrMode.IzY, 6),
        Instruction("nop", AddrMode.ZpX, 4),
        Instruction("ora", AddrMode.ZpX, 4),
        Instruction("asl", AddrMode.ZpX, 6),
        Instruction("slo", AddrMode.ZpX, 6),
        Instruction("clc", AddrMode.Imp, 2),
        Instruction("ora", AddrMode.AbsY, 4),
        Instruction("nop", AddrMode.Imp, 2),
        Instruction("slo", AddrMode.AbsY, 7),
        Instruction("nop", AddrMode.AbsX, 4),
        Instruction("ora", AddrMode.AbsX, 4),
        Instruction("asl", AddrMode.AbsX, 7),
        Instruction("slo", AddrMode.AbsX, 7),
        Instruction("jsr", AddrMode.Abs, 6),
        Instruction("and", AddrMode.IzX, 6),
        Instruction("???", AddrMode.Imp, 0),
        Instruction("rla", AddrMode.IzX, 8),
        Instruction("bit", AddrMode.Zp, 3),
        Instruction("and", AddrMode.Zp, 3),
        Instruction("rol", AddrMode.Zp, 5),
        Instruction("rla", AddrMode.Zp, 5),
        Instruction("plp", AddrMode.Imp, 4),
        Instruction("and", AddrMode.Imm, 2),
        Instruction("rol", AddrMode.Acc, 2),
        Instruction("anc", AddrMode.Imm, 2),
        Instruction("bit", AddrMode.Abs, 4),
        Instruction("and", AddrMode.Abs, 4),
        Instruction("rol", AddrMode.Abs, 6),
        Instruction("rla", AddrMode.Abs, 6),
        Instruction("bmi", AddrMode.Rel, 2),
        Instruction("and", AddrMode.IzY, 5),
        Instruction("???", AddrMode.Imp, 0),
        Instruction("rla", AddrMode.IzY, 8),
        Instruction("nop", AddrMode.ZpX, 4),
        Instruction("and", AddrMode.ZpX, 4),
        Instruction("rol", AddrMode.ZpX, 6),
        Instruction("rla", AddrMode.ZpX, 6),
        Instruction("sec", AddrMode.Imp, 2),
        Instruction("and", AddrMode.AbsY, 4),
        Instruction("nop", AddrMode.Imp, 2),
        Instruction("rla", AddrMode.AbsY, 7),
        Instruction("nop", AddrMode.AbsX, 4),
        Instruction("and", AddrMode.AbsX, 4),
        Instruction("rol", AddrMode.AbsX, 7),
        Instruction("rla", AddrMode.AbsX, 7),
        Instruction("rti", AddrMode.Imp, 6),
        Instruction("eor", AddrMode.IzX, 6),
        Instruction("???", AddrMode.Imp, 0),
        Instruction("sre", AddrMode.IzX, 8),
        Instruction("nop", AddrMode.Zp, 3),
        Instruction("eor", AddrMode.Zp, 3),
        Instruction("lsr", AddrMode.Zp, 5),
        Instruction("sre", AddrMode.Zp, 5),
        Instruction("pha", AddrMode.Imp, 3),
        Instruction("eor", AddrMode.Imm, 2),
        Instruction("lsr", AddrMode.Acc, 2),
        Instruction("alr", AddrMode.Imm, 2),
        Instruction("jmp", AddrMode.Abs, 3),
        Instruction("eor", AddrMode.Abs, 4),
        Instruction("lsr", AddrMode.Abs, 6),
        Instruction("sre", AddrMode.Abs, 6),
        Instruction("bvc", AddrMode.Rel, 2),
        Instruction("eor", AddrMode.IzY, 5),
        Instruction("???", AddrMode.Imp, 0),
        Instruction("sre", AddrMode.IzY, 8),
        Instruction("nop", AddrMode.ZpX, 4),
        Instruction("eor", AddrMode.ZpX, 4),
        Instruction("lsr", AddrMode.ZpX, 6),
        Instruction("sre", AddrMode.ZpX, 6),
        Instruction("cli", AddrMode.Imp, 2),
        Instruction("eor", AddrMode.AbsY, 4),
        Instruction("nop", AddrMode.Imp, 2),
        Instruction("sre", AddrMode.AbsY, 7),
        Instruction("nop", AddrMode.AbsX, 4),
        Instruction("eor", AddrMode.AbsX, 4),
        Instruction("lsr", AddrMode.AbsX, 7),
        Instruction("sre", AddrMode.AbsX, 7),
        Instruction("rts", AddrMode.Imp, 6),
        Instruction("adc", AddrMode.IzX, 6),
        Instruction("???", AddrMode.Imp, 0),
        Instruction("rra", AddrMode.IzX, 8),
        Instruction("nop", AddrMode.Zp, 3),
        Instruction("adc", AddrMode.Zp, 3),
        Instruction("ror", AddrMode.Zp, 5),
        Instruction("rra", AddrMode.Zp, 5),
        Instruction("pla", AddrMode.Imp, 4),
        Instruction("adc", AddrMode.Imm, 2),
        Instruction("ror", AddrMode.Acc, 2),
        Instruction("arr", AddrMode.Imm, 2),
        Instruction("jmp", AddrMode.Ind, 5),
        Instruction("adc", AddrMode.Abs, 4),
        Instruction("ror", AddrMode.Abs, 6),
        Instruction("rra", AddrMode.Abs, 6),
        Instruction("bvs", AddrMode.Rel, 2),
        Instruction("adc", AddrMode.IzY, 5),
        Instruction("???", AddrMode.Imp, 0),
        Instruction("rra", AddrMode.IzY, 8),
        Instruction("nop", AddrMode.ZpX, 4),
        Instruction("adc", AddrMode.ZpX, 4),
        Instruction("ror", AddrMode.ZpX, 6),
        Instruction("rra", AddrMode.ZpX, 6),
        Instruction("sei", AddrMode.Imp, 2),
        Instruction("adc", AddrMode.AbsY, 4),
        Instruction("nop", AddrMode.Imp, 2),
        Instruction("rra", AddrMode.AbsY, 7),
        Instruction("nop", AddrMode.AbsX, 4),
        Instruction("adc", AddrMode.AbsX, 4),
        Instruction("ror", AddrMode.AbsX, 7),
        Instruction("rra", AddrMode.AbsX, 7),
        Instruction("nop", AddrMode.Imm, 2),
        Instruction("sta", AddrMode.IzX, 6),
        Instruction("nop", AddrMode.Imm, 2),
        Instruction("sax", AddrMode.IzX, 6),
        Instruction("sty", AddrMode.Zp, 3),
        Instruction("sta", AddrMode.Zp, 3),
        Instruction("stx", AddrMode.Zp, 3),
        Instruction("sax", AddrMode.Zp, 3),
        Instruction("dey", AddrMode.Imp, 2),
        Instruction("nop", AddrMode.Imm, 2),
        Instruction("txa", AddrMode.Imp, 2),
        Instruction("xaa", AddrMode.Imm, 2),
        Instruction("sty", AddrMode.Abs, 4),
        Instruction("sta", AddrMode.Abs, 4),
        Instruction("stx", AddrMode.Abs, 4),
        Instruction("sax", AddrMode.Abs, 4),
        Instruction("bcc", AddrMode.Rel, 2),
        Instruction("sta", AddrMode.IzY, 6),
        Instruction("???", AddrMode.Imp, 0),
        Instruction("ahx", AddrMode.IzY, 6),
        Instruction("sty", AddrMode.ZpX, 4),
        Instruction("sta", AddrMode.ZpX, 4),
        Instruction("stx", AddrMode.ZpY, 4),
        Instruction("sax", AddrMode.ZpY, 4),
        Instruction("tya", AddrMode.Imp, 2),
        Instruction("sta", AddrMode.AbsY, 5),
        Instruction("txs", AddrMode.Imp, 2),
        Instruction("tas", AddrMode.AbsY, 5),
        Instruction("shy", AddrMode.AbsX, 5),
        Instruction("sta", AddrMode.AbsX, 5),
        Instruction("shx", AddrMode.AbsY, 5),
        Instruction("ahx", AddrMode.AbsY, 5),
        Instruction("ldy", AddrMode.Imm, 2),
        Instruction("lda", AddrMode.IzX, 6),
        Instruction("ldx", AddrMode.Imm, 2),
        Instruction("lax", AddrMode.IzX, 6),
        Instruction("ldy", AddrMode.Zp, 3),
        Instruction("lda", AddrMode.Zp, 3),
        Instruction("ldx", AddrMode.Zp, 3),
        Instruction("lax", AddrMode.Zp, 3),
        Instruction("tay", AddrMode.Imp, 2),
        Instruction("lda", AddrMode.Imm, 2),
        Instruction("tax", AddrMode.Imp, 2),
        Instruction("lax", AddrMode.Imm, 2),
        Instruction("ldy", AddrMode.Abs, 4),
        Instruction("lda", AddrMode.Abs, 4),
        Instruction("ldx", AddrMode.Abs, 4),
        Instruction("lax", AddrMode.Abs, 4),
        Instruction("bcs", AddrMode.Rel, 2),
        Instruction("lda", AddrMode.IzY, 5),
        Instruction("???", AddrMode.Imp, 0),
        Instruction("lax", AddrMode.IzY, 5),
        Instruction("ldy", AddrMode.ZpX, 4),
        Instruction("lda", AddrMode.ZpX, 4),
        Instruction("ldx", AddrMode.ZpY, 4),
        Instruction("lax", AddrMode.ZpY, 4),
        Instruction("clv", AddrMode.Imp, 2),
        Instruction("lda", AddrMode.AbsY, 4),
        Instruction("tsx", AddrMode.Imp, 2),
        Instruction("las", AddrMode.AbsY, 4),
        Instruction("ldy", AddrMode.AbsX, 4),
        Instruction("lda", AddrMode.AbsX, 4),
        Instruction("ldx", AddrMode.AbsY, 4),
        Instruction("lax", AddrMode.AbsY, 4),
        Instruction("cpy", AddrMode.Imm, 2),
        Instruction("cmp", AddrMode.IzX, 6),
        Instruction("nop", AddrMode.Imm, 2),
        Instruction("dcp", AddrMode.IzX, 8),
        Instruction("cpy", AddrMode.Zp, 3),
        Instruction("cmp", AddrMode.Zp, 3),
        Instruction("dec", AddrMode.Zp, 5),
        Instruction("dcp", AddrMode.Zp, 5),
        Instruction("iny", AddrMode.Imp, 2),
        Instruction("cmp", AddrMode.Imm, 2),
        Instruction("dex", AddrMode.Imp, 2),
        Instruction("axs", AddrMode.Imm, 2),
        Instruction("cpy", AddrMode.Abs, 4),
        Instruction("cmp", AddrMode.Abs, 4),
        Instruction("dec", AddrMode.Abs, 6),
        Instruction("dcp", AddrMode.Abs, 6),
        Instruction("bne", AddrMode.Rel, 2),
        Instruction("cmp", AddrMode.IzY, 5),
        Instruction("???", AddrMode.Imp, 0),
        Instruction("dcp", AddrMode.IzY, 8),
        Instruction("nop", AddrMode.ZpX, 4),
        Instruction("cmp", AddrMode.ZpX, 4),
        Instruction("dec", AddrMode.ZpX, 6),
        Instruction("dcp", AddrMode.ZpX, 6),
        Instruction("cld", AddrMode.Imp, 2),
        Instruction("cmp", AddrMode.AbsY, 4),
        Instruction("nop", AddrMode.Imp, 2),
        Instruction("dcp", AddrMode.AbsY, 7),
        Instruction("nop", AddrMode.AbsX, 4),
        Instruction("cmp", AddrMode.AbsX, 4),
        Instruction("dec", AddrMode.AbsX, 7),
        Instruction("dcp", AddrMode.AbsX, 7),
        Instruction("cpx", AddrMode.Imm, 2),
        Instruction("sbc", AddrMode.IzX, 6),
        Instruction("nop", AddrMode.Imm, 2),
        Instruction("isc", AddrMode.IzX, 8),
        Instruction("cpx", AddrMode.Zp, 3),
        Instruction("sbc", AddrMode.Zp, 3),
        Instruction("inc", AddrMode.Zp, 5),
        Instruction("isc", AddrMode.Zp, 5),
        Instruction("inx", AddrMode.Imp, 2),
        Instruction("sbc", AddrMode.Imm, 2),
        Instruction("nop", AddrMode.Imp, 2),
        Instruction("sbc", AddrMode.Imm, 2),
        Instruction("cpx", AddrMode.Abs, 4),
        Instruction("sbc", AddrMode.Abs, 4),
        Instruction("inc", AddrMode.Abs, 6),
        Instruction("isc", AddrMode.Abs, 6),
        Instruction("beq", AddrMode.Rel, 2),
        Instruction("sbc", AddrMode.IzY, 5),
        Instruction("???", AddrMode.Imp, 0),
        Instruction("isc", AddrMode.IzY, 8),
        Instruction("nop", AddrMode.ZpX, 4),
        Instruction("sbc", AddrMode.ZpX, 4),
        Instruction("inc", AddrMode.ZpX, 6),
        Instruction("isc", AddrMode.ZpX, 6),
        Instruction("sed", AddrMode.Imp, 2),
        Instruction("sbc", AddrMode.AbsY, 4),
        Instruction("nop", AddrMode.Imp, 2),
        Instruction("isc", AddrMode.AbsY, 7),
        Instruction("nop", AddrMode.AbsX, 4),
        Instruction("sbc", AddrMode.AbsX, 4),
        Instruction("inc", AddrMode.AbsX, 7),
        Instruction("isc", AddrMode.AbsX, 7)
    ).toTypedArray()

    private fun applyAddressingMode(addrMode: AddrMode) {
        when (addrMode) {
            AddrMode.Imp, AddrMode.Acc -> {
                fetchedData = A
            }
            AddrMode.Imm -> {
                fetchedData = readPc()
            }
            AddrMode.Zp -> {
                fetchedAddress = readPc()
            }
            AddrMode.ZpX -> {
                // note: zeropage index will not leave Zp when page boundary is crossed
                fetchedAddress = (readPc() + X) and 0xff
            }
            AddrMode.ZpY -> {
                // note: zeropage index will not leave Zp when page boundary is crossed
                fetchedAddress = (readPc() + Y) and 0xff
            }
            AddrMode.Rel -> {
                val relative = readPc()
                fetchedAddress = if (relative >= 0x80) {
                    PC - (256 - relative) and 0xffff
                } else
                    PC + relative and 0xffff
            }
            AddrMode.Abs -> {
                val lo = readPc()
                val hi = readPc()
                fetchedAddress = lo or (hi shl 8)
            }
            AddrMode.AbsX -> {
                val lo = readPc()
                val hi = readPc()
                fetchedAddress = X + (lo or (hi shl 8)) and 0xffff
            }
            AddrMode.AbsY -> {
                val lo = readPc()
                val hi = readPc()
                fetchedAddress = Y + (lo or (hi shl 8)) and 0xffff
            }
            AddrMode.Ind -> {
                // not able to fetch an address which crosses the page boundary (6502, fixed in 65C02)
                var lo = readPc()
                var hi = readPc()
                fetchedAddress = lo or (hi shl 8)
                if (lo == 0xff) {
                    // emulate bug
                    lo = read(fetchedAddress)
                    hi = read(fetchedAddress and 0xff00)
                } else {
                    // normal behavior
                    lo = read(fetchedAddress)
                    hi = read(fetchedAddress + 1)
                }
                fetchedAddress = lo or (hi shl 8)
            }
            AddrMode.IzX -> {
                // note: not able to fetch an adress which crosses the page boundary
                fetchedAddress = readPc()
                val lo = read((fetchedAddress + X) and 0xff)
                val hi = read((fetchedAddress + X + 1) and 0xff)
                fetchedAddress = lo or (hi shl 8)
            }
            AddrMode.IzY -> {
                // note: not able to fetch an adress which crosses the page boundary
                fetchedAddress = readPc()
                val lo = read(fetchedAddress)
                val hi = read((fetchedAddress + 1) and 0xff)
                fetchedAddress = Y + (lo or (hi shl 8)) and 0xffff
            }
        }
    }

    private fun dispatchOpcode(opcode: Int) {
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
    }


    // official instructions

    private fun iAdc() {
        val operand = getFetched()
        if (Status.D) {
            // BCD add
            // see http://www.6502.org/tutorials/decimal_mode.html
            // and http://nesdev.com/6502.txt
            // and https://sourceforge.net/p/vice-emu/code/HEAD/tree/trunk/vice/src/6510core.c#l598
            // (the implementation below is based on the code used by Vice)
            var tmp = (A and 0xf) + (operand and 0xf) + (if (Status.C) 1 else 0)
            if (tmp > 9) tmp += 6
            tmp = if (tmp <= 0x0f) {
                (tmp and 0xf) + (A and 0xf0) + (operand and 0xf0)
            } else {
                (tmp and 0xf) + (A and 0xf0) + (operand and 0xf0) + 0x10
            }
            Status.Z = A + operand + (if (Status.C) 1 else 0) and 0xff == 0
            Status.N = tmp and 0b10000000 != 0
            Status.V = (A xor tmp) and 0x80 != 0 && (A xor operand) and 0b10000000 == 0
            if (tmp and 0x1f0 > 0x90) tmp += 0x60
            Status.C = tmp > 0xf0     // original: (tmp and 0xff0) > 0xf0
            A = tmp and 0xff
        } else {
            // normal add
            val tmp = operand + A + if (Status.C) 1 else 0
            Status.N = (tmp and 0b10000000) != 0
            Status.Z = (tmp and 0xff) == 0
            Status.V = (A xor operand).inv() and (A xor tmp) and 0b10000000 != 0
            Status.C = tmp > 0xff
            A = tmp and 0xff
        }
    }

    private fun iAnd() {
        A = A and getFetched()
        Status.Z = A == 0
        Status.N = (A and 0b10000000) != 0
    }

    private fun iAsl() {
        if (currentInstruction.mode == AddrMode.Acc) {
            Status.C = (A and 0b10000000) != 0
            A = (A shl 1) and 0xff
            Status.Z = A == 0
            Status.N = (A and 0b10000000) != 0
        } else {
            val data = read(fetchedAddress)
            Status.C = (data and 0b10000000) != 0
            val shifted = (data shl 1) and 0xff
            write(fetchedAddress, shifted)
            Status.Z = shifted == 0
            Status.N = (shifted and 0b10000000) != 0
        }
    }

    private fun iBcc() {
        if (!Status.C) PC = fetchedAddress
    }

    private fun iBcs() {
        if (Status.C) PC = fetchedAddress
    }

    private fun iBeq() {
        if (Status.Z) PC = fetchedAddress
    }

    private fun iBit() {
        val operand = getFetched()
        Status.Z = (A and operand) == 0
        Status.V = (operand and 0b01000000) != 0
        Status.N = (operand and 0b10000000) != 0
    }

    private fun iBmi() {
        if (Status.N) PC = fetchedAddress
    }

    private fun iBne() {
        if (!Status.Z) PC = fetchedAddress
    }

    private fun iBpl() {
        if (!Status.N) PC = fetchedAddress
    }

    private fun iBrk() {
        // handle BRK ('software interrupt') or a real hardware IRQ
        val interrupt = pendingInterrupt
        val nmi = interrupt?.first == true
        if (interrupt != null) {
            pushStackAddr(PC - 1)
        } else {
            PC++
            pushStackAddr(PC)
        }
        Status.B = interrupt == null
        pushStack(Status)
        Status.I = true     // interrupts are now disabled
        // NMOS 6502 doesn't clear the D flag (CMOS version does...)
        PC = readWord(if (nmi) NMI_vector else IRQ_vector)
        pendingInterrupt = null
    }

    private fun iBvc() {
        if (!Status.V) PC = fetchedAddress
    }

    private fun iBvs() {
        if (Status.V) PC = fetchedAddress
    }

    private fun iClc() {
        Status.C = false
    }

    private fun iCld() {
        Status.D = false
    }

    private fun iCli() {
        Status.I = false
    }

    private fun iClv() {
        Status.V = false
    }

    private fun iCmp() {
        val fetched = getFetched()
        Status.C = A >= fetched
        Status.Z = A == fetched
        Status.N = ((A - fetched) and 0b10000000) != 0
    }

    private fun iCpx() {
        val fetched = getFetched()
        Status.C = X >= fetched
        Status.Z = X == fetched
        Status.N = ((X - fetched) and 0b10000000) != 0
    }

    private fun iCpy() {
        val fetched = getFetched()
        Status.C = Y >= fetched
        Status.Z = Y == fetched
        Status.N = ((Y - fetched) and 0b10000000) != 0
    }

    private fun iDec() {
        val data = (read(fetchedAddress) - 1) and 0xff
        write(fetchedAddress, data)
        Status.Z = data == 0
        Status.N = (data and 0b10000000) != 0
    }

    private fun iDex() {
        X = (X - 1) and 0xff
        Status.Z = X == 0
        Status.N = (X and 0b10000000) != 0
    }

    private fun iDey() {
        Y = (Y - 1) and 0xff
        Status.Z = Y == 0
        Status.N = (Y and 0b10000000) != 0
    }

    private fun iEor() {
        A = A xor getFetched()
        Status.Z = A == 0
        Status.N = (A and 0b10000000) != 0
    }

    private fun iInc() {
        val data = (read(fetchedAddress) + 1) and 0xff
        write(fetchedAddress, data)
        Status.Z = data == 0
        Status.N = (data and 0b10000000) != 0
    }

    private fun iInx() {
        X = (X + 1) and 0xff
        Status.Z = X == 0
        Status.N = (X and 0b10000000) != 0
    }

    private fun iIny() {
        Y = (Y + 1) and 0xff
        Status.Z = Y == 0
        Status.N = (Y and 0b10000000) != 0
    }

    private fun iJmp() {
        PC = fetchedAddress
    }

    private fun iJsr() {
        pushStackAddr(PC - 1)
        PC = fetchedAddress
    }

    private fun iLda() {
        A = getFetched()
        Status.Z = A == 0
        Status.N = (A and 0b10000000) != 0
    }

    private fun iLdx() {
        X = getFetched()
        Status.Z = X == 0
        Status.N = (X and 0b10000000) != 0
    }

    private fun iLdy() {
        Y = getFetched()
        Status.Z = Y == 0
        Status.N = (Y and 0b10000000) != 0
    }

    private fun iLsr() {
        if (currentInstruction.mode == AddrMode.Acc) {
            Status.C = (A and 1) == 1
            A = A ushr 1
            Status.Z = A == 0
            Status.N = (A and 0b10000000) != 0
        } else {
            val data = read(fetchedAddress)
            Status.C = (data and 1) == 1
            val shifted = data ushr 1
            write(fetchedAddress, shifted)
            Status.Z = shifted == 0
            Status.N = (shifted and 0b10000000) != 0
        }
    }

    private fun iNop() {}

    private fun iOra() {
        A = A or getFetched()
        Status.Z = A == 0
        Status.N = (A and 0b10000000) != 0
    }

    private fun iPha() {
        pushStack(A)
    }

    private fun iPhp() {
        val origBreakflag = Status.B
        Status.B = true
        pushStack(Status)
        Status.B = origBreakflag
    }

    private fun iPla() {
        A = popStack()
        Status.Z = A == 0
        Status.N = (A and 0b10000000) != 0
    }

    private fun iPlp() {
        Status.fromByte(popStack())
        Status.B = true  // break is always 1 except when pushing on stack
    }

    private fun iRol() {
        val oldCarry = Status.C
        if (currentInstruction.mode == AddrMode.Acc) {
            Status.C = (A and 0b10000000) != 0
            A = (A shl 1 and 0xff) or (if (oldCarry) 1 else 0)
            Status.Z = A == 0
            Status.N = (A and 0b10000000) != 0
        } else {
            val data = read(fetchedAddress)
            Status.C = (data and 0b10000000) != 0
            val shifted = (data shl 1 and 0xff) or (if (oldCarry) 1 else 0)
            write(fetchedAddress, shifted)
            Status.Z = shifted == 0
            Status.N = (shifted and 0b10000000) != 0
        }
    }

    private fun iRor() {
        val oldCarry = Status.C
        if (currentInstruction.mode == AddrMode.Acc) {
            Status.C = (A and 1) == 1
            A = (A ushr 1) or (if (oldCarry) 0b10000000 else 0)
            Status.Z = A == 0
            Status.N = (A and 0b10000000) != 0
        } else {
            val data = read(fetchedAddress)
            Status.C = (data and 1) == 1
            val shifted = (data ushr 1) or (if (oldCarry) 0b10000000 else 0)
            write(fetchedAddress, shifted)
            Status.Z = shifted == 0
            Status.N = (shifted and 0b10000000) != 0
        }
    }

    private fun iRti() {
        Status.fromByte(popStack())
        Status.B = true  // break is always 1 except when pushing on stack
        PC = popStackAddr()
    }

    private fun iRts() {
        PC = popStackAddr()
        PC = (PC + 1) and 0xffff
    }

    private fun iSbc() {
        val operand = getFetched()
        val tmp = (A - operand - if (Status.C) 0 else 1) and 0xffff
        Status.V = (A xor operand) and (A xor tmp) and 0b10000000 != 0
        if (Status.D) {
            // BCD subtract
            // see http://www.6502.org/tutorials/decimal_mode.html
            // and http://nesdev.com/6502.txt
            // and https://sourceforge.net/p/vice-emu/code/HEAD/tree/trunk/vice/src/6510core.c#l1396
            // (the implementation below is based on the code used by Vice)
            var tmpA = ((A and 0xf) - (operand and 0xf) - if (Status.C) 0 else 1) and 0xffff
            tmpA = if ((tmpA and 0x10) != 0) {
                ((tmpA - 6) and 0xf) or (A and 0xf0) - (operand and 0xf0) - 0x10
            } else {
                (tmpA and 0xf) or (A and 0xf0) - (operand and 0xf0)
            }
            if ((tmpA and 0x100) != 0) tmpA -= 0x60
            A = tmpA and 0xff
        } else {
            // normal subtract
            A = tmp and 0xff
        }
        Status.C = tmp < 0x100
        Status.Z = (tmp and 0xff) == 0
        Status.N = (tmp and 0b10000000) != 0
    }

    private fun iSec() {
        Status.C = true
    }

    private fun iSed() {
        Status.D = true
    }

    private fun iSei() {
        Status.I = true
    }

    private fun iSta() {
        write(fetchedAddress, A)
    }

    private fun iStx() {
        write(fetchedAddress, X)
    }

    private fun iSty() {
        write(fetchedAddress, Y)
    }

    private fun iTax() {
        X = A
        Status.Z = X == 0
        Status.N = (X and 0b10000000) != 0
    }

    private fun iTay() {
        Y = A
        Status.Z = Y == 0
        Status.N = (Y and 0b10000000) != 0
    }

    private fun iTsx() {
        X = SP
        Status.Z = X == 0
        Status.N = (X and 0b10000000) != 0
    }

    private fun iTxa() {
        A = X
        Status.Z = A == 0
        Status.N = (A and 0b10000000) != 0
    }

    private fun iTxs() {
        SP = X
    }

    private fun iTya() {
        A = Y
        Status.Z = A == 0
        Status.N = (A and 0b10000000) != 0
    }

    // unofficial/illegal instructions

    private fun iAhx() {
        TODO("ahx - ('illegal' instruction)")
    }

    private fun iAlr() {
        TODO("alr=asr - ('illegal' instruction)")
    }

    private fun iAnc() {
        TODO("anc - ('illegal' instruction)")
    }

    private fun iArr() {
        TODO("arr - ('illegal' instruction)")
    }

    private fun iAxs() {
        TODO("axs - ('illegal' instruction)")
    }

    private fun iDcp() {
        TODO("dcp - ('illegal' instruction)")
    }

    private fun iIsc() {
        TODO("isc=isb - ('illegal' instruction)")
    }

    private fun iLas() {
        TODO("las=lar - ('illegal' instruction)")
    }

    private fun iLax() {
        TODO("lax - ('illegal' instruction)")
    }

    private fun iRla() {
        TODO("rla - ('illegal' instruction)")
    }

    private fun iRra() {
        TODO("rra - ('illegal' instruction)")
    }

    private fun iSax() {
        TODO("sax - ('illegal' instruction)")
    }

    private fun iShx() {
        TODO("shx - ('illegal' instruction)")
    }

    private fun iShy() {
        TODO("shy - ('illegal' instruction)")
    }

    private fun iSlo() {
        TODO("slo=aso - ('illegal' instruction)")
    }

    private fun iSre() {
        TODO("sre=lse - ('illegal' instruction)")
    }

    private fun iTas() {
        TODO("tas - ('illegal' instruction)")
    }

    private fun iXaa() {
        TODO("xaa - ('illegal' instruction)")
    }

    // invalid instruction (JAM / KIL)
    private fun iInvalid() {
        throw InstructionError(
            "invalid instruction encountered: opcode=${hexB(
                currentOpcode
            )} instr=${currentInstruction.mnemonic}"
        )
    }
}
