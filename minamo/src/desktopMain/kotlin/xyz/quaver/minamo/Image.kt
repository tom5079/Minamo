package xyz.quaver.minamo

import java.awt.Image

actual class MinamoNativeImage(
    val image: Image
)

class MinamoImageImpl internal constructor(
    private var _vipsImage: VipsImagePtr = 0L
) : VipsImage {
    override val vipsImage: VipsImagePtr
        get() {
            check(_vipsImage != 0L) { "tried to access closed VipsImage" }
            return _vipsImage
        }

    override val hasAlpha: Boolean
        get() = hasAlpha(vipsImage)
    override val height: Int
        get() = getHeight(vipsImage)
    override val width: Int
        get() = getWidth(vipsImage)

    internal constructor(source: ImageSource) : this() {
        val vipsImage = load(source.vipsSource)
        check(vipsImage != 0L) { "failed to decode image" }
        _vipsImage = vipsImage
    }

    init {
        System.loadLibrary("minamo")
    }

    external override fun decode(rect: MinamoRect): MinamoNativeImage?
    external override fun decodeRaw(rect: MinamoRect): ByteArray?
    external override fun resize(scale: Float): MinamoImage

    private external fun hasAlpha(image: VipsImagePtr): Boolean
    private external fun getHeight(image: VipsImagePtr): Int
    private external fun getWidth(image: VipsImagePtr): Int

    private external fun load(image: VipsImagePtr): VipsImagePtr
    external override fun close()
}