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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

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

    val FIT_WIDTH: ScaleType = { (canvasWidth, _), (imageWidth, imageHeight) ->
        Rect(
            Offset.Zero,
            Size(canvasWidth, imageHeight * canvasWidth / imageWidth)
        )
    }

    val FIT_XY: ScaleType = { canvasSize, _ ->
        Rect(Offset.Zero, canvasSize)
    }
}
