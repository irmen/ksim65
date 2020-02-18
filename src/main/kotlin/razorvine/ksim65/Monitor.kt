package razorvine.ksim65

import kotlin.math.max

class Monitor(val bus: Bus, val cpu: Cpu6502) {

    private val disassembler = Disassembler(cpu)

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
                    val start = Assembler.parseNumber(parts[0])
                    val end = Assembler.parseNumber(parts[1])
                    val value = Assembler.parseNumber(parts[2]).toShort()
                    for (addr in start..end) {
                        bus.write(addr, value)
                    }
                    IVirtualMachine.MonitorCmdResult("ok", "", true)
                }
            }
            'm' -> {
                val addresses = command.substring(1).trim().split(' ')
                val start = Assembler.parseNumber(addresses[0])
                val end = if (addresses.size > 1) Assembler.parseNumber(addresses[1]) else start+1
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
                val address = Assembler.parseNumber(numbers[0])
                val values = numbers.drop(1).map { Assembler.parseNumber(it) }
                values.forEachIndexed { index, i -> bus.write(address+index, i.toShort()) }
                IVirtualMachine.MonitorCmdResult("ok", "", true)
            }
            'i' -> {
                val addresses = command.substring(1).trim().split(' ')
                val start = Assembler.parseNumber(addresses[0])
                val end = if (addresses.size > 1) Assembler.parseNumber(addresses[1]) else start+1
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
                val number = Assembler.parseNumber(command)
                val output = "$${hexW(number)}  #$number  %${number.toString(2)}"
                IVirtualMachine.MonitorCmdResult(output, "", true)
            }
            '#' -> {
                val number = Assembler.parseNumber(command)
                val output = "$${hexW(number)}  #$number  %${number.toString(2)}"
                IVirtualMachine.MonitorCmdResult(output, "", true)
            }
            '%' -> {
                val number = Assembler.parseNumber(command)
                val output = "$${hexW(number)}  #$number  %${number.toString(2)}"
                IVirtualMachine.MonitorCmdResult(output, "", true)
            }
            'g' -> {
                val address = Assembler.parseNumber(command.substring(1))
                cpu.regPC = address
                IVirtualMachine.MonitorCmdResult("", "", true)
            }
            'a' -> {
                val address = 0 // TODO parse from line
                val assembler = Assembler(cpu, bus.memoryComponentFor(address), address)
                val result = assembler.assemble(command.substring(1).trimStart())
                if(result.success) {
                    val memory = (result.startAddress..result.startAddress+result.numBytes).map { bus[it] }.toTypedArray()
                    val d = disassembler.disassembleOneInstruction(memory, 0, result.startAddress)
                    IVirtualMachine.MonitorCmdResult(d.first, "a$${hexW(result.startAddress+result.numBytes)} ", false)
                }
                else
                    IVirtualMachine.MonitorCmdResult(result.error, command, false)
            }
            'd' -> {
                val addresses = command.substring(1).trim().split(' ')
                val start = Assembler.parseNumber(addresses[0])
                val end = if (addresses.size > 1) Assembler.parseNumber(addresses[1]) else start
                val memory = (start .. max(0xffff, end+3)).map {bus[it]}.toTypedArray()
                val disassem = disassembler.disassemble(memory, 0 .. end-start, start)
                IVirtualMachine.MonitorCmdResult(disassem.first.joinToString("\n") { "d$it" }, "d$${hexW(disassem.second)}", false)
            }
            else -> {
                IVirtualMachine.MonitorCmdResult("?unknown command", "", true)
            }
        }
    }
}
