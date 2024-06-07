package xyz.quaver.minamo.aqua

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.quaver.minamo.*
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.roundToInt

class MinamoImageView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : SurfaceView(context, attrs, defStyleAttr, defStyleRes) {
    private val tileCache: TileCache = TileCache().apply {
        onTileLoaded = { _, _ -> repaint() }
    }

    var offset: MinamoIntOffset = MinamoIntOffset.Zero
    var scale: Float = -1.0f
        set(value) {
            if (field == value) return;
            field = value
            tileCache.level = -log2(value).toInt()
            repaint()
        }

    private var scaleType = ScaleTypes.CENTER_INSIDE
    var bound: Bound = Bounds.FORCE_OVERLAP_OF_CENTER

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

            canvas.drawColor(Color.WHITE)

            val imageSize = tileCache.image?.size
            val size = MinamoSize(canvas.width, canvas.height)
            if (scale < 0 && imageSize != null) {
                scaleType(size, imageSize).let { (offset, scale) ->
                    this@MinamoImageView.offset = offset
                    this@MinamoImageView.scale = scale
                }
            }
            bound(offset, scale, imageSize ?: MinamoSize.Zero, size)
                .let { (offset, scale) ->
                    this@MinamoImageView.offset = offset
                    this@MinamoImageView.scale = scale
                }

            val paint = Paint().apply {
                isAntiAlias = true
                color = Color.RED
                style = Paint.Style.STROKE
            }

            tileCache.tiles.forEach { tile: Tile ->
                val tileRect = MinamoRect(
                    offset.x + (tile.region.x * scale).roundToInt(),
                    offset.y + (tile.region.y * scale).roundToInt(),
                    ceil(tile.region.width * scale).toInt(),
                    ceil(tile.region.height * scale).roundToInt()
                )

                if (tileRect overlaps MinamoRect(MinamoIntOffset.Zero, size)) {
                    tile.load()
                } else {
                    tile.unload()
                }

                tile.tile?.bitmap?.let { bitmap ->
                    canvas.drawBitmap(
                        bitmap,
                        Rect(0, 0, bitmap.width, bitmap.height),
                        Rect(tileRect.x, tileRect.y, tileRect.x + tileRect.width, tileRect.y + tileRect.height),
                        null
                    )
                }

                canvas.drawRect(
                    tileRect.x.toFloat(),
                    tileRect.y.toFloat(),
                    tileRect.x.toFloat() + tileRect.width.toFloat(),
                    tileRect.y.toFloat() + tileRect.height.toFloat(),
                    paint
                )
            }

            holder.unlockCanvasAndPost(canvas)
        }
    }

    fun setImage(image: MinamoImage?) {
        tileCache.image = image

        this.scale = -1.0f
        repaint()
    }

    fun reset() {
        this.scale = -1.0f
        repaint()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        repaint()
    }

    companion object {
        @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
        private val coroutineContext = newSingleThreadContext("minamo_aqua_render_thread")
        private val mutex = Mutex()
    }

}