package xyz.quaver.graphics.subsampledimage

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

fun calculateSampleSize(scale: Float): Int {
    var sampleSize = 1

    while (scale <= 1f / (sampleSize * 2)) sampleSize *= 2

    return sampleSize
}

fun Offset.toIntOffset() = IntOffset(this.x.toInt(), this.y.toInt())
fun Size.toIntSize() = IntSize(this.width.toInt(), this.height.toInt())