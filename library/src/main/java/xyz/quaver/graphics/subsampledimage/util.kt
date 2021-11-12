package xyz.quaver.graphics.subsampledimage

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

fun calculateSampleSize(scale: Float): Int {
    var sampleSize = 1

    while (scale <= 1f / (sampleSize * 2)) sampleSize *= 2

    return sampleSize
}

fun getMaxSampleSize(canvasSize: Size, imageSize: Size): Int {
    val minScale =
        min(canvasSize.width / imageSize.width, canvasSize.height / imageSize.height)
    return calculateSampleSize(minScale)
}

fun Offset.toIntOffset() = IntOffset(this.x.toInt(), this.y.toInt())
fun Size.toIntSize() = IntSize(this.width.toInt(), this.height.toInt())