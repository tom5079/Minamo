package xyz.quaver.minamo

import java.awt.Image
import java.awt.image.BufferedImage

actual class MinamoNativeImage(
    val image: Image
) : AutoCloseable {
    override fun close() {}
}

actual fun MinamoNativeImage.pixelAt(x: Int, y: Int): Int {
    return when (image) {
        is BufferedImage -> image.getRGB(x, y)
        else -> throw UnsupportedOperationException("unsupported image type: ${image::class.simpleName}")
    }
}

class MinamoImageImpl internal constructor(
    override var vipsImage: VipsImagePtr = 0L
) : VipsImage {

    internal constructor(source: ImageSource) : this() {
        val vipsImage = load(source.vipsSource)
        check(vipsImage != 0L) { "failed to decode image" }
    }

    init {
        System.loadLibrary("minamo")
    }

    external override fun hasAlpha(): Result<Boolean>
    external override fun size(): Result<MinamoSize>
    external override fun decode(rect: MinamoRect): Result<MinamoNativeImage>
    external override fun resize(scale: Float): Result<MinamoImage>
    external override fun subsample(xFactor: Int, yFactor: Int): Result<MinamoImage>

    external override fun sink(
        tileSize: MinamoSize,
        maxTiles: Int,
        priority: Int,
        notify: (MinamoImage, MinamoRect) -> Unit
    ): Result<Pair<MinamoImage, MinamoImage>>

    external override fun copy(): Result<MinamoImage>

    private external fun load(image: VipsImagePtr): VipsImagePtr
    external override fun close()
    override fun hashCode(): Int {
        return vipsImage.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return vipsImage == (other as? MinamoImageImpl)?.vipsImage
    }
}