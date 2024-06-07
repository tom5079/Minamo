@file:JvmName("CommonMinamoImage")

package xyz.quaver.minamo

import kotlin.math.roundToInt

typealias VipsImagePtr = Long
typealias VipsRegionPtr = Long

expect class MinamoNativeImage

expect fun MinamoNativeImage.pixelAt(x: Int, y: Int): Int

data class MinamoSize(
    val width: Int,
    val height: Int
) {
    companion object {
        val Zero = MinamoSize(0, 0)
    }
}

operator fun MinamoSize.times(scale: Float) = MinamoSize((width * scale).roundToInt(), (height * scale).roundToInt())

data class MinamoOffset(
    val x: Float,
    val y: Float
) {
    companion object {
        val Zero = MinamoOffset(0f, 0f)
    }
}

data class MinamoIntOffset(
    val x: Int,
    val y: Int
) {
    companion object {
        val Zero = MinamoIntOffset(0, 0)
    }
}

operator fun MinamoIntOffset.plus(other: MinamoIntOffset) = MinamoIntOffset(x + other.x, y + other.y)

data class MinamoRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    constructor(offset: MinamoIntOffset, size: MinamoSize) : this(offset.x, offset.y, size.width, size.height)
}

val MinamoRect.left: Int
    get() = x

val MinamoRect.top: Int
    get() = y

val MinamoRect.right: Int
    get() = x + width

val MinamoRect.bottom: Int
    get() = y + height

val MinamoRect.center: MinamoIntOffset
    get() = MinamoIntOffset(x + width / 2, y + height / 2)

fun MinamoRect.translate(offset: MinamoIntOffset) = MinamoRect(x + offset.x, y + offset.y, width, height)
fun MinamoRect.translate(x: Int, y: Int) = MinamoRect(this.x + x, this.y + y, width, height)
fun MinamoRect.scale(scale: Float, origin: MinamoIntOffset = this.center) = MinamoRect(
    ((x - origin.x) * scale + origin.x).roundToInt(),
    ((y - origin.y) * scale + origin.y).roundToInt(),
    (width * scale).roundToInt(),
    (height * scale).roundToInt()
)

infix fun MinamoRect.overlaps(other: MinamoRect): Boolean {
    return x < other.right && right > other.x && y < other.bottom && bottom > other.y
}

interface MinamoImage : AutoCloseable {
    val hasAlpha: Boolean
    val height: Int
    val width: Int

    val size: MinamoSize
        get() = MinamoSize(width, height)

    fun decode(rect: MinamoRect = MinamoRect(0, 0, width, height)): MinamoNativeImage?
    fun resize(scale: Float): MinamoImage
    fun subsample(xFactor: Int, yFactor: Int = xFactor): MinamoImage

    fun sink(
        tileSize: MinamoSize,
        maxTiles: Int,
        priority: Int,
        notify: (MinamoImage, MinamoRect) -> Unit
    ): Pair<MinamoImage, MinamoImage>
}

internal interface VipsImage : MinamoImage {
    val vipsImage: VipsImagePtr
}