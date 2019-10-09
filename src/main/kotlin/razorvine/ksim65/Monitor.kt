package razorvine.ksim65

class Monitor(val bus: Bus, val cpu: Cpu6502) {

    private val instructions by lazy {
        val instr = cpu.instructions.withIndex().associate {
            Pair(it.value.mnemonic, it.value.mode) to it.index
        }.toMutableMap()
        instr[Pair("nop", Cpu6502.AddrMode.Imp)] = 0xea
        instr.toMap()
    }

    fun command(command: String): IVirtualMachine.MonitorCmdResult {
        if(command.isEmpty())
            return IVirtualMachine.MonitorCmdResult("", "", false)

        return when(command[0]) {
            'f' -> {
                val parts = command.substring(1).trim().split(' ')
                if(parts.size!=3)
                    IVirtualMachine.MonitorCmdResult("?syntax error", command, false)
                else {
                    val start = parseNumber(parts[0])
                    val end = parseNumber(parts[1])
                    val value = parseNumber(parts[2]).toShort()
                    for(addr in start..end) {
                        bus.write(addr, value)
                    }
                    IVirtualMachine.MonitorCmdResult("ok", "", true)
                }
            }
            'm' -> {
                // TODO add possibility to change bytes
                val addresses = command.substring(1).trim().split(' ')
                val start = parseNumber(addresses[0])
                val end = if(addresses.size>1) parseNumber(addresses[1]) else start+1
                val result = mutableListOf<String>()
                for(addr in start until end step 16) {
                    result.add(
                        "m$${hexW(addr)}  " +
                                (0..15).joinToString(" ") { hexB(bus.read(addr + it)) } + "  " +
                                (0..15).joinToString("") {
                                    val chr = bus.read(addr+it).toChar()
                                    if(chr.isLetterOrDigit())
                                        chr.toString()
                                    else
                                        "."
                                }
                    )
                }
                IVirtualMachine.MonitorCmdResult(result.joinToString("\n"), "", true)
            }
            'i' -> {
                val addresses = command.substring(1).trim().split(' ')
                val start = parseNumber(addresses[0])
                val end = if(addresses.size>1) parseNumber(addresses[1]) else start+1
                val result = mutableListOf<String>()
                for(addr in start until end step 64) {
                    result.add(
                        "i$${hexW(addr)}  " +
                                (0..63).joinToString("") {
                                    val chr = bus.read(addr+it).toChar()
                                    if(chr.isLetterOrDigit())
                                        chr.toString()
                                    else
                                        "."
                                }
                    )
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
                val end = if(addresses.size>1) parseNumber(addresses[1]) else start
                val disassem = cpu.disassemble(bus.memoryComponentFor(start), start, end)
                IVirtualMachine.MonitorCmdResult(disassem.first.joinToString("\n") { "d$it" }, "d$${hexW(disassem.second)}", false)
            }
            else -> {
                IVirtualMachine.MonitorCmdResult("?unknown command", "", true)
            }
        }
    }

    private fun assemble(command: String, parts: List<String>): IVirtualMachine.MonitorCmdResult {
        if(parts.size<2)
            return IVirtualMachine.MonitorCmdResult("done", "", false)

        val address = parseNumber(parts[0])
        val mnemonic = parts[1].toLowerCase()
        when {
            parts.size==2 -> {
                // implied or acc
                var instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.Imp)]
                if(instruction==null)
                    instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.Acc)]
                if(instruction==null)
                    return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                bus.write(address, instruction.toShort())
                val disassem = cpu.disassemble(bus.memoryComponentFor(address), address, address)
                return IVirtualMachine.MonitorCmdResult(disassem.first.single(), "a$${hexW(disassem.second)} ", false)
            }
            parts.size==3 -> {
                val arg = parts[2]
                when {
                    arg.startsWith('#') -> {
                        // immediate
                        val instruction = instructions[Pair(mnemonic, Cpu6502.AddrMode.Imm)]
                            ?: return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                        bus.write(address, instruction.toShort())
                        bus.write(address+1, parseNumber(arg.substring(1), true).toShort())
                        val disassem = cpu.disassemble(bus.memoryComponentFor(address), address, address)
                        return IVirtualMachine.MonitorCmdResult(disassem.first.single(), "a$${hexW(disassem.second)} ", false)
                    }
                    arg.startsWith('(') -> // indirect or indirect+indexed
                        TODO("assemble indirect addrmode $arg")
                    arg.contains(",x") -> // indexed x
                        TODO("assemble indexed X addrmode $arg")
                    arg.contains(",y") -> // indexed y
                        TODO("assemble indexed X addrmode $arg")
                    else -> {
                        val instr = instructions[Pair(mnemonic, Cpu6502.AddrMode.Rel)]
                        if(instr!=null) {
                            // relative address
                            val rel = parseNumber(arg)
                            bus.write(address, instr.toShort())
                            bus.write(address+1, (rel-address-2 and 255).toShort())
                        } else {
                            // absolute or absZp
                            val absAddress = parseNumber(arg)
                            if (absAddress <= 255) {
                                val absInstr = instructions[Pair(mnemonic, Cpu6502.AddrMode.Zp)]
                                    ?: return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                                bus.write(address, absInstr.toShort())
                                bus.write(address + 1, absAddress.toShort())
                            } else {
                                val absInstr = instructions[Pair(mnemonic, Cpu6502.AddrMode.Abs)]
                                    ?: return IVirtualMachine.MonitorCmdResult("?invalid instruction", command, false)
                                bus.write(address, absInstr.toShort())
                                bus.write(address + 1, (absAddress and 255).toShort())
                                bus.write(address + 2, (absAddress ushr 8).toShort())
                            }
                        }
                        val disassem = cpu.disassemble(bus.memoryComponentFor(address), address, address)
                        return IVirtualMachine.MonitorCmdResult(disassem.first.single(), "a$${hexW(disassem.second)} ", false)
                    }
                }
            }
            else -> return IVirtualMachine.MonitorCmdResult("?syntax error", command, false)
        }
    }

    private fun parseNumber(number: String, decimalFirst: Boolean = false): Int {
        val num=number.trim()
        if(num.isBlank())
            return 0
        if(decimalFirst && num[0].isDigit())
            return num.toInt(10)
        return when(num[0]) {
            '$' -> num.substring(1).trimStart().toInt(16)
            '#' -> num.substring(1).trimStart().toInt(10)
            '%' -> num.substring(1).trimStart().toInt(2)
            else -> num.toInt(16)
        }
    }
}
