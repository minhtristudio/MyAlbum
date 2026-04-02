package com.myalbum.app.data

import android.content.ContentUris
import android.provider.MediaStore

data class MediaItem(
    val id: Long,
    val uri: android.net.Uri,
    val name: String,
    val mimeType: String,
    val size: Long,
    val dateAdded: Long,
    val duration: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val bucketId: String = "",
    val bucketName: String = "",
    val isFavorite: Boolean = false,
    val isVideo: Boolean = false,
    val thumbnailUri: android.net.Uri? = null,
    val orientation: Int = 0
) {
    val formattedSize: String
        get() = when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024))
            else -> String.format("%.1f GB", size / (1024.0 * 1024 * 1024))
        }

    val formattedDuration: String
        get() {
            if (duration <= 0) return ""
            val seconds = (duration / 1000) % 60
            val minutes = (duration / (1000 * 60)) % 60
            val hours = duration / (1000 * 60 * 60)
            return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
            else "%02d:%02d".format(minutes, seconds)
        }

    val formattedResolution: String
        get() {
            if (width <= 0 || height <= 0) return ""
            return "${width}x${height}"
        }

    val contentUri: android.net.Uri
        get() = ContentUris.withAppendedId(
            if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            id
        )

    /** Smart aspect ratio for grid display */
    val gridAspectRatio: Float
        get() {
            if (width <= 0 || height <= 0) return 1f
            val w = if (orientation == 90 || orientation == 270) height else width
            val h = if (orientation == 90 || orientation == 270) width else height
            return (w.toFloat() / h.toFloat()).coerceIn(0.5f, 2f)
        }
}

data class AlbumInfo(
    val bucketId: String,
    val name: String,
    val coverUri: android.net.Uri?,
    val count: Int,
    val isVideo: Boolean = false,
    val videoCount: Int = 0,
    val recentUris: List<android.net.Uri> = emptyList()
)
