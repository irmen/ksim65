
	.cpu "6502"

	DISPLAY = $d000
	RTC	= $d100
	TIMER	= $d200
	MOUSE   = $d300
	KEYBOARD = $d400
	IRQVEC  = $fffe
	SCREEN_WIDTH=640

	* = $1000

start
	sei
	ldx  #$ff
	txs		; clear the stack
	; setup timer irq
	lda  #<irq
	sta  IRQVEC
	lda  #>irq
	sta  IRQVEC+1
	lda  #<2000
	sta  TIMER+1	; every 2000 clock cycles an irq
	lda  #>2000
	sta  TIMER+2
	lda  #0
	sta  TIMER+3
	lda  #1
	sta  TIMER+0
	cli		; enable irqs

; ------- print 'c64' screen
	lda  #4
	sta  DISPLAY+8
	lda  #1
	sta  DISPLAY+9
	ldy  #0
-	lda  _title1,y
	beq  +
	sta  DISPLAY+10
	iny
	bne  -
	beq  +

_title1 .text "**** COMMODORE 64 BASIC V2 ****", 10, 10, " 64K RAM SYSTEM  38911 BASIC BYTES FREE", 10, 10, "READY.",10,0


+

; ------- draw pixel line
pixelline
	ldx  pix_x
	stx  DISPLAY+3
	ldx  pix_x+1
	stx  DISPLAY+4
	ldx  pix_y
	stx  DISPLAY+5
	ldx  pix_y+1
	stx  DISPLAY+6
	lda  #1
	sta  DISPLAY+7		; plot
	lda  pix_x
	clc
	adc  #2
	sta  pix_x
	bcc  +
	inc  pix_x+1
+	inc  pix_y
	bne  +
	inc  pix_y+1
+	lda  pix_x+1
	cmp  #>SCREEN_WIDTH
	bcc  pixelline
	bne  stop1
	lda  pix_x
	cmp  #<SCREEN_WIDTH
	bcc  pixelline
	bcs  stop1
pix_x	.word 0
pix_y	.word 0

stop1

;--------- draw with mouse
mousedraw
	ldx  MOUSE+0
	ldy  MOUSE+2
	stx  DISPLAY+3
	sty  DISPLAY+5
	ldx  MOUSE+1
	ldy  MOUSE+3
	stx  DISPLAY+4
	sty  DISPLAY+6
	lda  #1
	sta  DISPLAY+7		; plot pixel
	jmp  mousedraw


character .byte 0

; ---------- irq routine
;   this one simply read the RTC and prints it on the bottom of the screen.
irq
	pha
	txa
	pha
	tya
	pha
	; we don't check for BRK flag because we're lazy
	lda  DISPLAY+0
	pha
	lda  DISPLAY+1
	pha
	ldy  #29
	sty  DISPLAY+1
	ldy  #0
	sty  DISPLAY+0
	lda  #<_time_msg
	ldy  #>_time_msg
	jsr  textout
	; read the clock now
	; YEAR
	lda  RTC+0
	ldy  RTC+1
	jsr  HexToDec65535
	lda  #<DecTenThousands
	ldy  #>DecTenThousands
	jsr  textout
	lda  #'-'
	sta  DISPLAY+2
	inc  DISPLAY+0
	; MONTH
	lda  RTC+2
	jsr  HexToDec99
	lda  #<DecTens
	ldy  #>DecTens
	jsr  textout
	lda  #'-'
	sta  DISPLAY+2
	inc  DISPLAY+0
	; DAY
	lda  RTC+3
	jsr  HexToDec99
	lda  #<DecTens
	ldy  #>DecTens
	jsr  textout
	lda  #'/'
	sta  DISPLAY+2
	inc  DISPLAY+0
	; HOUR
	lda  RTC+4
	jsr  HexToDec99
	lda  #<DecTens
	ldy  #>DecTens
	jsr  textout
	lda  #':'
	sta  DISPLAY+2
	inc  DISPLAY+0
	; MINUTE
	lda  RTC+5
	jsr  HexToDec99
	lda  #<DecTens
	ldy  #>DecTens
	jsr  textout
	lda  #':'
	sta  DISPLAY+2
	inc  DISPLAY+0
	; SECOND
	lda  RTC+6
	jsr  HexToDec99
	lda  #<DecTens
	ldy  #>DecTens
	jsr  textout
	lda  #'.'
	sta  DISPLAY+2
	inc  DISPLAY+0
	; MILLISECOND
	lda  RTC+6
	ldy  RTC+7
	jsr  HexToDec65535
	lda  #<DecTenThousands
	ldy  #>DecTenThousands
	jsr  textout

	pla
	sta  DISPLAY+1
	pla
	sta  DISPLAY+0
	pla
	tay
	pla
	tax
	pla
	rti

