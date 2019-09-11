package razorvine.ksim65.components

class Cpu65C02(stopOnBrk: Boolean) : Cpu6502(stopOnBrk) {

    enum class Wait {
        Normal,
        Waiting,
        Stopped
    }

    var waiting: Wait = Wait.Normal

    // TODO implement this CPU type 65C02, and re-enable the unit tests for that

    companion object {
        const val NMI_vector = Cpu6502.NMI_vector
        const val RESET_vector = Cpu6502.RESET_vector
        const val IRQ_vector = Cpu6502.NMI_vector
        const val resetCycles = Cpu6502.resetCycles
    }

    override fun clock() {
        when(waiting) {
            Wait.Normal -> super.clock()
            Wait.Waiting -> {
                if(pendingInterrupt!=null) {
                    // continue execution after hardware interrupt
                    waiting = Wait.Normal
                    instrCycles = 1
                }
            }
            Wait.Stopped -> {
                if(pendingInterrupt!=null) {
                    // jump to reset vector after hardware interrupt
                    PC = readWord(RESET_vector)
                }
            }
        }
    }

    // opcode list:  http://www.oxyron.de/html/opcodesc02.html
    override fun dispatchOpcode(opcode: Int) {
        when (opcode) {
            0x00 -> iBrk()
            0x01 -> iOra()
            0x02 -> iNop()
            0x03 -> iNop()
            0x04 -> iTsb()
            0x05 -> iOra()
            0x06 -> iAsl()
            0x07 -> iRmb0()
            0x08 -> iPhp()
            0x09 -> iOra()
            0x0a -> iAsl()
            0x0b -> iNop()
            0x0c -> iTsb()
            0x0d -> iOra()
            0x0e -> iAsl()
            0x0f -> iBrr0()

            0x10 -> iBpl()
            0x11 -> iOra()
            0x12 -> iOra()
            0x13 -> iNop()
            0x14 -> iTrb()
            0x15 -> iOra()
            0x16 -> iAsl()
            0x17 -> iRmb1()
            0x18 -> iClc()
            0x19 -> iOra()
            0x1a -> iInc()
            0x1b -> iNop()
            0x1c -> iTrb()
            0x1d -> iOra()
            0x1e -> iAsl()
            0x1f -> iBrr1()

            0x20 -> iJsr()
            0x21 -> iAnd()
            0x22 -> iNop()
            0x23 -> iNop()
            0x24 -> iBit()
            0x25 -> iAnd()
            0x26 -> iRol()
            0x27 -> iRmb2()
            0x28 -> iPlp()
            0x29 -> iAnd()
            0x2a -> iRol()
            0x2b -> iNop()
            0x2c -> iBit()
            0x2d -> iAnd()
            0x2e -> iRol()
            0x2f -> iBrr2()

            0x30 -> iBmi()
            0x31 -> iAnd()
            0x32 -> iAnd()
            0x33 -> iNop()
            0x34 -> iBit()
            0x35 -> iAnd()
            0x36 -> iRol()
            0x37 -> iRmb3()
            0x38 -> iSec()
            0x39 -> iAnd()
            0x3a -> iDec()
            0x3b -> iNop()
            0x3c -> iBit()
            0x3d -> iAnd()
            0x3e -> iRol()
            0x3f -> iBrr3()

            0x40 -> iRti()
            0x41 -> iEor()
            0x42 -> iNop()
            0x43 -> iNop()
            0x44 -> iNop()
            0x45 -> iEor()
            0x46 -> iLsr()
            0x47 -> iRmb4()
            0x48 -> iPha()
            0x49 -> iEor()
            0x4a -> iLsr()
            0x4b -> iNop()
            0x4c -> iJmp()
            0x4d -> iEor()
            0x4e -> iLsr()
            0x4f -> iBrr4()

            0x50 -> iBvc()
            0x51 -> iEor()
            0x52 -> iEor()
            0x53 -> iNop()
            0x54 -> iNop()
            0x55 -> iEor()
            0x56 -> iLsr()
            0x57 -> iRmb5()
            0x58 -> iCli()
            0x59 -> iEor()
            0x5a -> iPhy()
            0x5b -> iNop()
            0x5c -> iNop()
            0x5d -> iEor()
            0x5e -> iLsr()
            0x5f -> iBrr5()

            0x60 -> iRts()
            0x61 -> iAdc()
            0x62 -> iNop()
            0x63 -> iNop()
            0x64 -> iStz()
            0x65 -> iAdc()
            0x66 -> iRor()
            0x67 -> iRmb6()
            0x68 -> iPla()
            0x69 -> iAdc()
            0x6a -> iRor()
            0x6b -> iNop()
            0x6c -> iJmp()
            0x6d -> iAdc()
            0x6e -> iRor()
            0x6f -> iBrr6()

            0x70 -> iBvs()
            0x71 -> iAdc()
            0x72 -> iAdc()
            0x73 -> iNop()
            0x74 -> iStz()
            0x75 -> iAdc()
            0x76 -> iRor()
            0x77 -> iRmb7()
            0x78 -> iSei()
            0x79 -> iAdc()
            0x7a -> iPly()
            0x7b -> iNop()
            0x7c -> iJmp()
            0x7d -> iAdc()
            0x7e -> iRor()
            0x7f -> iBrr7()

            0x80 -> iBra()
            0x81 -> iSta()
            0x82 -> iNop()
            0x83 -> iNop()
            0x84 -> iSty()
            0x85 -> iSta()
            0x86 -> iStx()
            0x87 -> iSmb0()
            0x88 -> iDey()
            0x89 -> iBit()
            0x8a -> iTxa()
            0x8b -> iNop()
            0x8c -> iSty()
            0x8d -> iSta()
            0x8e -> iStx()
            0x8f -> iBbs0()

            0x90 -> iBcc()
            0x91 -> iSta()
            0x92 -> iSta()
            0x93 -> iNop()
            0x94 -> iSty()
            0x95 -> iSta()
            0x96 -> iStx()
            0x97 -> iSmb1()
            0x98 -> iTya()
            0x99 -> iSta()
            0x9a -> iTxs()
            0x9b -> iNop()
            0x9c -> iStz()
            0x9d -> iSta()
            0x9e -> iStz()
            0x9f -> iBbs1()

            0xa0 -> iLdy()
            0xa1 -> iLda()
            0xa2 -> iLdx()
            0xa3 -> iNop()
            0xa4 -> iLdy()
            0xa5 -> iLda()
            0xa6 -> iLdx()
            0xa7 -> iSmb2()
            0xa8 -> iTay()
            0xa9 -> iLda()
            0xaa -> iTax()
            0xab -> iNop()
            0xac -> iLdy()
            0xad -> iLda()
            0xae -> iLdx()
            0xaf -> iBbs2()

            0xb0 -> iBcs()
            0xb1 -> iLda()
            0xb2 -> iLda()
            0xb3 -> iNop()
            0xb4 -> iLdy()
            0xb5 -> iLda()
            0xb6 -> iLdx()
            0xb7 -> iSmb3()
            0xb8 -> iClv()
            0xb9 -> iLda()
            0xba -> iTsx()
            0xbb -> iNop()
            0xbc -> iLdy()
            0xbd -> iLda()
            0xbe -> iLdx()
            0xbf -> iBbs3()

            0xc0 -> iCpy()
            0xc1 -> iCmp()
            0xc2 -> iNop()
            0xc3 -> iNop()
            0xc4 -> iCpy()
            0xc5 -> iCmp()
            0xc6 -> iDec()
            0xc7 -> iSmb4()
            0xc8 -> iIny()
            0xc9 -> iCmp()
            0xca -> iDex()
            0xcb -> iWai()
            0xcc -> iCpy()
            0xcd -> iCmp()
            0xce -> iDec()
            0xcf -> iBbs4()

            0xd0 -> iBne()
            0xd1 -> iCmp()
            0xd2 -> iCmp()
            0xd3 -> iNop()
            0xd4 -> iNop()
            0xd5 -> iCmp()
            0xd6 -> iDec()
            0xd7 -> iSmb5()
            0xd8 -> iCld()
            0xd9 -> iCmp()
            0xda -> iPhx()
            0xdb -> iStp()
            0xdc -> iNop()
            0xdd -> iCmp()
            0xde -> iDec()
            0xdf -> iBbs5()

            0xe0 -> iCpx()
            0xe1 -> iSbc()
            0xe2 -> iNop()
            0xe3 -> iNop()
            0xe4 -> iCpx()
            0xe5 -> iSbc()
            0xe6 -> iInc()
            0xe7 -> iSmb6()
            0xe8 -> iInx()
            0xe9 -> iSbc()
            0xea -> iNop()
            0xeb -> iNop()
            0xec -> iCpx()
            0xed -> iSbc()
            0xee -> iInc()
            0xef -> iBbs6()

            0xf0 -> iBeq()
            0xf1 -> iSbc()
            0xf2 -> iSbc()
            0xf3 -> iNop()
            0xf4 -> iNop()
            0xf5 -> iSbc()
            0xf6 -> iInc()
            0xf7 -> iSmb7()
            0xf8 -> iSed()
            0xf9 -> iSbc()
            0xfa -> iPlx()
            0xfb -> iNop()
            0xfc -> iNop()
            0xfd -> iSbc()
            0xfe -> iInc()
            0xff -> iBbs7()
            else -> { /* can't occur */
            }
        }
    }

