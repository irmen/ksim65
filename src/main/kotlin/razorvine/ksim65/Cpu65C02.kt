package razorvine.ksim65

import razorvine.ksim65.components.Address

/**
 * 65C02 cpu simulation (the CMOS version of the 6502).
 */
class Cpu65C02 : Cpu6502() {
    override val name = "65C02"

    enum class Wait {
        Normal, Waiting, Stopped
    }

    var waiting: Wait = Wait.Normal


    /**
     * Process once clock cycle in the cpu
     * Use this if you need cycle-perfect instruction timing simulation.
     */
    override fun clock() {
        when (waiting) {
            Wait.Normal -> super.clock()
            Wait.Waiting -> {
                if (nmiAsserted || irqAsserted) {
                    // continue execution after hardware interrupt
                    waiting = Wait.Normal
                    instrCycles = 1
                }
            }
            Wait.Stopped -> {
                if (nmiAsserted || irqAsserted) {
                    // jump to reset vector after hardware interrupt
                    regPC = readWord(RESET_VECTOR)
                }
            }
        }
    }

    /**
     * Execute one single complete instruction.
     * Use this when you don't care about clock cycle instruction timing simulation.
     */
    override fun step() {
        totalCycles += instrCycles
        instrCycles = 0
        if (waiting == Wait.Normal) {
            clock()
            totalCycles += instrCycles
            instrCycles = 0
        }
    }

    // branch-relative address fetched by the ZpR addressing mode
    private var fetchedAddressZpr: Address = 0

    override fun applyAddressingMode(addrMode: AddrMode): Boolean {
        return when (addrMode) {
            AddrMode.Imp, AddrMode.Acc, AddrMode.Imm, AddrMode.Zp, AddrMode.ZpX, AddrMode.ZpY,
            AddrMode.Rel, AddrMode.Abs, AddrMode.AbsX, AddrMode.AbsY, AddrMode.IzX, AddrMode.IzY -> {
                super.applyAddressingMode(addrMode)
            }
            AddrMode.Ind -> {
                var lo = readPc()
                var hi = readPc()
                fetchedAddress = lo or (hi shl 8)
                // 65c02 doesn't have the page bug of the 6502
                lo = read(fetchedAddress)
                hi = read(fetchedAddress+1)
                fetchedAddress = lo or (hi shl 8)
                false
            }
            AddrMode.Zpr -> {
                // addressing mode used by the 65C02 only
                // combination of zp addressing + relative branch addressing
                fetchedAddress = readPc()
                val relative = readPc()
                fetchedAddressZpr =
                        if (relative >= 0x80) regPC-(256-relative) and 0xffff else regPC+relative and 0xffff
                false
            }
            AddrMode.Izp -> {
                // addressing mode used by the 65C02 only
                fetchedAddress = readPc()
                val lo = read((fetchedAddress) and 0xff)
                val hi = read((fetchedAddress+1) and 0xff)
                fetchedAddress = lo or (hi shl 8)
                false
            }
            AddrMode.IaX -> {
                // addressing mode used by the 65C02 only
                var lo = readPc()
                var hi = readPc()
                fetchedAddress = lo or (hi shl 8)
                lo = read((fetchedAddress+regX) and 0xffff)
                hi = read((fetchedAddress+regX+1) and 0xffff)
                fetchedAddress = lo or (hi shl 8)
                // if this address is a different page, extra clock cycle:
                (fetchedAddress and 0xff00) != hi shl 8
            }
        }
    }

    override fun dispatchOpcode(opcode: Int): Boolean {
        return when (opcode) {
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
            0x0f -> iBbr0()
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
            0x1f -> iBbr1()
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
            0x2f -> iBbr2()
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
            0x3f -> iBbr3()
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
            0x4f -> iBbr4()
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
            0x5f -> iBbr5()
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
            0x6f -> iBbr6()
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
            0x7f -> iBbr7()
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
            else -> false /* can't occur */
        }
    }

