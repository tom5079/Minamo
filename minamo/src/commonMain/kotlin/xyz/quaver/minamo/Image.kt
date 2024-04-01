package xyz.quaver.minamo

typealias VipsImagePtr = Long
typealias VipsRegionPtr = Long

expect class MinamoNativeImage

interface MinamoImageRegion {
    val height: Int
    val width: Int

    fun readImage(): MinamoNativeImage
}

interface MinamoImage : AutoCloseable {
    val hasAlpha: Boolean
    val height: Int
    val width: Int

    fun region(
        startX: Int = 0,
        startY: Int = 0,
        width: Int = this.width,
        height: Int = this.height,
    ): MinamoImageRegion
}

internal interface VipsRegion : MinamoImageRegion {
    val vipsRegion: VipsRegionPtr
}

internal interface VipsImage : MinamoImage {
    val vipsImage: VipsImagePtr
}