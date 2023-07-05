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
        private set

    var imageSize by mutableStateOf<Size?>(null)
        internal set

    /**
     * Image decoded as a lowest possible size that fits canvasSize to serve as a base layer
     */
    var baseTile by mutableStateOf<ImageBitmap?>(null)
        internal set

    var tiles by mutableStateOf<List<Tile>?>(null)
        internal set

    /**
     * Represents the area the image will occupy in canvas's coordinate
     */
    var imageRect by mutableStateOf<Rect?>(null)
        private set

    /**
     * Returns pair of width scale and height scale
     */
    val scale by derivedStateOf {
        imageSize?.let { imageSize ->
        imageRect?.let { imageRect ->
            imageRect.width / imageSize.width to imageRect.height / imageSize.height
        } }
    }

    internal fun setCanvasSizeWithBound(size: Size) {
        imageRectAnimationJob?.cancel()

        canvasSize = size
        imageRect = imageRect?.let { imageRect ->
            bound(imageRect, size)
        }
    }

    private var imageRectAnimationJob: Job? = null

    fun setImageRectWithBound(rect: Rect) {
        imageRectAnimationJob?.cancel()
        canvasSize?.let { canvasSize ->
            imageRect = bound(rect, canvasSize)
        }
    }

    /**
     * Sets rect as a new imageRect with animation
     * Does nothing when canvasSize is not initialized
     * Does not animate when imageRect is not initialized
     */
    suspend fun setImageRectWithBound(rect: Rect, animationSpec: AnimationSpec<Rect>) = coroutineScope {
        imageRectAnimationJob?.cancel()
        imageRectAnimationJob = launch {
            canvasSize?.let { canvasSize ->
            imageRect?.let { imageRect ->
                val animation = AnimationState(
                    Rect.VectorConverter,
                    imageRect
                )

                animation.animateTo(
                    targetValue = rect,
                    animationSpec = animationSpec
                ) {
                    if (!isActive) cancelAnimation()

                    this@SubSampledImageState.imageRect = bound(value, canvasSize)
                }
            } }
        }
    }

    var isGestureEnabled by mutableStateOf(false)

    fun resetImageRect() {
        imageSize?.let { imageSize ->
        canvasSize?.let { canvasSize ->
            setImageRectWithBound(scaleType(canvasSize, imageSize))
        } }
    }

    suspend fun resetImageRect(animationSpec: AnimationSpec<Rect>) {
        imageSize?.let { imageSize ->
        canvasSize?.let { canvasSize ->
            setImageRectWithBound(scaleType(canvasSize, imageSize), animationSpec)
        } }
    }
}