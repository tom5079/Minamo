package xyz.quaver.minamo

import android.content.ContextWrapper
import android.net.Uri

fun ContextWrapper.loadImageFromLocalUri(uri: Uri): MinamoImage =
    LocalUriImageSource(this, uri).use {
        MinamoImageImpl(it)
    }