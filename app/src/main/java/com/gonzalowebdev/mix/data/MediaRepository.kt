package com.gonzalowebdev.mix.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRepository(private val context: Context) {

    suspend fun load(): List<MediaFile> = withContext(Dispatchers.IO) {
        val all = ArrayList<MediaFile>()
        all += query(video = false)
        all += query(video = true)
        all
    }

    private fun query(video: Boolean): List<MediaFile> {
        val base = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = if (video) arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        ) else arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        val result = ArrayList<MediaFile>()
        context.contentResolver.query(base, projection, null, null, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val bucketCol = c.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val durCol = if (video) c.getColumnIndex(MediaStore.Video.Media.DURATION) else -1

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val folder = if (bucketCol >= 0) c.getString(bucketCol) else null
                result += MediaFile(
                    id = id,
                    uri = ContentUris.withAppendedId(base, id),
                    name = c.getString(nameCol) ?: "",
                    isVideo = video,
                    dateAdded = c.getLong(dateCol),
                    size = c.getLong(sizeCol),
                    durationMs = if (durCol >= 0) c.getLong(durCol) else 0L,
                    folder = if (folder.isNullOrBlank()) "Sin carpeta" else folder
                )
            }
        }
        return result
    }
}
