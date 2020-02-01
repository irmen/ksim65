	CIA1	= $DC00
	CIA2	= $DD00
	SCREEN	= $0400

	*=$0801
	.word (+), 2005
	.null $9e, format("%d", start)
+	.word 0

	* = 2084


start
	jsr  $e544	; clear screen
	lda  CIA1+15
	and  #%01111111
	sta  CIA1+15
	lda  CIA2+15
	and  #%01111111
	sta  CIA2+15

	lda  #$08
	sta  CIA1+11
	lda  #$13
	sta  CIA2+11
	lda  #$59
	sta  CIA1+10
	lda  #$20
	sta  CIA2+10
	lda  #$50
	sta  CIA1+9
	lda  #$40
	sta  CIA2+9
	lda  #$00
	sta  CIA1+8
	sta  CIA2+8

	lda  CIA1+8		; start TOD timer in CIA1
	lda  CIA2+8		; start TOD timer in CIA2

loop
	lda  CIA1+8
	ora  #'0'
	sta  SCREEN+0

	lda  CIA2+8
	ora  #'0'
	sta  SCREEN+0+40

	lda  CIA1+9
	tay
	lsr  a
	lsr  a
	lsr  a
	lsr  a
	ora  #'0'
	sta  SCREEN+2
	tya
	and  #$0f
	ora  #'0'
	sta  SCREEN+3

	lda  CIA2+9
	tay
	lsr  a
	lsr  a
	lsr  a
	lsr  a
	ora  #'0'
	sta  SCREEN+2+40
	tya
	and  #$0f
	ora  #'0'
	sta  SCREEN+3+40

	lda  CIA1+10
	tay
	lsr  a
	lsr  a
	lsr  a
	lsr  a
	ora  #'0'
	sta  SCREEN+5
	tya
	and  #$0f
	ora  #'0'
	sta  SCREEN+6

	lda  CIA2+10
	tay
	lsr  a
	lsr  a
	lsr  a
	lsr  a
	ora  #'0'
	sta  SCREEN+5+40
	tya
	and  #$0f
	ora  #'0'
	sta  SCREEN+6+40

	lda  CIA1+11
	tay
	lsr  a
	lsr  a
	lsr  a
	lsr  a
	ora  #'0'
	sta  SCREEN+8
	tya
	and  #$0f
	ora  #'0'
	sta  SCREEN+9

	lda  CIA2+11
	tay
	lsr  a
	lsr  a
	lsr  a
	lsr  a
	ora  #'0'
	sta  SCREEN+8+40
	tya
	and  #$0f
	ora  #'0'
	sta  SCREEN+9+40

	jmp  loop






