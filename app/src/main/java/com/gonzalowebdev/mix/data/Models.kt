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
    val folderId: String = "",
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

data class FolderInfo(
    val id: String,
    val name: String,
    val count: Int,
    val sample: MediaFile
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

private fun identity(): FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f
)

/** Devuelve la matriz que aplica primero b y después a. */
private fun mul(a: FloatArray, b: FloatArray): FloatArray {
    val r = FloatArray(20)
    for (i in 0 until 4) {
        for (j in 0 until 5) {
            var v = 0f
            for (k in 0 until 4) v += a[i * 5 + k] * b[k * 5 + j]
            if (j == 4) v += a[i * 5 + 4]
            r[i * 5 + j] = v
        }
    }
    return r
}

/** Aplica los pasos en el orden indicado. */
private fun chain(vararg steps: FloatArray): FloatArray {
    var acc = steps[0]
    for (i in 1 until steps.size) acc = mul(steps[i], acc)
    return acc
}

private fun tint(
    r: Float, g: Float, b: Float,
    offR: Float = 0f, offG: Float = 0f, offB: Float = 0f
): FloatArray = floatArrayOf(
    r, 0f, 0f, 0f, offR,
    0f, g, 0f, 0f, offG,
    0f, 0f, b, 0f, offB,
    0f, 0f, 0f, 1f, 0f
)

private fun contrast(c: Float): FloatArray {
    val o = 128f * (1f - c)
    return tint(c, c, c, o, o, o)
}

private fun brightness(o: Float): FloatArray = tint(1f, 1f, 1f, o, o, o)

private fun mix(a: FloatArray, b: FloatArray, t: Float): FloatArray =
    FloatArray(20) { a[it] * (1f - t) + b[it] * t }

private fun sepiaMatrix(): FloatArray = floatArrayOf(
    0.393f, 0.769f, 0.189f, 0f, 0f,
    0.349f, 0.686f, 0.168f, 0f, 0f,
    0.272f, 0.534f, 0.131f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f
)

/** Matrices 4x5 (formato ColorMatrix de Android). */
enum class ColorFilterType(val label: String, val values: FloatArray?) {
    NONE("Ninguno", null),
    SEPIA("Sepia", sepiaMatrix()),
    BW("Blanco y negro", saturation(0f)),
    KODAK("Kodak", chain(saturation(1.25f), tint(1.08f, 1.02f, 0.88f, 4f, 0f, -4f), contrast(1.08f))),
    POLAROID("Polaroid", chain(saturation(0.85f), tint(1.08f, 1.04f, 0.95f, 6f, 4f, 10f), contrast(0.88f))),
    EIGHTIES("Años 80", chain(saturation(1.4f), tint(1.12f, 0.92f, 1.2f, 6f, 0f, 10f), contrast(1.05f))),
    VINTAGE("Vintage", chain(mix(identity(), sepiaMatrix(), 0.55f), contrast(0.88f), brightness(8f))),
    GOLDEN("Hora dorada", chain(saturation(1.15f), tint(1.2f, 1.06f, 0.78f, 6f, 0f, -6f))),
    CINEMA("Cine", chain(contrast(1.15f), saturation(1.1f), tint(1.06f, 1.0f, 1.04f, -4f, 0f, 8f))),
    NEON("Neón", chain(saturation(1.8f), tint(1.1f, 0.85f, 1.3f), contrast(1.15f))),
    LOMO("Lomo", chain(saturation(1.4f), tint(1.05f, 1.08f, 0.9f), contrast(1.3f))),
    FADED("Mate", chain(saturation(0.8f), contrast(0.8f), brightness(14f))),
    NOIR("Noir", chain(saturation(0f), contrast(1.45f))),
    ROSE("Rosado", chain(tint(1.15f, 0.92f, 1.05f, 8f, 0f, 6f), saturation(1.1f))),
    WARM("Cálido", tint(1.12f, 1.0f, 0.80f, 8f, 0f, -8f)),
    COOL("Frío", tint(0.88f, 1.0f, 1.15f, 0f, 0f, 8f)),
    BLUE("Azul", tint(0.70f, 0.90f, 1.30f, 0f, 0f, 14f)),
    VIVID("Vívido", saturation(1.7f)),
    NIGHT("Noche", tint(0.90f, 0.35f, 0.25f))
}