    private fun iBra() {
        TODO("bra")
    }

    private fun iTrb() {
        TODO("trb")
    }

    private fun iTsb() {
        TODO("tsb")
    }

    private fun iStz() {
        TODO("stz")
    }

    private fun iWai() {
        waiting = Wait.Waiting
        PC--
    }

    private fun iStp() {
        waiting = Wait.Stopped
        PC--
    }

    private fun iPhx() {
        pushStack(X)
    }

    private fun iPlx() {
        X = popStack()
        Status.Z = X == 0
        Status.N = (X and 0b10000000) != 0
    }

    private fun iPhy() {
        pushStack(Y)
    }

    private fun iPly() {
        Y = popStack()
        Status.Z = Y == 0
        Status.N = (Y and 0b10000000) != 0
    }

    private fun iBrr0() {
        TODO("brr0")
        val x = hexB(2)
        val y = hexW(2)
        val z = hexB(3.toShort())
    }

    private fun iBrr1() {
        TODO("brr1")
    }

    private fun iBrr2() {
        TODO("brr2")
    }

    private fun iBrr3() {
        TODO("brr3")
    }

    private fun iBrr4() {
        TODO("brr4")
    }

    private fun iBrr5() {
        TODO("brr5")
    }

    private fun iBrr6() {
        TODO("brr6")
    }

