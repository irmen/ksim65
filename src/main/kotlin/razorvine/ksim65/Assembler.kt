package razorvine.ksim65

import razorvine.ksim65.components.Address
import razorvine.ksim65.components.MemMappedComponent


class Assembler(cpu: Cpu6502, val memory: MemMappedComponent, initialStartAddress: Address? = null) {

    companion object {
        fun parseRelativeToPC(relative: String, currentAddress: Int): Int {
            val rest = relative.substring(1).trim()
            if(rest.isNotEmpty()) {
                return when(rest[0]) {
                    '-' -> currentAddress-parseNumber(rest.substring(1))
                    '+' -> currentAddress+parseNumber(rest.substring(1))
                    else -> throw NumberFormatException("invalid address syntax")
                }
            }
            return currentAddress
        }

        fun parseNumber(number: String, decimalFirst: Boolean = false): Int {
            val num = number.trim()
            if (num.isBlank()) return 0
            if (decimalFirst && num[0].isDigit()) return num.toInt(10)
            return when (num[0]) {
                '$' -> num.substring(1).trimStart().toInt(16)
                '#' -> num.substring(1).trimStart().toInt(10)
                '%' -> num.substring(1).trimStart().toInt(2)
                else -> num.toInt(16)
            }
        }
    }


    private var address = initialStartAddress ?: 0
    private var assembledSize = 0

    private val instructions by lazy {
        val instr = cpu.instructions.withIndex().associate {
            Pair(it.value.mnemonic, it.value.mode) to it.index
        }.toMutableMap()
        instr[Pair("nop", Cpu6502.AddrMode.Imp)] = 0xea
        instr.toMap()
    }

    class Result(val success: Boolean, val error: String, val startAddress: Address, val numBytes: Int)

    fun assemble(lines: Iterable<String>): Result {
        for(line in lines) {
            val result = assemble(line)
            if(!result.success)
                return result
            assembledSize += result.numBytes
        }
        return Result(true, "", address, assembledSize)
    }

