package xyz.quaver.minamo

fun loadImageFromFile(file: String): MinamoImage =
    FileImageSource(file).use { source ->
        MinamoImageImpl(source)
    }