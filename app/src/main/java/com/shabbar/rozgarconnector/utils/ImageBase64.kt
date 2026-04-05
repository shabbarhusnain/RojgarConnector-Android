package com.shabbar.rozgarconnector.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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

private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxWidth) return bitmap
    val ratio = maxWidth.toFloat() / w.toFloat()
    val newH = (h * ratio).toInt()
    return Bitmap.createScaledBitmap(bitmap, maxWidth, newH, true)
}
