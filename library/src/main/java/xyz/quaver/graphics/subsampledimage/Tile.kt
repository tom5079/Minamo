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
