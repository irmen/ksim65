package razorvine.ksim65

class CpuNes: Cpu6502Core() {
    override fun reset() {
        super.reset()
        instrCycles = 7    // the nintendulator cpu emu starts with this number of cycles
    }

    override fun iAdc(): Boolean {
        // NES cpu doesn't have BCD mode
        val decimal = regP.D
        regP.D = false
        val result = super.iAdc()
        regP.D = decimal
        return result
    }

    override fun iSbc2(operand: Int): Boolean {
        // NES cpu doesn't have BCD mode
        val decimal = regP.D
        regP.D = false
        val result = super.iSbc2(operand)
        regP.D = decimal
        return result
    }
}
