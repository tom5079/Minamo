package xyz.quaver.graphics.subsampledimage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.log.Logger
import org.kodein.log.frontend.defaultLogFrontend
import kotlin.math.roundToInt

private val logger = Logger(
    tag = Logger.Tag("xyz.quaver.graphics.subsampledimage.SubsampledImage", "SubsampledImage"),
    frontEnds = listOf(defaultLogFrontend)
)



@Composable
fun rememberSubsampledImageState() = remember {
    SubsampledImageState()
}

class SubsampledImageState() {
    var scale by mutableStateOf(1F)
    var offset by mutableStateOf(Offset(0F, 0F))
}

@Preview
@Composable
fun SubsampledImage(
    modifier: Modifier = Modifier,
    image: ByteArray? = null,
    state: SubsampledImageState = rememberSubsampledImageState()
) {
    var canvasSize by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    val imageSize by produceState<Pair<Float, Float>?>(null, image) {
        value = image?.let { image ->
            withContext(Dispatchers.Unconfined) {
                with(BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }) {
                    BitmapFactory.decodeByteArray(image, 0, image.size, this)

                    logger.debug {
                        "Image Size ($outWidth, $outHeight)"
                    }

                    outWidth.toFloat() to outHeight.toFloat()
                }
            }
        }
    }

    val fitSizeAndOffset by remember(image, imageSize, canvasSize) { derivedStateOf {
        imageSize?.let { (imageWidth, imageHeight) ->
        canvasSize?.let { (canvasWidth, canvasHeight) ->
            val imageAspectRatio = imageWidth / imageHeight
            val canvasAspectRatio = canvasWidth / canvasHeight

            if (canvasAspectRatio > imageAspectRatio) // Canvas is wider than the image; Fit height
                (imageAspectRatio * canvasHeight to canvasHeight) to ((canvasWidth - imageAspectRatio * canvasHeight) / 2 to 0F)
            else // Canvas is narrower than or the same as the image; Fit width
                (canvasWidth to canvasWidth / imageAspectRatio) to (0F to (canvasHeight - canvasWidth / imageAspectRatio) / 2)
        } }.also {
            logger.debug {
                "FitSizeAndOffset $it"
            }
        }
    } }

    val bitmap by produceState<Bitmap?>(null) {
        value = image?.let { image ->
        fitSizeAndOffset?.let { (fitSize, _) ->
            withContext(Dispatchers.Unconfined) {
                with(BitmapFactory.Options().apply {
                    inSampleSize = (imageSize!!.first / fitSize.first).roundToInt().also {
                        logger.debug {
                            "InSampleSize $it"
                        }
                    }
                }) {
                    BitmapFactory.decodeByteArray(image, 0, image.size, this)
                }
            }
        } }.also {
            it?.let {
                logger.debug {
                    "Bitmap Size (${it.width}, ${it.height})"
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        if (size.width != 0F && size.height != 0F)
            canvasSize = size.width to size.height

        logger.debug {
            "Canvas Size $canvasSize"
        }

        bitmap?.let { bitmap ->
        fitSizeAndOffset?.let { (fitSize, fitOffset) ->
            drawImage(
                bitmap.asImageBitmap(),
                dstOffset = IntOffset(fitOffset.first.roundToInt(), fitOffset.second.roundToInt()),
                dstSize = IntSize(fitSize.first.roundToInt(), fitSize.second.roundToInt())
            )

            logger.debug {
                "Image Drawn"
            }
        } }
    }
}

