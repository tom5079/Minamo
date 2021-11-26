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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toAndroidRect
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.kodein.log.Logger
import org.kodein.log.LoggerFactory
import org.kodein.log.newLogger
import java.util.concurrent.Executors

/**
 * [rect] is in image's coordinate
 */
data class Tile(
    val rect: Rect,
    val sampleSize: Int
) {
    private val logger = Logger.newLogger(LoggerFactory.default)

    companion object {
        private val tileLoadCoroutineScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    }

    private val mutex = Mutex()
    private var loadingJob: Job? = null
    var bitmap by mutableStateOf<ImageBitmap?>(null)

    suspend fun load(imageSource: ImageSource) = coroutineScope {
        loadingJob?.cancel()

        if (bitmap != null) return@coroutineScope

        loadingJob = tileLoadCoroutineScope.launch {
            val time = System.currentTimeMillis()

            logger.debug {
                "Loading Bitmap on ${Thread.currentThread().name}"
            }

            imageSource.decodeRegion(rect, sampleSize).let {
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