_time_msg	.text  "The current date and time is: ",0



; ----- routines
textout
	sta  _mod+1
	sty  _mod+2
	ldy  #0
_mod	lda  $ffff,y	; modified
	beq  +
	sta  DISPLAY+2
	inc  DISPLAY+0
	iny
	bne  _mod
+	rts


ubyte2hex
	; ---- A to hex string in AY (first hex char in A, second hex char in Y)
		stx  _save_x
		pha
		and  #$0f
		tax
		ldy  _hex_digits,x
		pla
		lsr  a
		lsr  a
		lsr  a
		lsr  a
		tax
		lda  _hex_digits,x
		ldx  _save_x
		rts

_save_x		.byte 0
_hex_digits	.text "0123456789abcdef"	; can probably be reused for other stuff as well


uword2hex
	; ---- convert 16 bit uword in A/Y into 4-character hexadecimal string 'hex_output' (0-terminated)
		pha
		tya
		jsr  ubyte2hex
		sta  hex_output
		sty  hex_output+1
		pla
		jsr  ubyte2hex
		sta  hex_output+2
		sty  hex_output+3
		rts
hex_output	.text  "0000", $00      ; 0-terminated output buffer (to make printing easier)



;----------------------------------------------------------
;Convert 16 bit Hex to Decimal (0-65535) Rev 2
;By Omegamatrix
;Further optimizations by tepples
; routine from http://forums.nesdev.com/viewtopic.php?f=2&t=11341&start=15
;

;HexToDec99
; start in A
; end with A = 10's, decOnes

;HexToDec255
; start in A
; end with Y = 100's, A = 10's, decOnes

;HexToDec999
; start with A = high byte, Y = low byte
; end with Y = 100's, A = 10's, decOnes
; requires 1 extra temp register on top of decOnes, could combine
; these two if HexToDec65535 was eliminated...

;HexToDec65535
; start with A/Y (low/high) as 16 bit value
; end with decTenThousand, decThousand, Y = 100's, A = 10's, decOnes
; (irmen: I store Y and A in decHundreds and decTens too, so all of it can be easily printed)

decTenThousands   	.byte  0
decThousands    	.byte  0
decHundreds		.byte  0
decTens			.byte  0
decOnes   		.byte  0
			.byte  0		; zero-terminate the decimal output string

ASCII_OFFSET 	= $30
temp       	= $80	; byte  in zeropage
hexHigh      	= $81	; byte in zeropage
hexLow       	= $82	; byte in zeropage


Mod100Tab
    .byte 0,56,12,56+12

ShiftedBcdTab
    .byte $00,$01,$02,$03,$04,$08,$09,$0A,$0B,$0C
    .byte $10,$11,$12,$13,$14,$18,$19,$1A,$1B,$1C
    .byte $20,$21,$22,$23,$24,$28,$29,$2A,$2B,$2C
    .byte $30,$31,$32,$33,$34,$38,$39,$3A,$3B,$3C
    .byte $40,$41,$42,$43,$44,$48,$49,$4A,$4B,$4C

