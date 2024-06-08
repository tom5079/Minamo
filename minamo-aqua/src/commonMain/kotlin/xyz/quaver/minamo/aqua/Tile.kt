package xyz.quaver.minamo.aqua

import xyz.quaver.minamo.*
import kotlin.math.min

class Tile(
    val image: MinamoImage,
    val mask: MinamoImage?,
    val region: MinamoRect,
    val level: Int,
    var tile: MinamoNativeImage? = null
) {
    fun load() {
        if (tile != null) return
        val decodeRegion = region.scale(1 / (1 shl level).toFloat(), origin = MinamoIntOffset.Zero)
        val valid = mask?.decode(decodeRegion)?.pixelAt(0, 0)?.and(0xff) != 0
        tile = image.decode(decodeRegion).let {
            if (valid) it else null
        }
    }

    fun unload() {
        tile = null
    }
}

class TileCache {
    var image: MinamoImage? = null
        set(value) {
            if (field === value) return

            field = value
            tiles.clear()
            level = 0
        }
    var level = -1
        @Synchronized
        set(value) {
            val sanitized = value.coerceAtLeast(0)
            if (field == sanitized) return
            field = sanitized

            cached?.close()
            mask?.close()

            cached = null
            mask = null

            val image = image ?: return

            if (level > 0) {
                image.subsample(1 shl level).use{
                    val (cached, mask) = it.sink(MinamoSize(256, 256), 256, 0) { image, rect ->
                        if (image != cached) return@sink
                        onTileLoaded?.invoke(image, rect)
                    }

                    this.cached = cached
                    this.mask = mask
                }
            } else {
                val (cached, mask) = image.sink(MinamoSize(256, 256), 256, 0) { image, rect ->
                    if (image != cached) return@sink
                    onTileLoaded?.invoke(image, rect)
                }

                this.cached = cached
                this.mask = mask
            }
            populateTiles()
        }

    private var cached: MinamoImage? = null
    private var mask: MinamoImage? = null

    private val tileSize = 8

    var onTileLoaded: ((MinamoImage, MinamoRect) -> Unit)? = null

    val tiles = mutableListOf<Tile>()

    val tileCount: Pair<Int, Int>
        get() {
            val width = image?.width ?: 0
            val height = image?.height ?: 0

            return (width + (1 shl (level + tileSize)) - 1).ushr(level + tileSize) to (height + (1 shl (level + tileSize)) - 1).ushr(
                level + tileSize
            )
        }

    private fun populateTiles() {
        val (tileCountX, tileCountY) = tileCount
        val image = image ?: return

        tiles.clear()

        for (y in 0 until tileCountY) {
            for (x in 0 until tileCountX) {

                val imageRegion = MinamoRect(
                    x shl (level + tileSize),
                    y shl (level + tileSize),
                    min(1 shl (level + tileSize), image.width - (x shl (level + tileSize))),
                    min(1 shl (level + tileSize), image.height - (y shl (level + tileSize)))
                )

                tiles.add(Tile(cached ?: image, mask, imageRegion, level, null))
            }
        }
    }
}