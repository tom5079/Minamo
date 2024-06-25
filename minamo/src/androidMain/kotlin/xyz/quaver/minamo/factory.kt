package xyz.quaver.minamo

import android.content.ContextWrapper
import android.net.Uri

fun ContextWrapper.loadImageFromLocalUri(uri: Uri): Result<MinamoImage> = runCatching {
    LocalUriImageSource(this, uri).use {
        MinamoImageImpl(it)
    }
}