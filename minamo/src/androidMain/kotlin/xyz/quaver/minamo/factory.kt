package xyz.quaver.minamo

import android.content.Context
import android.net.Uri

fun loadImageFromLocalUri(context: Context, uri: Uri): MinamoImage =
    LocalUriImageSource(context, uri).use {
        MinamoImageImpl(it)
    }