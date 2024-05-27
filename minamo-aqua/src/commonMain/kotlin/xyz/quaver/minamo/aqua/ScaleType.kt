package xyz.quaver.minamo.aqua

import xyz.quaver.minamo.MinamoIntOffset
import xyz.quaver.minamo.MinamoSize
import kotlin.math.max
import kotlin.math.roundToInt

typealias ScaleType = (canvasSize: MinamoSize, imageSize: MinamoSize) -> Pair<MinamoIntOffset, Float>

object ScaleTypes {
    val CENTER_INSIDE: ScaleType = { (canvasWidth, canvasHeight), (imageWidth, imageHeight) ->
        val imageAspectRatio = imageWidth / imageHeight.toFloat()
        val canvasAspectRatio = canvasWidth / canvasHeight.toFloat()

        if (canvasAspectRatio > imageAspectRatio) { // Canvas is wider than the image; Fit height
            val targetWidth = (imageAspectRatio * canvasHeight).roundToInt()
            MinamoIntOffset(
                (canvasWidth - targetWidth) / 2,
                0
            ) to targetWidth / imageWidth.toFloat()
        } else { // Canvas is narrower than or the same as the image; Fit width
            val targetHeight = (canvasWidth / imageAspectRatio).roundToInt()
            MinamoIntOffset(
                0,
                (canvasHeight - targetHeight) / 2
            ) to targetHeight / imageHeight.toFloat()
        }
    }

    val CENTER_CROP: ScaleType = { (canvasWidth, canvasHeight), (imageWidth, imageHeight) ->
        val scale = max(canvasWidth / imageWidth.toFloat(), canvasHeight / imageHeight.toFloat())

        val scaledWidth = (scale * imageWidth).roundToInt()
        val scaledHeight = (scale * imageHeight).roundToInt()

        MinamoIntOffset(
            (canvasWidth - scaledWidth) / 2, (canvasHeight - scaledHeight) / 2
        ) to scale
    }

    val CENTER: ScaleType = { (canvasWidth, canvasHeight), (imageWidth, imageHeight) ->
        MinamoIntOffset(
            canvasWidth - imageWidth, canvasHeight - imageHeight
        ) to 1.0f
    }

    val FIT_WIDTH: ScaleType = { (canvasWidth, _), (imageWidth, imageHeight) ->
        MinamoIntOffset.Zero to canvasWidth / imageWidth.toFloat()
    }

    val FIT_HEIGHT: ScaleType = { (_, canvasHeight), (imageWidth, imageHeight) ->
        MinamoIntOffset.Zero to canvasHeight / imageHeight.toFloat()
    }
}