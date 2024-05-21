package xyz.quaver.minamo

import java.awt.image.BufferedImage

actual class MinamoNativeImage(
    val image: BufferedImage
)

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
        val vipsImage = load(source.vipsSource)

        check(vipsImage != 0L) { "failed to decode image" }

        _vipsImage = vipsImage
    }

    external override fun image(rect: MinamoRect): MinamoNativeImage
    private external fun hasAlpha(image: VipsImagePtr): Boolean
    private external fun getHeight(image: VipsImagePtr): Int
    private external fun getWidth(image: VipsImagePtr): Int

    private external fun load(image: VipsImagePtr): VipsImagePtr
    external override fun close()
}