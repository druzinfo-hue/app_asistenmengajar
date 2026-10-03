package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ImageSaverHelper {
    private const val TAG = "ImageSaverHelper"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun saveImageFromUrlToGallery(
        context: Context,
        imageUrl: String,
        description: String = "Gambar Asisten Mengajar"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(imageUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Failed to download image: ${response.code}")
                    return@withContext false
                }
                val bytes = response.body?.bytes() ?: return@withContext false
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext false
                return@withContext saveBitmapToGallery(context, bitmap, description)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image to gallery", e)
            false
        }
    }

    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        description: String = "Gambar Asisten Mengajar"
    ): Boolean {
        return try {
            val filename = "IMG_${System.currentTimeMillis()}.jpg"
            val resolver = context.contentResolver

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DESCRIPTION, description)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AsistenMengajar")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false

            resolver.openOutputStream(uri)?.use { os ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, os)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap", e)
            false
        }
    }
}