    // opcode list:  http://www.oxyron.de/html/opcodesc02.html
    override val instructions: Array<Instruction> = listOf(
            /* 00 */  Instruction("brk", AddrMode.Imp, 7),
            /* 01 */  Instruction("ora", AddrMode.IzX, 6),
            /* 02 */  Instruction("nop", AddrMode.Imm, 2),
            /* 03 */  Instruction("nop", AddrMode.Imp, 1),
            /* 04 */  Instruction("tsb", AddrMode.Zp, 5),
            /* 05 */  Instruction("ora", AddrMode.Zp, 3),
            /* 06 */  Instruction("asl", AddrMode.Zp, 5),
            /* 07 */  Instruction("rmb0", AddrMode.Zp, 5),
            /* 08 */  Instruction("php", AddrMode.Imp, 3),
            /* 09 */  Instruction("ora", AddrMode.Imm, 2),
            /* 0a */  Instruction("asl", AddrMode.Acc, 2),
            /* 0b */  Instruction("nop", AddrMode.Imp, 1),
            /* 0c */  Instruction("tsb", AddrMode.Abs, 6),
            /* 0d */  Instruction("ora", AddrMode.Abs, 4),
            /* 0e */  Instruction("asl", AddrMode.Abs, 6),
            /* 0f */  Instruction("bbr0", AddrMode.Zpr, 5),
            /* 10 */  Instruction("bpl", AddrMode.Rel, 2),
            /* 11 */  Instruction("ora", AddrMode.IzY, 5),
            /* 12 */  Instruction("ora", AddrMode.Izp, 5),
            /* 13 */  Instruction("nop", AddrMode.Imp, 1),
            /* 14 */  Instruction("trb", AddrMode.Zp, 5),
            /* 15 */  Instruction("ora", AddrMode.ZpX, 4),
            /* 16 */  Instruction("asl", AddrMode.ZpX, 6),
            /* 17 */  Instruction("rmb1", AddrMode.Zp, 5),
            /* 18 */  Instruction("clc", AddrMode.Imp, 2),
            /* 19 */  Instruction("ora", AddrMode.AbsY, 4),
            /* 1a */  Instruction("inc", AddrMode.Acc, 2),
            /* 1b */  Instruction("nop", AddrMode.Imp, 1),
            /* 1c */  Instruction("trb", AddrMode.Abs, 6),
            /* 1d */  Instruction("ora", AddrMode.AbsX, 4),
            /* 1e */  Instruction("asl", AddrMode.AbsX, 6),
            /* 1f */  Instruction("bbr1", AddrMode.Zpr, 5),
            /* 20 */  Instruction("jsr", AddrMode.Abs, 6),
            /* 21 */  Instruction("and", AddrMode.IzX, 6),
            /* 22 */  Instruction("nop", AddrMode.Imm, 2),
            /* 23 */  Instruction("nop", AddrMode.Imp, 1),
            /* 24 */  Instruction("bit", AddrMode.Zp, 3),
            /* 25 */  Instruction("and", AddrMode.Zp, 3),
            /* 26 */  Instruction("rol", AddrMode.Zp, 5),
            /* 27 */  Instruction("rmb2", AddrMode.Zp, 5),
            /* 28 */  Instruction("plp", AddrMode.Imp, 4),
            /* 29 */  Instruction("and", AddrMode.Imm, 2),
            /* 2a */  Instruction("rol", AddrMode.Acc, 2),
            /* 2b */  Instruction("nop", AddrMode.Imp, 1),
            /* 2c */  Instruction("bit", AddrMode.Abs, 4),
            /* 2d */  Instruction("and", AddrMode.Abs, 4),
            /* 2e */  Instruction("rol", AddrMode.Abs, 6),
            /* 2f */  Instruction("bbr2", AddrMode.Zpr, 5),
            /* 30 */  Instruction("bmi", AddrMode.Rel, 2),
            /* 31 */  Instruction("and", AddrMode.IzY, 5),
            /* 32 */  Instruction("and", AddrMode.Izp, 5),
            /* 33 */  Instruction("nop", AddrMode.Imp, 1),
            /* 34 */  Instruction("bit", AddrMode.ZpX, 4),
            /* 35 */  Instruction("and", AddrMode.ZpX, 4),
            /* 36 */  Instruction("rol", AddrMode.ZpX, 6),
            /* 37 */  Instruction("rmb3", AddrMode.Zp, 5),
            /* 38 */  Instruction("sec", AddrMode.Imp, 2),
            /* 39 */  Instruction("and", AddrMode.AbsY, 4),
            /* 3a */  Instruction("dec", AddrMode.Acc, 2),
            /* 3b */  Instruction("nop", AddrMode.Imp, 1),
            /* 3c */  Instruction("bit", AddrMode.AbsX, 4),
            /* 3d */  Instruction("and", AddrMode.AbsX, 4),
            /* 3e */  Instruction("rol", AddrMode.AbsX, 6),
            /* 3f */  Instruction("bbr3", AddrMode.Zpr, 5),
            /* 40 */  Instruction("rti", AddrMode.Imp, 6),
            /* 41 */  Instruction("eor", AddrMode.IzX, 6),
            /* 42 */  Instruction("nop", AddrMode.Imm, 2),
            /* 43 */  Instruction("nop", AddrMode.Imp, 1),
            /* 44 */  Instruction("nop", AddrMode.Zp, 3),
            /* 45 */  Instruction("eor", AddrMode.Zp, 3),
            /* 46 */  Instruction("lsr", AddrMode.Zp, 5),
            /* 47 */  Instruction("rmb4", AddrMode.Zp, 5),
            /* 48 */  Instruction("pha", AddrMode.Imp, 3),
            /* 49 */  Instruction("eor", AddrMode.Imm, 2),
            /* 4a */  Instruction("lsr", AddrMode.Acc, 2),
            /* 4b */  Instruction("nop", AddrMode.Imp, 1),
            /* 4c */  Instruction("jmp", AddrMode.Abs, 3),
            /* 4d */  Instruction("eor", AddrMode.Abs, 4),
            /* 4e */  Instruction("lsr", AddrMode.Abs, 6),
            /* 4f */  Instruction("bbr4", AddrMode.Zpr, 5),
            /* 50 */  Instruction("bvc", AddrMode.Rel, 2),
            /* 51 */  Instruction("eor", AddrMode.IzY, 5),
            /* 52 */  Instruction("eor", AddrMode.Izp, 5),
            /* 53 */  Instruction("nop", AddrMode.Imp, 1),
            /* 54 */  Instruction("nop", AddrMode.ZpX, 4),
            /* 55 */  Instruction("eor", AddrMode.ZpX, 4),
            /* 56 */  Instruction("lsr", AddrMode.ZpX, 6),
            /* 57 */  Instruction("rmb5", AddrMode.Zp, 5),
            /* 58 */  Instruction("cli", AddrMode.Imp, 2),
            /* 59 */  Instruction("eor", AddrMode.AbsY, 4),
            /* 5a */  Instruction("phy", AddrMode.Imp, 3),
            /* 5b */  Instruction("nop", AddrMode.Imp, 1),
            /* 5c */  Instruction("nop", AddrMode.Abs, 8),
            /* 5d */  Instruction("eor", AddrMode.AbsX, 4),
            /* 5e */  Instruction("lsr", AddrMode.AbsX, 6),
            /* 5f */  Instruction("bbr5", AddrMode.Zpr, 5),
            /* 60 */  Instruction("rts", AddrMode.Imp, 6),
            /* 61 */  Instruction("adc", AddrMode.IzX, 6),
            /* 62 */  Instruction("nop", AddrMode.Imm, 2),
            /* 63 */  Instruction("nop", AddrMode.Imp, 1),
            /* 64 */  Instruction("stz", AddrMode.Zp, 3),
            /* 65 */  Instruction("adc", AddrMode.Zp, 3),
            /* 66 */  Instruction("ror", AddrMode.Zp, 5),
            /* 67 */  Instruction("rmb6", AddrMode.Zp, 5),
            /* 68 */  Instruction("pla", AddrMode.Imp, 4),
            /* 69 */  Instruction("adc", AddrMode.Imm, 2),
            /* 6a */  Instruction("ror", AddrMode.Acc, 2),
            /* 6b */  Instruction("nop", AddrMode.Imp, 1),
            /* 6c */  Instruction("jmp", AddrMode.Ind, 6),
            /* 6d */  Instruction("adc", AddrMode.Abs, 4),
            /* 6e */  Instruction("ror", AddrMode.Abs, 6),
            /* 6f */  Instruction("bbr6", AddrMode.Zpr, 5),
            /* 70 */  Instruction("bvs", AddrMode.Rel, 2),
            /* 71 */  Instruction("adc", AddrMode.IzY, 5),
            /* 72 */  Instruction("adc", AddrMode.Izp, 5),
            /* 73 */  Instruction("nop", AddrMode.Imp, 1),
            /* 74 */  Instruction("stz", AddrMode.ZpX, 4),
            /* 75 */  Instruction("adc", AddrMode.ZpX, 4),
            /* 76 */  Instruction("ror", AddrMode.ZpX, 6),
            /* 77 */  Instruction("rmb7", AddrMode.Zp, 5),
            /* 78 */  Instruction("sei", AddrMode.Imp, 2),
            /* 79 */  Instruction("adc", AddrMode.AbsY, 4),
            /* 7a */  Instruction("ply", AddrMode.Imp, 4),
            /* 7b */  Instruction("nop", AddrMode.Imp, 1),
            /* 7c */  Instruction("jmp", AddrMode.IaX, 6),
            /* 7d */  Instruction("adc", AddrMode.AbsX, 4),
            /* 7e */  Instruction("ror", AddrMode.AbsX, 6),
            /* 7f */  Instruction("bbr7", AddrMode.Zpr, 5),
            /* 80 */  Instruction("bra", AddrMode.Rel, 3),
            /* 81 */  Instruction("sta", AddrMode.IzX, 6),
            /* 82 */  Instruction("nop", AddrMode.Imm, 2),
            /* 83 */  Instruction("nop", AddrMode.Imp, 1),
            /* 84 */  Instruction("sty", AddrMode.Zp, 3),
            /* 85 */  Instruction("sta", AddrMode.Zp, 3),
            /* 86 */  Instruction("stx", AddrMode.Zp, 3),
            /* 87 */  Instruction("smb0", AddrMode.Zp, 5),
            /* 88 */  Instruction("dey", AddrMode.Imp, 2),
            /* 89 */  Instruction("bit", AddrMode.Imm, 2),
            /* 8a */  Instruction("txa", AddrMode.Imp, 2),
            /* 8b */  Instruction("nop", AddrMode.Imp, 1),
            /* 8c */  Instruction("sty", AddrMode.Abs, 4),
            /* 8d */  Instruction("sta", AddrMode.Abs, 4),
            /* 8e */  Instruction("stx", AddrMode.Abs, 4),
            /* 8f */  Instruction("bbs0", AddrMode.Zpr, 5),
            /* 90 */  Instruction("bcc", AddrMode.Rel, 2),
            /* 91 */  Instruction("sta", AddrMode.IzY, 6),
            /* 92 */  Instruction("sta", AddrMode.Izp, 5),
            /* 93 */  Instruction("nop", AddrMode.Imp, 1),
            /* 94 */  Instruction("sty", AddrMode.ZpX, 4),
            /* 95 */  Instruction("sta", AddrMode.ZpX, 4),
            /* 96 */  Instruction("stx", AddrMode.ZpY, 4),
            /* 97 */  Instruction("smb1", AddrMode.Zp, 5),
            /* 98 */  Instruction("tya", AddrMode.Imp, 2),
            /* 99 */  Instruction("sta", AddrMode.AbsY, 5),
            /* 9a */  Instruction("txs", AddrMode.Imp, 2),
            /* 9b */  Instruction("nop", AddrMode.Imp, 1),
            /* 9c */  Instruction("stz", AddrMode.Abs, 4),
            /* 9d */  Instruction("sta", AddrMode.AbsX, 5),
            /* 9e */  Instruction("stz", AddrMode.AbsX, 5),
            /* 9f */  Instruction("bbs1", AddrMode.Zpr, 5),
            /* a0 */  Instruction("ldy", AddrMode.Imm, 2),
            /* a1 */  Instruction("lda", AddrMode.IzX, 6),
            /* a2 */  Instruction("ldx", AddrMode.Imm, 2),
            /* a3 */  Instruction("nop", AddrMode.Imp, 1),
            /* a4 */  Instruction("ldy", AddrMode.Zp, 3),
            /* a5 */  Instruction("lda", AddrMode.Zp, 3),
            /* a6 */  Instruction("ldx", AddrMode.Zp, 3),
            /* a7 */  Instruction("smb2", AddrMode.Zp, 5),
            /* a8 */  Instruction("tay", AddrMode.Imp, 2),
            /* a9 */  Instruction("lda", AddrMode.Imm, 2),
            /* aa */  Instruction("tax", AddrMode.Imp, 2),
            /* ab */  Instruction("nop", AddrMode.Imp, 1),
            /* ac */  Instruction("ldy", AddrMode.Abs, 4),
            /* ad */  Instruction("lda", AddrMode.Abs, 4),
            /* ae */  Instruction("ldx", AddrMode.Abs, 4),
            /* af */  Instruction("bbs2", AddrMode.Zpr, 5),
            /* b0 */  Instruction("bcs", AddrMode.Rel, 2),
            /* b1 */  Instruction("lda", AddrMode.IzY, 5),
            /* b2 */  Instruction("lda", AddrMode.Izp, 5),
            /* b3 */  Instruction("nop", AddrMode.Imp, 1),
            /* b4 */  Instruction("ldy", AddrMode.ZpX, 4),
            /* b5 */  Instruction("lda", AddrMode.ZpX, 4),
            /* b6 */  Instruction("ldx", AddrMode.ZpY, 4),
            /* b7 */  Instruction("smb3", AddrMode.Zp, 5),
            /* b8 */  Instruction("clv", AddrMode.Imp, 2),
            /* b9 */  Instruction("lda", AddrMode.AbsY, 4),
            /* ba */  Instruction("tsx", AddrMode.Imp, 2),
            /* bb */  Instruction("nop", AddrMode.Imp, 1),
            /* bc */  Instruction("ldy", AddrMode.AbsX, 4),
            /* bd */  Instruction("lda", AddrMode.AbsX, 4),
            /* be */  Instruction("ldx", AddrMode.AbsY, 4),
            /* bf */  Instruction("bbs3", AddrMode.Zpr, 5),
            /* c0 */  Instruction("cpy", AddrMode.Imm, 2),
            /* c1 */  Instruction("cmp", AddrMode.IzX, 6),
            /* c2 */  Instruction("nop", AddrMode.Imm, 2),
            /* c3 */  Instruction("nop", AddrMode.Imp, 1),
            /* c4 */  Instruction("cpy", AddrMode.Zp, 3),
            /* c5 */  Instruction("cmp", AddrMode.Zp, 3),
            /* c6 */  Instruction("dec", AddrMode.Zp, 5),
            /* c7 */  Instruction("smb4", AddrMode.Zp, 5),
            /* c8 */  Instruction("iny", AddrMode.Imp, 2),
            /* c9 */  Instruction("cmp", AddrMode.Imm, 2),
            /* ca */  Instruction("dex", AddrMode.Imp, 2),
            /* cb */  Instruction("wai", AddrMode.Imp, 3),
            /* cc */  Instruction("cpy", AddrMode.Abs, 4),
            /* cd */  Instruction("cmp", AddrMode.Abs, 4),
            /* ce */  Instruction("dec", AddrMode.Abs, 6),
            /* cf */  Instruction("bbs4", AddrMode.Zpr, 5),
            /* d0 */  Instruction("bne", AddrMode.Rel, 2),
            /* d1 */  Instruction("cmp", AddrMode.IzY, 5),
            /* d2 */  Instruction("cmp", AddrMode.Izp, 5),
            /* d3 */  Instruction("nop", AddrMode.Imp, 1),
            /* d4 */  Instruction("nop", AddrMode.ZpX, 4),
            /* d5 */  Instruction("cmp", AddrMode.ZpX, 4),
            /* d6 */  Instruction("dec", AddrMode.ZpX, 6),
            /* d7 */  Instruction("smb5", AddrMode.Zp, 5),
            /* d8 */  Instruction("cld", AddrMode.Imp, 2),
            /* d9 */  Instruction("cmp", AddrMode.AbsY, 4),
            /* da */  Instruction("phx", AddrMode.Imp, 3),
            /* db */  Instruction("stp", AddrMode.Imp, 3),
            /* dc */  Instruction("nop", AddrMode.Abs, 4),
            /* dd */  Instruction("cmp", AddrMode.AbsX, 4),
            /* de */  Instruction("dec", AddrMode.AbsX, 7),
            /* df */  Instruction("bbs5", AddrMode.Zpr, 5),
            /* e0 */  Instruction("cpx", AddrMode.Imm, 2),
            /* e1 */  Instruction("sbc", AddrMode.IzX, 6),
            /* e2 */  Instruction("nop", AddrMode.Imm, 2),
            /* e3 */  Instruction("nop", AddrMode.Imp, 1),
            /* e4 */  Instruction("cpx", AddrMode.Zp, 3),
            /* e5 */  Instruction("sbc", AddrMode.Zp, 3),
            /* e6 */  Instruction("inc", AddrMode.Zp, 5),
            /* e7 */  Instruction("smb6", AddrMode.Zp, 5),
            /* e8 */  Instruction("inx", AddrMode.Imp, 2),
            /* e9 */  Instruction("sbc", AddrMode.Imm, 2),
            /* ea */  Instruction("nop", AddrMode.Imp, 2),
            /* eb */  Instruction("nop", AddrMode.Imp, 1),
            /* ec */  Instruction("cpx", AddrMode.Abs, 4),
            /* ed */  Instruction("sbc", AddrMode.Abs, 4),
            /* ee */  Instruction("inc", AddrMode.Abs, 6),
            /* ef */  Instruction("bbs6", AddrMode.Zpr, 5),
            /* f0 */  Instruction("beq", AddrMode.Rel, 2),
            /* f1 */  Instruction("sbc", AddrMode.IzY, 5),
            /* f2 */  Instruction("sbc", AddrMode.Izp, 5),
            /* f3 */  Instruction("nop", AddrMode.Imp, 1),
            /* f4 */  Instruction("nop", AddrMode.ZpX, 4),
            /* f5 */  Instruction("sbc", AddrMode.ZpX, 4),
            /* f6 */  Instruction("inc", AddrMode.ZpX, 6),
            /* f7 */  Instruction("smb7", AddrMode.Zp, 5),
            /* f8 */  Instruction("sed", AddrMode.Imp, 2),
            /* f9 */  Instruction("sbc", AddrMode.AbsY, 4),
            /* fa */  Instruction("plx", AddrMode.Imp, 4),
            /* fb */  Instruction("nop", AddrMode.Imp, 1),
            /* fc */  Instruction("nop", AddrMode.AbsX, 4),
            /* fd */  Instruction("sbc", AddrMode.AbsX, 4),
            /* fe */  Instruction("inc", AddrMode.AbsX, 7),
            /* ff */  Instruction("bbs7", AddrMode.Zpr, 5)).toTypedArray()

