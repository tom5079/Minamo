package xyz.quaver.minamo.aqua

import xyz.quaver.minamo.MinamoImage
import xyz.quaver.minamo.MinamoSize
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.event.MouseInputListener

private fun Dimension.toMinamoSize() = MinamoSize(width, height)

class MinamoImagePanel : JPanel(), MouseInputListener {
    private val tileCache = TileCache()

    val imageSize: Pair<Int, Int>
        get() = (tileCache.image?.width ?: 0) to (tileCache.image?.height ?: 0)

    private var _imageRect: ImageRect = ImageRect(0, 0, 0, 0)
    var imageRect: ImageRect
        get() = _imageRect
        set(value) {
            _imageRect = value
            repaint()
        }

    init {
        addMouseListener(this)
        addMouseMotionListener(this)
    }

    fun setImage(image: MinamoImage?) {
        tileCache.image = image
        _imageRect = if (image != null && size.toMinamoSize() != MinamoSize.Zero)
            ScaleTypes.CENTER_INSIDE(size.toMinamoSize(), image.size)
        else ImageRect(0, 0, 0, 0)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        println("PAINTING COMPONENT ${size.toMinamoSize()}")

        g.fillRect(0, 0, width, height)

        tileCache.image?.let { image ->
            if (imageRect == ImageRect(0, 0, 0, 0)) {
                println("SETTING IMAGE RECT ${MinamoSize(width, height)} ${image.size}")
                _imageRect = ScaleTypes.CENTER_INSIDE(MinamoSize(width, height), image.size)
            }
        }

        g.color = Color.RED
        println("PAINTING ${imageRect}")
        g.drawRect(imageRect.x, imageRect.y, imageRect.width, imageRect.height)
//        tileCache.image?.let { image ->
//            g.drawImage(image.decode().image, 0, 0, null)
//        }
    }

    override fun mouseClicked(e: MouseEvent?) { }
    override fun mousePressed(e: MouseEvent?) {

    }
    override fun mouseReleased(e: MouseEvent?) {

    }
    override fun mouseEntered(e: MouseEvent?) { }
    override fun mouseExited(e: MouseEvent?) { }
    override fun mouseMoved(e: MouseEvent?) { }

    override fun mouseDragged(e: MouseEvent?) {
        println("DRAGGED ${e?.point}")
    }
}