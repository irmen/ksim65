import razorvine.ksim65.Cpu65C02
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

/*

Unit test suite adapted from Py65   https://github.com/mnaberez/py65

Copyright (c) 2008-2018, Mike Naberezny and contributors.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the copyright holder nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */


@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class Test65C02 : TestCommon6502() {
    //  CMOS 65C02 Tests
    override fun createCpu() = Cpu65C02()

    // Reset

    @Test
    fun test_reset_clears_decimal_flag() {
        // W65C02S Datasheet, Apr 14 2009, Table 7-1 Operational Enhancements
        // NMOS 6502 decimal flag = undetermined after reset, CMOS 65C02 = 0
        mpu.regP.D = true
        mpu.reset()
        assertFalse(mpu.regP.D)
    }

    // ADC Zero Page, Indirect

    @Test
    fun test_adc_bcd_off_zp_ind_carry_clear_in_accumulator_zeroes() {
        mpu.regA = 0x00
        // $0000 ADC ($0010)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0x72, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0x00
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0x00, mpu.regA)
        assertFalse(mpu.regP.C)
        assertFalse(mpu.regP.N)
        assertTrue(mpu.regP.Z)
    }

    @Test
    fun test_adc_bcd_off_zp_ind_carry_set_in_accumulator_zero() {
        mpu.regA = 0
        mpu.regP.C = true
        // $0000 ADC ($0010)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0x72, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0x00
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0x01, mpu.regA)
        assertFalse(mpu.regP.N)
        assertFalse(mpu.regP.Z)
        assertFalse(mpu.regP.C)
    }

    @Test
    fun test_adc_bcd_off_zp_ind_carry_clear_in_no_carry_clear_out() {
        mpu.regA = 0x01
        // $0000 ADC ($0010)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0x72, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0xFE
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0xFF, mpu.regA)
        assertTrue(mpu.regP.N)
        assertFalse(mpu.regP.C)
        assertFalse(mpu.regP.Z)
    }

    @Test
    fun test_adc_bcd_off_zp_ind_carry_clear_in_carry_set_out() {
        mpu.regA = 0x02
        // $0000 ADC ($0010)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0x72, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0xFF
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0x01, mpu.regA)
        assertTrue(mpu.regP.C)
        assertFalse(mpu.regP.N)
        assertFalse(mpu.regP.Z)
    }

    @Test
    fun test_adc_bcd_off_zp_ind_overflow_cleared_no_carry_01_plus_01() {
        mpu.regP.C = false
        mpu.regA = 0x01
        // $0000 ADC ($0010)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0x72, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0x01
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(0x02, mpu.regA)
        assertFalse(mpu.regP.V)
    }

    @Test
    fun test_adc_bcd_off_zp_ind_overflow_cleared_no_carry_01_plus_ff() {
        mpu.regP.C = false
        mpu.regA = 0x01
        // $0000 ADC ($0010)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0x72, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0xFF
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(0x00, mpu.regA)
        assertFalse(mpu.regP.V)
    }

    @Test
    fun test_adc_bcd_off_zp_ind_overflow_set_no_carry_7f_plus_01() {
        mpu.regP.C = false
        mpu.regA = 0x7f
        // $0000 ADC ($0010)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0x72, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0x01
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(0x80, mpu.regA)
        assertTrue(mpu.regP.V)
    }

    @Test
    fun test_adc_bcd_off_zp_ind_overflow_set_no_carry_80_plus_ff() {
        mpu.regP.C = false
        mpu.regA = 0x80
        // $0000 ADC ($0010)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0x72, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0xFF
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(0x7f, mpu.regA)
        assertTrue(mpu.regP.V)
    }

    @Test
    fun test_adc_bcd_off_zp_ind_overflow_set_on_40_plus_40() {
        mpu.regA = 0x40
        // $0000 ADC ($0010)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0x72, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0x40
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(0x80, mpu.regA)
        assertTrue(mpu.regP.N)
        assertTrue(mpu.regP.V)
        assertFalse(mpu.regP.Z)
    }

    // AND Zero Page, Indirect

    @Test
    fun test_and_zp_ind_all_zeros_setting_zero_flag() {
        mpu.regA = 0xFF
        // $0000 AND ($0010)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0x32, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0x00
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0x00, mpu.regA)
        assertTrue(mpu.regP.Z)
        assertFalse(mpu.regP.N)
    }

    @Test
    fun test_and_zp_ind_zeros_and_ones_setting_negative_flag() {
        mpu.regA = 0xFF
        // $0000 AND ($0010)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0x32, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0xAA
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0xAA, mpu.regA)
        assertTrue(mpu.regP.N)
        assertFalse(mpu.regP.Z)
    }

    // BIT (Absolute, X-Indexed)

    @Test
    fun test_bit_abs_x_copies_bit_7_of_memory_to_n_flag_when_0() {
        mpu.regP.N = false
        mpu.regX = 0x02
        // $0000 BIT $FEEB,X
        writeMem(memory, 0x0000, listOf(0x3C, 0xEB, 0xFE))
        memory[0xFEED] = 0xFF
        mpu.regA = 0xFF
        mpu.step()
        assertTrue(mpu.regP.N)
        assertEquals(12, mpu.totalCycles)
        assertEquals(0x0003, mpu.regPC)
    }

    @Test
    fun test_bit_abs_x_copies_bit_7_of_memory_to_n_flag_when_1() {
        mpu.regP.N = true
        mpu.regX = 0x02
        // $0000 BIT $FEEB,X
        writeMem(memory, 0x0000, listOf(0x3C, 0xEB, 0xFE))
        memory[0xFEED] = 0x00
        mpu.regA = 0xFF
        mpu.step()
        assertFalse(mpu.regP.N)
        assertEquals(12, mpu.totalCycles)
        assertEquals(0x0003, mpu.regPC)
    }

    @Test
    fun test_bit_abs_x_copies_bit_6_of_memory_to_v_flag_when_0() {
        mpu.regP.V = false
        mpu.regX = 0x02
        // $0000 BIT $FEEB,X
        writeMem(memory, 0x0000, listOf(0x3C, 0xEB, 0xFE))
        memory[0xFEED] = 0xFF
        mpu.regA = 0xFF
        mpu.step()
        assertTrue(mpu.regP.V)
        assertEquals(12, mpu.totalCycles)
        assertEquals(0x0003, mpu.regPC)
    }

    @Test
    fun test_bit_abs_x_copies_bit_6_of_memory_to_v_flag_when_1() {
        mpu.regP.V = true
        mpu.regX = 0x02
        // $0000 BIT $FEEB,X
        writeMem(memory, 0x0000, listOf(0x3C, 0xEB, 0xFE))
        memory[0xFEED] = 0x00
        mpu.regA = 0xFF
        mpu.step()
        assertFalse(mpu.regP.V)
        assertEquals(12, mpu.totalCycles)
        assertEquals(0x0003, mpu.regPC)
    }

    @Test
    fun test_bit_abs_x_stores_result_of_and_in_z_preserves_a_when_1() {
        mpu.regP.Z = false
        mpu.regX = 0x02
        // $0000 BIT $FEEB,X
        writeMem(memory, 0x0000, listOf(0x3C, 0xEB, 0xFE))
        memory[0xFEED] = 0x00
        mpu.regA = 0x01
        mpu.step()
        assertTrue(mpu.regP.Z)
        assertEquals(0x01, mpu.regA)
        assertEquals(0x00, memory[0xFEED])
        assertEquals(12, mpu.totalCycles)
        assertEquals(0x0003, mpu.regPC)
    }

    @Test
    fun test_bit_abs_x_stores_result_of_and_nonzero_in_z_preserves_a() {
        mpu.regP.Z = true
        mpu.regX = 0x02
        // $0000 BIT $FEEB,X
        writeMem(memory, 0x0000, listOf(0x3C, 0xEB, 0xFE))
        memory[0xFEED] = 0x01
        mpu.regA = 0x01
        mpu.step()
        assertFalse(mpu.regP.Z) // result of AND is non-zero
        assertEquals(0x01, mpu.regA)
        assertEquals(0x01, memory[0xFEED])
        assertEquals(12, mpu.totalCycles)
        assertEquals(0x0003, mpu.regPC)
    }

    @Test
    fun test_bit_abs_x_stores_result_of_and_when_zero_in_z_preserves_a() {
        mpu.regP.Z = false
        mpu.regX = 0x02
        // $0000 BIT $FEEB,X
        writeMem(memory, 0x0000, listOf(0x3C, 0xEB, 0xFE))
        memory[0xFEED] = 0x00
        mpu.regA = 0x01
        mpu.step()
        assertTrue(mpu.regP.Z) // result of AND is zero
        assertEquals(0x01, mpu.regA)
        assertEquals(0x00, memory[0xFEED])
        assertEquals(12, mpu.totalCycles)
        assertEquals(0x0003, mpu.regPC)
    }

    // BIT (Immediate)

    @Test
    fun test_bit_imm_does_not_affect_n_and_z_flags() {
        mpu.regP.N = true
        mpu.regP.V = true
        // $0000 BIT #$FF
        writeMem(memory, 0x0000, listOf(0x89, 0xff))
        mpu.regA = 0x00
        mpu.step()
        assertTrue(mpu.regP.N)
        assertTrue(mpu.regP.V)
        assertEquals(0x00, mpu.regA)
        assertEquals(10, mpu.totalCycles)
        assertEquals(0x02, mpu.regPC)
    }

    @Test
    fun test_bit_imm_stores_result_of_and_in_z_preserves_a_when_1() {
        mpu.regP.Z = false
        // $0000 BIT #$00
        writeMem(memory, 0x0000, listOf(0x89, 0x00))
        mpu.regA = 0x01
        mpu.step()
        assertTrue(mpu.regP.Z)
        assertEquals(0x01, mpu.regA)
        assertEquals(10, mpu.totalCycles)
        assertEquals(0x02, mpu.regPC)
    }

    @Test
    fun test_bit_imm_stores_result_of_and_when_nonzero_in_z_preserves_a() {
        mpu.regP.Z = true
        // $0000 BIT #$01
        writeMem(memory, 0x0000, listOf(0x89, 0x01))
        mpu.regA = 0x01
        mpu.step()
        assertFalse(mpu.regP.Z) // result of AND is non-zero
        assertEquals(0x01, mpu.regA)
        assertEquals(10, mpu.totalCycles)
        assertEquals(0x02, mpu.regPC)
    }

    @Test
    fun test_bit_imm_stores_result_of_and_when_zero_in_z_preserves_a() {
        mpu.regP.Z = false
        // $0000 BIT #$00
        writeMem(memory, 0x0000, listOf(0x89, 0x00))
        mpu.regA = 0x01
        mpu.step()
        assertTrue(mpu.regP.Z) // result of AND is zero
        assertEquals(0x01, mpu.regA)
        assertEquals(10, mpu.totalCycles)
        assertEquals(0x02, mpu.regPC)
    }

    // BIT (Zero Page, X-Indexed)

    @Test
    fun test_bit_zp_x_copies_bit_7_of_memory_to_n_flag_when_0() {
        mpu.regP.N = false
        // $0000 BIT $0010,X
        writeMem(memory, 0x0000, listOf(0x34, 0x10))
        memory[0x0013] = 0xFF
        mpu.regX = 0x03
        mpu.regA = 0xFF
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(12, mpu.totalCycles)
        assertTrue(mpu.regP.N)
    }

    @Test
    fun test_bit_zp_x_copies_bit_7_of_memory_to_n_flag_when_1() {
        mpu.regP.N = true
        // $0000 BIT $0010,X
        writeMem(memory, 0x0000, listOf(0x34, 0x10))
        memory[0x0013] = 0x00
        mpu.regX = 0x03
        mpu.regA = 0xFF
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(12, mpu.totalCycles)
        assertFalse(mpu.regP.N)
    }

    @Test
    fun test_bit_zp_x_copies_bit_6_of_memory_to_v_flag_when_0() {
        mpu.regP.V = false
        // $0000 BIT $0010,X
        writeMem(memory, 0x0000, listOf(0x34, 0x10))
        memory[0x0013] = 0xFF
        mpu.regX = 0x03
        mpu.regA = 0xFF
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(12, mpu.totalCycles)
        assertTrue(mpu.regP.V)
    }

    @Test
    fun test_bit_zp_x_copies_bit_6_of_memory_to_v_flag_when_1() {
        mpu.regP.V = true
        // $0000 BIT $0010,X
        writeMem(memory, 0x0000, listOf(0x34, 0x10))
        memory[0x0013] = 0x00
        mpu.regX = 0x03
        mpu.regA = 0xFF
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(12, mpu.totalCycles)
        assertFalse(mpu.regP.V)
    }

    @Test
    fun test_bit_zp_x_stores_result_of_and_in_z_preserves_a_when_1() {
        mpu.regP.Z = false
        // $0000 BIT $0010,X
        writeMem(memory, 0x0000, listOf(0x34, 0x10))
        memory[0x0013] = 0x00
        mpu.regX = 0x03
        mpu.regA = 0x01
        mpu.step()
        assertTrue(mpu.regP.Z)
        assertEquals(0x0002, mpu.regPC)
        assertEquals(12, mpu.totalCycles)
        assertEquals(0x01, mpu.regA)
        assertEquals(0x00, memory[0x0010 + mpu.regX])
    }

    @Test
    fun test_bit_zp_x_stores_result_of_and_when_nonzero_in_z_preserves_a() {
        mpu.regP.Z = true
        // $0000 BIT $0010,X
        writeMem(memory, 0x0000, listOf(0x34, 0x10))
        memory[0x0013] = 0x01
        mpu.regX = 0x03
        mpu.regA = 0x01
        mpu.step()
        assertFalse(mpu.regP.Z) // result of AND is non-zero
        assertEquals(0x0002, mpu.regPC)
        assertEquals(12, mpu.totalCycles)
        assertEquals(0x01, mpu.regA)
        assertEquals(0x01, memory[0x0010 + mpu.regX])
    }

    @Test
    fun test_bit_zp_x_stores_result_of_and_when_zero_in_z_preserves_a() {
        mpu.regP.Z = false
        // $0000 BIT $0010,X
        writeMem(memory, 0x0000, listOf(0x34, 0x10))
        memory[0x0013] = 0x00
        mpu.regX = 0x03
        mpu.regA = 0x01
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(12, mpu.totalCycles)
        assertTrue(mpu.regP.Z) // result of AND is zero
        assertEquals(0x01, mpu.regA)
        assertEquals(0x00, memory[0x0010 + mpu.regX])
    }

    // BRK

    @Test
    fun test_brk_clears_decimal_flag() {
        mpu.regP.D = true
        // $C000 BRK
        memory[0xC000] = 0x00
        mpu.regPC = 0xC000
        mpu.step()
        assertTrue(mpu.regP.B)
        assertFalse(mpu.regP.D)
    }

    // CMP Zero Page, Indirect

    @Test
    fun test_cmp_zpi_sets_z_flag_if_equal() {
        mpu.regA = 0x42
        // $0000 AND ($10)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0xd2, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0x42
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0x42, mpu.regA)
        assertTrue(mpu.regP.Z)
        assertFalse(mpu.regP.N)
    }

    @Test
    fun test_cmp_zpi_resets_z_flag_if_unequal() {
        mpu.regA = 0x43
        // $0000 AND ($10)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0xd2, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0x42
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0x43, mpu.regA)
        assertFalse(mpu.regP.Z)
        assertFalse(mpu.regP.N)
    }

    // EOR Zero Page, Indirect

    @Test
    fun test_eor_zp_ind_flips_bits_over_setting_z_flag() {
        mpu.regA = 0xFF
        // $0000 EOR ($0010)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0x52, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0xFF
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0x00, mpu.regA)
        assertEquals(0xFF, memory[0xABCD])
        assertTrue(mpu.regP.Z)
    }

    @Test
    fun test_eor_zp_ind_flips_bits_over_setting_n_flag() {
        mpu.regA = 0x00
        // $0000 EOR ($0010)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0x52, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0xFF
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0xFF, mpu.regA)
        assertEquals(0xFF, memory[0xABCD])
        assertTrue(mpu.regP.N)
        assertFalse(mpu.regP.Z)
    }

    // INC Accumulator

    @Test
    fun test_inc_acc_increments_accum() {
        memory[0x0000] = 0x1A
        mpu.regA = 0x42
        mpu.step()
        assertEquals(0x0001, mpu.regPC)
        assertEquals(0x43, mpu.regA)
        assertFalse(mpu.regP.Z)
        assertFalse(mpu.regP.N)
    }

    @Test
    fun test_inc_acc_increments_accum_rolls_over_and_sets_zero_flag() {
        memory[0x0000] = 0x1A
        mpu.regA = 0xFF
        mpu.step()
        assertEquals(0x0001, mpu.regPC)
        assertEquals(0x00, mpu.regA)
        assertTrue(mpu.regP.Z)
        assertFalse(mpu.regP.N)
    }

    @Test
    fun test_inc_acc_sets_negative_flag_when_incrementing_above_7F() {
        memory[0x0000] = 0x1A
        mpu.regA = 0x7F
        mpu.step()
        assertEquals(0x0001, mpu.regPC)
        assertEquals(0x80, mpu.regA)
        assertFalse(mpu.regP.Z)
        assertTrue(mpu.regP.N)
    }

    // JMP Indirect

    @Test
    fun test_jmp_ind_does_not_have_page_wrap_bug() {
        writeMem(memory, 0x10FF, listOf(0xCD, 0xAB))
        // $0000 JMP ($10FF)
        writeMem(memory, 0, listOf(0x6c, 0xFF, 0x10))
        mpu.step()
        assertEquals(0xABCD, mpu.regPC)
        assertEquals(14, mpu.totalCycles)
    }

    // JMP Indirect Absolute X-Indexed

    @Test
    fun test_jmp_iax_jumps_to_address() {
        mpu.regX = 2
        // $0000 JMP ($ABCD,X)
        // $ABCF Vector to $1234
        writeMem(memory, 0x0000, listOf(0x7C, 0xCD, 0xAB))
        writeMem(memory, 0xABCF, listOf(0x34, 0x12))
        mpu.step()
        assertEquals(0x1234, mpu.regPC)
        assertEquals(14, mpu.totalCycles)
    }

    // LDA Zero Page, Indirect

    @Test
    fun test_lda_zp_ind_loads_a_sets_n_flag() {
        mpu.regA = 0x00
        // $0000 LDA ($0010)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0xB2, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0x80
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0x80, mpu.regA)
        assertTrue(mpu.regP.N)
        assertFalse(mpu.regP.Z)
    }

    @Test
    fun test_lda_zp_ind_loads_a_sets_z_flag() {
        mpu.regA = 0x00
        // $0000 LDA ($0010)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0xB2, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0x00
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0x00, mpu.regA)
        assertTrue(mpu.regP.Z)
        assertFalse(mpu.regP.N)
    }

    // ORA Zero Page, Indirect

    @Test
    fun test_ora_zp_ind_zeroes_or_zeros_sets_z_flag() {
        mpu.regP.Z = false
        mpu.regA = 0x00
        mpu.regY = 0x12  // These should not affect the ORA
        mpu.regX = 0x34
        // $0000 ORA ($0010)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0x12, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0x00
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0x00, mpu.regA)
        assertTrue(mpu.regP.Z)
    }

    @Test
    fun test_ora_zp_ind_turns_bits_on_sets_n_flag() {
        mpu.regP.N = false
        mpu.regA = 0x03
        // $0000 ORA ($0010)
        // $0010 Vector to $ABCD
        writeMem(memory, 0x0000, listOf(0x12, 0x10))
        writeMem(memory, 0x0010, listOf(0xCD, 0xAB))
        memory[0xABCD] = 0x82
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0x83, mpu.regA)
        assertTrue(mpu.regP.N)
        assertFalse(mpu.regP.Z)
    }

    // PHX

    @Test
    fun test_phx_pushes_x_and_updates_sp() {
        mpu.regX = 0xAB
        mpu.regSP = 0xff
        // $0000 PHX
        memory[0x0000] = 0xDA
        mpu.step()
        assertEquals(0x0001, mpu.regPC)
        assertEquals(0xAB, mpu.regX)
        assertEquals(0xAB, memory[0x01FF])
        assertEquals(0xFE, mpu.regSP)
        assertEquals(11, mpu.totalCycles)
    }

    // PHY

    @Test
    fun test_phy_pushes_y_and_updates_sp() {
        mpu.regY = 0xAB
        mpu.regSP = 0xff
        // $0000 PHY
        memory[0x0000] = 0x5A
        mpu.step()
        assertEquals(0x0001, mpu.regPC)
        assertEquals(0xAB, mpu.regY)
        assertEquals(0xAB, memory[0x01FF])
        assertEquals(0xFE, mpu.regSP)
        assertEquals(11, mpu.totalCycles)
    }

    // PLX

    @Test
    fun test_plx_pulls_top_byte_from_stack_into_x_and_updates_sp() {
        // $0000 PLX
        memory[0x0000] = 0xFA
        memory[0x01FF] = 0xAB
        mpu.regSP = 0xFE
        mpu.step()
        assertEquals(0x0001, mpu.regPC)
        assertEquals(0xAB, mpu.regX)
        assertEquals(0xFF, mpu.regSP)
        assertEquals(12, mpu.totalCycles)
    }

    // PLY

    @Test
    fun test_ply_pulls_top_byte_from_stack_into_y_and_updates_sp() {
        // $0000 PLY
        memory[0x0000] = 0x7A
        memory[0x01FF] = 0xAB
        mpu.regSP = 0xFE
        mpu.step()
        assertEquals(0x0001, mpu.regPC)
        assertEquals(0xAB, mpu.regY)
        assertEquals(0xFF, mpu.regSP)
        assertEquals(12, mpu.totalCycles)
    }

    // RMB0

    @Test
    fun test_rmb0_clears_bit_0_without_affecting_other_bits() {
        memory[0x0043] = 0b11111111
        // $0000 RMB0 $43
        writeMem(memory, 0x0000, listOf(0x07, 0x43))
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        val expected = 0b11111110
        assertEquals(expected, memory[0x0043].toInt())
    }

    @Test
    fun test_rmb0_does_not_affect_status_register() {
        memory[0x0043] = 0b11111111
        // $0000 RMB0 $43
        writeMem(memory, 0x0000, listOf(0x07, 0x43))
        val expected = 0b01110101
        mpu.regP.fromInt(expected)
        mpu.step()
        assertEquals(expected, mpu.regP.asInt())
    }

    // RMB1

    @Test
    fun test_rmb1_clears_bit_1_without_affecting_other_bits() {
        memory[0x0043] = 0b11111111
        // $0000 RMB1 $43
        writeMem(memory, 0x0000, listOf(0x17, 0x43))
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        val expected = 0b11111101
        assertEquals(expected, memory[0x0043].toInt())
    }

    @Test
    fun test_rmb1_does_not_affect_status_register() {
        memory[0x0043] = 0b11111111
        // $0000 RMB1 $43
        writeMem(memory, 0x0000, listOf(0x17, 0x43))
        val expected = 0b01110101
        mpu.regP.fromInt(expected)
        mpu.step()
        assertEquals(expected, mpu.regP.asInt())
    }

    // RMB2

    @Test
    fun test_rmb2_clears_bit_2_without_affecting_other_bits() {
        memory[0x0043] = 0b11111111
        // $0000 RMB2 $43
        writeMem(memory, 0x0000, listOf(0x27, 0x43))
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        val expected = 0b11111011.toShort()
        assertEquals(expected, memory[0x0043])

    }

    @Test
    fun test_rmb2_does_not_affect_status_register() {
        memory[0x0043] = 0b11111111
        // $0000 RMB2 $43
        writeMem(memory, 0x0000, listOf(0x27, 0x43))
        val expected = 0b01110101
        mpu.regP.fromInt(expected)
        mpu.step()
        assertEquals(expected, mpu.regP.asInt())
    }

    // RMB3

    @Test
    fun test_rmb3_clears_bit_3_without_affecting_other_bits() {
        memory[0x0043] = 0b11111111
        // $0000 RMB3 $43
        writeMem(memory, 0x0000, listOf(0x37, 0x43))
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        val expected = 0b11110111
        assertEquals(expected, memory[0x0043].toInt())
    }

    @Test
    fun test_rmb3_does_not_affect_status_register() {
        memory[0x0043] = 0b11111111
        // $0000 RMB3 $43
        writeMem(memory, 0x0000, listOf(0x37, 0x43))
        val expected = 0b01110101
        mpu.regP.fromInt(expected)
        mpu.step()
        assertEquals(expected, mpu.regP.asInt())
    }

    // RMB4

    @Test
    fun test_rmb4_clears_bit_4_without_affecting_other_bits() {
        memory[0x0043] = 0b11111111
        // $0000 RMB4 $43
        writeMem(memory, 0x0000, listOf(0x47, 0x43))
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        val expected = 0b11101111
        assertEquals(expected, memory[0x0043].toInt())
    }

    @Test
    fun test_rmb4_does_not_affect_status_register() {
        memory[0x0043] = 0b11111111
        // $0000 RMB4 $43
        writeMem(memory, 0x0000, listOf(0x47, 0x43))
        val expected = 0b01110101
        mpu.regP.fromInt(expected)
        mpu.step()
        assertEquals(expected, mpu.regP.asInt())
    }

    // RMB5

    @Test
    fun test_rmb5_clears_bit_5_without_affecting_other_bits() {
        memory[0x0043] = 0b11111111
        // $0000 RMB5 $43
        writeMem(memory, 0x0000, listOf(0x57, 0x43))
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        val expected = 0b11011111
        assertEquals(expected, memory[0x0043].toInt())
    }

    @Test
    fun test_rmb5_does_not_affect_status_register() {
        memory[0x0043] = 0b11111111
        // $0000 RMB5 $43
        writeMem(memory, 0x0000, listOf(0x57, 0x43))
        val expected = 0b01110101
        mpu.regP.fromInt(expected)
        mpu.step()
        assertEquals(expected, mpu.regP.asInt())
    }

    // RMB6

    @Test
    fun test_rmb6_clears_bit_6_without_affecting_other_bits() {
        memory[0x0043] = 0b11111111
        // $0000 RMB6 $43
        writeMem(memory, 0x0000, listOf(0x67, 0x43))
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        val expected = 0b10111111.toShort()
        assertEquals(expected, memory[0x0043])
    }

    @Test
    fun test_rmb6_does_not_affect_status_register() {
        memory[0x0043] = 0b11111111
        // $0000 RMB6 $43
        writeMem(memory, 0x0000, listOf(0x67, 0x43))
        val expected = 0b01110101
        mpu.regP.fromInt(expected)
        mpu.step()
        assertEquals(expected, mpu.regP.asInt())
    }

    // RMB7

    @Test
    fun test_rmb7_clears_bit_7_without_affecting_other_bits() {
        memory[0x0043] = 0b11111111
        // $0000 RMB7 $43
        writeMem(memory, 0x0000, listOf(0x77, 0x43))
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        val expected = 0b01111111
        assertEquals(expected, memory[0x0043].toInt())

    }

    @Test
    fun test_rmb7_does_not_affect_status_register() {
        memory[0x0043] = 0b11111111
        // $0000 RMB7 $43
        writeMem(memory, 0x0000, listOf(0x77, 0x43))
        val expected = 0b01110101
        mpu.regP.fromInt(expected)
        mpu.step()
        assertEquals(expected, mpu.regP.asInt())
    }

    // STA Zero Page, Indirect

    @Test
    fun test_sta_zp_ind_stores_a_leaves_a_and_n_flag_unchanged() {
        val flags = 0xFF and F_NEGATIVE.inv()
        mpu.regP.fromInt(flags)
        mpu.regA = 0xFF
        // $0000 STA ($0010)
        // $0010 Vector to $FEED
        writeMem(memory, 0x0000, listOf(0x92, 0x10))
        writeMem(memory, 0x0010, listOf(0xED, 0xFE))
        memory[0xFEED] = 0x00
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0xFF, memory[0xFEED])
        assertEquals(0xFF, mpu.regA)
        assertEquals(flags, mpu.regP.asInt())
    }

    @Test
    fun test_sta_zp_ind_stores_a_leaves_a_and_z_flag_unchanged() {
        val flags = 0xFF and F_ZERO.inv()
        mpu.regP.fromInt(flags)
        mpu.regA = 0x00
        // $0000 STA ($0010)
        // $0010 Vector to $FEED
        writeMem(memory, 0x0000, listOf(0x92, 0x10))
        writeMem(memory, 0x0010, listOf(0xED, 0xFE))
        memory[0xFEED] = 0xFF
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0x00, memory[0xFEED])
        assertEquals(0x00, mpu.regA)
        assertEquals(flags, mpu.regP.asInt())
    }

    // SMB0

    @Test
    fun test_smb0_sets_bit_0_without_affecting_other_bits() {
        memory[0x0043] = 0b00000000
        // $0000 SMB0 $43
        writeMem(memory, 0x0000, listOf(0x87, 0x43))
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        val expected = 0b00000001.toShort()
        assertEquals(expected, memory[0x0043])
    }

    @Test
    fun test_smb0_does_not_affect_status_register() {
        memory[0x0043] = 0b00000000
        // $0000 SMB0 $43
        writeMem(memory, 0x0000, listOf(0x87, 0x43))
        val expected = 0b11101100
        mpu.regP.fromInt(expected)
        mpu.step()
        assertEquals(expected, mpu.regP.asInt())
    }

    // SMB1

    @Test
    fun test_smb1_sets_bit_1_without_affecting_other_bits() {
        memory[0x0043] = 0b00000000
        // $0000 SMB1 $43
        writeMem(memory, 0x0000, listOf(0x97, 0x43))
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        val expected = 0b00000010
        assertEquals(expected, memory[0x0043].toInt())
    }

    @Test
    fun test_smb1_does_not_affect_status_register() {
        memory[0x0043] = 0b00000000
        // $0000 SMB1 $43
        writeMem(memory, 0x0000, listOf(0x97, 0x43))
        val expected = 0b11101100
        mpu.regP.fromInt(expected)
        mpu.step()
        assertEquals(expected, mpu.regP.asInt())
    }

    // SMB2

    @Test
    fun test_smb2_sets_bit_2_without_affecting_other_bits() {
        memory[0x0043] = 0b00000000
        // $0000 SMB2 $43
        writeMem(memory, 0x0000, listOf(0xA7, 0x43))
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        val expected = 0b00000100
        assertEquals(expected, memory[0x0043].toInt())

    }

    @Test
    fun test_smb2_does_not_affect_status_register() {
        memory[0x0043] = 0b00000000
        // $0000 SMB2 $43
        writeMem(memory, 0x0000, listOf(0xA7, 0x43))
        val expected = 0b11101100
        mpu.regP.fromInt(expected)
        mpu.step()
        assertEquals(expected, mpu.regP.asInt())
    }

    // SMB3

    @Test
    fun test_smb3_sets_bit_3_without_affecting_other_bits() {
        memory[0x0043] = 0b00000000
        // $0000 SMB3 $43
        writeMem(memory, 0x0000, listOf(0xB7, 0x43))
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        val expected = 0b00001000
        assertEquals(expected, memory[0x0043].toInt())

    }

    @Test
    fun test_smb3_does_not_affect_status_register() {
        memory[0x0043] = 0b00000000
        // $0000 SMB3 $43
        writeMem(memory, 0x0000, listOf(0xB7, 0x43))
        val expected = 0b11101100
        mpu.regP.fromInt(expected)
        mpu.step()
        assertEquals(expected, mpu.regP.asInt())
    }

    // SMB4

    @Test
    fun test_smb4_sets_bit_4_without_affecting_other_bits() {
        memory[0x0043] = 0b00000000
        // $0000 SMB4 $43
        writeMem(memory, 0x0000, listOf(0xC7, 0x43))
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        val expected = 0b00010000
        assertEquals(expected, memory[0x0043].toInt())

    }

    @Test
    fun test_smb4_does_not_affect_status_register() {
        memory[0x0043] = 0b00000000
        // $0000 SMB4 $43
        writeMem(memory, 0x0000, listOf(0xC7, 0x43))
        val expected = 0b11101100
        mpu.regP.fromInt(expected)
        mpu.step()
        assertEquals(expected, mpu.regP.asInt())
    }

    // SMB5

    @Test
    fun test_smb5_sets_bit_5_without_affecting_other_bits() {
        memory[0x0043] = 0b00000000
        // $0000 SMB5 $43
        writeMem(memory, 0x0000, listOf(0xD7, 0x43))
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        val expected = 0b00100000
        assertEquals(expected, memory[0x0043].toInt())
    }

    @Test
    fun test_smb5_does_not_affect_status_register() {
        memory[0x0043] = 0b00000000
        // $0000 SMB5 $43
        writeMem(memory, 0x0000, listOf(0xD7, 0x43))
        val expected = 0b11101100
        mpu.regP.fromInt(expected)
        mpu.step()
        assertEquals(expected, mpu.regP.asInt())
    }

    // SMB6

    @Test
    fun test_smb6_sets_bit_6_without_affecting_other_bits() {
        memory[0x0043] = 0b00000000
        // $0000 SMB6 $43
        writeMem(memory, 0x0000, listOf(0xE7, 0x43))
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        val expected = 0b01000000
        assertEquals(expected, memory[0x0043].toInt())
    }

    @Test
    fun test_smb6_does_not_affect_status_register() {
        memory[0x0043] = 0b00000000
        // $0000 SMB6 $43
        writeMem(memory, 0x0000, listOf(0xE7, 0x43))
        val expected = 0b11101100
        mpu.regP.fromInt(expected)
        mpu.step()
        assertEquals(expected, mpu.regP.asInt())
    }

    // SMB7

    @Test
    fun test_smb7_sets_bit_7_without_affecting_other_bits() {
        memory[0x0043] = 0b00000000
        // $0000 SMB7 $43
        writeMem(memory, 0x0000, listOf(0xF7, 0x43))
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        val expected = 0b10000000
        assertEquals(expected, memory[0x0043].toInt())
    }

    @Test
    fun test_smb7_does_not_affect_status_register() {
        memory[0x0043] = 0b00000000
        // $0000 SMB7 $43
        writeMem(memory, 0x0000, listOf(0xF7, 0x43))
        val expected = 0b11101100
        mpu.regP.fromInt(expected)
        mpu.step()
        assertEquals(expected, mpu.regP.asInt())
    }

    // SBC Zero Page, Indirect

    @Test
    fun test_sbc_zp_ind_all_zeros_and_no_borrow_is_zero() {
        mpu.regP.D = false
        mpu.regP.C = true  // borrow = 0
        mpu.regA = 0x00
        // $0000 SBC ($10)
        // $0010 Vector to $FEED
        writeMem(memory, 0x0000, listOf(0xF2, 0x10))
        writeMem(memory, 0x0010, listOf(0xED, 0xFE))
        memory[0xFEED] = 0x00
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0x00, mpu.regA)
        assertFalse(mpu.regP.N)
        assertTrue(mpu.regP.C)
        assertTrue(mpu.regP.Z)
    }

    @Test
    fun test_sbc_zp_ind_downto_zero_no_borrow_sets_z_clears_n() {
        mpu.regP.D = false
        mpu.regP.C = true  // borrow = 0
        mpu.regA = 0x01
        // $0000 SBC ($10)
        // $0010 Vector to $FEED
        writeMem(memory, 0x0000, listOf(0xF2, 0x10))
        writeMem(memory, 0x0010, listOf(0xED, 0xFE))
        memory[0xFEED] = 0x01
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0x00, mpu.regA)
        assertFalse(mpu.regP.N)
        assertTrue(mpu.regP.C)
        assertTrue(mpu.regP.Z)
    }

    @Test
    fun test_sbc_zp_ind_downto_zero_with_borrow_sets_z_clears_n() {
        mpu.regP.D = false
        mpu.regP.C = false  // borrow = 1
        mpu.regA = 0x01
        // $0000 SBC ($10)
        // $0010 Vector to $FEED
        writeMem(memory, 0x0000, listOf(0xF2, 0x10))
        writeMem(memory, 0x0010, listOf(0xED, 0xFE))
        memory[0xFEED] = 0x00
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0x00, mpu.regA)
        assertFalse(mpu.regP.N)
        assertTrue(mpu.regP.C)
        assertTrue(mpu.regP.Z)
    }

    @Test
    fun test_sbc_zp_ind_downto_four_with_borrow_clears_z_n() {
        mpu.regP.D = false
        mpu.regP.C = false  // borrow = 1
        mpu.regA = 0x07
        // $0000 SBC ($10)
        // $0010 Vector to $FEED
        writeMem(memory, 0x0000, listOf(0xF2, 0x10))
        writeMem(memory, 0x0010, listOf(0xED, 0xFE))
        memory[0xFEED] = 0x02
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
        assertEquals(0x04, mpu.regA)
        assertFalse(mpu.regP.N)
        assertFalse(mpu.regP.Z)
        assertTrue(mpu.regP.C)
    }

    // STZ Zero Page

    @Test
    fun test_stz_zp_stores_zero() {
        memory[0x0032] = 0x88
        // #0000 STZ $32
        memory[0x0000] = 0x64
        memory[0x0001] = 0x32
        assertEquals(0x88, memory[0x0032])
        mpu.step()
        assertEquals(0x00, memory[0x0032])
        assertEquals(0x0002, mpu.regPC)
        assertEquals(11, mpu.totalCycles)
    }

    // STZ Zero Page, X-Indexed

    @Test
    fun test_stz_zp_x_stores_zero() {
        memory[0x0032] = 0x88
        // $0000 STZ $32,X
        memory[0x0000] = 0x74
        memory[0x0001] = 0x32
        assertEquals(0x88, memory[0x0032])
        mpu.step()
        assertEquals(0x00, memory[0x0032])
        assertEquals(0x0002, mpu.regPC)
        assertEquals(12, mpu.totalCycles)
    }

    // STZ Absolute

    @Test
    fun test_stz_abs_stores_zero() {
        memory[0xFEED] = 0x88
        // $0000 STZ $FEED
        writeMem(memory, 0x0000, listOf(0x9C, 0xED, 0xFE))
        assertEquals(0x88, memory[0xFEED])
        mpu.step()
        assertEquals(0x00, memory[0xFEED])
        assertEquals(0x0003, mpu.regPC)
        assertEquals(12, mpu.totalCycles)
    }

    // STZ Absolute, X-Indexed

    @Test
    fun test_stz_abs_x_stores_zero() {
        memory[0xFEED] = 0x88
        mpu.regX = 0x0D
        // $0000 STZ $FEE0,X
        writeMem(memory, 0x0000, listOf(0x9E, 0xE0, 0xFE))
        assertEquals(0x88, memory[0xFEED])
        assertEquals(0x0D, mpu.regX)
        mpu.step()
        assertEquals(0x00, memory[0xFEED])
        assertEquals(0x0003, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
    }

    // TSB Zero Page

    @Test
    fun test_tsb_zp_ones() {
        memory[0x00BB] = 0xE0
        // $0000 TSB $BD
        writeMem(memory, 0x0000, listOf(0x04, 0xBB))
        mpu.regA = 0x70
        assertEquals(0xE0, memory[0x00BB])
        mpu.step()
        assertEquals(0xF0, memory[0x00BB])
        assertFalse(mpu.regP.Z)
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
    }

    @Test
    fun test_tsb_zp_zeros() {
        memory[0x00BB] = 0x80
        // $0000 TSB $BD
        writeMem(memory, 0x0000, listOf(0x04, 0xBB))
        mpu.regA = 0x60
        assertEquals(0x80, memory[0x00BB])
        mpu.step()
        assertEquals(0xE0, memory[0x00BB])
        assertTrue(mpu.regP.Z)
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
    }

    // TSB Absolute

    @Test
    fun test_tsb_abs_ones() {
        memory[0xFEED] = 0xE0
        // $0000 TSB $FEED
        writeMem(memory, 0x0000, listOf(0x0C, 0xED, 0xFE))
        mpu.regA = 0x70
        assertEquals(0xE0, memory[0xFEED])
        mpu.step()
        assertEquals(0xF0, memory[0xFEED])
        assertFalse(mpu.regP.Z)
        assertEquals(0x0003, mpu.regPC)
        assertEquals(14, mpu.totalCycles)
    }

    @Test
    fun test_tsb_abs_zeros() {
        memory[0xFEED] = 0x80
        // $0000 TSB $FEED
        writeMem(memory, 0x0000, listOf(0x0C, 0xED, 0xFE))
        mpu.regA = 0x60
        assertEquals(0x80, memory[0xFEED])
        mpu.step()
        assertEquals(0xE0, memory[0xFEED])
        assertTrue(mpu.regP.Z)
        assertEquals(0x0003, mpu.regPC)
        assertEquals(14, mpu.totalCycles)
    }

    // TRB Zero Page

    @Test
    fun test_trb_zp_ones() {
        memory[0x00BB] = 0xE0
        // $0000 TRB $BD
        writeMem(memory, 0x0000, listOf(0x14, 0xBB))
        mpu.regA = 0x70
        assertEquals(0xE0, memory[0x00BB])
        mpu.step()
        assertEquals(0x80, memory[0x00BB])
        assertFalse(mpu.regP.Z)
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
    }

    @Test
    fun test_trb_zp_zeros() {
        memory[0x00BB] = 0x80
        // $0000 TRB $BD
        writeMem(memory, 0x0000, listOf(0x14, 0xBB))
        mpu.regA = 0x60
        assertEquals(0x80, memory[0x00BB])
        mpu.step()
        assertEquals(0x80, memory[0x00BB])
        assertTrue(mpu.regP.Z)
        assertEquals(0x0002, mpu.regPC)
        assertEquals(13L, mpu.totalCycles)
    }

    // TRB Absolute

    @Test
    fun test_trb_abs_ones() {
        memory[0xFEED] = 0xE0
        // $0000 TRB $FEED
        writeMem(memory, 0x0000, listOf(0x1C, 0xED, 0xFE))
        mpu.regA = 0x70
        assertEquals(0xE0, memory[0xFEED])
        mpu.step()
        assertEquals(0x80, memory[0xFEED])
        assertFalse(mpu.regP.Z)
        assertEquals(0x0003, mpu.regPC)
        assertEquals(14, mpu.totalCycles)
    }

    @Test
    fun test_trb_abs_zeros() {
        memory[0xFEED] = 0x80
        // $0000 TRB $FEED
        writeMem(memory, 0x0000, listOf(0x1C, 0xED, 0xFE))
        mpu.regA = 0x60
        assertEquals(0x80, memory[0xFEED])
        mpu.step()
        assertEquals(0x80, memory[0xFEED])
        assertTrue(mpu.regP.Z)
        assertEquals(0x0003, mpu.regPC)
        assertEquals(14, mpu.totalCycles)
    }

    @Test
    fun test_dec_a_decreases_a() {
        // $0000 DEC A
        memory[0x0000] = 0x3a
        mpu.regA = 0x48
        mpu.step()
        assertFalse(mpu.regP.Z)
        assertFalse(mpu.regP.N)
        assertEquals(0x47, mpu.regA)
    }

    @Test
    fun test_dec_a_sets_zero_flag() {
        // $0000 DEC A
        memory[0x0000] = 0x3a
        mpu.regA = 0x01
        mpu.step()
        assertTrue(mpu.regP.Z)
        assertFalse(mpu.regP.N)
        assertEquals(0x00, mpu.regA)
    }

    @Test
    fun test_dec_a_wraps_at_zero() {
        // $0000 DEC A
        memory[0x0000] = 0x3a
        mpu.regA = 0x00
        mpu.step()
        assertFalse(mpu.regP.Z)
        assertTrue(mpu.regP.N)
        assertEquals(0xFF, mpu.regA)
    }

    @Test
    fun test_bra_forward() {
        // $0000 BRA $10
        writeMem(memory, 0x0000, listOf(0x80, 0x10))
        mpu.step()
        assertEquals(0x12, mpu.regPC)
        assertEquals(11, mpu.totalCycles)
    }

    @Test
    fun test_bra_backward() {
        // $0240 BRA $F0
        writeMem(memory, 0x0204, listOf(0x80, 0xF0))
        mpu.regPC = 0x0204
        mpu.step()
        assertEquals(0x1F6, mpu.regPC)
        assertEquals(11, mpu.totalCycles)  // Crossed boundary
    }

    // WAI

    @Test
    fun test_wai_sets_waiting() {
        mpu as Cpu65C02
        assertEquals(Cpu65C02.Wait.Normal, mpu.waiting)
        // $0240 WAI
        memory[0x0204] = 0xcb
        mpu.regPC = 0x0204
        mpu.step()
        assertEquals(Cpu65C02.Wait.Waiting, mpu.waiting)
        assertEquals(0x0205, mpu.regPC)
        assertEquals(11, mpu.totalCycles)
    }

    //  BBR and BBS
    @Test
    fun test_bbr_all_set_doesnt_branch() {
        mpu as Cpu65C02
        mpu.regPC = 0
        memory[0xfe] = 0xff
        writeMem(memory, 0, listOf(0x0f, 0xfe, 0x40,
            0x1f, 0xfe, 0x40,
            0x2f, 0xfe, 0x40,
            0x3f, 0xfe, 0x40,
            0x4f, 0xfe, 0x40,
            0x5f, 0xfe, 0x40,
            0x6f, 0xfe, 0x40,
            0x7f, 0xfe, 0x40,
            0xea))
        repeat(8) { mpu.step() }
        assertNotEquals(0x0040, mpu.regPC)
        assertEquals(0xea, memory[mpu.regPC])
    }

    @Test
    fun test_bbr_branches() {
        mpu as Cpu65C02
        mpu.regPC = 0
        memory[0xfe] = 0b10111111   // bit 6 cleared
        writeMem(memory, 0, listOf(0x6f, 0xfe, 0x40))   // BBR6 $fe, $0040
        mpu.step()
        assertEquals(0x0043, mpu.regPC)
    }

    @Test
    fun test_bbs_all_clear_doesnt_branch() {
        mpu as Cpu65C02
        mpu.regPC = 0
        memory[0xfe] = 0
        writeMem(memory, 0, listOf(0x8f, 0xfe, 0x40,
            0x9f, 0xfe, 0x40,
            0xaf, 0xfe, 0x40,
            0xbf, 0xfe, 0x40,
            0xcf, 0xfe, 0x40,
            0xdf, 0xfe, 0x40,
            0xef, 0xfe, 0x40,
            0xff, 0xfe, 0x40,
            0xea))
        repeat(8) { mpu.step() }
        assertNotEquals(0x0040, mpu.regPC)
        assertEquals(0xea, memory[mpu.regPC])
    }

    @Test
    fun test_bbs_branches() {
        mpu as Cpu65C02
        mpu.regPC = 0
        memory[0xfe] = 0b01000000   // bit 6 set
        writeMem(memory, 0, listOf(0xef, 0xfe, 0x40))   // BBS6 $fe, $0040
        mpu.step()
        assertEquals(0x0043, mpu.regPC)
    }

    @Test
    fun test_sbc_bcd_on_immediate_20_minus_0a_carry_unset() {
        mpu.regP.D = true
        mpu.regP.C = false
        mpu.regA = 0x20
        // $0000 SBC #$0a
        writeMem(memory, 0x0000, listOf(0xe9, 0x0a))
        mpu.step()
        assertEquals(0x0002, mpu.regPC)
        assertEquals(0x0f, mpu.regA)       // 0x1f on 6502, 0x0f on 65c02
        assertFalse(mpu.regP.Z)
        assertTrue(mpu.regP.C)
        assertFalse(mpu.regP.N)
        assertFalse(mpu.regP.V)
    }

    @Test
    fun test_adc_bcd_on_immediate_9c_plus_9d() {
        mpu.regP.D = true
        mpu.regP.C = false
        mpu.regP.N = true
        mpu.regA = 0x9c
        // $0000 ADC #$9d
        // $0002 ADC #$9d
        writeMem(memory, 0x0000, listOf(0x69, 0x9d))
        writeMem(memory, 0x0002, listOf(0x69, 0x9d))
        mpu.step()
        assertEquals(0x9f, mpu.regA)
        assertTrue(mpu.regP.C)
        mpu.step()
        assertEquals(0x0004, mpu.regPC)
        assertEquals(0x93, mpu.regA)
        assertFalse(mpu.regP.Z)
        assertTrue(mpu.regP.C)
        assertTrue(mpu.regP.V)
        assertTrue(mpu.regP.N)   // False on 6502,  True on 65C02
    }
}
