package com.example.downloader

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object VideoDownloader {

    private val client = OkHttpClient()

    suspend fun downloadVideo(
        context: Context,
        videoUrl: String,
        videoId: String,
        onProgress: (Float) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(videoUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body ?: return@withContext null
            val contentLength = body.contentLength()
            val inputStream: InputStream = body.byteStream()

            val fileName = "TikTok_${videoId}_${System.currentTimeMillis()}.mp4"
            var outputStream: OutputStream? = null
            var finalPathOrUri: String? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/TikTokDownloader")
                    }
                    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        outputStream = resolver.openOutputStream(uri)
                        finalPathOrUri = uri.toString()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (outputStream == null) {
                // Fallback for older Android versions or if MediaStore fails
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "TikTokDownloader")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val file = File(dir, fileName)
                outputStream = FileOutputStream(file)
                finalPathOrUri = file.absolutePath
            }

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead: Long = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (contentLength > 0) {
                    val progress = (totalBytesRead.toFloat() / contentLength.toFloat())
                    onProgress(progress)
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            return@withContext finalPathOrUri
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
