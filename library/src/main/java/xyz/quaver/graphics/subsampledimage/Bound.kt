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
import androidx.compose.ui.geometry.center
import org.kodein.log.LoggerFactory
import org.kodein.log.newLogger
import kotlin.math.max
import kotlin.math.min

typealias Bound = (imageRect: Rect, canvasSize: Size) -> Rect

/**
 * Rules:
 * 1. Don't change the aspect ratio of imageRect
 */
object Bounds {

    private val logger = newLogger(LoggerFactory.default)

    val NO_BOUND: Bound = { imageRect, _ -> imageRect }

    val FORCE_OVERLAP: Bound = { imageRect, canvasSize ->
        val zoom = max(max(canvasSize.width / imageRect.width, canvasSize.height / imageRect.height), 1f)
        val center = canvasSize.center
        var rect = Rect(
            Offset(
                center.x - (center.x - imageRect.left) * zoom,
                center.y - (center.y - imageRect.top) * zoom
            ),
            imageRect.size * zoom
        )

        if (rect.left > 0f)
            rect = rect.translate(-rect.left, 0f)
        if (rect.top > 0f)
            rect = rect.translate(0f, -rect.top)
        if (rect.right < canvasSize.width)
            rect = rect.translate(canvasSize.width - rect.right, 0f)
        if (rect.bottom < canvasSize.height)
            rect = rect.translate(0f, canvasSize.height - rect.bottom)

        rect
    }

    val FORCE_OVERLAP_OR_CENTER: Bound = { imageRect, canvasSize ->
        val zoom = max(min(canvasSize.width / imageRect.width, canvasSize.height / imageRect.height), 1f)
        val center = canvasSize.center
        var rect = Rect(
            Offset(
                center.x - (center.x - imageRect.left) * zoom,
                center.y - (center.y - imageRect.top) * zoom
            ),
            imageRect.size * zoom
        )

        val isWidthSmaller = rect.width < canvasSize.width
        val isHeightSmaller = rect.height < canvasSize.height

        if (isWidthSmaller)
            rect = Rect(
                Offset(
                    center.x - rect.width / 2,
                    rect.top
                ), rect.size
            )

        if (isHeightSmaller)
            rect = Rect(
                Offset(
                    rect.left,
                    center.y - rect.height / 2
                ), rect.size
            )

        if (rect.left > 0f && !isWidthSmaller)
            rect = rect.translate(-rect.left, 0f)
        if (rect.top > 0f && !isHeightSmaller)
            rect = rect.translate(0f, -rect.top)
        if (rect.right < canvasSize.width && !isWidthSmaller)
            rect = rect.translate(canvasSize.width - rect.right, 0f)
        if (rect.bottom < canvasSize.height && !isHeightSmaller)
            rect = rect.translate(0f, canvasSize.height - rect.bottom)

        rect
    }
}