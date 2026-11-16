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
    val folder: String,
    val width: Int = 0,
    val height: Int = 0,
    val isExtra: Boolean = false
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

/** Lista inteligente: se recalcula sola con las reglas indicadas. */
data class SmartList(
    val id: Long,
    val name: String,
    val type: TypeFilter = TypeFilter.ALL,
    val folder: String? = null,
    val minMinutes: Int = 0,
    val maxMinutes: Int = 0,
    val query: String = "",
    val favoritesOnly: Boolean = false
)

private fun saturation(s: Float): FloatArray {
    val r = 0.2126f
    val g = 0.7152f
    val b = 0.0722f
    return floatArrayOf(
        r + (1 - r) * s, g - g * s, b - b * s, 0f, 0f,
        r - r * s, g + (1 - g) * s, b - b * s, 0f, 0f,
        r - r * s, g - g * s, b + (1 - b) * s, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
}

/** Matrices 4x5 (formato ColorMatrix de Android). */
enum class ColorFilterType(val label: String, val values: FloatArray?) {
    NONE("Ninguno", null),
    SEPIA(
        "Sepia",
        floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    BW("Blanco y negro", saturation(0f)),
    WARM(
        "Cálido",
        floatArrayOf(
            1.12f, 0f, 0f, 0f, 8f,
            0f, 1.0f, 0f, 0f, 0f,
            0f, 0f, 0.80f, 0f, -8f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    COOL(
        "Frío",
        floatArrayOf(
            0.88f, 0f, 0f, 0f, 0f,
            0f, 1.0f, 0f, 0f, 0f,
            0f, 0f, 1.15f, 0f, 8f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    BLUE(
        "Azul",
        floatArrayOf(
            0.70f, 0f, 0f, 0f, 0f,
            0f, 0.90f, 0f, 0f, 0f,
            0f, 0f, 1.30f, 0f, 14f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    VIVID("Vívido", saturation(1.7f)),
    NIGHT(
        "Noche",
        floatArrayOf(
            0.90f, 0f, 0f, 0f, 0f,
            0f, 0.35f, 0f, 0f, 0f,
            0f, 0f, 0.25f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
}
