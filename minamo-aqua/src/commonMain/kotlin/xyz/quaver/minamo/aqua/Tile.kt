package xyz.quaver.minamo.aqua

import xyz.quaver.minamo.*
import kotlin.math.abs
import kotlin.math.min

class Tile(
    val x: Int,
    val y: Int,
    val region: MinamoRect
) {
    var tile: MinamoNativeImage? = null
        private set

    var image: MinamoImage? = null
        internal set
    var mask: MinamoImage? = null
        internal set

    fun load() {
        if (tile != null) return

        val image = image ?: return
        if (image.isClosed) return

        val decodeRegion =
            MinamoRect(x * 256, y * 256, min(image.width - x * 256, 256), min(image.height - y * 256, 256))
        val valid = mask?.decode(decodeRegion)?.pixelAt(0, 0)?.and(0xff) != 0
        tile = image.decode(decodeRegion).let { if (valid) it else null }
    }

    fun unload() {
        tile?.close()
        tile = null
    }
}

class TileCache(val image: MinamoImage) {
    var level = -1
        set(level) {
            val sanitized = level.coerceIn(0, tiles.size - 1)
            if (field == sanitized) return
            field = sanitized

            tiles.forEach {
                it.forEach { tile ->
                    tile.image?.close()
                    tile.mask?.close()
                }
            }

            if (image.isClosed) return

            val callback: (MinamoImage, MinamoRect) -> Unit = cb@{ image, rect ->
                if (image != tiles[sanitized].first().image) return@cb
                onTileLoaded?.invoke(image, rect)
            }

            val (cached, mask) = if (sanitized > 0) {
                image.subsample(1 shl sanitized).use {
                    it.sink(MinamoSize(256, 256), 128, 0, callback)
                }
            } else {
                image.sink(MinamoSize(256, 256), 128, 0, callback)
            }

            tiles[sanitized].forEach {
                it.image = cached
                it.mask = mask
            }
        }

    private val tileSize = 8

    var onTileLoaded: ((MinamoImage, MinamoRect) -> Unit)? = null

    private val tiles: List<List<Tile>> = buildList {
        val imageWidth = image.width
        val imageHeight = image.height

        repeat(maxLevel() + 1) { level ->
            add(buildList {
                val tileCountX = (imageWidth + (1 shl (level + tileSize)) - 1).ushr(level + tileSize)
                val tileCountY = (imageHeight + (1 shl (level + tileSize)) - 1).ushr(level + tileSize)

                for (y in 0 until tileCountY) {
                    for (x in 0 until tileCountX) {
                        val imageRegion = MinamoRect(
                            x shl (level + tileSize),
                            y shl (level + tileSize),
                            min(1 shl (level + tileSize), image.width - (x shl (level + tileSize))),
                            min(1 shl (level + tileSize), image.height - (y shl (level + tileSize)))
                        )

                        add(Tile(x, y, imageRegion))
                    }
                }
            })
        }
    }

    fun forEachTilesIn(region: MinamoRect, action: (Tile) -> Unit) {
        val tileCountX = tiles[level].last().x + 1

        var countAbove = 0
        var countBelow = 0

        tiles[level].forEach { tile ->
            if (tile.region overlaps region) tile.load()
        }

        tiles.forEachIndexed { tileLevel, tilesInLevel ->
            tilesInLevel.forEach tile@{ tile ->
                if (!(tile.region overlaps region)) {
                    tile.unload()
                    return@tile
                }

                val levelDiff = abs(tileLevel - level)

                if (tile.tile == null && tileLevel != level) return@tile

                when {
                    tileLevel > level -> {
                        var shouldUnload = true

                        tiles[level].forEach { tilesOnLevel ->
                            shouldUnload =
                                shouldUnload && (!(tilesOnLevel.region overlaps tile.region) || !(tilesOnLevel.region overlaps region) || tilesOnLevel.tile != null)
                        }

                        if (shouldUnload) tile.unload()

                        if (tile.tile != null) {
                            action(tile); countAbove++
                        }
                    }

                    tileLevel < level -> {
                        if (tiles[level][
                                (tile.y shr levelDiff) * tileCountX + (tile.x shr levelDiff)
                            ].tile != null
                        ) tile.unload()

                        if (tile.tile != null) {
                            action(tile); countBelow++
                        }
                    }

                    else -> {
                        if (tile.tile != null) action(tile)
                    }
                }
            }
        }
    }

    fun close() {
        tiles.forEach {
            it.forEach { tile ->
                tile.unload()
                tile.image?.close()
                tile.mask?.close()
            }
        }
    }

    private fun maxLevel(): Int {
        var maxLevel = 0
        var tileWidth = (image.width - 1) shr tileSize
        var tileHeight = (image.height - 1) shr tileSize

        while (tileWidth > 0 || tileHeight > 0) {
            tileWidth = tileWidth shr 1
            tileHeight = tileHeight shr 1
            maxLevel += 1
        }

        return maxLevel
    }
}