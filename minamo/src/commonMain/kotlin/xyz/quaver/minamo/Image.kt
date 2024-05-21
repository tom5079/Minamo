package xyz.quaver.minamo

typealias VipsImagePtr = Long
typealias VipsRegionPtr = Long

expect class MinamoNativeImage

data class MinamoRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

interface MinamoImage : AutoCloseable {
    val hasAlpha: Boolean
    val height: Int
    val width: Int

    fun image(rect: MinamoRect = MinamoRect(0, 0, width, height)): MinamoNativeImage
}

internal interface VipsImage : MinamoImage {
    val vipsImage: VipsImagePtr
}