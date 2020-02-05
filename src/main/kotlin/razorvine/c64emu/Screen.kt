package razorvine.c64emu

import razorvine.ksim65.components.Address
import razorvine.ksim65.components.MemoryComponent
import razorvine.ksim65.components.Rom
import razorvine.ksim65.components.UByte
import java.awt.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import javax.swing.JPanel

/**
 * The rendering logic of the screen of the C64.
 * It supports: Character mode,
 * High res bitmap mode (320*200), Multicolor bitmap mode (160*200).
 * TODO: Multicolor character mode.   Extended background color mode.
 */
internal class Screen(private val chargen: Rom, val ram: MemoryComponent) : JPanel() {

    private val fullscreenImage: BufferedImage
    private val fullscreenG2d: Graphics2D
    private val normalCharacters = loadCharacters(false)
    private val shiftedCharacters = loadCharacters(true)

    init {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val gd = ge.defaultScreenDevice.defaultConfiguration
        fullscreenImage = gd.createCompatibleImage(ScreenDefs.SCREEN_WIDTH+2*ScreenDefs.BORDER_SIZE,
                                                   ScreenDefs.SCREEN_HEIGHT+2*ScreenDefs.BORDER_SIZE, Transparency.OPAQUE)
        fullscreenG2d = fullscreenImage.graphics as Graphics2D

        val size = Dimension(fullscreenImage.width*ScreenDefs.PIXEL_SCALING.toInt(),
                             (fullscreenImage.height*ScreenDefs.ASPECT_RATIO*ScreenDefs.PIXEL_SCALING).toInt())
        minimumSize = size
        maximumSize = size
        preferredSize = size
        isFocusable = true
        isDoubleBuffered = false
        requestFocusInWindow()
    }

    private fun loadCharacters(shifted: Boolean): Array<BufferedImage> {
        val chars = Array(256) { BufferedImage(8, 8, BufferedImage.TYPE_BYTE_BINARY) }
        val offset = if (shifted) 256*8 else 0
        for (char in 0..255) {
            for (line in 0..7) {
                val charbyte = chargen[offset+char*8+line].toInt()
                for (x in 0..7) {
                    if (charbyte and (0b10000000 ushr x) != 0) chars[char].setRGB(x, line, 0xffffff)
                }
            }
        }
        return chars
    }

    override fun paint(graphics: Graphics) {
        val windowG2d = graphics as Graphics2D
        val vicSCROLY = ram[0xd011].toInt()
        if (vicSCROLY and 0b10000 == 0) {
            // screen blanked, only display border color
            fullscreenG2d.background = ScreenDefs.colorPalette[ram[0xd020]]
            fullscreenG2d.clearRect(0, 0, ScreenDefs.SCREEN_WIDTH+2*ScreenDefs.BORDER_SIZE,
                                    ScreenDefs.SCREEN_HEIGHT+2*ScreenDefs.BORDER_SIZE)
        } else {
            val vicSCROLX = ram[0xd016].toInt()
            val vicVMCSB = ram[0xd018].toInt()
            val multiColorMode = vicSCROLX and 0b00010000 != 0
            val vicBank = when (ram[0xdd00].toInt() and 0b00000011) {
                0b00 -> 0xc000
                0b01 -> 0x8000
                0b10 -> 0x4000
                else -> 0x0000
            }

            if (vicSCROLY and 0b00100000 != 0) renderBitmapMode(vicBank, vicVMCSB, multiColorMode)
            else {
                fullscreenG2d.background = ScreenDefs.colorPalette[ram[0xd021]]
                fullscreenG2d.clearRect(ScreenDefs.BORDER_SIZE, ScreenDefs.BORDER_SIZE, ScreenDefs.SCREEN_WIDTH, ScreenDefs.SCREEN_HEIGHT)
                renderCharacterMode(vicBank, vicVMCSB, multiColorMode)
            }

            renderSprites(vicBank)
            renderBorder()
        }

        // scale and draw the image to the window, and simulate a slight scan-line effect
        windowG2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        windowG2d.drawImage(fullscreenImage, 0, 0, (fullscreenImage.width*ScreenDefs.PIXEL_SCALING).toInt(),
                            (fullscreenImage.height*ScreenDefs.ASPECT_RATIO*ScreenDefs.PIXEL_SCALING).toInt(), null)
        windowG2d.color = Color(0, 0, 0, 40)
        windowG2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        val width = fullscreenImage.width*ScreenDefs.PIXEL_SCALING.toInt()
        val height = (fullscreenImage.height*ScreenDefs.ASPECT_RATIO*ScreenDefs.PIXEL_SCALING).toInt()
        for (y in 0 until height step (ScreenDefs.ASPECT_RATIO*ScreenDefs.PIXEL_SCALING).toInt()) {
            windowG2d.drawLine(0, y, width, y)
        }
        Toolkit.getDefaultToolkit().sync()
    }

