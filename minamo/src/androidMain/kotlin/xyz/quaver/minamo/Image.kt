package xyz.quaver.minamo

actual class MinamoImageRegion {

}

class MinamoImageImpl internal constructor(
    source: ImageSource
) : VipsImage {
    private var _vipsImage: VipsImagePtr? = null
    override val vipsImage: VipsImagePtr
        get() = _vipsImage ?: error("tried to access closed VipsImage")

    override val hasAlpha: Boolean
        get() = hasAlpha(vipsImage)
    override val height: Int
        get() = getHeight(vipsImage)
    override val width: Int
        get() = getWidth(vipsImage)

    init {
        System.loadLibrary("minamo")
        _vipsImage = load(source.vipsSource) ?: error("error decoding file")
    }

    override fun readRegion(
        startX: Int,
        startY: Int,
        width: Int,
        height: Int
    ): MinamoImageRegion {
        return MinamoImageRegion()
    }

    private external fun readPixels(
        buffer: ByteArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        bufferOffset: Int,
        stride: Int
    ): Int

    private external fun hasAlpha(image: VipsImagePtr): Boolean
    private external fun getHeight(image: VipsImagePtr): Int
    private external fun getWidth(image: VipsImagePtr): Int

    private external fun load(image: VipsImagePtr): VipsImagePtr?
    external override fun close()
}