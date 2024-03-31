package xyz.quaver.minamo

typealias VipsImagePtr = Long

expect class MinamoImageRegion

interface MinamoImage : AutoCloseable {
    val hasAlpha: Boolean
    val height: Int
    val width: Int

    fun readRegion(
        startX: Int = 0,
        startY: Int = 0,
        width: Int = this.width,
        height: Int = this.height,
    ): MinamoImageRegion
}

internal interface VipsImage : MinamoImage {
    val vipsImage: VipsImagePtr
}