    private fun renderBorder() {
        // draw the screen border
        fullscreenG2d.background = ScreenDefs.colorPalette[ram[0xd020]]
        fullscreenG2d.clearRect(0, 0, ScreenDefs.SCREEN_WIDTH+2*ScreenDefs.BORDER_SIZE, ScreenDefs.BORDER_SIZE)
        fullscreenG2d.clearRect(0, ScreenDefs.SCREEN_HEIGHT+ScreenDefs.BORDER_SIZE, ScreenDefs.SCREEN_WIDTH+2*ScreenDefs.BORDER_SIZE,
                                ScreenDefs.BORDER_SIZE)
        fullscreenG2d.clearRect(0, ScreenDefs.BORDER_SIZE, ScreenDefs.BORDER_SIZE, ScreenDefs.SCREEN_HEIGHT)
        fullscreenG2d.clearRect(ScreenDefs.SCREEN_WIDTH+ScreenDefs.BORDER_SIZE, ScreenDefs.BORDER_SIZE, ScreenDefs.BORDER_SIZE,
                                ScreenDefs.SCREEN_HEIGHT)
    }

    private fun renderSprites(vicBank: Address) {
        // TODO sprite-background priorities
        // TODO multicolor sprites
        val spriteImage = fullscreenG2d.deviceConfiguration.createCompatibleImage(24, 21, Transparency.TRANSLUCENT)
        val spriteGfx = spriteImage.graphics
        spriteGfx.color = Color(255, 255, 255, 0)
        val spritePixels = (spriteImage.raster.dataBuffer as DataBufferInt).data
        val vicSPENA = ram[0xd015].toInt()
        val vicXXPAND = ram[0xd01d].toInt()
        val vicYXPAND = ram[0xd017].toInt()
        for (sprite in 7 downTo 0) {
            val bit = 1 shl sprite
            if (vicSPENA and bit != 0) {
                val mx = ram[0xd010].toInt() and (1 shl sprite) != 0
                val xpos = ram[0xd000+sprite*2].toInt()+if (mx) 256 else 0
                val ypos = ram[0xd001+sprite*2].toInt()
                if (xpos in 1..343 && ypos in 30..249) {
                    spriteGfx.fillRect(0, 0, 24, 21)
                    renderSprite(sprite, spritePixels, vicBank)
                    fullscreenG2d.drawImage(spriteImage, xpos+ScreenDefs.BORDER_SIZE-24, ypos+ScreenDefs.BORDER_SIZE-50,
                                            if (vicXXPAND and bit == 0) 24 else 48, if (vicYXPAND and bit == 0) 21 else 42, null)
                }
            }
        }
    }

    private fun renderSprite(sprite: Int, spritePixels: IntArray, vicBank: Address) {
        // note: the sprite pixels must all have been cleared to transparency already
        val sprptr = ram[vicBank+2040+sprite]*64
        val sprdata = ram.getBlock(sprptr, 21*3)
        val color = ScreenDefs.colorPalette[ram[0xd027+sprite]].rgb
        for (i in spritePixels.indices step 8) {
            val bits = sprdata[i/8].toInt()
            bits2Pixels(bits, spritePixels, i, color)
        }
    }

    private fun renderCharacterMode(vicBank: Address, vicVMCSB: Int, multiColorMode: Boolean) {
        if (multiColorMode) {
            TODO("multicolor character mode")
        } else {
            // normal character mode
            val screenAddress = vicBank+(vicVMCSB ushr 4) shl 10
            val charsetAddress = (vicVMCSB and 0b00001110) shl 10
            for (y in 0 until ScreenDefs.SCREEN_HEIGHT_CHARS) {
                for (x in 0 until ScreenDefs.SCREEN_WIDTH_CHARS) {
                    val char = ram[screenAddress+x+y*ScreenDefs.SCREEN_WIDTH_CHARS].toInt()
                    val color = ram[0xd800+x+y*ScreenDefs.SCREEN_WIDTH_CHARS].toInt()   // colors always at $d800
                    drawColoredChar(x, y, char, color, vicBank+charsetAddress)
                }
            }
        }
    }

