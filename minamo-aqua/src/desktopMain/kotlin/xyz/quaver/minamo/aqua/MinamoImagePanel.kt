package xyz.quaver.minamo.aqua

import xyz.quaver.minamo.MinamoImage
import xyz.quaver.minamo.MinamoIntOffset
import xyz.quaver.minamo.MinamoRect
import xyz.quaver.minamo.MinamoSize
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import javax.swing.JPanel
import javax.swing.event.MouseInputListener
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.roundToInt

private fun Dimension.toMinamoSize() = MinamoSize(width, height)

class MinamoImagePanel : JPanel(), MouseInputListener, MouseWheelListener {
    private var tileCache: TileCache? = null

    var offset: MinamoIntOffset = MinamoIntOffset.Zero
    var scale: Float = -1.0f

    private var scaleType = ScaleTypes.CENTER_INSIDE
    var bound: Bound = Bounds.FORCE_OVERLAP_OR_CENTER

    init {
        addMouseListener(this)
        addMouseMotionListener(this)
        addMouseWheelListener(this)
    }

    fun setImage(image: MinamoImage?) {
        tileCache?.close()
        tileCache = image?.let {
            TileCache(image).apply {
                onTileLoaded = { _, _ -> repaint() }
            }
        }
        this.scale = -1.0f
        repaint()
    }

    fun reset() {
        this.scale = -1.0f
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        if (width == 0 || height == 0) {
            return
        }

        val tileCache = tileCache ?: return

        val imageSize = tileCache.image.size
        if (scale < 0) {
            scaleType(size.toMinamoSize(), imageSize).let { (offset, scale) ->
                this.offset = offset
                this.scale = scale
            }
        }
        bound(offset, scale, imageSize, size.toMinamoSize())
            .let { (offset, scale) ->
                this.offset = offset
                this.scale = scale
            }

        tileCache.level = -log2(this.scale).toInt()

        val canvasRect = MinamoRect(
            (-offset.x / scale).roundToInt(),
            (-offset.y / scale).roundToInt(),
            (width / scale).roundToInt(),
            (height / scale).roundToInt()
        )

        tileCache.forEachTilesIn(canvasRect) { tile ->
            g.drawImage(
                tile.tile?.image,
                offset.x + (tile.region.x * scale).roundToInt(),
                offset.y + (tile.region.y * scale).roundToInt(),
                ceil(tile.region.width * scale).toInt(),
                ceil(tile.region.height * scale).roundToInt(),
                null
            )
        }
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
        repaint()
    }
}