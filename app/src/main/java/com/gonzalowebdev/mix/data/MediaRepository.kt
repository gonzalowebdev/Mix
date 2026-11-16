package com.gonzalowebdev.mix.data

import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRepository(private val context: Context) {

    suspend fun load(): List<MediaFile> = withContext(Dispatchers.IO) {
        val all = ArrayList<MediaFile>()
        all += query(video = false)
        all += query(video = true)
        all
    }

    suspend fun loadExtras(uris: Set<String>): List<MediaFile> = withContext(Dispatchers.IO) {
        uris.mapNotNull { s ->
            try {
                buildExtra(Uri.parse(s))
            } catch (e: Exception) {
                null
            }
        }
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
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        ) else arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        val result = ArrayList<MediaFile>()
        context.contentResolver.query(base, projection, null, null, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val wCol = c.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val hCol = c.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
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
                    folder = if (folder.isNullOrBlank()) "Sin carpeta" else folder,
                    width = if (wCol >= 0) c.getInt(wCol) else 0,
                    height = if (hCol >= 0) c.getInt(hCol) else 0
                )
            }
        }
        return result
    }

    private fun buildExtra(uri: Uri): MediaFile? {
        val cr = context.contentResolver
        val mime = cr.getType(uri) ?: return null
        val video = mime.startsWith("video")
        if (!video && !mime.startsWith("image")) return null

        var name = ""
        var size = 0L
        cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                name = c.getString(0) ?: ""
                size = if (!c.isNull(1)) c.getLong(1) else 0L
            }
        }
        // Verifica que todavía se pueda leer
        cr.openFileDescriptor(uri, "r")?.close() ?: return null

        var duration = 0L
        var w = 0
        var h = 0
        if (video) {
            val r = MediaMetadataRetriever()
            try {
                r.setDataSource(context, uri)
                duration = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            } catch (e: Exception) {
                // sin metadatos
            } finally {
                r.release()
            }
        } else {
            try {
                cr.openInputStream(uri)?.use { input ->
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(input, null, opts)
                    w = opts.outWidth
                    h = opts.outHeight
                }
            } catch (e: Exception) {
                // sin medidas
            }
        }

        return MediaFile(
            id = uri.hashCode().toLong(),
            uri = uri,
            name = name.ifEmpty { uri.lastPathSegment ?: "Archivo" },
            isVideo = video,
            dateAdded = System.currentTimeMillis() / 1000,
            size = size,
            durationMs = duration,
            folder = "Agregados",
            width = w,
            height = h,
            isExtra = true
        )
    }
}
