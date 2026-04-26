package com.shabbar.rozgarconnector.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.shabbar.rozgarconnector.R
import java.io.ByteArrayOutputStream

/**
 * Converts Image Uri to Base64 String with high compression for speed.
 */
fun uriToBase64Jpeg(
    context: Context,
    uri: Uri,
    maxWidth: Int = 500,
    quality: Int = 40
): String {
    return try {
        val input = context.contentResolver.openInputStream(uri)!!
        val originalBitmap = BitmapFactory.decodeStream(input)
        input.close()

        val resized = resizeBitmap(originalBitmap, maxWidth)
        val baos = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) {
        ""
    }
}

/**
 * Decodes Base64 to Bitmap with caching support using Glide.
 */
fun loadBase64Image(
    context: Context,
    base64String: String?,
    imageView: ImageView,
    placeholder: Int = R.drawable.ic_profile
) {
    if (base64String.isNullOrEmpty()) {
        imageView.setImageResource(placeholder)
        return
    }

    try {
        val imageByteArray = Base64.decode(base64String, Base64.DEFAULT)
        Glide.with(context)
            .asBitmap()
            .load(imageByteArray)
            .placeholder(placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView)
    } catch (e: Exception) {
        imageView.setImageResource(placeholder)
    }
}

/**
 * Maintained for compatibility.
 */
fun decodeBase64BitmapAsync(
    base64String: String,
    onSuccess: (Bitmap) -> Unit,
    onError: (() -> Unit)? = null
) {
    android.os.AsyncTask.execute {
        try {
            val decodedString = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onSuccess(bitmap)
            }
        } catch (e: Exception) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onError?.invoke()
            }
        }
    }
}

private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxWidth) return bitmap
    val ratio = maxWidth.toFloat() / w.toFloat()
    val newH = (h * ratio).toInt()
    return Bitmap.createScaledBitmap(bitmap, maxWidth, newH, true)
}