    override fun iBrk(): Boolean {
        // handle BRK ('software interrupt')
        regPC++
        pushStackAddr(regPC)
        regP.B = true
        pushStack(regP)
        regP.I = true     // interrupts are now disabled
        regP.D = false    // this is different from NMOS 6502
        regPC = readWord(IRQ_VECTOR)

        // TODO prevent NMI from triggering immediately after IRQ/BRK... how does that work exactly?
        return false
    }

    override fun handleInterrupt() {
        super.handleInterrupt()
        regP.D = false    // this is different from NMOS 6502
    }

    override fun iBit(): Boolean {
        val data = getFetched()
        regP.Z = (regA and data) == 0
        if (currentInstruction.mode != AddrMode.Imm) {
            regP.V = (data and 0b01000000) != 0
            regP.N = (data and 0b10000000) != 0
        }
        return false
    }

    override fun iAdc(): Boolean {
        val value = getFetched()
        if (regP.D) {
            // BCD add
            // see http://www.6502.org/tutorials/decimal_mode.html
            // and https://sourceforge.net/p/vice-emu/code/HEAD/tree/trunk/vice/src/65c02core.c#l542
            // (the implementation below is based on the code used by Vice)
            var tmp = (regA and 0x0f)+(value and 0x0f)+if (regP.C) 1 else 0
            var tmp2 = (regA and 0xf0)+(value and 0xf0)
            if (tmp > 9) {
                tmp2 += 0x10
                tmp += 6
            }
            regP.V = (regA xor value).inv() and (regA xor tmp2) and 0b10000000 != 0
            if (tmp2 > 0x90) tmp2 += 0x60
            regP.C = tmp2 >= 0x100
            tmp = (tmp and 0x0f)+(tmp2 and 0xf0)
            regP.N = (tmp and 0b10000000) != 0
            regP.Z = tmp == 0
            regA = tmp and 0xff
        } else {
            // normal add (identical to 6502)
            val tmp = value+regA+if (regP.C) 1 else 0
            regP.N = (tmp and 0b10000000) != 0
            regP.Z = (tmp and 0xff) == 0
            regP.V = (regA xor value).inv() and (regA xor tmp) and 0b10000000 != 0
            regP.C = tmp > 0xff
            regA = tmp and 0xff
        }
        return false
    }

