/*
 * Copyright 2021 tom5079
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.quaver.graphics.subsampledimage

import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import androidx.compose.animation.core.*
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.*
import org.kodein.log.Logger
import org.kodein.log.frontend.defaultLogFrontend
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min

private val logger = Logger(
    tag = Logger.Tag("xyz.quaver.graphics.subsampledimage.SubsampledImage", "SubsampledImage"),
    frontEnds = listOf(defaultLogFrontend)
)

@Preview
@Composable
fun SubSampledImage(
    modifier: Modifier = Modifier,
    imageSource: ImageSource,
    state: SubSampledImageState = rememberSubSampledImageState()
) {
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(imageSource) {
        state.imageSize = imageSource.imageSize
    }

    if (state.imageRect == null)
        LaunchedEffect(state.canvasSize, state.imageSize) {
            logger.debug {
                "initializing imageRect"
            }

            state.resetImageRect()
        }

    // Bitmap of whole image with lower resolution that acts like a base layer
    LaunchedEffect(state.canvasSize, state.imageSize) {
        state.canvasSize?.let { canvasSize ->
        state.imageSize?.let { imageSize ->
            state.baseTile = imageSource.decodeRegion(Rect(Offset(0f, 0f), imageSize), getMaxSampleSize(canvasSize, imageSize))
        } }
    }

    LaunchedEffect(state.imageSize, state.imageRect?.size) {
        logger.info {
            "imageRect size: ${state.imageRect?.size}"
        }
        state.canvasSize?.let { canvasSize ->
        state.imageRect?.let { imageRect ->
        state.imageSize?.let { imageSize ->
            val targetScale =
                min(imageRect.width / imageSize.width, imageRect.height / imageSize.height)

            val sampleSize = calculateSampleSize(targetScale)

            if (state.tiles?.firstOrNull()?.sampleSize == sampleSize) return@LaunchedEffect

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

            state.tiles = mutableListOf<Tile>().apply {
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
        state.imageSize?.let { imageSize ->
        state.canvasSize?.let { canvasSize ->
        state.imageRect?.let { imageRect ->
            val canvasRect = Rect(
                Offset(0f, 0f),
                canvasSize
            )

            state.tiles?.forEach { tile ->
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

                if (canvasRect.overlaps(tileRect)) tile.load(imageSource) else tile.unload()
            }
        } } }
    }

    var flingJob: Job? = null
    val flingSpec = rememberSplineBasedDecay<Float>()

    val onGesture: (Offset, Offset, Float, Float) -> Boolean = { centroid, pan, zoom, _ ->
        state.imageRect?.let {
            logger.debug {
                """
                        transformGestures
                        centroid $centroid
                        pan $pan
                        zoom $zoom
                    """.trimIndent()
            }
            val rect = Rect(
                it.left + pan.x + (it.left - centroid.x) * (zoom - 1),
                it.top + pan.y + (it.top - centroid.y) * (zoom - 1),
                it.right + pan.x + (it.right - centroid.x) * (zoom - 1),
                it.bottom + pan.y + (it.bottom - centroid.y) * (zoom - 1)
            )

            state.setImageRectWithBound(rect)

            state.imageRect == rect
        } ?: false
    }

    Canvas(
        modifier
            .clipToBounds()
            .pointerInput(Unit) {
                forEachGesture {
                    awaitPointerEventScope {
                        var rotation = 0f
                        var zoom = 1f
                        var pan = Offset.Zero
                        var pastTouchSlop = false
                        val touchSlop = viewConfiguration.touchSlop

                        var lastDrag = Offset.Zero
                        var lastDragTime = System.currentTimeMillis()
                        var lastDragPeriod = 1L

                        awaitFirstDown(requireUnconsumed = false)
                        flingJob?.cancel()
                        do {
                            val event = awaitPointerEvent()
                            val canceled = event.changes.fastAny { it.positionChangeConsumed() }
                            if (!canceled) {
                                val zoomChange = event.calculateZoom()
                                val rotationChange = event.calculateRotation()
                                val panChange = event.calculatePan()

                                if (!pastTouchSlop) {
                                    zoom *= zoomChange
                                    rotation += rotationChange
                                    pan += panChange

                                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                    val zoomMotion = abs(1 - zoom) * centroidSize
                                    val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                                    val panMotion = pan.getDistance()

                                    if (zoomMotion > touchSlop ||
                                            rotationMotion > touchSlop ||
                                            panMotion > touchSlop
                                    ) {
                                        pastTouchSlop = true
                                    }
                                }

                                if (pastTouchSlop) {
                                    val centroid = event.calculateCentroid(useCurrent = false)
                                    if (rotationChange != 0f ||
                                            zoomChange != 1f ||
                                            panChange != Offset.Zero
                                    ) {
                                        if (onGesture(centroid, panChange, zoomChange, rotationChange)) {
                                            event.changes.fastForEach {
                                                it.consumeAllChanges()
                                            }
                                        }

                                        lastDrag = panChange
                                        val time = System.currentTimeMillis()
                                        lastDragPeriod = time - lastDragTime
                                        lastDragTime = time
                                    }

                                    if (event.changes.fastAll { !it.pressed } && event.calculateCentroidSize() > 0f) {
                                        // Prevent lastDragPeriod = 0
                                        lastDragPeriod += 1

                                        logger.debug {
                                            "dragend with D: ${lastDrag.getDistance()} P: $lastDragPeriod ms}"
                                        }
                                        flingJob = coroutineScope.launch {
                                            var lastValue = 0f
                                            val flingDistance = lastDrag.getDistance()
                                            val flingVector = lastDrag / flingDistance
                                            AnimationState(
                                                    initialValue = 0f,
                                                    initialVelocity = flingDistance / lastDragPeriod * 1000
                                            ).animateDecay(flingSpec) {
                                                if (!isActive) return@animateDecay

                                                val delta = value - lastValue
                                                logger.debug {
                                                    "fling $delta"
                                                }
                                                state.imageRect?.let {
                                                    state.setImageRectWithBound(it.translate(flingVector * delta))
                                                }
                                                lastValue = value
                                            }
                                        }
                                    }
                                }
                            }
                        } while (!canceled && event.changes.fastAny { it.pressed })
                    }
                }
            }.pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { centroid ->
                    coroutineScope.launch {
                        state.zoom(1f, centroid, true)
                    }
                })
            }
    ) {
        if (size.width != 0F && size.height != 0F)
            state.canvasSize = size.copy()

        logger.debug {
            "Canvas Size ${state.canvasSize}"
        }

        state.imageSize?.let { imageSize ->
        state.imageRect?.let { imageRect ->
            state.tiles?.forEach { tile ->
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
                } ?: state.baseTile?.let { baseTile ->
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
            }
        } }
    }
}

