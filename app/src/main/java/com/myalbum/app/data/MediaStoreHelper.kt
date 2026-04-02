package com.myalbum.app.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Locale

class MediaStoreHelper(private val context: Context) {

    companion object {
        private const val TAG = "MediaStoreHelper"
    }

    fun getAllMedia(
        mediaType: MediaType = MediaType.ALL,
        sortOrder: SortOrder = SortOrder.DATE_DESC,
        query: String? = null
    ): Flow<List<MediaItem>> = flow {
        val items = mutableListOf<MediaItem>()
        val resolver = context.contentResolver

        val uris = when (mediaType) {
            MediaType.ALL -> listOf(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
            MediaType.PHOTOS -> listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            MediaType.VIDEOS -> listOf(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        }

        for (uri in uris) {
            val isVideo = uri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            try {
                val projection = getProjection(isVideo)
                val selection = buildSelection(query)
                val selectionArgs = query?.let {
                    arrayOf("%${it.lowercase(Locale.getDefault())}%")
                }

                resolver.query(uri, projection, selection, selectionArgs, sortOrder.value)?.use { cursor ->
                    while (cursor.moveToNext()) {
                        try {
                            val item = cursorToMediaItem(cursor, isVideo)
                            items.add(item)
                        } catch (e: Exception) {
                            Log.w(TAG, "Skipping media item due to read error", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to query media from $uri", e)
            }
        }

        emit(items.sortedByDescending { it.dateAdded })
    }.flowOn(Dispatchers.IO)

    fun getAlbums(): Flow<List<AlbumInfo>> = flow {
        val albums = mutableMapOf<String, AlbumInfo>()
        val albumRecentUris = mutableMapOf<String, MutableList<Uri>>()
        val resolver = context.contentResolver

        for (uri in listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )) {
            val isVideo = uri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            try {
                val projection = arrayOf(
                    MediaStore.MediaColumns.BUCKET_ID,
                    MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DATE_ADDED
                )

                resolver.query(uri, projection, null, null, "${MediaStore.MediaColumns.DATE_ADDED} DESC")
                    ?.use { cursor ->
                    val bucketIdIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
                    val bucketNameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                    val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)

                    while (cursor.moveToNext()) {
                        try {
                            val bucketId = cursor.getString(bucketIdIndex) ?: "unknown"
                            val bucketName = cursor.getString(bucketNameIndex) ?: "Unknown"
                            val id = cursor.getLong(idIndex)
                            val itemUri = ContentUris.withAppendedId(uri, id)

                            // Track recent URIs for album cover grid
                            val recentList = albumRecentUris.getOrPut(bucketId) { mutableListOf() }
                            if (recentList.size < 4) recentList.add(itemUri)

                            val existing = albums[bucketId]
                            if (existing != null) {
                                val newVideoCount = if (isVideo) existing.videoCount + 1 else existing.videoCount
                                albums[bucketId] = existing.copy(
                                    count = existing.count + 1,
                                    videoCount = newVideoCount,
                                    recentUris = recentList.toList()
                                )
                            } else {
                                albums[bucketId] = AlbumInfo(
                                    bucketId = bucketId,
                                    name = bucketName,
                                    coverUri = itemUri,
                                    count = 1,
                                    isVideo = isVideo,
                                    videoCount = if (isVideo) 1 else 0,
                                    recentUris = recentList.toList()
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Skipping album entry due to read error", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to query albums from $uri", e)
            }
        }

        emit(albums.values.sortedByDescending { it.count })
    }.flowOn(Dispatchers.IO)

    fun getMediaByAlbum(bucketId: String): Flow<List<MediaItem>> = flow {
        val items = mutableListOf<MediaItem>()
        val resolver = context.contentResolver

        for (uri in listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )) {
            val isVideo = uri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            try {
                val projection = getProjection(isVideo)
                val selection = "${MediaStore.MediaColumns.BUCKET_ID} = ?"
                val selectionArgs = arrayOf(bucketId)

                resolver.query(uri, projection, selection, selectionArgs, "${MediaStore.MediaColumns.DATE_ADDED} DESC")
                    ?.use { cursor ->
                        while (cursor.moveToNext()) {
                            try {
                                items.add(cursorToMediaItem(cursor, isVideo))
                            } catch (e: Exception) {
                                Log.w(TAG, "Skipping album media item", e)
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to query album media from $uri", e)
            }
        }

        emit(items.sortedByDescending { it.dateAdded })
    }.flowOn(Dispatchers.IO)

    fun getFavorites(): Flow<List<MediaItem>> = flow {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val items = mutableListOf<MediaItem>()
            val resolver = context.contentResolver

            for (uri in listOf(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )) {
                val isVideo = uri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                try {
                    val projection = getProjection(isVideo)
                    val selection = "${MediaStore.MediaColumns.IS_FAVORITE} = ?"
                    val selectionArgs = arrayOf("1")

                    resolver.query(uri, projection, selection, selectionArgs, "${MediaStore.MediaColumns.DATE_ADDED} DESC")
                        ?.use { cursor ->
                            while (cursor.moveToNext()) {
                                try {
                                    items.add(cursorToMediaItem(cursor, isVideo))
                                } catch (e: Exception) {
                                    Log.w(TAG, "Skipping favorite item", e)
                                }
                            }
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to query favorites from $uri", e)
                }
            }

            emit(items.sortedByDescending { it.dateAdded })
        } else {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    private fun getProjection(isVideo: Boolean): Array<String> {
        val base = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.ORIENTATION
        )
        return if (isVideo) {
            base + MediaStore.Video.Media.DURATION
        } else {
            base
        }
    }

    private fun buildSelection(query: String?): String? {
        if (query.isNullOrBlank()) return null
        return "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
    }

    private fun cursorToMediaItem(cursor: Cursor, isVideo: Boolean): MediaItem {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
        val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)) ?: "Unknown"
        val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)) ?: ""
        val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
        val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED))
        val width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH))
        val height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT))
        val bucketId = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)) ?: ""
        val bucketName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)) ?: ""

        val orientation = cursor.getColumnIndex(MediaStore.MediaColumns.ORIENTATION).let { idx ->
            if (idx >= 0) cursor.getInt(idx) else 0
        }

        val duration = if (isVideo) {
            val durationIndex = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            if (durationIndex >= 0) cursor.getLong(durationIndex) else 0L
        } else 0L

        val contentUri = ContentUris.withAppendedId(
            if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            id
        )

        // For videos: use MediaStore thumbnail URI (works on API 29+)
        val thumbnailUri: Uri? = if (isVideo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                id
            )
        } else {
            null
        }

        val isFavorite = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val favIndex = cursor.getColumnIndex(MediaStore.MediaColumns.IS_FAVORITE)
            if (favIndex >= 0) cursor.getInt(favIndex) == 1 else false
        } else false

        return MediaItem(
            id = id,
            uri = contentUri,
            name = name,
            mimeType = mimeType,
            size = size,
            dateAdded = dateAdded,
            duration = duration,
            width = width,
            height = height,
            bucketId = bucketId,
            bucketName = bucketName,
            isFavorite = isFavorite,
            isVideo = isVideo,
            thumbnailUri = thumbnailUri,
            orientation = orientation
        )
    }

    enum class MediaType { ALL, PHOTOS, VIDEOS }
    enum class SortOrder(val value: String) {
        DATE_DESC("${MediaStore.MediaColumns.DATE_ADDED} DESC"),
        DATE_ASC("${MediaStore.MediaColumns.DATE_ADDED} ASC"),
        NAME_ASC("${MediaStore.MediaColumns.DISPLAY_NAME} ASC"),
        NAME_DESC("${MediaStore.MediaColumns.DISPLAY_NAME} DESC"),
        SIZE_DESC("${MediaStore.MediaColumns.SIZE} DESC"),
        SIZE_ASC("${MediaStore.MediaColumns.SIZE} ASC")
    }
}