    override fun iSbc2(value: Int): Boolean {
        // see http://www.6502.org/tutorials/decimal_mode.html
        // and https://sourceforge.net/p/vice-emu/code/HEAD/tree/trunk/vice/src/65c02core.c#l1205
        // (the implementation below is based on the code used by Vice)
        var tmp = (regA-value-if (regP.C) 0 else 1) and 0xffff
        regP.V = (regA xor tmp) and (regA xor value) and 0b10000000 != 0
        if (regP.D) {
            if (tmp > 0xff) tmp = (tmp-0x60) and 0xffff
            val tmp2 = ((regA and 0x0f)-(value and 0x0f)-if (regP.C) 0 else 1) and 0xffff
            if (tmp2 > 0xff) tmp -= 6
        }
        regP.C = (regA-if (regP.C) 0 else 1) >= value
        regP.Z = (tmp and 0xff) == 0
        regP.N = (tmp and 0b10000000) != 0
        regA = tmp and 0xff
        return false
    }

    override fun iDec(): Boolean {
        return if (currentInstruction.mode == AddrMode.Acc) {
            regA = (regA-1) and 0xff
            regP.Z = regA == 0
            regP.N = (regA and 0b10000000) != 0
            false
        } else super.iDec()
    }

    override fun iInc(): Boolean {
        return if (currentInstruction.mode == AddrMode.Acc) {
            regA = (regA+1) and 0xff
            regP.Z = regA == 0
            regP.N = (regA and 0b10000000) != 0
            false
        } else super.iInc()
    }

