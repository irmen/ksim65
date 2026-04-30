.cpu  'w65c02'
.enc  'none'

	* = $1000
	
	SERIAL_IN = $f200
	SERIAL_OUT = $f201
	PORT_RESET = $f202
	PORT_POWEROFF = $f203
	
start:
	ldy  #0
-	lda  message,y
	beq  _end
	sta  SERIAL_OUT
	iny
	bne  -

_end	stz  PORT_POWEROFF
	; notreached
	
message
	.text "hello, world!",10,0
