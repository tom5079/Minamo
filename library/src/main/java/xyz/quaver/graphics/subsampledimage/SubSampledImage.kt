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
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.*
import kotlin.math.min

@Composable
fun SubSampledImage(
    modifier: Modifier = Modifier,
    imageSource: ImageSource? = null,
    state: SubSampledImageState = rememberSubSampledImageState(),
    onError: (Throwable) -> Unit = { }
) {
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(imageSource) {
        launch (Dispatchers.Default) {
            state.imageSize = runCatching {
                imageSource?.imageSize
            }.onFailure(onError).getOrNull()
        }
    }

    if (state.imageRect == null)
        LaunchedEffect(state.canvasSize, state.imageSize) {
            launch (Dispatchers.Default) {
                state.resetImageRect()
            }
        }

    // Bitmap of whole image with lower resolution that acts like a base layer
    LaunchedEffect(state.canvasSize, state.imageSize) {
        state.canvasSize?.let { canvasSize ->
        state.imageSize?.let { imageSize ->
            launch (Dispatchers.Default) {
                state.baseTile = runCatching {
                    imageSource?.decodeRegion(
                        Rect(Offset(0f, 0f), imageSize),
                        getMaxSampleSize(canvasSize, imageSize)
                    )
                }.onFailure(onError).getOrNull()
            }
        } }
    }

    LaunchedEffect(state.imageSize, state.imageRect?.size) {
        state.canvasSize?.let { canvasSize ->
        state.imageRect?.let { imageRect ->
        state.imageSize?.let { imageSize ->
            val targetScale =
                min(imageRect.width / imageSize.width, imageRect.height / imageSize.height)

            val sampleSize = calculateSampleSize(targetScale)

            if (state.tiles?.firstOrNull()?.sampleSize == sampleSize) return@LaunchedEffect

            launch (Dispatchers.Default) {
                val maxSampleSize = getMaxSampleSize(canvasSize, imageSize)

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
            }
        } } }
    }

    LaunchedEffect(state.imageRect) {
        imageSource?.let { imageSource ->
        state.imageSize?.let { imageSize ->
        state.canvasSize?.let { canvasSize ->
        state.imageRect?.let { imageRect ->
            launch (Dispatchers.Default) {
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

                    if (canvasRect.overlaps(tileRect)) tile.load(imageSource, onError) else tile.unload()
                }
            }
        } } } }
    }

    val flingSpec = rememberSplineBasedDecay<Float>()

    val onGesture: (Offset, Offset, Float, Float) -> Boolean = { centroid, pan, zoom, _ ->
        state.imageRect?.let {
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

    val onFling: suspend CoroutineScope.(Offset, Long) -> Unit = { lastDrag, lastDragPeriod ->
        var lastValue = 0f
        val flingDistance = lastDrag.getDistance()
        val flingVector = lastDrag / flingDistance
        AnimationState(
            initialValue = 0f,
            initialVelocity = flingDistance / (lastDragPeriod+1) * 1000 // Prevent lastDragPeriod = 0
        ).animateDecay(flingSpec) {
            if (!isActive) cancelAnimation()

            val delta = value - lastValue
            state.imageRect?.let {
                state.setImageRectWithBound(it.translate(flingVector * delta))
            }
            lastValue = value
        }
    }

    Canvas(
        modifier
            .clipToBounds()
            .onGloballyPositioned {
                if (it.size.width != 0 && it.size.height != 0 && it.size.toSize() != state.canvasSize)
                    state.setCanvasSizeWithBound(it.size.toSize())
            }
            .run {
                if (state.isGestureEnabled)
                    pointerInput(Unit) {
                        detectGesturesAndFling(coroutineScope, onGesture, onFling)
                    }
                else
                    this
            }
    ) {
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

