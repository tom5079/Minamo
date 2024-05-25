package xyz.quaver.minamo

class FileImageSource(
    file: String
) : ImageSource {

    private var _vipsSource: VipsSourcePtr = 0L
    override val vipsSource: VipsSourcePtr
        get() {
            check(_vipsSource != 0L) { "tried to access closed VipsSource" }
            return _vipsSource
        }

    init {
        System.loadLibrary("minamo")
        _vipsSource = load(file)
        check(_vipsSource != 0L) { "failed to open image $file" }
    }

    private external fun load(file: String): VipsSourcePtr

    external override fun close()
}