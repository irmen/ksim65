
	*=$0801
	.word (+), 2005
	.null $9e, format("%d", start)
+	.word 0

	* = 2084


start
	sei
	lda  #$7f
	sta  $dc0d
	and  $d011
	sta  $d011
	lda  #123
	sta  $d012
	lda  #<irq
	sta  $0314
	lda  #>irq
	sta  $0315
	lda  #1
	sta  $d01a
	cli
	rts

irq
	inc  $0400
	asl  $d019
	jmp  $ea31