HexToDec65535; SUBROUTINE
    sty    hexHigh               ;3  @9
    sta    hexLow                ;3  @12
    tya
    tax                          ;2  @14
    lsr                          ;2  @16
    lsr                          ;2  @18   integer divide 1024 (result 0-63)

    cpx    #$A7                  ;2  @20   account for overflow of multiplying 24 from 43,000 ($A7F8) onward,
    adc    #1                    ;2  @22   we can just round it to $A700, and the divide by 1024 is fine...

    ;at this point we have a number 1-65 that we have to times by 24,
    ;add to original sum, and Mod 1024 to get a remainder 0-999


    sta    temp                  ;3  @25
    asl                          ;2  @27
    adc    temp                  ;3  @30  x3
    tay                          ;2  @32
    lsr                          ;2  @34
    lsr                          ;2  @36
    lsr                          ;2  @38
    lsr                          ;2  @40
    lsr                          ;2  @42
    tax                          ;2  @44
    tya                          ;2  @46
    asl                          ;2  @48
    asl                          ;2  @50
    asl                          ;2  @52
    clc                          ;2  @54
    adc    hexLow                ;3  @57
    sta    hexLow                ;3  @60
    txa                          ;2  @62
    adc    hexHigh               ;3  @65
    sta    hexHigh               ;3  @68
    ror                          ;2  @70
    lsr                          ;2  @72
    tay                          ;2  @74    integer divide 1,000 (result 0-65)

    lsr                          ;2  @76    split the 1,000 and 10,000 digit
    tax                          ;2  @78
    lda    ShiftedBcdTab,X       ;4  @82
    tax                          ;2  @84
    rol                          ;2  @86
    and    #$0F                  ;2  @88
    ora    #ASCII_OFFSET
    sta    decThousands          ;3  @91
    txa                          ;2  @93
    lsr                          ;2  @95
    lsr                          ;2  @97
    lsr                          ;2  @99
    ora    #ASCII_OFFSET
    sta    decTenThousands       ;3  @102

    lda    hexLow                ;3  @105
    cpy    temp                  ;3  @108
    bmi    _doSubtract           ;2³ @110/111
    beq    _useZero               ;2³ @112/113
    adc    #23 + 24              ;2  @114
_doSubtract
    sbc    #23                   ;2  @116
    sta    hexLow                ;3  @119
_useZero
    lda    hexHigh               ;3  @122
    sbc    #0                    ;2  @124

Start100s
    and    #$03                  ;2  @126
    tax                          ;2  @128   0,1,2,3
    cmp    #2                    ;2  @130
    rol                          ;2  @132   0,2,5,7
    ora    #ASCII_OFFSET
    tay                          ;2  @134   Y = Hundreds digit

    lda    hexLow                ;3  @137
    adc    Mod100Tab,X           ;4  @141    adding remainder of 256, 512, and 256+512 (all mod 100)
    bcs    hex_doSub200             ;2³ @143/144

hex_try200
    cmp    #200                  ;2  @145
    bcc    hex_try100               ;2³ @147/148
hex_doSub200
    iny                          ;2  @149
    iny                          ;2  @151
    sbc    #200                  ;2  @153
hex_try100
    cmp    #100                  ;2  @155
    bcc    HexToDec99            ;2³ @157/158
    iny                          ;2  @159
    sbc    #100                  ;2  @161

HexToDec99; SUBROUTINE
    lsr                          ;2  @163
    tax                          ;2  @165
    lda    ShiftedBcdTab,X       ;4  @169
    tax                          ;2  @171
    rol                          ;2  @173
    and    #$0F                  ;2  @175
    ora    #ASCII_OFFSET
    sta    decOnes               ;3  @178
    txa                          ;2  @180
    lsr                          ;2  @182
    lsr                          ;2  @184
    lsr                          ;2  @186
    ora    #ASCII_OFFSET

    ; irmen: store Y and A too, for easy printing afterwards
    sty  decHundreds
    sta  decTens
    rts                          ;6  @192   A = tens digit


HexToDec255; SUBROUTINE
    ldy    #ASCII_OFFSET         ;2  @8
    bne    hex_try200               ;3  @11    always branch

HexToDec999; SUBROUTINE
    sty    hexLow                ;3  @9
    jmp    Start100s             ;3  @12

