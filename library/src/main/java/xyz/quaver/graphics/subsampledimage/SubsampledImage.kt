package xyz.quaver.graphics.subsampledimage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import androidx.compose.animation.core.*
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.kodein.log.Logger
import org.kodein.log.frontend.defaultLogFrontend
import java.util.concurrent.Executors
import kotlin.math.min
import kotlin.math.roundToInt

private val logger = Logger(
    tag = Logger.Tag("xyz.quaver.graphics.subsampledimage.SubsampledImage", "SubsampledImage"),
    frontEnds = listOf(defaultLogFrontend)
)

@Composable
fun rememberSubsampledImageState() = remember {
    SubsampledImageState()
}

class SubsampledImageState {
    /**
     * Represents the area the image will occupy in canvas's coordinate
     */
    var imageRect by mutableStateOf<Rect?>(null)

    private var zoomAnimationJob: Job? = null
    /**
     * Enlarge [imageRect] by [amount] centered around [centroid]
     *
     * For example, [amount] 0.2 inflates [imageRect] by 20%
     *              [amount] -0.2 deflates [imageRect] by 20%
     */
    suspend fun zoom(amount: Float, centroid: Offset, isAnimated: Boolean = false) = coroutineScope {
        zoomAnimationJob?.cancelAndJoin()

        zoomAnimationJob = launch {
            imageRect?.let { imageRect ->
                val animationSpec: AnimationSpec<Float> = if (isAnimated) spring() else snap()

                val anim = AnimationState(
                    initialValue = 0f,
                    initialVelocity = 0f
                )

                anim.animateTo(
                    targetValue = amount,
                    animationSpec = animationSpec,
                    sequentialAnimation = false
                ) {
                    if (!this@launch.isActive) cancelAnimation()

                    this@SubsampledImageState.imageRect = Rect(
                        imageRect.left + (imageRect.left - centroid.x) * value,
                        imageRect.top + (imageRect.top - centroid.y) * value,
                        imageRect.right + (imageRect.right - centroid.x) * value,
                        imageRect.bottom + (imageRect.bottom - centroid.y) * value
                    )
                }
            }
        }
    }
}

/**
 * [rect] is in image's coordinate
 */
internal data class Tile(
    val rect: Rect,
    val sampleSize: Int
) {
    companion object {
        private val tileLoadCoroutineScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    }

    private val mutex = Mutex()
    private var loadingJob: Job? = null
    var bitmap by mutableStateOf<ImageBitmap?>(null)

    suspend fun load(decoder: BitmapRegionDecoder) = coroutineScope {
        loadingJob?.cancel()

        if (bitmap != null) return@coroutineScope

        loadingJob = tileLoadCoroutineScope.launch {
            val time = System.currentTimeMillis()

            logger.debug {
                "Loading Bitmap on ${Thread.currentThread().name}"
            }

            decoder.decodeRegion(rect.toAndroidRect(), BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }).asImageBitmap().let {
                if (!isActive) return@let
                mutex.withLock {
                    if (!isActive) return@withLock
                    bitmap = it
                }
                logger.debug { "Finished loading bitmap in ${System.currentTimeMillis() - time} ms" }
            }
        }
    }

    suspend fun unload() {
        loadingJob?.cancel()

        mutex.withLock {
            bitmap = null
        }
    }
}

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

