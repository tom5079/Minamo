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

@Suppress("INAPPLICABLE_JVM_NAME")
class MinamoImageImpl internal constructor(
    override var vipsImage: VipsImagePtr = 0L
) : VipsImage {

    internal constructor(source: ImageSource) : this() {
        vipsImage = load(source.vipsSource).getOrThrow()
    }

    init {
        System.loadLibrary("minamo")
    }

    @JvmName("hasAlpha")
    external override fun hasAlpha(): Result<Boolean>

    @JvmName("size")
    external override fun size(): Result<MinamoSize>

    @JvmName("decode")
    external override fun decode(rect: MinamoRect): Result<MinamoNativeImage>

    @JvmName("resize")
    external override fun resize(scale: Float): Result<MinamoImage>

    @JvmName("subsample")
    external override fun subsample(xFactor: Int, yFactor: Int): Result<MinamoImage>

    @JvmName("sink")
    external override fun sink(
        tileSize: MinamoSize,
        maxTiles: Int,
        priority: Int,
        notify: (MinamoImage, MinamoRect) -> Unit
    ): Result<Pair<MinamoImage, MinamoImage>>

    @JvmName("copy")
    external override fun copy(): Result<MinamoImage>

    @JvmName("load")
    private external fun load(image: VipsImagePtr): Result<VipsImagePtr>

    @JvmName("close")
    external override fun close()
    override fun hashCode(): Int {
        return vipsImage.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return vipsImage == (other as? MinamoImageImpl)?.vipsImage
    }
}