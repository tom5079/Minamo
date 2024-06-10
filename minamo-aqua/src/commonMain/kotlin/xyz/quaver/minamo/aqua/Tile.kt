package xyz.quaver.minamo.aqua

import xyz.quaver.minamo.*
import kotlin.math.min

class Tile(
    val image: MinamoImage,
    val mask: MinamoImage?,
    val x: Int,
    val y: Int,
    val region: MinamoRect
) {
    var tile: MinamoNativeImage? = null
        private set

    fun load() {
        if (tile != null) return
        val decodeRegion =
            MinamoRect(x * 256, y * 256, min(image.width - x * 256, 256), min(image.height - y * 256, 256))
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
            level = 0
        }
    var level = -1
        set(level) {
            val sanitized = level.coerceAtLeast(0)
            if (field == sanitized) return
            field = sanitized

            cached?.close()
            mask?.close()

            cached = null
            mask = null

            val image = image ?: return

            if (level > 0) {
                image.subsample(1 shl level).use {
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

    @Synchronized
    fun forEachTiles(action: (Tile) -> Unit) {
        tiles.forEach(action)
    }

    private val tiles = mutableListOf<Tile>()
    private val tileCount: Pair<Int, Int>
        get() {
            val width = image?.width ?: 0
            val height = image?.height ?: 0

            return (width + (1 shl (level + tileSize)) - 1).ushr(level + tileSize) to (height + (1 shl (level + tileSize)) - 1).ushr(
                level + tileSize
            )
        }

    @Synchronized
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

                tiles.add(Tile(cached ?: image, mask, x, y, imageRegion))
            }
        }
    }
}