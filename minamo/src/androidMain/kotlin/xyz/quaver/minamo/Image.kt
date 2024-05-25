package xyz.quaver.minamo

import android.graphics.Bitmap

actual class MinamoNativeImage(
    val bitmap: Bitmap
)

class MinamoImageImpl internal constructor(
    source: ImageSource
) : VipsImage {
    private var _vipsImage: VipsImagePtr
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

    init {
        System.loadLibrary("minamo")
        _vipsImage = load(source.vipsSource)
        check(_vipsImage != 0L) { "failed to decode image" }
    }

    external override fun decode(rect: MinamoRect): MinamoNativeImage
    external override fun resize(scale: Double): MinamoImage

    private external fun hasAlpha(image: VipsImagePtr): Boolean
    private external fun getHeight(image: VipsImagePtr): Int
    private external fun getWidth(image: VipsImagePtr): Int

    private external fun load(image: VipsImagePtr): VipsImagePtr
    external override fun close()
}