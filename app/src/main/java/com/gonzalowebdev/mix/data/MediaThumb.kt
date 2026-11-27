package com.gonzalowebdev.mix.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options
import coil.size.pxOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Miniatura de una imagen o video. Usa las miniaturas que ya guarda el sistema
 * (mucho más rápido que decodificar el archivo o sacar un fotograma cada vez).
 */
data class MediaThumb(val uri: Uri, val isVideo: Boolean)

class MediaThumbKeyer : Keyer<MediaThumb> {
    override fun key(data: MediaThumb, options: Options): String = "thumb:${data.uri}"
}

class MediaThumbFetcher(
    private val data: MediaThumb,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val ctx = options.context
        val w = options.size.width.pxOrElse { 512 }.coerceAtLeast(32)
        val h = options.size.height.pxOrElse { 512 }.coerceAtLeast(32)
        val bmp = withContext(Dispatchers.IO) { load(ctx, w, h) }
            ?: throw IllegalStateException("Sin miniatura")
        return DrawableResult(
            drawable = BitmapDrawable(ctx.resources, bmp),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }

    private fun load(ctx: Context, w: Int, h: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                return ctx.contentResolver.loadThumbnail(data.uri, Size(w, h), null)
            } catch (e: Exception) {
                // sin miniatura del sistema: se arma a mano
            }
        }
        return if (data.isVideo) videoFrame(ctx, w) else decodeImage(ctx, w, h)
    }

    private fun videoFrame(ctx: Context, w: Int): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(ctx, data.uri)
            val frame = r.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: r.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (frame != null && frame.width > w) {
                val ratio = w.toFloat() / frame.width
                Bitmap.createScaledBitmap(frame, w, (frame.height * ratio).toInt().coerceAtLeast(1), true)
            } else {
                frame
            }
        } catch (e: Exception) {
            null
        } finally {
            r.release()
        }
    }

    private fun decodeImage(ctx: Context, w: Int, h: Int): Bitmap? = try {
        val cr = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(data.uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= w && bounds.outHeight / (sample * 2) >= h) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        cr.openInputStream(data.uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    } catch (e: Exception) {
        null
    }

    class Factory : Fetcher.Factory<MediaThumb> {
        override fun create(data: MediaThumb, options: Options, imageLoader: ImageLoader): Fetcher =
            MediaThumbFetcher(data, options)
    }
}
