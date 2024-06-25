package xyz.quaver.minamo

import android.content.ContextWrapper
import android.net.Uri

internal class LocalUriImageSource(
    context: ContextWrapper,
    uri: Uri
) : ImageSource {
    override var vipsSource: VipsSourcePtr
        private set

    private val descriptor = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")
    }.getOrNull() ?: error("failed to open image $uri")

    init {
        System.loadLibrary("minamo")
        vipsSource = load(descriptor.fd).getOrThrow()
    }

    private external fun load(descriptor: Int): Result<VipsSourcePtr>
    external override fun close()
}