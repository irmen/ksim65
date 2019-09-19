package razorvine.c64emu

import razorvine.ksim65.components.Address
import razorvine.ksim65.components.MemMappedComponent
import razorvine.ksim65.components.UByte

class VicII(startAddress: Address, endAddress: Address): MemMappedComponent(startAddress, endAddress) {
    private var ramBuffer = Array<UByte>(endAddress - startAddress + 1) { 0 }
    private var rasterIrqLine = 0
    var currentRasterLine = 1
    private var totalClocks = 0L
    private var interruptStatusRegisterD019 = 0

    override fun clock() {
        totalClocks++
        if(totalClocks % 63L == 0L) {
            currentRasterLine++
            if(currentRasterLine >= 312)
                currentRasterLine = 0
            interruptStatusRegisterD019 = if(currentRasterLine == rasterIrqLine) {
                // signal that current raster line is equal to the desired IRQ raster line
                interruptStatusRegisterD019 or 0b00000001
            } else
                interruptStatusRegisterD019 and 0b11111110
        }
    }

    override fun reset() {
        rasterIrqLine = 0
        currentRasterLine = 1
        totalClocks = 0L
        interruptStatusRegisterD019 = 0
    }

    override fun get(address: Address): UByte {
        val register = (address - startAddress) and 63
        // println("VIC GET ${register.toString(16)}")
        return when(register) {
            0x11 -> (0b00011011 or ((currentRasterLine and 0b100000000) ushr 1)).toShort()
            0x12 -> {
                // println("   read raster: $currentRasterLine")
                (currentRasterLine and 255).toShort()
            }
            0x19 -> interruptStatusRegisterD019.toShort()
            else -> ramBuffer[register]
        }
    }

    override fun set(address: Address, data: UByte) {
        val register = (address - startAddress) and 63
        ramBuffer[register] = data
        when(register) {
            0x11  -> {
                val rasterHigh = (data.toInt() ushr 7) shl 8
                rasterIrqLine = (rasterIrqLine and 0x00ff) or rasterHigh
            }
            0x12 -> rasterIrqLine = (rasterIrqLine and 0xff00) or data.toInt()
        }
        // println("VIC SET ${register.toString(16)} = ${data.toString(16)}  (rasterIrqLine= $rasterIrqLine)")
    }
}