    private fun iBra(): Boolean {
        // unconditional branch
        regPC = fetchedAddress
        return false
    }

    private fun iTrb(): Boolean {
        val data = getFetched()
        regP.Z = data and regA == 0
        write(fetchedAddress, data and regA.inv())
        return false
    }

    private fun iTsb(): Boolean {
        val data = getFetched()
        regP.Z = data and regA == 0
        write(fetchedAddress, data or regA)
        return false
    }

    private fun iStz(): Boolean {
        write(fetchedAddress, 0)
        return false
    }

    private fun iWai(): Boolean {
        waiting = Wait.Waiting
        return false
    }

    private fun iStp(): Boolean {
        waiting = Wait.Stopped
        return false
    }

    private fun iPhx(): Boolean {
        pushStack(regX)
        return false
    }

    private fun iPlx(): Boolean {
        regX = popStack()
        regP.Z = regX == 0
        regP.N = (regX and 0b10000000) != 0
        return false
    }

    private fun iPhy(): Boolean {
        pushStack(regY)
        return false
    }

    private fun iPly(): Boolean {
        regY = popStack()
        regP.Z = regY == 0
        regP.N = (regY and 0b10000000) != 0
        return false
    }

    private fun iBbr0(): Boolean {
        val data = getFetched()
        if (data and 1 == 0) {
            regPC = fetchedAddressZpr
            instrCycles++
        }
        return false
    }

