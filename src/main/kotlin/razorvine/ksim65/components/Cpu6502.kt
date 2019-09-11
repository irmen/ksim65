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

    class Instruction(
        val opcode: UByte,
        val mnemonic: String,
        val mode: AddrMode,
        val cycles: Int,
        val execute: () -> Unit
    ) {
        override fun toString(): String {
            return "[${hexB(opcode)}: $mnemonic $mode]"
        }
    }

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

    private val addressingModes = mapOf<AddrMode, () -> Unit>(
        AddrMode.Imp to ::amImp,
        AddrMode.Acc to ::amAcc,
        AddrMode.Imm to ::amImm,
        AddrMode.Zp to ::amZp,
        AddrMode.ZpX to ::amZpx,
        AddrMode.ZpY to ::amZpy,
        AddrMode.Rel to ::amRel,
        AddrMode.Abs to ::amAbs,
        AddrMode.AbsX to ::amAbsx,
        AddrMode.AbsY to ::amAbsy,
        AddrMode.Ind to ::amInd,
        AddrMode.IzX to ::amIzx,
        AddrMode.IzY to ::amIzy
    )

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
            val opcode = opcodes[byte.toInt()]
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
        currentInstruction = opcodes[0]
    }

    override fun clock() {
        if (instrCycles == 0) {
            if (pendingInterrupt != null) {
                // NMI or IRQ interrupt.
                // handled by the BRK instruction logic.
                currentOpcode = 0
                currentInstruction = opcodes[0]
            } else {
                // no interrupt, continue with next instruction
                currentOpcode = read(PC)
                currentInstruction = opcodes[currentOpcode]

                if (tracing) printState()

                breakpoints[PC]?.let {
                    val oldPC = PC
                    val oldOpcode = currentOpcode
                    it(this, PC)
                    if (PC != oldPC)
                        return clock()
                    if (oldOpcode != currentOpcode)
                        currentInstruction = opcodes[currentOpcode]
                }

                if (currentOpcode == 0 && stopOnBrk) {
                    throw InstructionError(
                        "stopped on BRK instruction at ${hexW(
                            PC
                        )}"
                    )
                }
            }

            PC++
            instrCycles = currentInstruction.cycles
            addressingModes.getValue(currentInstruction.mode)()
            currentInstruction.execute()
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

    private fun amImp() {
        fetchedData = A
    }

    private fun amAcc() {
        fetchedData = A
    }

    private fun amImm() {
        fetchedData = readPc()
    }

    private fun amZp() {
        fetchedAddress = readPc()
    }

    private fun amZpx() {
        // note: zeropage index will not leave Zp when page boundary is crossed
        fetchedAddress = (readPc() + X) and 0xff
    }

    private fun amZpy() {
        // note: zeropage index will not leave Zp when page boundary is crossed
        fetchedAddress = (readPc() + Y) and 0xff
    }

    private fun amRel() {
        val relative = readPc()
        fetchedAddress = if (relative >= 0x80) {
            PC - (256 - relative) and 0xffff
        } else
            PC + relative and 0xffff
    }

    private fun amAbs() {
        val lo = readPc()
        val hi = readPc()
        fetchedAddress = lo or (hi shl 8)
    }

    private fun amAbsx() {
        val lo = readPc()
        val hi = readPc()
        fetchedAddress = X + (lo or (hi shl 8)) and 0xffff
    }

    private fun amAbsy() {
        val lo = readPc()
        val hi = readPc()
        fetchedAddress = Y + (lo or (hi shl 8)) and 0xffff
    }

    private fun amInd() {
        // not able to fetch an adress which crosses the page boundary (6502, fixed in 65C02)
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

    private fun amIzx() {
        // note: not able to fetch an adress which crosses the page boundary
        fetchedAddress = readPc()
        val lo = read((fetchedAddress + X) and 0xff)
        val hi = read((fetchedAddress + X + 1) and 0xff)
        fetchedAddress = lo or (hi shl 8)
    }

    private fun amIzy() {
        // note: not able to fetch an adress which crosses the page boundary
        fetchedAddress = readPc()
        val lo = read(fetchedAddress)
        val hi = read((fetchedAddress + 1) and 0xff)
        fetchedAddress = Y + (lo or (hi shl 8)) and 0xffff
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
    private val opcodes = listOf(
        Instruction(0x00, "brk", AddrMode.Imp, 7, ::iBrk),
        Instruction(0x01, "ora", AddrMode.IzX, 6, ::iOra),
        Instruction(0x02, "???", AddrMode.Imp, 0, ::iInvalid),
        Instruction(0x03, "slo", AddrMode.IzX, 8, ::iSlo),
        Instruction(0x04, "nop", AddrMode.Zp, 3, ::iNop),
        Instruction(0x05, "ora", AddrMode.Zp, 3, ::iOra),
        Instruction(0x06, "asl", AddrMode.Zp, 5, ::iAsl),
        Instruction(0x07, "slo", AddrMode.Zp, 5, ::iSlo),
        Instruction(0x08, "php", AddrMode.Imp, 3, ::iPhp),
        Instruction(0x09, "ora", AddrMode.Imm, 2, ::iOra),
        Instruction(0x0a, "asl", AddrMode.Acc, 2, ::iAsl),
        Instruction(0x0b, "anc", AddrMode.Imm, 2, ::iAnc),
        Instruction(0x0c, "nop", AddrMode.Abs, 4, ::iNop),
        Instruction(0x0d, "ora", AddrMode.Abs, 4, ::iOra),
        Instruction(0x0e, "asl", AddrMode.Abs, 6, ::iAsl),
        Instruction(0x0f, "slo", AddrMode.Abs, 6, ::iSlo),
        Instruction(0x10, "bpl", AddrMode.Rel, 2, ::iBpl),
        Instruction(0x11, "ora", AddrMode.IzY, 5, ::iOra),
        Instruction(0x12, "???", AddrMode.Imp, 0, ::iInvalid),
        Instruction(0x13, "slo", AddrMode.IzY, 6, ::iSlo),
        Instruction(0x14, "nop", AddrMode.ZpX, 4, ::iNop),
        Instruction(0x15, "ora", AddrMode.ZpX, 4, ::iOra),
        Instruction(0x16, "asl", AddrMode.ZpX, 6, ::iAsl),
        Instruction(0x17, "slo", AddrMode.ZpX, 6, ::iSlo),
        Instruction(0x18, "clc", AddrMode.Imp, 2, ::iClc),
        Instruction(0x19, "ora", AddrMode.AbsY, 4, ::iOra),
        Instruction(0x1a, "nop", AddrMode.Imp, 2, ::iNop),
        Instruction(0x1b, "slo", AddrMode.AbsY, 7, ::iSlo),
        Instruction(0x1c, "nop", AddrMode.AbsX, 4, ::iNop),
        Instruction(0x1d, "ora", AddrMode.AbsX, 4, ::iOra),
        Instruction(0x1e, "asl", AddrMode.AbsX, 7, ::iAsl),
        Instruction(0x1f, "slo", AddrMode.AbsX, 7, ::iSlo),
        Instruction(0x20, "jsr", AddrMode.Abs, 6, ::iJsr),
        Instruction(0x21, "and", AddrMode.IzX, 6, ::iAnd),
        Instruction(0x22, "???", AddrMode.Imp, 0, ::iInvalid),
        Instruction(0x23, "rla", AddrMode.IzX, 8, ::iRla),
        Instruction(0x24, "bit", AddrMode.Zp, 3, ::iBit),
        Instruction(0x25, "and", AddrMode.Zp, 3, ::iAnd),
        Instruction(0x26, "rol", AddrMode.Zp, 5, ::iRol),
        Instruction(0x27, "rla", AddrMode.Zp, 5, ::iRla),
        Instruction(0x28, "plp", AddrMode.Imp, 4, ::iPlp),
        Instruction(0x29, "and", AddrMode.Imm, 2, ::iAnd),
        Instruction(0x2a, "rol", AddrMode.Acc, 2, ::iRol),
        Instruction(0x2b, "anc", AddrMode.Imm, 2, ::iAnc),
        Instruction(0x2c, "bit", AddrMode.Abs, 4, ::iBit),
        Instruction(0x2d, "and", AddrMode.Abs, 4, ::iAnd),
        Instruction(0x2e, "rol", AddrMode.Abs, 6, ::iRol),
        Instruction(0x2f, "rla", AddrMode.Abs, 6, ::iRla),
        Instruction(0x30, "bmi", AddrMode.Rel, 2, ::iBmi),
        Instruction(0x31, "and", AddrMode.IzY, 5, ::iAnd),
        Instruction(0x32, "???", AddrMode.Imp, 0, ::iInvalid),
        Instruction(0x33, "rla", AddrMode.IzY, 8, ::iRla),
        Instruction(0x34, "nop", AddrMode.ZpX, 4, ::iNop),
        Instruction(0x35, "and", AddrMode.ZpX, 4, ::iAnd),
        Instruction(0x36, "rol", AddrMode.ZpX, 6, ::iRol),
        Instruction(0x37, "rla", AddrMode.ZpX, 6, ::iRla),
        Instruction(0x38, "sec", AddrMode.Imp, 2, ::iSec),
        Instruction(0x39, "and", AddrMode.AbsY, 4, ::iAnd),
        Instruction(0x3a, "nop", AddrMode.Imp, 2, ::iNop),
        Instruction(0x3b, "rla", AddrMode.AbsY, 7, ::iRla),
        Instruction(0x3c, "nop", AddrMode.AbsX, 4, ::iNop),
        Instruction(0x3d, "and", AddrMode.AbsX, 4, ::iAnd),
        Instruction(0x3e, "rol", AddrMode.AbsX, 7, ::iRol),
        Instruction(0x3f, "rla", AddrMode.AbsX, 7, ::iRla),
        Instruction(0x40, "rti", AddrMode.Imp, 6, ::iRti),
        Instruction(0x41, "eor", AddrMode.IzX, 6, ::iEor),
        Instruction(0x42, "???", AddrMode.Imp, 0, ::iInvalid),
        Instruction(0x43, "sre", AddrMode.IzX, 8, ::iSre),
        Instruction(0x44, "nop", AddrMode.Zp, 3, ::iNop),
        Instruction(0x45, "eor", AddrMode.Zp, 3, ::iEor),
        Instruction(0x46, "lsr", AddrMode.Zp, 5, ::iLsr),
        Instruction(0x47, "sre", AddrMode.Zp, 5, ::iSre),
        Instruction(0x48, "pha", AddrMode.Imp, 3, ::iPha),
        Instruction(0x49, "eor", AddrMode.Imm, 2, ::iEor),
        Instruction(0x4a, "lsr", AddrMode.Acc, 2, ::iLsr),
        Instruction(0x4b, "alr", AddrMode.Imm, 2, ::iAlr),
        Instruction(0x4c, "jmp", AddrMode.Abs, 3, ::iJmp),
        Instruction(0x4d, "eor", AddrMode.Abs, 4, ::iEor),
        Instruction(0x4e, "lsr", AddrMode.Abs, 6, ::iLsr),
        Instruction(0x4f, "sre", AddrMode.Abs, 6, ::iSre),
        Instruction(0x50, "bvc", AddrMode.Rel, 2, ::iBvc),
        Instruction(0x51, "eor", AddrMode.IzY, 5, ::iEor),
        Instruction(0x52, "???", AddrMode.Imp, 0, ::iInvalid),
        Instruction(0x53, "sre", AddrMode.IzY, 8, ::iSre),
        Instruction(0x54, "nop", AddrMode.ZpX, 4, ::iNop),
        Instruction(0x55, "eor", AddrMode.ZpX, 4, ::iEor),
        Instruction(0x56, "lsr", AddrMode.ZpX, 6, ::iLsr),
        Instruction(0x57, "sre", AddrMode.ZpX, 6, ::iSre),
        Instruction(0x58, "cli", AddrMode.Imp, 2, ::iCli),
        Instruction(0x59, "eor", AddrMode.AbsY, 4, ::iEor),
        Instruction(0x5a, "nop", AddrMode.Imp, 2, ::iNop),
        Instruction(0x5b, "sre", AddrMode.AbsY, 7, ::iSre),
        Instruction(0x5c, "nop", AddrMode.AbsX, 4, ::iNop),
        Instruction(0x5d, "eor", AddrMode.AbsX, 4, ::iEor),
        Instruction(0x5e, "lsr", AddrMode.AbsX, 7, ::iLsr),
        Instruction(0x5f, "sre", AddrMode.AbsX, 7, ::iSre),
        Instruction(0x60, "rts", AddrMode.Imp, 6, ::iRts),
        Instruction(0x61, "adc", AddrMode.IzX, 6, ::iAdc),
        Instruction(0x62, "???", AddrMode.Imp, 0, ::iInvalid),
        Instruction(0x63, "rra", AddrMode.IzX, 8, ::iRra),
        Instruction(0x64, "nop", AddrMode.Zp, 3, ::iNop),
        Instruction(0x65, "adc", AddrMode.Zp, 3, ::iAdc),
        Instruction(0x66, "ror", AddrMode.Zp, 5, ::iRor),
        Instruction(0x67, "rra", AddrMode.Zp, 5, ::iRra),
        Instruction(0x68, "pla", AddrMode.Imp, 4, ::iPla),
        Instruction(0x69, "adc", AddrMode.Imm, 2, ::iAdc),
        Instruction(0x6a, "ror", AddrMode.Acc, 2, ::iRor),
        Instruction(0x6b, "arr", AddrMode.Imm, 2, ::iArr),
        Instruction(0x6c, "jmp", AddrMode.Ind, 5, ::iJmp),
        Instruction(0x6d, "adc", AddrMode.Abs, 4, ::iAdc),
        Instruction(0x6e, "ror", AddrMode.Abs, 6, ::iRor),
        Instruction(0x6f, "rra", AddrMode.Abs, 6, ::iRra),
        Instruction(0x70, "bvs", AddrMode.Rel, 2, ::iBvs),
        Instruction(0x71, "adc", AddrMode.IzY, 5, ::iAdc),
        Instruction(0x72, "???", AddrMode.Imp, 0, ::iInvalid),
        Instruction(0x73, "rra", AddrMode.IzY, 8, ::iRra),
        Instruction(0x74, "nop", AddrMode.ZpX, 4, ::iNop),
        Instruction(0x75, "adc", AddrMode.ZpX, 4, ::iAdc),
        Instruction(0x76, "ror", AddrMode.ZpX, 6, ::iRor),
        Instruction(0x77, "rra", AddrMode.ZpX, 6, ::iRra),
        Instruction(0x78, "sei", AddrMode.Imp, 2, ::iSei),
        Instruction(0x79, "adc", AddrMode.AbsY, 4, ::iAdc),
        Instruction(0x7a, "nop", AddrMode.Imp, 2, ::iNop),
        Instruction(0x7b, "rra", AddrMode.AbsY, 7, ::iRra),
        Instruction(0x7c, "nop", AddrMode.AbsX, 4, ::iNop),
        Instruction(0x7d, "adc", AddrMode.AbsX, 4, ::iAdc),
        Instruction(0x7e, "ror", AddrMode.AbsX, 7, ::iRor),
        Instruction(0x7f, "rra", AddrMode.AbsX, 7, ::iRra),
        Instruction(0x80, "nop", AddrMode.Imm, 2, ::iNop),
        Instruction(0x81, "sta", AddrMode.IzX, 6, ::iSta),
        Instruction(0x82, "nop", AddrMode.Imm, 2, ::iNop),
        Instruction(0x83, "sax", AddrMode.IzX, 6, ::iSax),
        Instruction(0x84, "sty", AddrMode.Zp, 3, ::iSty),
        Instruction(0x85, "sta", AddrMode.Zp, 3, ::iSta),
        Instruction(0x86, "stx", AddrMode.Zp, 3, ::iStx),
        Instruction(0x87, "sax", AddrMode.Zp, 3, ::iSax),
        Instruction(0x88, "dey", AddrMode.Imp, 2, ::iDey),
        Instruction(0x89, "nop", AddrMode.Imm, 2, ::iNop),
        Instruction(0x8a, "txa", AddrMode.Imp, 2, ::iTxa),
        Instruction(0x8b, "xaa", AddrMode.Imm, 2, ::iXaa),
        Instruction(0x8c, "sty", AddrMode.Abs, 4, ::iSty),
        Instruction(0x8d, "sta", AddrMode.Abs, 4, ::iSta),
        Instruction(0x8e, "stx", AddrMode.Abs, 4, ::iStx),
        Instruction(0x8f, "sax", AddrMode.Abs, 4, ::iSax),
        Instruction(0x90, "bcc", AddrMode.Rel, 2, ::iBcc),
        Instruction(0x91, "sta", AddrMode.IzY, 6, ::iSta),
        Instruction(0x92, "???", AddrMode.Imp, 0, ::iInvalid),
        Instruction(0x93, "ahx", AddrMode.IzY, 6, ::iAhx),
        Instruction(0x94, "sty", AddrMode.ZpX, 4, ::iSty),
        Instruction(0x95, "sta", AddrMode.ZpX, 4, ::iSta),
        Instruction(0x96, "stx", AddrMode.ZpY, 4, ::iStx),
        Instruction(0x97, "sax", AddrMode.ZpY, 4, ::iSax),
        Instruction(0x98, "tya", AddrMode.Imp, 2, ::iTya),
        Instruction(0x99, "sta", AddrMode.AbsY, 5, ::iSta),
        Instruction(0x9a, "txs", AddrMode.Imp, 2, ::iTxs),
        Instruction(0x9b, "tas", AddrMode.AbsY, 5, ::iTas),
        Instruction(0x9c, "shy", AddrMode.AbsX, 5, ::iShy),
        Instruction(0x9d, "sta", AddrMode.AbsX, 5, ::iSta),
        Instruction(0x9e, "shx", AddrMode.AbsY, 5, ::iShx),
        Instruction(0x9f, "ahx", AddrMode.AbsY, 5, ::iAhx),
        Instruction(0xa0, "ldy", AddrMode.Imm, 2, ::iLdy),
        Instruction(0xa1, "lda", AddrMode.IzX, 6, ::iLda),
        Instruction(0xa2, "ldx", AddrMode.Imm, 2, ::iLdx),
        Instruction(0xa3, "lax", AddrMode.IzX, 6, ::iLax),
        Instruction(0xa4, "ldy", AddrMode.Zp, 3, ::iLdy),
        Instruction(0xa5, "lda", AddrMode.Zp, 3, ::iLda),
        Instruction(0xa6, "ldx", AddrMode.Zp, 3, ::iLdx),
        Instruction(0xa7, "lax", AddrMode.Zp, 3, ::iLax),
        Instruction(0xa8, "tay", AddrMode.Imp, 2, ::iTay),
        Instruction(0xa9, "lda", AddrMode.Imm, 2, ::iLda),
        Instruction(0xaa, "tax", AddrMode.Imp, 2, ::iTax),
        Instruction(0xab, "lax", AddrMode.Imm, 2, ::iLax),
        Instruction(0xac, "ldy", AddrMode.Abs, 4, ::iLdy),
        Instruction(0xad, "lda", AddrMode.Abs, 4, ::iLda),
        Instruction(0xae, "ldx", AddrMode.Abs, 4, ::iLdx),
        Instruction(0xaf, "lax", AddrMode.Abs, 4, ::iLax),
        Instruction(0xb0, "bcs", AddrMode.Rel, 2, ::iBcs),
        Instruction(0xb1, "lda", AddrMode.IzY, 5, ::iLda),
        Instruction(0xb2, "???", AddrMode.Imp, 0, ::iInvalid),
        Instruction(0xb3, "lax", AddrMode.IzY, 5, ::iLax),
        Instruction(0xb4, "ldy", AddrMode.ZpX, 4, ::iLdy),
        Instruction(0xb5, "lda", AddrMode.ZpX, 4, ::iLda),
        Instruction(0xb6, "ldx", AddrMode.ZpY, 4, ::iLdx),
        Instruction(0xb7, "lax", AddrMode.ZpY, 4, ::iLax),
        Instruction(0xb8, "clv", AddrMode.Imp, 2, ::iClv),
        Instruction(0xb9, "lda", AddrMode.AbsY, 4, ::iLda),
        Instruction(0xba, "tsx", AddrMode.Imp, 2, ::iTsx),
        Instruction(0xbb, "las", AddrMode.AbsY, 4, ::iLas),
        Instruction(0xbc, "ldy", AddrMode.AbsX, 4, ::iLdy),
        Instruction(0xbd, "lda", AddrMode.AbsX, 4, ::iLda),
        Instruction(0xbe, "ldx", AddrMode.AbsY, 4, ::iLdx),
        Instruction(0xbf, "lax", AddrMode.AbsY, 4, ::iLax),
        Instruction(0xc0, "cpy", AddrMode.Imm, 2, ::iCpy),
        Instruction(0xc1, "cmp", AddrMode.IzX, 6, ::iCmp),
        Instruction(0xc2, "nop", AddrMode.Imm, 2, ::iNop),
        Instruction(0xc3, "dcp", AddrMode.IzX, 8, ::iDcp),
        Instruction(0xc4, "cpy", AddrMode.Zp, 3, ::iCpy),
        Instruction(0xc5, "cmp", AddrMode.Zp, 3, ::iCmp),
        Instruction(0xc6, "dec", AddrMode.Zp, 5, ::iDec),
        Instruction(0xc7, "dcp", AddrMode.Zp, 5, ::iDcp),
        Instruction(0xc8, "iny", AddrMode.Imp, 2, ::iIny),
        Instruction(0xc9, "cmp", AddrMode.Imm, 2, ::iCmp),
        Instruction(0xca, "dex", AddrMode.Imp, 2, ::iDex),
        Instruction(0xcb, "axs", AddrMode.Imm, 2, ::iAxs),
        Instruction(0xcc, "cpy", AddrMode.Abs, 4, ::iCpy),
        Instruction(0xcd, "cmp", AddrMode.Abs, 4, ::iCmp),
        Instruction(0xce, "dec", AddrMode.Abs, 6, ::iDec),
        Instruction(0xcf, "dcp", AddrMode.Abs, 6, ::iDcp),
        Instruction(0xd0, "bne", AddrMode.Rel, 2, ::iBne),
        Instruction(0xd1, "cmp", AddrMode.IzY, 5, ::iCmp),
        Instruction(0xd2, "???", AddrMode.Imp, 0, ::iInvalid),
        Instruction(0xd3, "dcp", AddrMode.IzY, 8, ::iDcp),
        Instruction(0xd4, "nop", AddrMode.ZpX, 4, ::iNop),
        Instruction(0xd5, "cmp", AddrMode.ZpX, 4, ::iCmp),
        Instruction(0xd6, "dec", AddrMode.ZpX, 6, ::iDec),
        Instruction(0xd7, "dcp", AddrMode.ZpX, 6, ::iDcp),
        Instruction(0xd8, "cld", AddrMode.Imp, 2, ::iCld),
        Instruction(0xd9, "cmp", AddrMode.AbsY, 4, ::iCmp),
        Instruction(0xda, "nop", AddrMode.Imp, 2, ::iNop),
        Instruction(0xdb, "dcp", AddrMode.AbsY, 7, ::iDcp),
        Instruction(0xdc, "nop", AddrMode.AbsX, 4, ::iNop),
        Instruction(0xdd, "cmp", AddrMode.AbsX, 4, ::iCmp),
        Instruction(0xde, "dec", AddrMode.AbsX, 7, ::iDec),
        Instruction(0xdf, "dcp", AddrMode.AbsX, 7, ::iDcp),
        Instruction(0xe0, "cpx", AddrMode.Imm, 2, ::iCpx),
        Instruction(0xe1, "sbc", AddrMode.IzX, 6, ::iSbc),
        Instruction(0xe2, "nop", AddrMode.Imm, 2, ::iNop),
        Instruction(0xe3, "isc", AddrMode.IzX, 8, ::iIsc),
        Instruction(0xe4, "cpx", AddrMode.Zp, 3, ::iCpx),
        Instruction(0xe5, "sbc", AddrMode.Zp, 3, ::iSbc),
        Instruction(0xe6, "inc", AddrMode.Zp, 5, ::iInc),
        Instruction(0xe7, "isc", AddrMode.Zp, 5, ::iIsc),
        Instruction(0xe8, "inx", AddrMode.Imp, 2, ::iInx),
        Instruction(0xe9, "sbc", AddrMode.Imm, 2, ::iSbc),
        Instruction(0xea, "nop", AddrMode.Imp, 2, ::iNop),
        Instruction(0xeb, "sbc", AddrMode.Imm, 2, ::iSbc),
        Instruction(0xec, "cpx", AddrMode.Abs, 4, ::iCpx),
        Instruction(0xed, "sbc", AddrMode.Abs, 4, ::iSbc),
        Instruction(0xee, "inc", AddrMode.Abs, 6, ::iInc),
        Instruction(0xef, "isc", AddrMode.Abs, 6, ::iIsc),
        Instruction(0xf0, "beq", AddrMode.Rel, 2, ::iBeq),
        Instruction(0xf1, "sbc", AddrMode.IzY, 5, ::iSbc),
        Instruction(0xf2, "???", AddrMode.Imp, 0, ::iInvalid),
        Instruction(0xf3, "isc", AddrMode.IzY, 8, ::iIsc),
        Instruction(0xf4, "nop", AddrMode.ZpX, 4, ::iNop),
        Instruction(0xf5, "sbc", AddrMode.ZpX, 4, ::iSbc),
        Instruction(0xf6, "inc", AddrMode.ZpX, 6, ::iInc),
        Instruction(0xf7, "isc", AddrMode.ZpX, 6, ::iIsc),
        Instruction(0xf8, "sed", AddrMode.Imp, 2, ::iSed),
        Instruction(0xf9, "sbc", AddrMode.AbsY, 4, ::iSbc),
        Instruction(0xfa, "nop", AddrMode.Imp, 2, ::iNop),
        Instruction(0xfb, "isc", AddrMode.AbsY, 7, ::iIsc),
        Instruction(0xfc, "nop", AddrMode.AbsX, 4, ::iNop),
        Instruction(0xfd, "sbc", AddrMode.AbsX, 4, ::iSbc),
        Instruction(0xfe, "inc", AddrMode.AbsX, 7, ::iInc),
        Instruction(0xff, "isc", AddrMode.AbsX, 7, ::iIsc)
    ).toTypedArray()


    private fun iInvalid() {
        throw InstructionError(
            "invalid instruction encountered: opcode=${hexB(
                currentOpcode
            )} instr=${currentInstruction.mnemonic}"
        )
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
}
