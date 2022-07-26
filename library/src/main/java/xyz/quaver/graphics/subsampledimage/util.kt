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

import android.graphics.BitmapRegionDecoder
import android.os.Build
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeConsumed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.*
import java.io.InputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
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

fun newBitmapRegionDecoder(data: ByteArray, offset: Int, length: Int) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        BitmapRegionDecoder.newInstance(data, offset, length)
    else
        BitmapRegionDecoder.newInstance(data, offset, length, false)

fun newBitmapRegionDecoder(pathName: String) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        BitmapRegionDecoder.newInstance(pathName)
    else
        BitmapRegionDecoder.newInstance(pathName, false)

fun newBitmapRegionDecoder(`is`: InputStream) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        BitmapRegionDecoder.newInstance(`is`)!!
    else
        BitmapRegionDecoder.newInstance(`is`, false)!!

private var flingJob: Job? = null
suspend fun PointerInputScope.detectGesturesAndFling(coroutineScope: CoroutineScope, onGesture: (Offset, Offset, Float, Float) -> Boolean, onFling: suspend CoroutineScope.(Offset, Long) -> Unit) {
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

                            if (onGesture(centroid, panChange, zoomChange, rotationChange) || zoomChange != 1f) {
                                event.changes.fastForEach {
                                    it.consumeAllChanges()
                                }
                            }

                            lastDrag = panChange
                            val time = System.currentTimeMillis()
                            lastDragPeriod = time - lastDragTime
                            lastDragTime = time
                        }

                        if (!event.changes.fastAny { it.pressed } && zoomChange == 1f) {
                            flingJob = coroutineScope.launch { onFling.invoke(coroutineScope, lastDrag, lastDragPeriod) }
                        }
                    }
                }
            } while (!canceled && event.changes.fastAny { it.pressed })
        }
    }
}

fun Modifier.wrapContent(
    state: SubSampledImageState
) = then(
    state.imageSize?.let { imageSize ->
        Modifier.aspectRatio(imageSize.width / imageSize.height)
    } ?: Modifier
)

fun Modifier.doubleClickCycleZoom(
    state: SubSampledImageState,
    scale: Float = 2f,
    animationSpec: AnimationSpec<Rect> = spring()
) = composed {
    val initialImageRect by produceState<Rect?>(null, state.canvasSize, state.imageSize) {
        state.canvasSize?.let { canvasSize ->
            state.imageSize?.let { imageSize ->
                value = state.bound(state.scaleType(canvasSize, imageSize), canvasSize)
            } }
    }

    val coroutineScope = rememberCoroutineScope()

    pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = { centroid ->
                val imageRect = state.imageRect
                coroutineScope.launch {
                    if (imageRect == null || imageRect != initialImageRect)
                        state.resetImageRect(animationSpec)
                    else {
                        state.setImageRectWithBound(
                            Rect(
                                Offset(
                                    centroid.x - (centroid.x - imageRect.left) * scale,
                                    centroid.y - (centroid.y - imageRect.top) * scale
                                ),
                                imageRect.size * scale
                            ), animationSpec
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun Modifier.doubleClickContinuousZoom(
    state: SubSampledImageState,
    scale: Float = 2f,
    animationSpec: AnimationSpec<Rect> = spring()
) = composed {
    val coroutineScope = rememberCoroutineScope()

    pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = { centroid ->
                state.imageRect?.let { imageRect ->
                    coroutineScope.launch {
                        state.setImageRectWithBound(
                            Rect(
                                Offset(
                                    centroid.x - (centroid.x - imageRect.left) * scale,
                                    centroid.y - (centroid.y - imageRect.top) * scale
                                ),
                                imageRect.size * scale
                            ), animationSpec
                        )
                    }
                }
            }
        )
    }
}