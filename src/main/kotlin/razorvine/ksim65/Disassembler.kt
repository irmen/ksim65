package razorvine.ksim65

import razorvine.ksim65.components.Address
import razorvine.ksim65.components.UByte


class Disassembler(cpu: Cpu6502) {

    private val instructions = cpu.instructions

    fun disassemble(memory: Array<UByte>, range: IntRange, baseAddress: Address): Pair<List<String>, Address> {
        var offset = range.first
        val result = mutableListOf<String>()
        while (offset <= range.last) {
            val dis = disassembleOneInstruction(memory, offset, baseAddress)
            result.add(dis.first)
            offset += dis.second
        }
        return Pair(result, offset+baseAddress)
    }

    fun disassembleOneInstruction(memory: Array<UByte>, offset: Int, baseAddress: Address): Pair<String, Int> {
        val spacing1 = "        "
        val spacing2 = "     "
        val spacing3 = "  "
        val byte = memory[offset]
        val line = "\$${hexW(offset+baseAddress)}  ${hexB(byte)} "
        val opcode = instructions[byte.toInt()]
        return when (opcode.mode) {
            Cpu6502.AddrMode.Acc -> {
                Pair(line+"$spacing1 ${opcode.mnemonic}  a", 1)
            }
            Cpu6502.AddrMode.Imp -> {
                Pair(line+"$spacing1 ${opcode.mnemonic}", 1)
            }
            Cpu6502.AddrMode.Imm -> {
                val value = memory[offset+1]
                Pair(line+"${hexB(value)} $spacing2 ${opcode.mnemonic}  #\$${hexB(value)}", 2)
            }
            Cpu6502.AddrMode.Zp -> {
                val zpAddr = memory[offset+1]
                Pair(line+"${hexB(zpAddr)} $spacing2 ${opcode.mnemonic}  \$${hexB(zpAddr)}", 2)
            }
            Cpu6502.AddrMode.Zpr -> {
                // addressing mode used by the 65C02, put here for convenience rather than the subclass
                val zpAddr = memory[offset+1]
                val rel = memory[offset+2]
                val target = (if (rel <= 0x7f) offset+3+rel+baseAddress else offset+3-(256-rel)+baseAddress) and 0xffff
                Pair(line+"${hexB(zpAddr)} ${hexB(rel)} $spacing3 ${opcode.mnemonic}  \$${hexB(zpAddr)}, \$${hexW(target, true)}", 3)
            }
            Cpu6502.AddrMode.Izp -> {
                // addressing mode used by the 65C02, put here for convenience rather than the subclass
                val zpAddr = memory[offset+1]
                Pair(line+"${hexB(zpAddr)} $spacing2 ${opcode.mnemonic}  \$(${hexB(zpAddr)})", 2)
            }
            Cpu6502.AddrMode.IaX -> {
                // addressing mode used by the 65C02, put here for convenience rather than the subclass
                val lo = memory[offset+1]
                val hi = memory[offset+2]
                val absAddr = lo.toInt() or (hi.toInt() shl 8)
                Pair(line+"${hexB(lo)} ${hexB(hi)} $spacing3 ${opcode.mnemonic}  \$(${hexW(absAddr)},x)", 3)
            }
            Cpu6502.AddrMode.ZpX -> {
                val zpAddr = memory[offset+1]
                Pair(line+"${hexB(zpAddr)} $spacing2 ${opcode.mnemonic}  \$${hexB(zpAddr)},x", 2)
            }
            Cpu6502.AddrMode.ZpY -> {
                val zpAddr = memory[offset+1]
                Pair(line+"${hexB(zpAddr)} $spacing2 ${opcode.mnemonic}  \$${hexB(zpAddr)},y", 2)
            }
            Cpu6502.AddrMode.Rel -> {
                val rel = memory[offset+1]
                val target = (if (rel <= 0x7f) offset+2+rel+baseAddress else offset+2-(256-rel)+baseAddress) and 0xffff
                Pair(line+"${hexB(rel)} $spacing2 ${opcode.mnemonic}  \$${hexW(target, true)}", 2)
            }
            Cpu6502.AddrMode.Abs -> {
                val lo = memory[offset+1]
                val hi = memory[offset+2]
                val absAddr = lo.toInt() or (hi.toInt() shl 8)
                Pair(line+"${hexB(lo)} ${hexB(hi)} $spacing3 ${opcode.mnemonic}  \$${hexW(absAddr)}", 3)
            }
            Cpu6502.AddrMode.AbsX -> {
                val lo = memory[offset+1]
                val hi = memory[offset+2]
                val absAddr = lo.toInt() or (hi.toInt() shl 8)
                Pair(line+"${hexB(lo)} ${hexB(hi)} $spacing3 ${opcode.mnemonic}  \$${hexW(absAddr)},x", 3)
            }
            Cpu6502.AddrMode.AbsY -> {
                val lo = memory[offset+1]
                val hi = memory[offset+2]
                val absAddr = lo.toInt() or (hi.toInt() shl 8)
                Pair(line+"${hexB(lo)} ${hexB(hi)} $spacing3 ${opcode.mnemonic}  \$${hexW(absAddr)},y", 3)
            }
            Cpu6502.AddrMode.Ind -> {
                val lo = memory[offset+1]
                val hi = memory[offset+2]
                val indirectAddr = lo.toInt() or (hi.toInt() shl 8)
                Pair(line+"${hexB(lo)} ${hexB(hi)} $spacing3 ${opcode.mnemonic}  (\$${hexW(indirectAddr)})", 3)
            }
            Cpu6502.AddrMode.IzX -> {
                val zpAddr = memory[offset+1]
                Pair(line+"${hexB(zpAddr)} $spacing2 ${opcode.mnemonic}  (\$${hexB(zpAddr)},x)", 2)
            }
            Cpu6502.AddrMode.IzY -> {
                val zpAddr = memory[offset+1]
                Pair(line+"${hexB(zpAddr)} $spacing2 ${opcode.mnemonic}  (\$${hexB(zpAddr)}),y", 2)
            }
        }
    }
}
