package xyz.quaver.minamo.aqua

import xyz.quaver.minamo.MinamoIntOffset
import xyz.quaver.minamo.MinamoSize
import xyz.quaver.minamo.plus

typealias Bound = (offset: MinamoIntOffset, scale: Float, imageSize: MinamoSize, canvasSize: MinamoSize) -> Pair<MinamoIntOffset, Float>

object Bounds {
    val NO_BOUND: Bound = { offset, scale, _, _ -> offset to scale }

    val FORCE_OVERLAP: Bound = { offset, scale, (imageWidth, imageHeight), (canvasWidth, canvasHeight) ->
        val minScale =
            maxOf(canvasWidth / imageWidth.toFloat(), canvasHeight / imageHeight.toFloat())

        val newScale = scale.coerceAtLeast(minScale)
        var newOffset = offset + MinamoIntOffset(
            (imageWidth * (newScale - scale) / 2).toInt(),
            (imageHeight * (newScale - scale) / 2).toInt()
        )

        if (newOffset.x > 0) {
            newOffset = newOffset.copy(x = 0)
        }

        if (newOffset.y > 0) {
            newOffset = newOffset.copy(y = 0)
        }

        if (newOffset.x + imageWidth * newScale < canvasWidth) {
            newOffset = newOffset.copy(x = canvasWidth - (imageWidth * newScale).toInt())
        }

        if (newOffset.y + imageHeight * newScale < canvasHeight) {
            newOffset = newOffset.copy(y = canvasHeight - (imageHeight * newScale).toInt())
        }

        newOffset to newScale
    }

    
}