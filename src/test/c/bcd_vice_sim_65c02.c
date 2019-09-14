#include <stdio.h>

/*
    testing 65c02 core ADC and SBC from Vice
*/

static int flag_Z;
static int flag_C;
static int flag_N;
static int flag_V;

unsigned int ADC(unsigned int A, unsigned int value, unsigned int bcd_mode)
{
        unsigned int tmp, tmp2;
        unsigned int tmp_value = value;

        if (bcd_mode) {
            tmp = (A & 0xf) + (tmp_value & 0xf) + flag_C;
            tmp2 = (A & 0xf0) + (tmp_value & 0xf0);
            if (tmp > 9) {
                tmp2 += 0x10;
                tmp += 6;
            }
            flag_V = ~(A ^ tmp_value) & (A ^ tmp2) & 0x80;
            if (tmp2 > 0x90) {
                tmp2 += 0x60;
            }
            flag_C = (tmp2 & 0xff00)!=0;
            tmp = (tmp & 0xf) + (tmp2 & 0xf0);
            flag_Z = tmp==0;
            flag_N = tmp&0x80!=0;
        } else {
            tmp = tmp_value + A + flag_C;
            flag_Z = (tmp&0xff)==0;
            flag_N = (tmp&0x80)!=0;
            flag_V = !((A ^ tmp_value) & 0x80) && ((A ^ tmp) & 0x80);
            flag_C = tmp > 0xff;
        }
        return tmp & 0xff;
}

unsigned int SBC(unsigned int A, unsigned int value, unsigned int bcd_mode)
{
        unsigned int src = value;
        unsigned int tmp = A - src + flag_C - 1;
        flag_V = (((A ^ tmp) & 0x80) && ((A ^ src) & 0x80));
        if (bcd_mode) {
            if (tmp > 0xff) {
                tmp -= 0x60;
            }
            unsigned int tmp2 = (A & 0xf) - (src & 0xf) + flag_C - 1;
            if (tmp2 > 0xff) {
                tmp -= 6;
            }
        }
        flag_C = (A + flag_C - 1 >= src);
        flag_Z = (tmp&0xff)==0;
        flag_N = (tmp&0x80)!=0;
        return tmp & 0xff;
}


void print_result(int result) {
    printf(" %02x", result);
    printf(" N=%d", flag_N? 1:0);
    printf(" V=%d", flag_V? 1:0);
    printf(" Z=%d", flag_Z? 1:0);
    printf(" C=%d", flag_C? 1:0);
    printf("\n");
}

int main(char* argv) {
    printf("65c02   adc/sbc simulation\n");

    flag_C = flag_N = flag_V = flag_Z = 0;

    for(unsigned int A=0; A<256; ++A) {
        for(unsigned int v=0; v<256; ++v) {
            flag_C = 0;
            printf("adc,normal,carry0:  %02x + %02x = ", A, v);
            unsigned int result = ADC(A, v, 0);
            print_result(result);
        }
    }
    printf("\n");
    for(unsigned int A=0; A<256; ++A) {
        for(unsigned int v=0; v<256; ++v) {
            flag_C = 1;
            printf("adc,normal,carry1:  %02x + %02x = ", A, v);
            unsigned int result = ADC(A, v, 0);
            print_result(result);
        }
    }
    printf("\n");
    for(unsigned int A=0; A<256; ++A) {
        for(unsigned int v=0; v<256; ++v) {
            flag_C = 0;
            printf("adc,bcd,carry0:  %02x + %02x = ", A, v);
            unsigned int result = ADC(A, v, 1);
            print_result(result);
        }
    }
    printf("\n");
    for(unsigned int A=0; A<256; ++A) {
        for(unsigned int v=0; v<256; ++v) {
            flag_C = 1;
            printf("adc,bcd,carry1:  %02x + %02x = ", A, v);
            unsigned int result = ADC(A, v, 1);
            print_result(result);
        }
    }



        for(unsigned int A=0; A<256; ++A) {
            for(unsigned int v=0; v<256; ++v) {
                flag_C = 0;
                printf("sbc,normal,carry0:  %02x - %02x = ", A, v);
                unsigned int result = SBC(A, v, 0);
                print_result(result);
            }
        }
        printf("\n");
        for(unsigned int A=0; A<256; ++A) {
            for(unsigned int v=0; v<256; ++v) {
                flag_C = 1;
                printf("sbc,normal,carry1:  %02x - %02x = ", A, v);
                unsigned int result = SBC(A, v, 0);
                print_result(result);
            }
        }
        printf("\n");
        for(unsigned int A=0; A<256; ++A) {
            for(unsigned int v=0; v<256; ++v) {
                flag_C = 0;
                printf("sbc,bcd,carry0:  %02x - %02x = ", A, v);
                unsigned int result = SBC(A, v, 1);
                print_result(result);
            }
        }
        printf("\n");
        for(unsigned int A=0; A<256; ++A) {
            for(unsigned int v=0; v<256; ++v) {
                flag_C = 1;
                printf("sbc,bcd,carry1:  %02x - %02x = ", A, v);
                unsigned int result = SBC(A, v, 1);
                print_result(result);
            }
        }
}

