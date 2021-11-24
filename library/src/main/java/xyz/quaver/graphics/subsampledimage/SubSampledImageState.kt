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
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.*

@Composable
fun rememberSubSampledImageState(scaleType: ScaleType = ScaleTypes.CENTER_INSIDE, bound: Bound = Bounds.FORCE_OVERLAP_OR_CENTER) = remember {
    SubSampledImageState(scaleType, bound)
}

class SubSampledImageState(var scaleType: ScaleType, var bound: Bound) {
    var canvasSize by mutableStateOf<Size?>(null)
        internal set

    var imageSize by mutableStateOf<Size?>(null)
        internal set

    /**
     * Image decoded as a lowest possible size that fits canvasSize to serve as a base layer
     */
    var baseTile: ImageBitmap? = null
        internal set

    var tiles by mutableStateOf<List<Tile>?>(null)
        internal set

    /**
     * Represents the area the image will occupy in canvas's coordinate
     */
    var imageRect by mutableStateOf<Rect?>(null)
        private set

    fun setImageRectWithBound(rect: Rect) {
        zoomAnimationJob?.cancel()

        canvasSize?.let { canvasSize ->
            imageRect = bound(rect, canvasSize)
        }
    }

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

                    this@SubSampledImageState.imageRect = Rect(
                        imageRect.left + (imageRect.left - centroid.x) * value,
                        imageRect.top + (imageRect.top - centroid.y) * value,
                        imageRect.right + (imageRect.right - centroid.x) * value,
                        imageRect.bottom + (imageRect.bottom - centroid.y) * value
                    )
                }
            }
        }
    }

    fun resetImageRect() {
        imageSize?.let { imageSize ->
        canvasSize?.let { canvasSize ->
            setImageRectWithBound(scaleType.invoke(canvasSize, imageSize))
        } }
    }
}