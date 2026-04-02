package com.myalbum.app.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object VideoThumbnailUtil {

    private const val TAG = "VideoThumbnailUtil"
    private const val THUMB_SIZE = 512

    /**
     * Generate a video thumbnail and save to cache.
     * Returns the cached file Uri, or null on failure.
     */
    fun getOrCreateThumbnail(context: Context, uri: Uri): Uri? {
        try {
            val cacheDir = File(context.cacheDir, "video_thumbs")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val fileName = "thumb_${uri.lastPathSegment?.replace("/", "_") ?: uri.hashCode()}.jpg"
            val cacheFile = File(cacheDir, fileName)

            // Return cached file if exists
            if (cacheFile.exists() && cacheFile.length() > 0) {
                return Uri.fromFile(cacheFile)
            }

            // Extract thumbnail
            val bitmap = extractVideoThumbnail(context, uri) ?: return null

            // Save to cache
            FileOutputStream(cacheFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            }
            bitmap.recycle()

            return Uri.fromFile(cacheFile)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to generate thumbnail for: $uri", e)
            return null
        }
    }

    private fun extractVideoThumbnail(context: Context, uri: Uri): Bitmap? {
        // Try ContentResolver.loadThumbnail on API 29+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                return context.contentResolver.loadThumbnail(
                    uri,
                    android.util.Size(THUMB_SIZE, THUMB_SIZE),
                    null
                )
            } catch (e: Exception) {
                Log.w(TAG, "loadThumbnail failed, trying MediaMetadataRetriever", e)
            }
        }

        // Fallback: MediaMetadataRetriever
        return extractFrameLegacy(context, uri)
    }

    private fun extractFrameLegacy(context: Context, uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(
                0,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: return null

            // Scale down if needed to save memory
            if (frame.width > THUMB_SIZE || frame.height > THUMB_SIZE) {
                val ratio = minOf(
                    THUMB_SIZE.toFloat() / frame.width,
                    THUMB_SIZE.toFloat() / frame.height
                )
                val scaled = Bitmap.createScaledBitmap(
                    frame,
                    (frame.width * ratio).toInt(),
                    (frame.height * ratio).toInt(),
                    true
                )
                if (scaled != frame) frame.recycle()
                scaled
            } else {
                frame
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaMetadataRetriever failed for: $uri", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
