package xyz.quaver.minamo.aqua

import xyz.quaver.minamo.MinamoImage
import xyz.quaver.minamo.MinamoRect
import xyz.quaver.minamo.MinamoNativeImage
import kotlin.math.min

data class Tile(
    val region: ImageRect,
    val image: MinamoNativeImage
)

class TileCache {
    var image: MinamoImage? = null
        set(value) {
            field = value
            level = 0
            tiles.clear()
        }
    var level = 0
        set(value) {
            field = value
            tiles.clear()
        }

    private val tiles = mutableListOf<Tile>()

    val tileCount: Pair<Int, Int>
        get() =
            (image?.width ?: 0).ushr(level + 7) to (image?.height ?: 0).ushr(level + 7)

    fun getTile(x: Int, y: Int): Tile? {
        val image = image ?: return null

        val imageRegion = ImageRect(
            x shl level + 7,
            y shl level + 7,
            min(1 shl level + 7, image.width - x shl level + 7),
            min(1 shl level + 7, image.height - y shl level + 7)
        )

        return Tile(
            imageRegion,
            image.decode()
        )
    }
}