package xyz.quaver.minamo

@Suppress("INAPPLICABLE_JVM_NAME")
class FileImageSource(file: String) : ImageSource {
    override var vipsSource: VipsSourcePtr
        private set

    init {
        System.loadLibrary("minamo")
        vipsSource = load(file).getOrThrow()
    }

    @JvmName("load")
    private external fun load(file: String): Result<VipsSourcePtr>

    @JvmName("close")
    external override fun close()
}