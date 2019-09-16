package razorvine.ksim65.components

import razorvine.examplemachine.ScreenDefs
import razorvine.ksim65.IHostInterface
import kotlin.math.min

/**
 * Text mode and graphics (bitmap) mode display.
 * Note that the character matrix and pixel matrix are NOT memory mapped,
 * this display device is controlled by sending char/pixel commands to it.
 *
 * Requires a host display to actually view the stuff, obviously.
 *
 * reg.    value
 * ----    -----
 *  00      char X position
 *  01      char Y position
 *  02      r/w character at cX,cY (doesn't change cursor position)
 *  03      pixel X pos (lsb)
 *  04      pixel X pos (msb)
 *  05      pixel Y pos (lsb)
 *  06      pixel Y pos (msb)
 *  07      read or write pixel value at pX, pY
 *  08      cursor X position (r/w)
 *  09      cursor Y position (r/w)
 *  0a      read or write character at cursor pos, updates cursor position, scrolls up if necessary
 *          control characters: 0x08=backspace, 0x09=tab, 0x0a=newline, 0x0c=formfeed(clear screen), 0x0d=carriagereturn
 *
 *          TODO: cursor blinking, blink speed (0=off)
 */
class Display(
    startAddress: Address, endAddress: Address,
    private val host: IHostInterface,
    private val charWidth: Int,
    private val charHeight: Int,
    private val pixelWidth: Int,
    private val pixelHeight: Int
) : MemMappedComponent(startAddress, endAddress) {


    init {
        require(endAddress - startAddress + 1 == 11) { "display needs exactly 11 memory bytes" }
    }

    private var cursorX = 0
    private var cursorY = 0
    private var charposX = 0
    private var charposY = 0
    private var pixelX = 0
    private var pixelY = 0
    private val charMatrix = Array<ShortArray>(charHeight) { ShortArray(charWidth) }    // matrix[y][x] to access

    override fun clock() {
        // if the system clock is synced to the display refresh,
        // you *could* add a Vertical Blank interrupt here.
    }

    override fun reset() {
        charMatrix.forEach { it.fill(' '.toShort()) }
        cursorX = 0
        cursorY = 0
        charposX = 0
        charposY = 0
        pixelX = 0
        pixelY = 0
        host.clearScreen()
    }

    override operator fun get(address: Address): UByte {
        return when(address-startAddress) {
            0x00 -> charposX.toShort()
            0x01 -> charposY.toShort()
            0x02 -> {
                if(charposY in 0 until charHeight && charposX in 0 until charWidth) {
                    charMatrix[charposY][charposX]
                } else 0xff
            }
            0x03 -> (pixelX and 0xff).toShort()
            0x04 -> (pixelX ushr 8).toShort()
            0x05 -> (pixelY and 0xff).toShort()
            0x06 -> (pixelY ushr 8).toShort()
            0x07 -> if(host.getPixel(pixelX, pixelY)) 1 else 0
            0x08 -> cursorX.toShort()
            0x09 -> cursorY.toShort()
            0x0a -> {
                if(cursorY in 0 until charHeight && cursorX in 0 until charWidth) {
                    charMatrix[cursorY][cursorX]
                } else 0xff
            }
            else -> return 0xff
        }
    }

    override operator fun set(address: Address, data: UByte) {
        when(address-startAddress) {
            0x00 -> charposX = data.toInt()
            0x01 -> charposY = data.toInt()
            0x02 -> {
                if(charposY in 0 until charHeight && charposX in 0 until charWidth) {
                    charMatrix[charposY][charposX] = data
                    host.setChar(charposX, charposY, data.toChar())
                }
            }
            0x03 -> pixelX = (pixelX and 0xff00) or data.toInt()
            0x04 -> pixelX = (pixelX and 0x00ff) or (data.toInt() shl 8)
            0x05 -> pixelY = (pixelY and 0xff00) or data.toInt()
            0x06 -> pixelY = (pixelY and 0x00ff) or (data.toInt() shl 8)
            0x07 -> {
                if(pixelX in 0 until ScreenDefs.SCREEN_WIDTH && pixelY in 0 until ScreenDefs.SCREEN_HEIGHT) {
                    if (data == 0.toShort()) host.clearPixel(pixelX, pixelY)
                    else host.setPixel(pixelX, pixelY)
                }
            }
            0x08 -> cursorX = min(data.toInt() and 65535, charWidth-1)
            0x09 -> cursorY = min(data.toInt() and 65535, charHeight-1)
            0x0a -> {
                if(cursorY in 0 until charHeight && cursorX in 0 until charWidth) {
                    when(data.toInt()) {
                        0x08 -> {
                            // backspace
                            cursorX--
                            if(cursorX<0) {
                                if(cursorY>0) {
                                    cursorY--
                                    cursorX = charWidth - 1
                                }
                            }
                            charMatrix[cursorY][cursorX] = ' '.toShort()
                            host.setChar(cursorX, cursorY, ' ')
                        }
                        0x09 -> {
                            // tab
                            cursorX = (cursorX and 248) + 8
                            if(cursorX >= charWidth) {
                                cursorX = 0
                                cursorDown()
                            }
                        }
                        0x0a -> {
                            // newline
                            cursorX = 0
                            cursorDown()
                        }
                        0x0c -> reset()         // clear screen
                        0x0d -> cursorX = 0     // carriage return
                        else -> {
                            // set character on screen
                            charMatrix[cursorY][cursorX] = data
                            host.setChar(cursorX, cursorY, data.toChar())
                            cursorX++
                            if(cursorX >= charWidth) {
                                cursorX = 0
                                cursorDown()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun cursorDown() {
        cursorY++
        while(cursorY >= charHeight) {
            // scroll up 1 line
            for(y in 0 .. charHeight-2) {
                charMatrix[y+1].copyInto(charMatrix[y])
            }
            for(x in 0 until charWidth) {
                charMatrix[charHeight-1][x] = ' '.toShort()
            }
            cursorY--
            host.scrollUp()
        }
    }
}
