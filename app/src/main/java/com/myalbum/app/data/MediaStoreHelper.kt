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
            val projection = getProjection(isVideo)
            val selection = buildSelection(query)
            val selectionArgs = query?.let {
                arrayOf("%${it.lowercase(Locale.getDefault())}%")
            }

            resolver.query(uri, projection, selection, selectionArgs, sortOrder.value)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val item = cursorToMediaItem(cursor, isVideo)
                    items.add(item)
                }
            }
        }

        emit(items.sortedByDescending { it.dateAdded })
    }.flowOn(Dispatchers.IO)

    fun getAlbums(): Flow<List<AlbumInfo>> = flow {
        val albums = mutableMapOf<String, AlbumInfo>()
        val resolver = context.contentResolver

        for (uri in listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )) {
            val isVideo = uri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI
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
                        val bucketId = cursor.getString(bucketIdIndex) ?: "unknown"
                        val bucketName = cursor.getString(bucketNameIndex) ?: "Unknown"
                        val id = cursor.getLong(idIndex)

                        val existing = albums[bucketId]
                        if (existing != null) {
                            albums[bucketId] = existing.copy(count = existing.count + 1)
                        } else {
                            val coverUri = ContentUris.withAppendedId(uri, id)
                            albums[bucketId] = AlbumInfo(
                                bucketId = bucketId,
                                name = bucketName,
                                coverUri = coverUri,
                                count = 1,
                                isVideo = isVideo
                            )
                        }
                    }
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
            val projection = getProjection(isVideo)
            val selection = "${MediaStore.MediaColumns.BUCKET_ID} = ?"
            val selectionArgs = arrayOf(bucketId)

            resolver.query(uri, projection, selection, selectionArgs, "${MediaStore.MediaColumns.DATE_ADDED} DESC")
                ?.use { cursor ->
                    while (cursor.moveToNext()) {
                        items.add(cursorToMediaItem(cursor, isVideo))
                    }
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
                val projection = getProjection(isVideo)
                val selection = "${MediaStore.MediaColumns.IS_FAVORITE} = ?"
                val selectionArgs = arrayOf("1")

                resolver.query(uri, projection, selection, selectionArgs, "${MediaStore.MediaColumns.DATE_ADDED} DESC")
                    ?.use { cursor ->
                        while (cursor.moveToNext()) {
                            items.add(cursorToMediaItem(cursor, isVideo))
                        }
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
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
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

        val duration = if (isVideo) {
            val durationIndex = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            if (durationIndex >= 0) cursor.getLong(durationIndex) else 0L
        } else 0L

        val uri = ContentUris.withAppendedId(
            if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            id
        )

        val isFavorite = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val favIndex = cursor.getColumnIndex(MediaStore.MediaColumns.IS_FAVORITE)
            if (favIndex >= 0) cursor.getInt(favIndex) == 1 else false
        } else false

        return MediaItem(
            id = id,
            uri = uri,
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
            isVideo = isVideo
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
