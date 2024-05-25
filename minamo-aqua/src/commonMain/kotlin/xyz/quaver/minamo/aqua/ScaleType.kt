package xyz.quaver.minamo.aqua

import xyz.quaver.minamo.MinamoIntOffset
import xyz.quaver.minamo.MinamoOffset
import xyz.quaver.minamo.MinamoRect
import xyz.quaver.minamo.MinamoSize
import kotlin.math.max
import kotlin.math.roundToInt

typealias ScaleType = (canvasSize: MinamoSize, imageSize: MinamoSize) -> ImageRect

object ScaleTypes {
    val CENTER_INSIDE: ScaleType = { (canvasWidth, canvasHeight), (imageWidth, imageHeight) ->
        val imageAspectRatio = imageWidth / imageHeight.toFloat()
        val canvasAspectRatio = canvasWidth / canvasHeight.toFloat()

        if (canvasAspectRatio > imageAspectRatio) { // Canvas is wider than the image; Fit height
            val targetHeight = (imageAspectRatio * canvasWidth).roundToInt()
            ImageRect(
                MinamoIntOffset(
                    (canvasWidth - targetHeight) / 2,
                    0
                ),
                MinamoSize(targetHeight, canvasHeight)
            )
        } else { // Canvas is narrower than or the same as the image; Fit width
            val targetWidth = (canvasHeight / imageAspectRatio).roundToInt()
            ImageRect(
                MinamoIntOffset(
                    0,
                    (canvasHeight - targetWidth) / 2
                ),
                MinamoSize(canvasWidth, targetWidth)
            )
        }
    }

    val CENTER_CROP: ScaleType = { (canvasWidth, canvasHeight), (imageWidth, imageHeight) ->
        val scale = max(canvasWidth / imageWidth.toFloat(), canvasHeight / imageHeight.toFloat())

        val scaledWidth = (scale * imageWidth).roundToInt()
        val scaledHeight = (scale * imageHeight).roundToInt()

        MinamoRect(
            MinamoIntOffset(
                (canvasWidth - scaledWidth) / 2, (canvasHeight - scaledHeight) / 2
            ),
            MinamoSize(
                scaledWidth, scaledHeight
            )
        )
    }

    val CENTER: ScaleType = { (canvasWidth, canvasHeight), (imageWidth, imageHeight) ->
        MinamoRect(
            MinamoIntOffset(
                canvasWidth - imageWidth, canvasHeight - imageHeight
            ),
            MinamoSize(
                imageWidth, imageHeight
            )
        )
    }

    val FIT_WIDTH: ScaleType = { (canvasWidth, _), (imageWidth, imageHeight) ->
        MinamoRect(
            MinamoIntOffset.Zero,
            MinamoSize(canvasWidth, imageHeight * canvasWidth / imageWidth)
        )
    }

    val FIT_HEIGHT: ScaleType = { (_, canvasHeight), (imageWidth, imageHeight) ->
        MinamoRect(
            MinamoIntOffset.Zero,
            MinamoSize(imageWidth * canvasHeight / imageHeight, canvasHeight)
        )
    }

    val FIT_XY: ScaleType = { canvasSize, _ ->
        MinamoRect(MinamoIntOffset.Zero, canvasSize)
    }
}