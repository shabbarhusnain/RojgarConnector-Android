package com.shabbar.rozgarconnector.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import java.io.ByteArrayOutputStream

fun uriToBase64Jpeg(
    context: Context,
    uri: Uri,
    maxWidth: Int = 720,
    quality: Int = 45
): String {
    val input = context.contentResolver.openInputStream(uri)!!
    val originalBytes = input.readBytes()
    input.close()

    val originalBitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
    val resized = resizeBitmap(originalBitmap, maxWidth)

    val baos = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, quality, baos)

    return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
}

fun decodeBase64BitmapAsync(
    base64String: String,
    onSuccess: (Bitmap) -> Unit,
    onError: (() -> Unit)? = null
) {
    Thread {
        try {
            val decodedString = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            Handler(Looper.getMainLooper()).post {
                onSuccess(bitmap)
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                onError?.invoke()
            }
        }
    }.start()
}

private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxWidth) return bitmap
    val ratio = maxWidth.toFloat() / w.toFloat()
    val newH = (h * ratio).toInt()
    return Bitmap.createScaledBitmap(bitmap, maxWidth, newH, true)
}
