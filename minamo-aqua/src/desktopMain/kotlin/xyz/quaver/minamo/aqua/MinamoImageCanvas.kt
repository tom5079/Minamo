package xyz.quaver.minamo.aqua

import xyz.quaver.minamo.*
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.awt.image.DirectColorModel
import java.awt.image.Raster
import javax.swing.JPanel
import javax.swing.event.MouseInputListener
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.roundToInt

private fun Dimension.toMinamoSize() = MinamoSize(width, height)

class MinamoImageCanvas : JPanel(), MouseInputListener, MouseWheelListener {
    private val tileCache = TileCache()

    var offset: MinamoIntOffset = MinamoIntOffset.Zero
    var scale: Float = -1.0f
        set(value) {
            if (field == value) return;
            field = value
            tileCache.level = -log2(value).toInt()
            repaint()
        }

    private var scaleType = ScaleTypes.CENTER_INSIDE

    init {
        addMouseListener(this)
        addMouseMotionListener(this)
        addMouseWheelListener(this)
    }

    fun setImage(image: MinamoImage?, scaleType: ScaleType = ScaleTypes.CENTER_INSIDE) {
        tileCache.image = image ?: return
        this.scaleType = scaleType

        if (width == 0 || height == 0) {
            this.scale = -1.0f
            return
        }

        scaleType(size.toMinamoSize(), image.size).let { (offset, scale) ->
            this.offset = offset
            this.scale = scale
        }
    }

    fun reset() {
        val imageSize = tileCache.image?.size ?: return

        scaleType(size.toMinamoSize(), imageSize).let { (offset, scale) ->
            this.offset = offset
            this.scale = scale
        }
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val g2d = g as Graphics2D

        if (width == 0 || height == 0) {
            return
        }

        val imageSize = tileCache.image?.size
        if (scale < 0 && imageSize != null) {
            scaleType(size.toMinamoSize(), imageSize).let { (offset, scale) ->
                this.offset = offset
                this.scale = scale
            }
        }
        Bounds.FORCE_OVERLAP(offset, scale, imageSize ?: MinamoSize.Zero, size.toMinamoSize()).let { (offset, scale) ->
            this.offset = offset
            this.scale = scale
        }

        g.color = Color.RED
        g2d.setRenderingHints(mapOf(RenderingHints.KEY_INTERPOLATION to RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR))

        var loadAcc = 0
        var actualAcc = 0
        var drawAcc = 0
        var lossDraw = 0
        var count = 0

        val imageRect = MinamoRect(
            (-offset.x / scale).roundToInt(),
            (-offset.y / scale).roundToInt(),
            floor(width / scale).toInt(),
            floor(height / scale).toInt()
        )

        println("imageRect: $imageRect")

        var timer = System.currentTimeMillis()

        val image = tileCache.image?.resize(1 / (1 shl tileCache.level).toFloat()).use {
            it?.decode(imageRect.scale(1 / (1 shl tileCache.level).toFloat(), origin = MinamoIntOffset.Zero))
        }

        loadAcc = (System.currentTimeMillis() - timer).toInt()
        timer = System.currentTimeMillis()

        println("decoded image size: ${image?.image?.getWidth(null)} x ${image?.image?.getHeight(null)}")

        if (image != null) {
            g.drawImage(image.image, 0, 0, null)
        }

        drawAcc = (System.currentTimeMillis() - timer).toInt()

//        var timer = System.currentTimeMillis()
//
//        tileCache.tiles.forEach { tile ->
//            val tileRect = MinamoRect(
//                offset.x + (tile.region.x * scale).roundToInt(),
//                offset.y + (tile.region.y * scale).roundToInt(),
//                ceil(tile.region.width * scale).toInt(),
//                ceil(tile.region.height * scale).roundToInt()
//            )
//
//            val loadTimer = System.currentTimeMillis()
//
//            var flag = tile.tile == null
//
//            if (tileRect overlaps MinamoRect(MinamoIntOffset.Zero, size.toMinamoSize())) {
//                tile.load()
//                count += 1
//            } else {
//                flag = false
//                tile.unload()
//            }
//
//            if (flag) actualAcc += (System.currentTimeMillis() - loadTimer).toInt()
//
//            loadAcc += (System.currentTimeMillis() - loadTimer).toInt()
//
//            val drawTimer = System.currentTimeMillis()
//
//            g.drawImage(
//                tile.tile?.image,
//                offset.x + (tile.region.x * scale).roundToInt(),
//                offset.y + (tile.region.y * scale).roundToInt(),
//                ceil(tile.region.width * scale).toInt(),
//                ceil(tile.region.height * scale).roundToInt(),
//                null
//            )
//
//            g.drawRect(
//                offset.x + (tile.region.x * scale).roundToInt(),
//                offset.y + (tile.region.y * scale).roundToInt(),
//                ceil(tile.region.width * scale).toInt(),
//                ceil(tile.region.height * scale).roundToInt()
//            )
//
//            drawAcc += (System.currentTimeMillis() - drawTimer).toInt()
//            lossDraw += if (tile.tile == null) (System.currentTimeMillis() - drawTimer).toInt() else 0
//        }
//
        if (loadAcc > 50 || drawAcc > 50) {
            println("!!!Loading $loadAcc ms actual $actualAcc Drawing $drawAcc ms Loss $lossDraw ms Count $count average ${loadAcc / count.toFloat()}ms")
        }

        println("Rendering took ${System.currentTimeMillis() - timer}ms")
    }

    override fun mouseClicked(e: MouseEvent?) {}

    private var dragStart: MinamoIntOffset? = null
    private var originalOffset: MinamoIntOffset? = null
    override fun mousePressed(e: MouseEvent?) {
        dragStart = MinamoIntOffset(e!!.x, e.y)
        originalOffset = offset
    }

    override fun mouseReleased(e: MouseEvent?) {
        dragStart = null
        originalOffset = null
    }

    override fun mouseEntered(e: MouseEvent?) {}
    override fun mouseExited(e: MouseEvent?) {}
    override fun mouseMoved(e: MouseEvent?) {}

    override fun mouseDragged(e: MouseEvent?) {
        val dragStart = dragStart ?: return
        val originalOffset = originalOffset ?: return

        offset = MinamoIntOffset(
            originalOffset.x + e!!.x - dragStart.x,
            originalOffset.y + e.y - dragStart.y
        )

        repaint()
    }

    override fun mouseWheelMoved(e: MouseWheelEvent?) {
        if (e?.isControlDown != true) return


        val scale = scale * (1 + e.wheelRotation * 0.1f)
        this.offset = MinamoIntOffset(
            (offset.x - (e.x - offset.x) * (scale / this.scale - 1)).roundToInt(),
            (offset.y - (e.y - offset.y) * (scale / this.scale - 1)).roundToInt()
        )
        this.scale = scale
    }
}