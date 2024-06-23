package xyz.quaver.minamo.aqua

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.util.AttributeSet
import android.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import xyz.quaver.minamo.MinamoImage
import xyz.quaver.minamo.MinamoIntOffset
import xyz.quaver.minamo.MinamoRect
import xyz.quaver.minamo.MinamoSize
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.roundToInt

private inline fun <R> SurfaceHolder.useCanvas(block: (Canvas) -> R) {
    val canvas = lockCanvas() ?: return
    block(canvas)
    unlockCanvasAndPost(canvas)
}

class MinamoImageView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : SurfaceView(context, attrs, defStyleAttr, defStyleRes) {

    private var tileCache: TileCache? = null

    var offset: MinamoIntOffset = MinamoIntOffset.Zero
    var scale: Float = -1.0f

    private var scaleType = ScaleTypes.CENTER_INSIDE
    var bound: Bound = Bounds.FORCE_OVERLAP_OR_CENTER

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            offset = MinamoIntOffset(offset.x - distanceX.roundToInt(), offset.y - distanceY.roundToInt())
            repaint()
            return true
        }
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scale *= detector.scaleFactor
            offset = MinamoIntOffset(
                (offset.x - (detector.focusX - offset.x) * (detector.scaleFactor - 1)).roundToInt(),
                (offset.y - (detector.focusY - offset.y) * (detector.scaleFactor - 1)).roundToInt()
            )
            repaint()
            return true
        }
    }

    private val gestureDetector = GestureDetector(context, gestureListener)
    private val scaleGestureDetector = ScaleGestureDetector(context, scaleListener)

    private var surfaceReady = false
    private var redrawOnReady = false

    init {
        holder.setFormat(PixelFormat.RGBA_8888)
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                surfaceReady = true
                if (redrawOnReady) {
                    redrawOnReady = false
                    repaint()
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                repaint()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                surfaceReady = false
            }
        })
    }

    val repaintSemaphore = Semaphore(2)

    private fun repaint() = CoroutineScope(coroutineContext).launch {
        if (!surfaceReady) {
            redrawOnReady = true
            return@launch
        }

        val tileCache = tileCache ?: return@launch

        if (!repaintSemaphore.tryAcquire()) return@launch

        holder.useCanvas { canvas ->
            canvas.drawColor(Color.WHITE)

            val imageSize = kotlin.runCatching { tileCache.image.size }.getOrNull() ?: return@useCanvas
            val size = MinamoSize(canvas.width, canvas.height)

            var offset = offset
            var scale = scale

            if (scale < 0) {
                scaleType(size, imageSize).let { (newOffset, newScale) ->
                    this@MinamoImageView.offset = newOffset
                    this@MinamoImageView.scale = newScale
                    offset = newOffset
                    scale = newScale
                }
            }
            bound(offset, scale, imageSize, size)
                .let { (newOffset, newScale) ->
                    this@MinamoImageView.offset = newOffset
                    this@MinamoImageView.scale = newScale
                    offset = newOffset
                    scale = newScale
                }

            tileCache.level = -log2(scale).toInt()

            val canvasRect = MinamoRect(
                (-offset.x / scale).roundToInt(),
                (-offset.y / scale).roundToInt(),
                (width / scale).roundToInt(),
                (height / scale).roundToInt()
            )

            tileCache.forEachTilesIn(canvasRect) { tile ->
                val tileRect = MinamoRect(
                    offset.x + (tile.region.x * scale).roundToInt(),
                    offset.y + (tile.region.y * scale).roundToInt(),
                    ceil(tile.region.width * scale).toInt(),
                    ceil(tile.region.height * scale).roundToInt()
                )

                tile.tile?.bitmap?.let { bitmap ->
                    canvas.drawBitmap(
                        bitmap,
                        Rect(0, 0, bitmap.width, bitmap.height),
                        Rect(tileRect.x, tileRect.y, tileRect.x + tileRect.width, tileRect.y + tileRect.height),
                        null
                    )
                }
            }
        }

        repaintSemaphore.release()
    }

    override fun onDetachedFromWindow() {
        tileCache?.close()
        tileCache?.level = -1
        super.onDetachedFromWindow()
    }

    fun setImage(image: MinamoImage?) {
        tileCache?.close()
        tileCache = image?.let {
            TileCache(image).apply {
                onTileLoaded = { image, region ->
                    repaint()
                }
            }
        }
        this.scale = -1.0f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        repaint()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)
        return true
    }

    companion object {
        @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
        private val coroutineContext = newSingleThreadContext("minamo_aqua_render_thread")
    }

}