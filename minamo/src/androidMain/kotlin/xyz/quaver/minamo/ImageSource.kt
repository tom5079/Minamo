package xyz.quaver.minamo

import android.content.ContextWrapper
import android.net.Uri

class LocalUriImageSource(
    context: ContextWrapper,
    uri: Uri
) : ImageSource {
    private var _vipsSource: VipsSourcePtr = 0L
    override val vipsSource: VipsSourcePtr
        get() {
            check(_vipsSource != 0L) { "tried to access closed VipsSource" }
            return _vipsSource
        }

    private val descriptor = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")
    }.getOrNull() ?: error("failed to open image $uri")

    init {
        System.loadLibrary("minamo")
        _vipsSource = load(descriptor.fd)
        check(_vipsSource != 0L) { "failed to open image $uri" }
    }

    private external fun load(descriptor: Int): VipsSourcePtr
    external override fun close()
}