    private fun iBbr1(): Boolean {
        val data = getFetched()
        if (data and 2 == 0) {
            regPC = fetchedAddressZpr
            instrCycles++
        }
        return false
    }

    private fun iBbr2(): Boolean {
        val data = getFetched()
        if (data and 4 == 0) {
            regPC = fetchedAddressZpr
            instrCycles++
        }
        return false
    }

    private fun iBbr3(): Boolean {
        val data = getFetched()
        if (data and 8 == 0) {
            regPC = fetchedAddressZpr
            instrCycles++
        }
        return false
    }

    private fun iBbr4(): Boolean {
        val data = getFetched()
        if (data and 16 == 0) {
            regPC = fetchedAddressZpr
            instrCycles++
        }
        return false
    }

    private fun iBbr5(): Boolean {
        val data = getFetched()
        if (data and 32 == 0) {
            regPC = fetchedAddressZpr
            instrCycles++
        }
        return false
    }

    private fun iBbr6(): Boolean {
        val data = getFetched()
        if (data and 64 == 0) {
            regPC = fetchedAddressZpr
            instrCycles++
        }
        return false
    }

    private fun iBbr7(): Boolean {
        val data = getFetched()
        if (data and 128 == 0) {
            regPC = fetchedAddressZpr
            instrCycles++
        }
        return false
    }

