package xyz.quaver.graphics.subsampledimage

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

typealias ScaleType = (canvasSize: Size, imageSize: Size) -> Rect

object ScaleTypes {
    val CENTER_INSIDE: ScaleType = { (canvasWidth, canvasHeight), (imageWidth, imageHeight) ->
        val imageAspectRatio = imageWidth / imageHeight
        val canvasAspectRatio = canvasWidth / canvasHeight

        if (canvasAspectRatio > imageAspectRatio) // Canvas is wider than the image; Fit height
            Rect(
                Offset(
                    (canvasWidth - imageAspectRatio * canvasHeight) / 2,
                    0F
                ),
                Size(imageAspectRatio * canvasHeight, canvasHeight)
            )
        else // Canvas is narrower than or the same as the image; Fit width
            Rect(
                Offset(
                    0F,
                    (canvasHeight - canvasWidth / imageAspectRatio) / 2
                ),
                Size(canvasWidth, canvasWidth / imageAspectRatio)
            )
    }

    val CENTER: ScaleType = { (canvasWidth, canvasHeight), (imageWidth, imageHeight) ->
        Rect(
            Offset(
                canvasWidth - imageWidth, canvasHeight - imageHeight
            ),
            Size(
                imageWidth, imageHeight
            )
        )
    }

    val FIT_XY: ScaleType = { canvasSize, _ ->
        Rect(Offset.Zero, canvasSize)
    }
}
