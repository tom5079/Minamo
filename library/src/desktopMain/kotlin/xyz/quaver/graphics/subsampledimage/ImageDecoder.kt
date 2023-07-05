package xyz.quaver.graphics.subsampledimage

class ImageDecoder {
    init {
        System.loadLibrary("ssi")
    }

    external fun test(): Int
}