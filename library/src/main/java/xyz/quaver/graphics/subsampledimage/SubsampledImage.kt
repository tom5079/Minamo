package xyz.quaver.graphics.subsampledimage

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun SubsampledImage(
    modifier: Modifier = Modifier,
    image: ByteArray? = null
) {
    Canvas(modifier = modifier) {
        drawImage(SubsampledImageBitmap(ByteArray(0)))
    }
}