    private fun iBrr7() {
        TODO("brr7")
    }

    private fun iBbs0() {
        TODO("bbs0")
    }

    private fun iBbs1() {
        TODO("bbs1")
    }

    private fun iBbs2() {
        TODO("bbs2")
    }

    private fun iBbs3() {
        TODO("bbs3")
    }

    private fun iBbs4() {
        TODO("bbs4")
    }

    private fun iBbs5() {
        TODO("bbs5")
    }

    private fun iBbs6() {
        TODO("bbs6")
    }

    private fun iBbs7() {
        TODO("bbs7")
    }

    private fun iSmb0() {
        TODO("smb0")
    }

    private fun iSmb1() {
        TODO("smb1")
    }

    private fun iSmb2() {
        TODO("smb2")
    }

    private fun iSmb3() {
        TODO("sm30")
    }

    private fun iSmb4() {
        TODO("smb4")
    }

    private fun iSmb5() {
        TODO("smb5")
    }

    private fun iSmb6() {
        TODO("smb6")
    }

    private fun iSmb7() {
        TODO("smb7")
    }

    private fun iRmb0() {
        TODO("rmb0")
    }

    private fun iRmb1() {
        TODO("rmb1")
    }

    private fun iRmb2() {
        TODO("rmb2")
    }

    private fun iRmb3() {
        TODO("rmb3")
    }

    private fun iRmb4() {
        TODO("rmb4")
    }

    private fun iRmb5() {
        TODO("rmb5")
    }

    private fun iRmb6() {
        TODO("rmb6")
    }

    private fun iRmb7() {
        TODO("rmb7")
    }
}