    private fun renderBitmapMode(vicBank: Address, vicVMCSB: Int, multiColorMode: Boolean) {
        val bitmap = ram.getBlock(vicBank+if (vicVMCSB and 0b00001000 != 0) 32*256 else 0, 32*256)
        val colorBytes = ram.getBlock(vicBank+((vicVMCSB ushr 4) shl 2)*256, 4*256)
        val pixels: IntArray = (fullscreenImage.raster.dataBuffer as DataBufferInt).data
        val screenColor = ScreenDefs.colorPalette[ram[0xd021]].rgb
        if (multiColorMode) {
            // multicolor bitmap mode 160x200
            val fourColors = IntArray(4)
            fourColors[0b00] = screenColor
            for (y in 0 until ScreenDefs.SCREEN_HEIGHT) {
                for (x in 0 until ScreenDefs.SCREEN_WIDTH/2 step 4) {
                    val colorIdx = ScreenDefs.SCREEN_WIDTH_CHARS*(y ushr 3)+(x ushr 2)
                    fourColors[0b01] = ScreenDefs.colorPalette[colorBytes[colorIdx].toInt() ushr 4].rgb
                    fourColors[0b10] = ScreenDefs.colorPalette[colorBytes[colorIdx].toInt()].rgb
                    fourColors[0b11] = ScreenDefs.colorPalette[ram[0xd800+colorIdx].toInt()].rgb
                    draw4bitmapPixelsMc(pixels, x, y, bitmap, fourColors)
                }
            }
        } else {
            // bitmap mode 320x200
            for (y in 0 until ScreenDefs.SCREEN_HEIGHT) {
                for (x in 0 until ScreenDefs.SCREEN_WIDTH step 8) {
                    val colorIdx = ScreenDefs.SCREEN_WIDTH_CHARS*(y ushr 3)+(x ushr 3)
                    val bgColor = ScreenDefs.colorPalette[colorBytes[colorIdx].toInt()].rgb
                    val fgColor = ScreenDefs.colorPalette[colorBytes[colorIdx].toInt() ushr 4].rgb
                    draw8bitmapPixels(pixels, x, y, bitmap, fgColor, bgColor)
                }
            }
        }
    }

    private fun draw8bitmapPixels(pixels: IntArray, xstart: Int, y: Int, bitmap: Array<UByte>, fgColorRgb: Int, bgColorRgb: Int) {
        val offset = ScreenDefs.BORDER_SIZE+ScreenDefs.BORDER_SIZE*fullscreenImage.width+xstart+y*fullscreenImage.width
        val byte = bitmap[ScreenDefs.SCREEN_WIDTH_CHARS*(y and 248)+(y and 7)+(xstart and 504)].toInt()
        pixels[offset] = if (byte and 0b10000000 != 0) fgColorRgb else bgColorRgb
        pixels[offset+1] = if (byte and 0b01000000 != 0) fgColorRgb else bgColorRgb
        pixels[offset+2] = if (byte and 0b00100000 != 0) fgColorRgb else bgColorRgb
        pixels[offset+3] = if (byte and 0b00010000 != 0) fgColorRgb else bgColorRgb
        pixels[offset+4] = if (byte and 0b00001000 != 0) fgColorRgb else bgColorRgb
        pixels[offset+5] = if (byte and 0b00000100 != 0) fgColorRgb else bgColorRgb
        pixels[offset+6] = if (byte and 0b00000010 != 0) fgColorRgb else bgColorRgb
        pixels[offset+7] = if (byte and 0b00000001 != 0) fgColorRgb else bgColorRgb
    }

    private fun draw4bitmapPixelsMc(pixels: IntArray, xstart: Int, y: Int, bitmap: Array<UByte>, fourColors: IntArray) {
        val realx = xstart*2
        val offset = ScreenDefs.BORDER_SIZE+ScreenDefs.BORDER_SIZE*fullscreenImage.width+realx+y*fullscreenImage.width
        val byte = bitmap[ScreenDefs.SCREEN_WIDTH_CHARS*(y and 248)+(y and 7)+(realx and 504)].toInt()
        val colors = listOf(byte and 0b11000000 ushr 6, byte and 0b00110000 ushr 4, byte and 0b00001100 ushr 2, byte and 0b00000011)
        pixels[offset] = fourColors[colors[0]]
        pixels[offset+1] = fourColors[colors[0]]
        pixels[offset+2] = fourColors[colors[1]]
        pixels[offset+3] = fourColors[colors[1]]
        pixels[offset+4] = fourColors[colors[2]]
        pixels[offset+5] = fourColors[colors[2]]
        pixels[offset+6] = fourColors[colors[3]]
        pixels[offset+7] = fourColors[colors[3]]
    }

