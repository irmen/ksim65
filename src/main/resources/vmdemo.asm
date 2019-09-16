
	.cpu "6502"

	DISPLAY = $d000
	RTC	= $d100
	TIMER	= $d200
	MOUSE   = $d300
	KEYBOARD = $d400
	SCREEN_WIDTH=640

	* = $1000

start
	sei
	ldx  #$ff
	txs
	cli

; ------- print stuff
	lda  #10
	sta  DISPLAY+8
	sta  DISPLAY+9
	ldx  #5
printloop
	ldy  #32
printloop2
	sty  DISPLAY+10
	iny
	bne  printloop2
	dex
	bne  printloop

; ------- fill the screen

	ldx  #0
	ldy  #0
fillscreen
	stx  DISPLAY+0
	sty  DISPLAY+1
	lda  character
	sta  DISPLAY+2		; plot the char on the screen
	inc  character
	inx
	cpx  #80
	bne  fillscreen
	ldx  #0
	iny
	cpy  #30
	bne  fillscreen

; ------- clear the screen
	lda  #$0c		; form feed (clear screen)
	sta  DISPLAY+$0a

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


;--------- RTC display  TODO
	ldy  #0
	lda  #0
	sta  DISPLAY+0
	sta  DISPLAY+1
rtcloop
	ldx  #0
readrtc
	lda  RTC,x
	sta  DISPLAY+2
	inc  DISPLAY+0
	inx
	cpx  #9
	bne  readrtc
	lda  #0
	sta  DISPLAY+0
	iny
	sty  DISPLAY+1
	cpy  #20
	bne  rtcloop


done	jmp  done


character .byte 0
