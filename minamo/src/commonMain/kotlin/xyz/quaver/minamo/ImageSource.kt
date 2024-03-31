package xyz.quaver.minamo

typealias VipsSourcePtr = Long

internal interface ImageSource : AutoCloseable {
    val vipsSource: VipsSourcePtr
}