@Preview
@Composable
fun SubsampledImage(
    modifier: Modifier = Modifier,
    image: ByteArray? = null,
    state: SubsampledImageState = rememberSubsampledImageState(),
    scaleType: ScaleType = ScaleTypes.CENTER_INSIDE
) {
    val coroutineScope = rememberCoroutineScope()

    var canvasSize by remember { mutableStateOf<Size?>(null) }

    val decoder = remember(image) {
        image?.let { image ->
            BitmapRegionDecoder.newInstance(image, 0, image.size, false)
        }
    }

    val imageSize by produceState<Size?>(null, image) {
        value = image?.let { image ->
            withContext(Dispatchers.Unconfined) {
                with(BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }) {
                    BitmapFactory.decodeByteArray(image, 0, image.size, this)

                    logger.debug {
                        "Image Size ($outWidth, $outHeight)"
                    }

                    Size(outWidth.toFloat(), outHeight.toFloat())
                }
            }
        }
    }

    if (state.imageRect == null)
        LaunchedEffect(canvasSize, imageSize) {
            canvasSize?.let { canvasSize ->
                imageSize?.let { imageSize ->
                    logger.debug {
                        "initializing imageRect"
                    }
                    state.imageRect = scaleType.invoke(canvasSize, imageSize)
                }
            }
        }

    // Bitmap of whole image with lower resolution that acts like a base layer
    val baseTile: ImageBitmap? = remember(canvasSize, imageSize) {
        canvasSize?.let { canvasSize ->
        imageSize?.let { imageSize ->
            decoder?.decodeRegion(Rect(Offset(0f, 0f), imageSize).toAndroidRect(), BitmapFactory.Options().apply {
                inSampleSize = getMaxSampleSize(canvasSize, imageSize)
            })?.asImageBitmap()
        } }
    }

    val tiles by produceState<List<Tile>?>(null, imageSize, state.imageRect?.size) {
        logger.info {
            "imageRect size: ${state.imageRect?.size}"
        }
        canvasSize?.let { canvasSize ->
        state.imageRect?.let { imageRect ->
        imageSize?.let { imageSize ->
            val targetScale =
                min(imageRect.width / imageSize.width, imageRect.height / imageSize.height)

            val sampleSize = calculateSampleSize(targetScale)

            if (value?.firstOrNull()?.sampleSize == sampleSize) return@produceState

            val maxSampleSize = getMaxSampleSize(canvasSize, imageSize)

            logger.debug {
                """
                tiles
                TargetRect $imageRect
                TargetScale $targetScale
                SampleSize $sampleSize
                MaxSampleSize $maxSampleSize
                """.trim()
            }

            value = mutableListOf<Tile>().apply {
                val tileWidth = imageSize.width * sampleSize / maxSampleSize
                val tileHeight = imageSize.height * sampleSize / maxSampleSize

                var y = 0f

                while (y < imageSize.height) {
                    var x = 0f
                    while (x < imageSize.width) {
                        add(
                            Tile(
                                Rect(
                                    Offset(x, y),
                                    Size(
                                        if (x + tileWidth > imageSize.width) imageSize.width - x else tileWidth,
                                        if (y + tileHeight > imageSize.height) imageSize.height - y else tileHeight
                                    )
                                ),
                                sampleSize
                            )
                        )
                        x += tileWidth
                    }
                    y += tileHeight
                }

            }.toList()
        } } }
    }

    LaunchedEffect(state.imageRect) {
        imageSize?.let { imageSize ->
        canvasSize?.let { canvasSize ->
        decoder?.let { decoder ->
        state.imageRect?.let { imageRect ->
            val canvasRect = Rect(
                Offset(0f, 0f),
                canvasSize
            )

            tiles?.forEach { tile ->
                // use baseTile if available
                if (tile.sampleSize == getMaxSampleSize(canvasSize, imageSize)) return@forEach

                val widthRatio = imageRect.width / imageSize.width
                val heightRatio = imageRect.height / imageSize.height

                val tileRect = Rect(
                    Offset(
                        imageRect.left + tile.rect.left * widthRatio,
                        imageRect.top + tile.rect.top * heightRatio
                    ),
                    Size(
                        tile.rect.width * widthRatio,
                        tile.rect.height * heightRatio
                    )
                )

                if (canvasRect.overlaps(tileRect)) tile.load(decoder) else tile.unload()
            }
        } } } }
    }

    val flingSpec = rememberSplineBasedDecay<Float>()

    Canvas(
        modifier
            .pointerInput(Unit) {
                var lastDrag = Offset.Zero
                var lastDragTime = System.currentTimeMillis()
                var lastDragPeriod = 1L

                detectDragGestures(onDragEnd = {
                    // Prevent lastDragPeriod = 0
                    lastDragPeriod += 1

                    logger.debug {
                        "dragend with D: ${lastDrag.getDistance()} P: $lastDragPeriod ms}"
                    }
                    coroutineScope.launch {
                        var lastValue = 0f
                        val flingDistance = lastDrag.getDistance()
                        val flingVector = lastDrag / flingDistance
                        AnimationState(
                            initialValue = 0f,
                            initialVelocity = flingDistance / lastDragPeriod * 1000
                        ).animateDecay(flingSpec) {
                            val delta = value - lastValue
                            logger.debug {
                                "fling $delta"
                            }
                            state.imageRect?.let {
                                state.imageRect = it.translate(flingVector * delta)
                            }
                            lastValue = value
                        }
                    }
                }) { change, dragAmount ->
                    state.imageRect?.let {
                        change.consumeAllChanges()
                        state.imageRect = it.translate(dragAmount)

                        lastDrag = dragAmount
                        val time = System.currentTimeMillis()
                        lastDragPeriod = time - lastDragTime
                        lastDragTime = time
                    }
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    state.imageRect?.let {
                        logger.info {
                            """
                                transformGestures
                                centroid $centroid
                                pan $pan
                                zoom $zoom
                            """.trimIndent()
                        }
                        state.imageRect = Rect(
                            it.left + pan.x + (it.left - centroid.x) * (zoom - 1),
                            it.top + pan.y + (it.top - centroid.y) * (zoom - 1),
                            it.right + pan.x + (it.right - centroid.x) * (zoom - 1),
                            it.bottom + pan.y + (it.bottom - centroid.y) * (zoom - 1)
                        )
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { centroid ->
                    coroutineScope.launch {
                        state.zoom(1f, centroid, true)
                    }
                })
            }
    ) {
        if (size.width != 0F && size.height != 0F)
            canvasSize = size.copy()

        logger.debug {
            "Canvas Size $canvasSize"
        }

        imageSize?.let { imageSize ->
        state.imageRect?.let { imageRect ->
            tiles?.forEach { tile ->
                val widthRatio = imageRect.width / imageSize.width
                val heightRatio = imageRect.height / imageSize.height

                val tileRect = Rect(
                    Offset(
                        imageRect.left + tile.rect.left * widthRatio,
                        imageRect.top + tile.rect.top * heightRatio
                    ),
                    Size(
                        tile.rect.width * widthRatio,
                        tile.rect.height * heightRatio
                    )
                )

                tile.bitmap?.let { bitmap ->
                    drawImage(
                        bitmap,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(bitmap.width, bitmap.height),
                        dstOffset = tileRect.topLeft.toIntOffset(),
                        dstSize = tileRect.size.toIntSize()
                    )
                } ?: baseTile?.let { baseTile ->
                    val baseTileRect = Rect(
                        tile.rect.left / imageSize.width * baseTile.width,
                        tile.rect.top / imageSize.height * baseTile.height,
                        tile.rect.right / imageSize.width * baseTile.width,
                        tile.rect.bottom / imageSize.height * baseTile.height,
                    )

                    logger.debug {
                        "baseTileRect $baseTileRect"
                    }

                    drawImage(
                        baseTile,
                        srcOffset = baseTileRect.topLeft.toIntOffset(),
                        srcSize = baseTileRect.size.toIntSize(),
                        dstOffset = tileRect.topLeft.toIntOffset(),
                        dstSize = tileRect.size.toIntSize()
                    )
                }

                drawRect(
                    Color.Cyan,
                    tileRect.topLeft,
                    tileRect.size,
                    style = Stroke(width = 5f)
                )
            }
        } }
    }
}