    private fun iBbs0(): Boolean {
        val data = getFetched()
        if (data and 1 != 0) {
            regPC = fetchedAddressZpr
            instrCycles++
        }
        return false
    }

    private fun iBbs1(): Boolean {
        val data = getFetched()
        if (data and 2 != 0) {
            regPC = fetchedAddressZpr
            instrCycles++
        }
        return false
    }

    private fun iBbs2(): Boolean {
        val data = getFetched()
        if (data and 4 != 0) {
            regPC = fetchedAddressZpr
            instrCycles++
        }
        return false
    }

    private fun iBbs3(): Boolean {
        val data = getFetched()
        if (data and 8 != 0) {
            regPC = fetchedAddressZpr
            instrCycles++
        }
        return false
    }

    private fun iBbs4(): Boolean {
        val data = getFetched()
        if (data and 16 != 0) {
            regPC = fetchedAddressZpr
            instrCycles++
        }
        return false
    }

    private fun iBbs5(): Boolean {
        val data = getFetched()
        if (data and 32 != 0) {
            regPC = fetchedAddressZpr
            instrCycles++
        }
        return false
    }

    private fun iBbs6(): Boolean {
        val data = getFetched()
        if (data and 64 != 0) {
            regPC = fetchedAddressZpr
            instrCycles++
        }
        return false
    }

