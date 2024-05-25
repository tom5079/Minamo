package xyz.quaver.minamo

import kotlin.math.roundToInt

typealias VipsImagePtr = Long
typealias VipsRegionPtr = Long

expect class MinamoNativeImage

data class MinamoSize(
    val width: Int,
    val height: Int
) {
    companion object {
        val Zero = MinamoSize(0, 0)
    }
}

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

data class MinamoRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    constructor(offset: MinamoIntOffset, size: MinamoSize) : this(offset.x, offset.y, size.width, size.height)
}

interface MinamoImage : AutoCloseable {
    val hasAlpha: Boolean
    val height: Int
    val width: Int

    val size: MinamoSize
        get() = MinamoSize(width, height)

    fun decode(rect: MinamoRect = MinamoRect(0, 0, width, height)): MinamoNativeImage
    fun resize(scale: Double): MinamoImage
}

internal interface VipsImage : MinamoImage {
    val vipsImage: VipsImagePtr
}