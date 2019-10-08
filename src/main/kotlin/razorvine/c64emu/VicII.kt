package razorvine.c64emu

import razorvine.ksim65.Cpu6502
import razorvine.ksim65.components.Address
import razorvine.ksim65.components.MemMappedComponent
import razorvine.ksim65.components.UByte

/**
 * Minimal simulation of the VIC-II graphics chip
 * It only has some logic to keep track of the raster line
 * This chip is the PAL version (50 Hz screen refresh, 312 vertical raster lines)
 */
class VicII(startAddress: Address, endAddress: Address, val cpu: Cpu6502): MemMappedComponent(startAddress, endAddress) {
    private var ramBuffer = Array<UByte>(endAddress - startAddress + 1) { 0xff }
    private var rasterIrqLine = 0
    private var scanlineClocks = 0
    private var interruptStatusRegisterD019 = 0
    var currentRasterLine = 1
    val vsync: Boolean
        get() = currentRasterLine==0

    companion object {
        const val framerate = 50
        const val rasterlines = 312
    }

    init {
        require(endAddress - startAddress + 1 == 0x400) { "vic-II requires exactly 1024 memory bytes (64*16 mirrored)" }
        ramBuffer[0x1a] = 0   // initially, disable IRQs
    }

    override fun clock() {
        scanlineClocks++
        if (scanlineClocks == 63) {
            scanlineClocks = 0
            currentRasterLine++
            if (currentRasterLine >= rasterlines)
                currentRasterLine = 0
            interruptStatusRegisterD019 = if (currentRasterLine == rasterIrqLine) {
                // signal that current raster line is equal to the desired IRQ raster line
                // schedule an IRQ as well if the raster interrupt is enabled
                if((ramBuffer[0x1a].toInt() and 1) != 0)
                    cpu.irq()
                interruptStatusRegisterD019 or 0b10000001
            } else
                interruptStatusRegisterD019 and 0b11111110
        }
    }

    override fun reset() {
        rasterIrqLine = 0
        currentRasterLine = 1
        interruptStatusRegisterD019 = 0
    }

    override fun get(address: Address): UByte {
        return when(val register = (address - startAddress) and 63) {
            0x11 -> (0b00011011 or ((currentRasterLine and 0b100000000) ushr 1)).toShort()
            0x12 -> {
                (currentRasterLine and 255).toShort()
            }
            0x19 -> interruptStatusRegisterD019.toShort()
            else -> ramBuffer[register]
        }
    }

    override fun set(address: Address, data: UByte) {
        val register = (address - startAddress) and 63
        ramBuffer[register] = data
        when (register) {
            0x11 -> {
                val rasterHigh = (data.toInt() ushr 7) shl 8
                rasterIrqLine = (rasterIrqLine and 0x00ff) or rasterHigh
            }
            0x12 -> rasterIrqLine = (rasterIrqLine and 0xff00) or data.toInt()
        }
    }
}
