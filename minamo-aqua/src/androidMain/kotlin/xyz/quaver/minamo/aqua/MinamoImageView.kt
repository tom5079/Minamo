package xyz.quaver.minamo.aqua

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceView
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.quaver.minamo.MinamoImage

class MinamoImageView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : SurfaceView(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
        private val coroutineContext = newSingleThreadContext("minamo_aqua_render_thread")
        private val mutex = Mutex()
    }

    private fun repaint() = CoroutineScope(coroutineContext).launch {
        mutex.withLock {
            val canvas = run {
                var canvas = holder.lockCanvas()

                while (canvas == null) {
                    delay(100)
                    canvas = holder.lockCanvas()
                }

                canvas
            }

            val paint = Paint().apply {
                isAntiAlias = true
                color = Color.RED
                style = Paint.Style.STROKE
            }

            canvas.drawRect(0f, 0f, 100f, 100f, paint)

            holder.unlockCanvasAndPost(canvas)
        }
    }

    fun setImage(image: MinamoImage) {
        repaint()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        repaint()
    }

}