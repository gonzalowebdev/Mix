package com.gonzalowebdev.mix

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import com.gonzalowebdev.mix.data.MediaThumbFetcher
import com.gonzalowebdev.mix.data.MediaThumbKeyer
import com.gonzalowebdev.mix.data.MixDb

class MixApp : Application(), ImageLoaderFactory {

    val db: MixDb by lazy { MixDb.create(this) }
    val analysisDb: com.gonzalowebdev.mix.data.AnalysisDb by lazy { com.gonzalowebdev.mix.data.AnalysisDb.create(this) }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
                add(VideoFrameDecoder.Factory())
                add(MediaThumbKeyer())
                add(MediaThumbFetcher.Factory())
            }
            .crossfade(true)
            .build()
}
