package razorvine.fonts

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.GZIPInputStream


class PsfFont(name: String) {

    // font format info: https://www.win.tue.nl/~aeb/linux/kbd/font-formats-1.html

    val numChars: Int
    val bytesPerChar: Int
    val height: Int
    val width: Int
    private val hasUnicodeTable: Boolean
    private val rawBitmaps: List<ByteArray>

    init {
        val data: ByteArray
        val fontsDirectory = "/usr/share/kbd/consolefonts"
        var stream = javaClass.getResourceAsStream("/charset/$name.psfu.gz") ?:
                     javaClass.getResourceAsStream("/charset/$name.psf.gz") ?:
                     (if(File("$fontsDirectory/$name.psfu.gz").exists()) FileInputStream("$fontsDirectory/$name.psfu.gz") else null ) ?:
                     (if(File("$fontsDirectory/$name.psf.gz").exists()) FileInputStream("$fontsDirectory/$name.psf.gz") else null ) ?:
                     (if(File("$fontsDirectory/$name.fnt.gz").exists()) FileInputStream("$fontsDirectory/$name.fnt.gz") else null )
        if(stream==null) {
            stream = javaClass.getResourceAsStream("/charset/$name.psfu") ?:
                     javaClass.getResourceAsStream("/charset/$name.psf") ?:
                     (if(File("$fontsDirectory/$name.psfu").exists()) FileInputStream("$fontsDirectory/$name.psfu") else null ) ?:
                     (if(File("$fontsDirectory/$name.psf").exists()) FileInputStream("$fontsDirectory/$name.psf") else null ) ?:
                     (if(File("$fontsDirectory/$name.fnt").exists()) FileInputStream("$fontsDirectory/$name.fnt") else null ) ?:
                     throw IOException("no such font: $name")
            data = stream.readBytes()
        } else {
            GZIPInputStream(stream).use { data = it.readBytes() }
        }
        stream.close()

        if (data[0] == 0x36.toByte() && data[1] == 0x04.toByte()) {
            // continue reading PSF1 font
            val mode = data[2].toInt()
            numChars = if (mode and 1 != 0) 512 else 256
            bytesPerChar = data[3].toInt()
            hasUnicodeTable = mode and 2 != 0
            height = bytesPerChar
            width = 8
            rawBitmaps = (0..numChars).map {
                data.sliceArray(3+it*bytesPerChar..3+(it+1)*bytesPerChar)
            }
            // ignore unicode table for now: val table = stream.readAllBytes()
        } else {
            if (data[0] == 0x72.toByte() && data[1] == 0xb5.toByte() && data[2] == 0x4a.toByte() && data[3] == 0x86.toByte()) {
                // continue reading PSF2 font
                // skip the version  val version = makeInt(data, 4)
                val headersize = makeInt(data, 8)
                val flags = makeInt(data, 12)
                hasUnicodeTable = flags and 1 != 0
                numChars = makeInt(data, 16)
                bytesPerChar = makeInt(data, 20)
                height = makeInt(data, 24)
                width = makeInt(data, 28)
                rawBitmaps = (0..numChars).map {
                    data.sliceArray(headersize+it*bytesPerChar..headersize+(it+1)*bytesPerChar)
                }
            } else {
                hasUnicodeTable = false
                numChars = 0
                bytesPerChar = 0
                height = 0
                width = 0
                rawBitmaps = emptyList()
            }
        }
    }

    fun convertToImage(gfx: Graphics2D, textColor: Color): BufferedImage {
        // create a single image with all the characters in a vertical column from top to bottom.
        val bitmap = gfx.deviceConfiguration.createCompatibleImage((width+7) and 0b11111000, height*numChars, Transparency.BITMASK)
        val bytesHoriz = (width+7)/8
        val color = textColor.rgb
        val nopixel = Color(0, 0, 0, 0).rgb
        for (char in 0 until numChars) {
            for (b in 0 until bytesPerChar) {
                val c = rawBitmaps[char][b].toInt()
                val ix = 8*(b%bytesHoriz)
                val iy = b/bytesHoriz+char*height
                bitmap.setRGB(ix, iy, if (c and 0b10000000 != 0) color else nopixel)
                bitmap.setRGB(ix+1, iy, if (c and 0b01000000 != 0) color else nopixel)
                bitmap.setRGB(ix+2, iy, if (c and 0b00100000 != 0) color else nopixel)
                bitmap.setRGB(ix+3, iy, if (c and 0b00010000 != 0) color else nopixel)
                bitmap.setRGB(ix+4, iy, if (c and 0b00001000 != 0) color else nopixel)
                bitmap.setRGB(ix+5, iy, if (c and 0b00000100 != 0) color else nopixel)
                bitmap.setRGB(ix+6, iy, if (c and 0b00000010 != 0) color else nopixel)
                bitmap.setRGB(ix+7, iy, if (c and 0b00000001 != 0) color else nopixel)
            }
        }
        return bitmap
    }

    private fun makeInt(bytes: ByteArray, offset: Int) =
            makeInt(bytes[offset], bytes[offset+1], bytes[offset+2], bytes[offset+3])
    private fun makeInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte) =
            b0.toInt() or (b1.toInt() shl 8) or (b2.toInt() shl 16) or (b3.toInt() shl 24)
}



//    private fun loadFallbackCharacters(): Array<BufferedImage> {
//        val img = ImageIO.read(javaClass.getResourceAsStream("/charset/unscii8x16.png"))
//        val charactersImage = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
//        charactersImage.createGraphics().drawImage(img, 0, 0, null)
//
//        val black = Color(0, 0, 0).rgb
//        val foreground = FG_COLOR.rgb
//        val nopixel = Color(0, 0, 0, 0).rgb
//        for (y in 0 until charactersImage.height) {
//            for (x in 0 until charactersImage.width) {
//                val col = charactersImage.getRGB(x, y)
//                if (col == black) charactersImage.setRGB(x, y, nopixel)
//                else charactersImage.setRGB(x, y, foreground)
//            }
//        }
//
//        val numColumns = charactersImage.width/8
//        val charImages = (0..255).map {
//            val charX = it%numColumns
//            val charY = it/numColumns
//            charactersImage.getSubimage(charX*8, charY*16, 8, 16)
//        }
//
//        return charImages.toTypedArray()
//    }
