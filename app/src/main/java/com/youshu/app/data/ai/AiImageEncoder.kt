package com.youshu.app.data.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiImageEncoder @Inject constructor() {

    fun encodeImages(paths: List<String>, maxCount: Int = 3): List<String> {
        return paths
            .take(maxCount)
            .mapNotNull(::encodeImage)
    }

    private fun encodeImage(path: String): String? {
        val file = File(path)
        if (!file.exists()) return null

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }

        val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 78, output)
            bitmap.recycle()
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        val maxSide = maxOf(width, height)
        while (maxSide / sampleSize > 1280) {
            sampleSize *= 2
        }
        return sampleSize.coerceAtLeast(1)
    }
}