    fun assemble(line: String): Result {

        /*
        The command is a line of the form:

        "<address>    <instruction>  [<arguments>]"
        "             <instruction>  [<arguments>]"
        "* = <address>"
         */

        var args = line.trim().split(' ')
        if(args.isEmpty() || args.size == 1 && args[0] == "")
            return Result(true, "", address, 0)
        if(args[0].startsWith("*=") && args.size==1) {
            address = parseNumber(args[0].substring(2))
            return Result(true, "", address, 0)
        }
        else if(args[0] == "*" && args[1] == "=") {
            address = parseNumber(args[2])
            return Result(true, "", address, 0)
        } else {
            // line with an instruction, may be preceded by a 4 or 5 char address
            if(args[0].length == 4 || args[0].length==5) {
                if(args.size!=2 && args.size !=3)
                    return Result(false, "syntax error", address, 0)
                address = parseNumber(args[0])
                args = args.drop(1)
            }
        }

        val instructionSize: Int
        val mnemonic = args[0].toLowerCase().trim()
        when (args.size) {
            1 -> {
                // implied or acc
                instructionSize = 1
                var instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.Imp)]
                if (instruction == null) instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.Acc)]
                if (instruction == null) return Result(false, "invalid instruction", this.address, 0)
                memory[address] = instruction.toShort()
            }
            2 -> {
                val arg = args[1]
                when {
                    arg.startsWith('#') -> {
                        // immediate
                        val instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.Imm)] ?: return Result(false, "invalid instruction",
                                                                                                              this.address, 0)
                        memory[address] = instruction.toShort()
                        memory[address+1] = parseNumber(arg.substring(1), decimalFirst = true).toShort()
                        instructionSize = 2
                    }
                    arg.startsWith("(") && arg.endsWith(",x)") -> {
                        // indirect X
                        val indAddress = try {
                            parseNumber(arg.substring(1, arg.length-3))
                        } catch (x: NumberFormatException) {
                            return Result(false, "invalid instruction", this.address, 0)
                        }
                        val instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.IzX)] ?: return Result(false, "invalid instruction",
                                                                                                              this.address, 0)
                        memory[address] = instruction.toShort()
                        memory[address+1] = indAddress.toShort()
                        instructionSize = 2
                    }
                    arg.startsWith("(") && arg.endsWith("),y") -> {
                        // indirect Y
                        val indAddress = try {
                            parseNumber(arg.substring(1, arg.length-3))
                        } catch (x: NumberFormatException) {
                            return Result(false, "invalid instruction", this.address, 0)
                        }
                        val instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.IzY)] ?: return Result(false, "invalid instruction",
                                                                                                              this.address, 0)
                        memory[address] = instruction.toShort()
                        memory[address+1] = indAddress.toShort()
                        instructionSize = 2
                    }
                    arg.endsWith(",x") -> {
                        // indexed X or zpIndexed X
                        val indAddress = try {
                            parseNumber(arg.substring(1, arg.length-2))
                        } catch (x: NumberFormatException) {
                            return Result(false, "invalid instruction", this.address, 0)
                        }
                        instructionSize = if (indAddress <= 255) {
                            val instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.ZpX)] ?: return Result(false, "invalid instruction",
                                                                                                                  this.address, 0)
                            memory[address] = instruction.toShort()
                            memory[address+1] = indAddress.toShort()
                            2
                        } else {
                            val instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.AbsX)] ?: return Result(false, "invalid instruction",
                                                                                                                   this.address, 0)
                            memory[address] = instruction.toShort()
                            memory[address+1] = (indAddress and 255).toShort()
                            memory[address+2] = (indAddress ushr 8).toShort()
                            3
                        }
                    }
                    arg.endsWith(",y") -> {
                        // indexed Y or zpIndexed Y
                        val indAddress = try {
                            parseNumber(arg.substring(1, arg.length-2))
                        } catch (x: NumberFormatException) {
                            return Result(false, "invalid instruction", this.address, 0)
                        }
                        instructionSize = if (indAddress <= 255) {
                            val instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.ZpY)] ?: return Result(false, "invalid instruction",
                                                                                                                  this.address, 0)
                            memory[address] = instruction.toShort()
                            memory[address+1] = indAddress.toShort()
                            2
                        } else {
                            val instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.AbsY)] ?: return Result(false, "invalid instruction",
                                                                                                                   this.address, 0)
                            memory[address] = instruction.toShort()
                            memory[address+1] = (indAddress and 255).toShort()
                            memory[address+2] = (indAddress ushr 8).toShort()
                            3
                        }
                    }
                    arg.endsWith(")") -> {
                        // indirect (jmp)
                        val indAddress = try {
                            parseNumber(arg.substring(1, arg.length-1))
                        } catch (x: NumberFormatException) {
                            return Result(false, "invalid instruction", this.address, 0)
                        }
                        val instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.Ind)]
                                          ?: return Result(false, "invalid instruction", this.address, 0)
                        memory[address] = instruction.toShort()
                        memory[address+1] = (indAddress and 255).toShort()
                        memory[address+2] = (indAddress ushr 8).toShort()
                        instructionSize = 3
                    }
                    else -> {
                        val instr = instructions[Pair(mnemonic, Cpu6502.AddrMode.Rel)]
                        if (instr != null) {
                            // relative address
                            val rel = try {
                                parseRelativeToPC(arg, address)
                            } catch (x: NumberFormatException) {
                                return Result(false, "invalid numeral", this.address, 0)
                            }
                            memory[address] = instr.toShort()
                            memory[address+1] = (rel-address-2 and 255).toShort()
                            instructionSize = 2
                        } else {
                            // absolute or absZp
                            val absAddress = try {
                                if(arg.startsWith('*')) parseRelativeToPC(arg, address) else parseNumber(arg)
                            } catch (x: NumberFormatException) {
                                return Result(false, "invalid numeral", this.address, 0)
                            }
                            val zpInstruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.Zp)]
                            instructionSize = if (absAddress <= 255 && zpInstruction != null) {
                                memory[address] = zpInstruction.toShort()
                                memory[address+1] = absAddress.toShort()
                                2
                            } else {
                                val absInstr = instructions[Pair(mnemonic, Cpu6502.AddrMode.Abs)] ?: return Result(false, "invalid instruction",
                                                                                                                   this.address, 0)
                                memory[address] = absInstr.toShort()
                                memory[address+1] = (absAddress and 255).toShort()
                                memory[address+2] = (absAddress ushr 8).toShort()
                                3
                            }
                        }
                    }
                }
            }
            else ->
                return Result(false, "syntax error", this.address, 0)
        }

        return Result(true, "", this.address, instructionSize)
    }
}
