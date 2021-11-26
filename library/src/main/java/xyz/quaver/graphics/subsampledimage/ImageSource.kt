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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toAndroidRect
import java.io.InputStream

interface ImageSource {
    val imageSize: Size
    fun decodeRegion(region: Rect, sampleSize: Int): ImageBitmap
}

@Composable
fun rememberByteArrayImageSource(image: ByteArray) = remember {
    object: ImageSource {
        private val decoder = newBitmapRegionDecoder(image, 0, image.size)

        override val imageSize = Size(decoder.width.toFloat(), decoder.height.toFloat())

        override fun decodeRegion(region: Rect, sampleSize: Int) =
            decoder.decodeRegion(region.toAndroidRect(), BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }).asImageBitmap()

    }
}

@Composable
fun rememberInputStreamImageSource(inputStream: InputStream) = remember {
    object: ImageSource {
        private val decoder = newBitmapRegionDecoder(inputStream)

        override val imageSize = Size(decoder.width.toFloat(), decoder.height.toFloat())

        override fun decodeRegion(region: Rect, sampleSize: Int) =
            decoder.decodeRegion(region.toAndroidRect(), BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }).asImageBitmap()
    }
}