    private val coloredCharacterImageCache = mutableMapOf<Triple<Int, Int, Boolean>, BufferedImage>()

    private fun drawColoredChar(x: Int, y: Int, char: Int, color: Int, charsetAddr: Address) {
        // The vic 'sees' the charset ROM at these addresses: $1000(normal) + $1800(shifted), $9000(normal) + $9800(shifted)
        // so we can use pre-loaded images to efficiently draw the default ROM characters.
        // If the address is different, the vic takes charset data from RAM instead (thus allowing user defined charsets)
        // A user-supplied character set in RAM must begin at an address that is 2048 byte aligned,
        // and lies within the same 16K VIC bank as the screen character memory.
        when (charsetAddr) {
            0x1000, 0x1800, 0x9000, 0x9800 -> {
                val charImage = getCharFromROM(char, color, charsetAddr and 0x0800 != 0)
                fullscreenG2d.drawImage(charImage, x*8+ScreenDefs.BORDER_SIZE, y*8+ScreenDefs.BORDER_SIZE, null)
            }
            else -> {
                val charImg = getCharFromRAM(char, color, charsetAddr)
                fullscreenG2d.drawImage(charImg, x*8+ScreenDefs.BORDER_SIZE, y*8+ScreenDefs.BORDER_SIZE, null)
            }
        }
    }

    private val userCharacter = fullscreenG2d.deviceConfiguration.createCompatibleImage(8, 8, BufferedImage.BITMASK)

    private fun getCharFromRAM(char: Int, color: Int, charsetAddr: Address): BufferedImage {
        val chardefBytes = ram.getBlock(charsetAddr+char*8, 8)
        val charPixels = (userCharacter.raster.dataBuffer as DataBufferInt).data
        val rgb = ScreenDefs.colorPalette[color].rgb
        val gfx = userCharacter.graphics
        gfx.color = ScreenDefs.colorPalette[ram[0xd021]]
        gfx.fillRect(0, 0, 8, 8)
        for (y in 0..7) {
            val bits = chardefBytes[y].toInt()
            bits2Pixels(bits, charPixels, y*8, rgb)
        }
        return userCharacter
    }

    private fun bits2Pixels(bits: Int, bitmap: IntArray, yoffset: Int, colorRgb: Int) {
        if (bits and 0b10000000 != 0) bitmap[yoffset] = colorRgb
        if (bits and 0b01000000 != 0) bitmap[yoffset+1] = colorRgb
        if (bits and 0b00100000 != 0) bitmap[yoffset+2] = colorRgb
        if (bits and 0b00010000 != 0) bitmap[yoffset+3] = colorRgb
        if (bits and 0b00001000 != 0) bitmap[yoffset+4] = colorRgb
        if (bits and 0b00000100 != 0) bitmap[yoffset+5] = colorRgb
        if (bits and 0b00000010 != 0) bitmap[yoffset+6] = colorRgb
        if (bits and 0b00000001 != 0) bitmap[yoffset+7] = colorRgb
    }

    private fun getCharFromROM(char: Int, color: Int, shifted: Boolean): BufferedImage {
        val key = Triple(char, color, shifted)

        fun makeCachedImage(): BufferedImage {
            val monoImg = if (shifted) shiftedCharacters[char] else normalCharacters[char]
            val coloredImg = fullscreenG2d.deviceConfiguration.createCompatibleImage(8, 8, BufferedImage.BITMASK)
            val srcPixel = IntArray(4)
            val rgbs = IntArray(4)
            val javaColor = ScreenDefs.colorPalette[color]
            rgbs[0] = javaColor.red
            rgbs[1] = javaColor.green
            rgbs[2] = javaColor.blue
            rgbs[3] = javaColor.alpha
            val sourceRaster = monoImg.raster
            val coloredRaster = coloredImg.raster
            for (pixelY in 0..7) {
                for (pixelX in 0..7) {
                    val source = sourceRaster.getPixel(pixelX, pixelY, srcPixel)
                    if (source[0] != 0) {
                        coloredRaster.setPixel(pixelX, pixelY, rgbs)
                    }
                }
            }
            coloredCharacterImageCache[key] = coloredImg
            return coloredImg
        }

        return coloredCharacterImageCache[key] ?: makeCachedImage()
    }
}
