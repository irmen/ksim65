package razorvine.ksim65

import kotlin.math.max

class Monitor(val bus: Bus, val cpu: Cpu6502) {

    private val instructions by lazy {
        val instr = cpu.instructions.withIndex().associate {
            Pair(it.value.mnemonic, it.value.mode) to it.index
        }.toMutableMap()
        instr[Pair("nop", Cpu6502.AddrMode.Imp)] = 0xea
        instr.toMap()
    }

    fun command(command: String): IVirtualMachine.MonitorCmdResult {
        if (command.isEmpty()) return IVirtualMachine.MonitorCmdResult("", "", false)

        return when (command[0]) {
            'h', '?' -> {
                val text = "h)elp  m)emory  i)nspect  f)ill  p)oke  g)o  a)ssemble  d)isassemble\n$ # % for hex, dec, bin number"
                IVirtualMachine.MonitorCmdResult(text, "", false)
            }
            'f' -> {
                val parts = command.substring(1).trim().split(' ')
                if (parts.size != 3) IVirtualMachine.MonitorCmdResult("?syntax error", command, false)
                else {
                    val start = parseNumber(parts[0])
                    val end = parseNumber(parts[1])
                    val value = parseNumber(parts[2]).toShort()
                    for (addr in start..end) {
                        bus.write(addr, value)
                    }
                    IVirtualMachine.MonitorCmdResult("ok", "", true)
                }
            }
            'm' -> {
                val addresses = command.substring(1).trim().split(' ')
                val start = parseNumber(addresses[0])
                val end = if (addresses.size > 1) parseNumber(addresses[1]) else start+1
                val result = mutableListOf<String>()
                for (addr in start until end step 16) {
                    result.add("m$${hexW(addr)}  "+(0..15).joinToString(" ") { hexB(bus.read(addr+it)) }+"  "+(0..15).joinToString("") {
                        val chr = bus.read(addr+it).toChar()
                        if (chr.isLetterOrDigit()) chr.toString()
                        else "."
                    })
                }
                IVirtualMachine.MonitorCmdResult(result.joinToString("\n"), "", true)
            }
            'p' -> {
                val numbers = command.substring(1).trim().split(' ')
                val address = parseNumber(numbers[0])
                val values = numbers.drop(1).map { parseNumber(it) }
                values.forEachIndexed { index, i -> bus.write(address+index, i.toShort()) }
                IVirtualMachine.MonitorCmdResult("ok", "", true)
            }
            'i' -> {
                val addresses = command.substring(1).trim().split(' ')
                val start = parseNumber(addresses[0])
                val end = if (addresses.size > 1) parseNumber(addresses[1]) else start+1
                val result = mutableListOf<String>()
                for (addr in start until end step 64) {
                    result.add("i$${hexW(addr)}  "+(0..63).joinToString("") {
                        val chr = bus.read(addr+it).toChar()
                        if (chr.isLetterOrDigit()) chr.toString()
                        else "."
                    })
                }
                IVirtualMachine.MonitorCmdResult(result.joinToString("\n"), "", true)
            }
            '$' -> {
                val number = parseNumber(command)
                val output = "$${hexW(number)}  #$number  %${number.toString(2)}"
                IVirtualMachine.MonitorCmdResult(output, "", true)
            }
            '#' -> {
                val number = parseNumber(command)
                val output = "$${hexW(number)}  #$number  %${number.toString(2)}"
                IVirtualMachine.MonitorCmdResult(output, "", true)
            }
            '%' -> {
                val number = parseNumber(command)
                val output = "$${hexW(number)}  #$number  %${number.toString(2)}"
                IVirtualMachine.MonitorCmdResult(output, "", true)
            }
            'g' -> {
                val address = parseNumber(command.substring(1))
                cpu.regPC = address
                IVirtualMachine.MonitorCmdResult("", "", true)
            }
            'a' -> {
                val parts = command.substring(1).trim().split(' ')
                assemble(command, parts)
            }
            'd' -> {
                val addresses = command.substring(1).trim().split(' ')
                val start = parseNumber(addresses[0])
                val end = if (addresses.size > 1) parseNumber(addresses[1]) else start
                val memory = (start .. max(0xffff, end+3)).map {bus[it]}.toTypedArray()
                val disassem = cpu.disassemble(memory, 0 .. end-start, start)
                IVirtualMachine.MonitorCmdResult(disassem.first.joinToString("\n") { "d$it" }, "d$${hexW(disassem.second)}", false)
            }
            else -> {
                IVirtualMachine.MonitorCmdResult("?unknown command", "", true)
            }
        }
    }

