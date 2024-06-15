package xyz.quaver.minamo.aqua

import xyz.quaver.minamo.MinamoIntOffset
import xyz.quaver.minamo.MinamoOffset
import xyz.quaver.minamo.MinamoSize
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

typealias Bound = (offset: MinamoIntOffset, scale: Float, imageSize: MinamoSize, canvasSize: MinamoSize) -> Pair<MinamoIntOffset, Float>

object Bounds {
    val NO_BOUND: Bound = { offset, scale, _, _ -> offset to scale }

    val FORCE_OVERLAP: Bound = { offset, scale, (imageWidth, imageHeight), (canvasWidth, canvasHeight) ->
        val minScale =
            maxOf(canvasWidth / imageWidth.toFloat(), canvasHeight / imageHeight.toFloat())

        val newScale = scale.coerceAtLeast(minScale)
        val newOffset = MinamoIntOffset(
            min(
                max(offset.x + imageWidth * (newScale - scale) / 2, canvasWidth - imageWidth * newScale),
                0f
            ).roundToInt(),
            min(
                max(offset.y + imageHeight * (newScale - scale) / 2, canvasHeight - imageHeight * newScale),
                0f
            ).roundToInt()
        )

        newOffset to newScale
    }

    val FORCE_OVERLAP_OR_CENTER: Bound =
        bound@{ offset, scale, (imageWidth, imageHeight), (canvasWidth, canvasHeight) ->
            val thresh = max(canvasWidth / imageWidth.toFloat(), canvasHeight / imageHeight.toFloat())
            val minScale = min(canvasWidth / imageWidth.toFloat(), canvasHeight / imageHeight.toFloat())

            val newOffset = MinamoIntOffset(
                x = min(max(offset.x, (canvasWidth - imageWidth * scale).roundToInt()), 0),
                y = min(max(offset.y, (canvasHeight - imageHeight * scale).roundToInt()), 0)
            )

            when {
                scale > thresh -> newOffset to scale
                scale < minScale -> {
                    val center = MinamoOffset(canvasWidth / 2f, canvasHeight / 2f)

                    MinamoIntOffset(
                        (center.x - imageWidth * minScale / 2).roundToInt(),
                        (center.y - imageHeight * minScale / 2).roundToInt()
                    ) to minScale
                }

                else -> {
                    val center = MinamoOffset(canvasWidth / 2f, canvasHeight / 2f)

                    val canvasAspectRatio = canvasWidth.toFloat() / canvasHeight
                    val imageAspectRatio = imageWidth.toFloat() / imageHeight

                    if (canvasAspectRatio > imageAspectRatio) {
                        newOffset.copy(x = (center.x - imageWidth * scale / 2).roundToInt())
                    } else {
                        newOffset.copy(y = (center.y - imageHeight * scale / 2).roundToInt())
                    } to scale
                }
            }
        }
}