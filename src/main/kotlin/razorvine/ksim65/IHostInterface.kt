package razorvine.ksim65

/**
 * The virtual machine uses this to interface with the host system,
 * to connect to the "real" devices.
 */
interface IHostInterface {
    fun clearScreen()
    fun getPixel(x: Int, y: Int): Boolean
    fun setPixel(x: Int, y: Int)
    fun clearPixel(x: Int, y: Int)
    fun setChar(x: Int, y: Int, character: Char)
    fun scrollUp()
    fun mouse(): MouseInfo
    fun keyboard(): Char?

    class MouseInfo(val x: Int, val y: Int, val left: Boolean, val right: Boolean, val middle: Boolean)
}
