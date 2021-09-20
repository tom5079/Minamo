package xyz.quaver.graphics.subsampledimage

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces

class SubsampledImageBitmap(image: ByteArray) : ImageBitmap {

    override val colorSpace: ColorSpace
        get() = ColorSpaces.Srgb
    override val config: ImageBitmapConfig
        get() = ImageBitmapConfig.Rgb565
    override val hasAlpha: Boolean
        get() = false
    override val height: Int
    override val width: Int

    init {
        /*
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
        }

        BitmapFactory.decodeByteArray(image, 0, image.size, options)

        height = options.outHeight
        width = options.outWidth
         */

        width = 320
        height = 320
    }

    override fun prepareToDraw() { }

    override fun readPixels(
        buffer: IntArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        bufferOffset: Int,
        stride: Int
    ) {
        for (i in 0 until width * height) {
            buffer[bufferOffset+i] = -0x7F00FFFF
        }
    }

}