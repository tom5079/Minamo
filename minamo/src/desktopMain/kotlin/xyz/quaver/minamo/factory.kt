package xyz.quaver.minamo

fun loadImageFromFile(file: String): Result<MinamoImage> = runCatching {
    FileImageSource(file).use { source ->
        MinamoImageImpl(source)
    }
}