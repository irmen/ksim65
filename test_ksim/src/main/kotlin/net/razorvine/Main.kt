package net.razorvine

import razorvine.ksim65.Version
import razorvine.ksim65.testing.CpuType
import razorvine.ksim65.testing.TestMachine
import java.io.File

fun main(args: Array<String>) {
    val serial = CapturingSerialIO(printToStdout = true)
    val machine = TestMachine(CpuType.CPU65C02, serialAndPower = serial)
    println(Version.copyright)
    println("Machine created with CPU type: ${machine.cpu.name}")

    machine.loadFileInRam(File(args[0]), null)

    println()
    machine.ram.hexDump(0x1000, 0x103f)
    println()
    val disassembler = razorvine.ksim65.Disassembler(machine.cpu)
    val (lines, _) = disassembler.disassemble(machine.ram.data, 0x1000..0x1020, 0)
    lines.forEach { println(it) }

    println("\n~~Machine powered on~~\n")

    val maxSteps = 1_000_000
    try {
        repeat(maxSteps) { machine.step() }
        println("\n~~Machine execution loop finished (max steps reached), final state:~~")
    } catch(_: CapturingSerialIO.PoweroffMachine) {
        println("\n~~Machine powered off, final state:~~")
    }

    // Assert the output
    serial.assertOutputEquals("hello, world!\n")

    println(machine.cpu.snapshot())
}



