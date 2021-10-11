package xyz.quaver.graphics.subsampledimage.sample

import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.kodein.log.LoggerFactory
import org.kodein.log.newLogger
import java.text.NumberFormat
import kotlin.system.measureTimeMillis

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    val logger = newLogger(LoggerFactory.default)

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("xyz.quaver.graphics.subsampledimage.sample", appContext.packageName)
    }

    @Test
    fun initializeDecoder() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val numberFormat = NumberFormat.getNumberInstance()

        appContext.resources.assets.open("card.png").use { `is` ->
            var decoder: BitmapRegionDecoder
            measureTimeMillis {
                decoder = BitmapRegionDecoder.newInstance(`is`, false) ?: error("")
            }.let {
                logger.info { "INIT $it ms" }
            }

            var rect = Rect(0, 0, decoder.width, decoder.height)

            while (rect.right > 0 && rect.bottom > 0) {
                measureTimeMillis {
                    decoder.decodeRegion(rect, BitmapFactory.Options()).allocationByteCount
                }.let {
                    val px = rect.right * rect.bottom

                    if (it != 0L)
                        logger.info { "DECODE ${rect.right}x${rect.bottom}\t(${numberFormat.format(px)} px)\t$it ms\t(${px/it} px/ms)" }
                }

                rect.right /= 2
                rect.bottom /= 2
            }
        }
    }
}