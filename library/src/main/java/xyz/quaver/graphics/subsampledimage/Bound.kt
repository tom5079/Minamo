package xyz.quaver.graphics.subsampledimage

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import kotlin.math.max
import kotlin.math.min

typealias Bound = (imageRect: Rect, canvasSize: Size) -> Rect

/**
 * Rules:
 * 1. Don't change the aspect ratio of imageRect
 */
object Bounds {
    val NO_BOUND: Bound = { imageRect, _ -> imageRect }

    val FORCE_OVERLAP: Bound = { imageRect, canvasSize ->
        val zoom = max(max(canvasSize.width / imageRect.width, canvasSize.height / imageRect.height), 1f)
        val center = canvasSize.center
        var rect = Rect(
            Offset(
                center.x - (center.x - imageRect.left) * zoom,
                center.y - (center.y - imageRect.top) * zoom
            ),
            imageRect.size * zoom
        )

        if (rect.left > 0f)
            rect = rect.translate(-rect.left, 0f)
        if (rect.top > 0f)
            rect = rect.translate(0f, -rect.top)
        if (rect.right < canvasSize.width)
            rect = rect.translate(canvasSize.width - rect.right, 0f)
        if (rect.bottom < canvasSize.height)
            rect = rect.translate(0f, canvasSize.height - rect.bottom)

        rect
    }

    val FORCE_OVERLAP_IF_POSSIBLE: Bound = { imageRect, canvasSize ->
        TODO("force minScale, when imageRect < canvasSize force center.")
    }
}