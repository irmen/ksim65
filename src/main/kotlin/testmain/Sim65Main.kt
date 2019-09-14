package testmain

import razorvine.ksim65.Bus
import razorvine.ksim65.Cpu6502
import razorvine.ksim65.Version
import razorvine.ksim65.components.*


fun main(args: Array<String>) {
    println(Version.copyright)
    startSimulator(args)
}


private fun startSimulator(args: Array<String>) {

    // create a computer system.
    // note that the order in which components are added to the bus, is important:
    // it determines the priority of reads and writes.
    val cpu = Cpu6502(true)
    val ram = Ram(0, 0xffff)
    ram[Cpu6502.RESET_vector] = 0x00
    ram[Cpu6502.RESET_vector + 1] = 0x10
    ram[Cpu6502.IRQ_vector] = 0x00
    ram[Cpu6502.IRQ_vector + 1] = 0x20
    ram[Cpu6502.NMI_vector] = 0x00
    ram[Cpu6502.NMI_vector + 1] = 0x30

//    // read the RTC and write the date+time to $2000
//    for(b in listOf(0xa0, 0x00, 0xb9, 0x00, 0xd1, 0x99, 0x00, 0x20, 0xc8, 0xc0, 0x09, 0xd0, 0xf5, 0x00).withIndex()) {
//        ram[0x1000+b.index] = b.value.toShort()
//    }

    // set the timer to $22aa00 and enable it on regular irq
    for(b in listOf(0xa9, 0x00, 0x8d, 0x00, 0xd2, 0xa9, 0x00, 0x8d, 0x01, 0xd2, 0xa9, 0xaa, 0x8d, 0x02,
            0xd2, 0xa9, 0x22, 0x8d, 0x03, 0xd2, 0xa9, 0x01, 0x8d, 0x00, 0xd2, 0x4c, 0x19, 0x10).withIndex()) {
        ram[0x1000+b.index] = b.value.toShort()
    }


    // load the irq routine that prints  'irq!' to the parallel port
    for(b in listOf(0x48, 0xa9, 0x09, 0x8d, 0x00, 0xd0, 0xee, 0x01, 0xd0, 0xa9, 0x12, 0x8d, 0x00, 0xd0,
            0xee, 0x01, 0xd0, 0xa9, 0x11, 0x8d, 0x00, 0xd0, 0xee, 0x01, 0xd0, 0xa9, 0x21, 0x8d, 0x00, 0xd0,
            0xee, 0x01, 0xd0, 0x68, 0x40).withIndex()) {
        ram[0x2000+b.index] = b.value.toShort()
    }

    val parallel = ParallelPort(0xd000, 0xd001)
    val clock = RealTimeClock(0xd100, 0xd108)
    val timer = Timer(0xd200, 0xd203, cpu)

    val bus = Bus()
    bus.add(cpu)
    bus.add(parallel)
    bus.add(clock)
    bus.add(timer)
    bus.add(ram)
    bus.reset()

    cpu.regP.I = false    // enable interrupts

    // TODO
//    try {
//        while (true) {
//            bus.clock()
//        }
//    } catch (ix: Cpu6502.InstructionError) {
//        println("Hmmm... $ix")
//    }

    ram.hexDump(0x1000, 0x1020)
    val dis = cpu.disassemble(ram, 0x1000, 0x1020)
    println(dis.joinToString("\n"))
    ram.hexDump(0x2000, 0x2008)
}