    private fun assemble(command: String, parts: List<String>): IVirtualMachine.MonitorCmdResult {
        if (parts.size < 2) return IVirtualMachine.MonitorCmdResult("done", "", false)

        val address = parseNumber(parts[0])
        val mnemonic = parts[1].toLowerCase()
        when (parts.size) {
            2 -> {
                // implied or acc
                var instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.Imp)]
                if (instruction == null) instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.Acc)]
                if (instruction == null) return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                bus.write(address, instruction.toShort())
            }
            3 -> {
                val arg = parts[2]
                when {
                    arg.startsWith('#') -> {
                        // immediate
                        val instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.Imm)] ?: return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                        bus.write(address, instruction.toShort())
                        bus.write(address+1, parseNumber(arg.substring(1), decimalFirst = true).toShort())
                    }
                    arg.startsWith("(") && arg.endsWith(",x)") -> {
                        // indirect X
                        val indAddress = try {
                            parseNumber(arg.substring(1, arg.length-3))
                        } catch (x: NumberFormatException) {
                            return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                        }
                        val instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.IzX)] ?: return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                        bus.write(address, instruction.toShort())
                        bus.write(address+1, indAddress.toShort())
                    }
                    arg.startsWith("(") && arg.endsWith("),y") -> {
                        // indirect Y
                        val indAddress = try {
                            parseNumber(arg.substring(1, arg.length-3))
                        } catch (x: NumberFormatException) {
                            return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                        }
                        val instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.IzY)] ?: return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                        bus.write(address, instruction.toShort())
                        bus.write(address+1, indAddress.toShort())
                    }
                    arg.endsWith(",x") -> {
                        // indexed X or zpIndexed X
                        val indAddress = try {
                            parseNumber(arg.substring(1, arg.length-2))
                        } catch (x: NumberFormatException) {
                            return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                        }
                        if (indAddress <= 255) {
                            val instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.ZpX)] ?: return IVirtualMachine.MonitorCmdResult(
                                    "?invalid instruction", command, false)
                            bus.write(address, instruction.toShort())
                            bus.write(address+1, indAddress.toShort())
                        } else {
                            val instruction =
                                    instructions[Pair(mnemonic, Cpu6502.AddrMode.AbsX)] ?: return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                            bus.write(address, instruction.toShort())
                            bus.write(address+1, (indAddress and 255).toShort())
                            bus.write(address+2, (indAddress ushr 8).toShort())
                        }
                    }
                    arg.endsWith(",y") -> {
                        // indexed Y or zpIndexed Y
                        val indAddress = try {
                            parseNumber(arg.substring(1, arg.length-2))
                        } catch (x: NumberFormatException) {
                            return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                        }
                        if (indAddress <= 255) {
                            val instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.ZpY)] ?: return IVirtualMachine.MonitorCmdResult(
                                    "?invalid instruction", command, false)
                            bus.write(address, instruction.toShort())
                            bus.write(address+1, indAddress.toShort())
                        } else {
                            val instruction =
                                    instructions[Pair(mnemonic, Cpu6502.AddrMode.AbsY)] ?: return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                            bus.write(address, instruction.toShort())
                            bus.write(address+1, (indAddress and 255).toShort())
                            bus.write(address+2, (indAddress ushr 8).toShort())
                        }
                    }
                    arg.endsWith(")") -> {
                        // indirect (jmp)
                        val indAddress = try {
                            parseNumber(arg.substring(1, arg.length-1))
                        } catch (x: NumberFormatException) {
                            return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                        }
                        val instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.Ind)] ?: return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                        bus.write(address, instruction.toShort())
                        bus.write(address+1, (indAddress and 255).toShort())
                        bus.write(address+2, (indAddress ushr 8).toShort())
                    }
                    else -> {
                        val instr = instructions[Pair(mnemonic, Cpu6502.AddrMode.Rel)]
                        if (instr != null) {
                            // relative address
                            val rel = try {
                                parseRelativeToPC(arg, address)
                            } catch (x: NumberFormatException) {
                                return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                            }
                            bus.write(address, instr.toShort())
                            bus.write(address+1, (rel-address-2 and 255).toShort())
                        } else {
                            // absolute or absZp
                            val absAddress = try {
                                if(arg.startsWith('*')) parseRelativeToPC(arg, address) else parseNumber(arg)
                            } catch (x: NumberFormatException) {
                                return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                            }
                            val zpInstruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.Zp)]
                            if (absAddress <= 255 && zpInstruction!=null) {
                                bus.write(address, zpInstruction.toShort())
                                bus.write(address+1, absAddress.toShort())
                            } else {
                                val absInstr = instructions[Pair(mnemonic, Cpu6502.AddrMode.Abs)] ?: return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                                bus.write(address, absInstr.toShort())
                                bus.write(address+1, (absAddress and 255).toShort())
                                bus.write(address+2, (absAddress ushr 8).toShort())
                            }
                        }
                    }
                }
            }
            else -> return IVirtualMachine.MonitorCmdResult("?syntax error", command, false)
        }

        val memory = listOf(bus[address], bus[address+1], bus[address+2]).toTypedArray()
        val disassem = cpu.disassembleOneInstruction(memory, 0, address)
        return IVirtualMachine.MonitorCmdResult(disassem.first, "a$${hexW(disassem.second + address)} ", false)
    }

    private fun parseRelativeToPC(relative: String, currentAddress: Int): Int {
        val rest = relative.substring(1).trim()
        if(rest.isNotEmpty()) {
            return when(rest[0]) {
                '-' -> currentAddress-parseNumber(rest.substring(1))
                '+' -> currentAddress+parseNumber(rest.substring(1))
                else -> throw Cpu6502.InstructionError("invalid address syntax")
            }
        }
        return currentAddress
    }

    private fun parseNumber(number: String, decimalFirst: Boolean = false): Int {
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
