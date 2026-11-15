package com.gonzalowebdev.mix.data

import android.net.Uri

data class MediaFile(
    val id: Long,
    val uri: Uri,
    val name: String,
    val isVideo: Boolean,
    val dateAdded: Long,
    val size: Long,
    val durationMs: Long,
    val folder: String
) {
    val key: String = uri.toString()
    fun toQueueItem() = QueueItem(key, name, isVideo, durationMs)
}

data class QueueItem(
    val uri: String,
    val name: String,
    val isVideo: Boolean,
    val durationMs: Long
)

enum class TypeFilter { ALL, IMAGES, VIDEOS }

enum class SortField(val label: String) {
    NAME("Nombre"),
    DATE("Fecha"),
    SIZE("Tamaño"),
    DURATION("Duración"),
    RANDOM("Aleatorio")
}