    private fun iBbs7(): Boolean {
        val data = getFetched()
        if (data and 128 != 0) {
            regPC = fetchedAddressZpr
            instrCycles++
        }
        return false
    }

    private fun iSmb0(): Boolean {
        val data = getFetched()
        write(fetchedAddress, data or 1)
        return false
    }

    private fun iSmb1(): Boolean {
        val data = getFetched()
        write(fetchedAddress, data or 2)
        return false
    }

    private fun iSmb2(): Boolean {
        val data = getFetched()
        write(fetchedAddress, data or 4)
        return false
    }

    private fun iSmb3(): Boolean {
        val data = getFetched()
        write(fetchedAddress, data or 8)
        return false
    }

    private fun iSmb4(): Boolean {
        val data = getFetched()
        write(fetchedAddress, data or 16)
        return false
    }

    private fun iSmb5(): Boolean {
        val data = getFetched()
        write(fetchedAddress, data or 32)
        return false
    }

    private fun iSmb6(): Boolean {
        val data = getFetched()
        write(fetchedAddress, data or 64)
        return false
    }

    private fun iSmb7(): Boolean {
        val data = getFetched()
        write(fetchedAddress, data or 128)
        return false
    }

    private fun iRmb0(): Boolean {
        val data = getFetched()
        write(fetchedAddress, data and 0b11111110)
        return false
    }

    private fun iRmb1(): Boolean {
        val data = getFetched()
        write(fetchedAddress, data and 0b11111101)
        return false
    }

    private fun iRmb2(): Boolean {
        val data = getFetched()
        write(fetchedAddress, data and 0b11111011)
        return false
    }

    private fun iRmb3(): Boolean {
        val data = getFetched()
        write(fetchedAddress, data and 0b11110111)
        return false
    }

    private fun iRmb4(): Boolean {
        val data = getFetched()
        write(fetchedAddress, data and 0b11101111)
        return false
    }

    private fun iRmb5(): Boolean {
        val data = getFetched()
        write(fetchedAddress, data and 0b11011111)
        return false
    }

    private fun iRmb6(): Boolean {
        val data = getFetched()
        write(fetchedAddress, data and 0b10111111)
        return false
    }

    private fun iRmb7(): Boolean {
        val data = getFetched()
        write(fetchedAddress, data and 0b01111111)
        return false
    }
}
