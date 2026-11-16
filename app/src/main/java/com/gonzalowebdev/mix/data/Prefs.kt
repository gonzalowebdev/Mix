package com.gonzalowebdev.mix.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("mix", Context.MODE_PRIVATE)
    private val positions = context.getSharedPreferences("mix_positions", Context.MODE_PRIVATE)
    private val subtitles = context.getSharedPreferences("mix_subtitles", Context.MODE_PRIVATE)

    var imageSeconds: Int
        get() = sp.getInt("imageSeconds", 5)
        set(v) = sp.edit().putInt("imageSeconds", v).apply()

    var shuffle: Boolean
        get() = sp.getBoolean("shuffle", false)
        set(v) = sp.edit().putBoolean("shuffle", v).apply()

    /** 0 = no repetir, 1 = repetir todo, 2 = repetir uno */
    var repeatMode: Int
        get() = sp.getInt("repeatMode", 1)
        set(v) = sp.edit().putInt("repeatMode", v).apply()

    /** 0 = ninguna, 1 = fundido, 2 = deslizar */
    var transition: Int
        get() = sp.getInt("transition", 1)
        set(v) = sp.edit().putInt("transition", v).apply()

    var speed: Float
        get() = sp.getFloat("speed", 1f)
        set(v) = sp.edit().putFloat("speed", v).apply()

    var colorFilter: String
        get() = sp.getString("colorFilter", "NONE") ?: "NONE"
        set(v) = sp.edit().putString("colorFilter", v).apply()

    var bgPlayback: Boolean
        get() = sp.getBoolean("bgPlayback", false)
        set(v) = sp.edit().putBoolean("bgPlayback", v).apply()

    var mosaic: Boolean
        get() = sp.getBoolean("mosaic", true)
        set(v) = sp.edit().putBoolean("mosaic", v).apply()

    var extraUris: Set<String>
        get() = (sp.getStringSet("extraUris", emptySet()) ?: emptySet()).toSet()
        set(v) = sp.edit().putStringSet("extraUris", v).apply()

    var favorites: Set<String>
        get() = (sp.getStringSet("favorites", emptySet()) ?: emptySet()).toSet()
        set(v) = sp.edit().putStringSet("favorites", v).apply()

    var smartListsJson: String
        get() = sp.getString("smartLists", "[]") ?: "[]"
        set(v) = sp.edit().putString("smartLists", v).apply()

    fun getPosition(uri: String): Long = positions.getLong(uri, 0L)
    fun setPosition(uri: String, ms: Long) = positions.edit().putLong(uri, ms).apply()
    fun clearPosition(uri: String) = positions.edit().remove(uri).apply()

    /** Formato guardado: "uriDelSubtitulo|mimeType" */
    fun getSubtitle(uri: String): String? = subtitles.getString(uri, null)
    fun setSubtitle(uri: String, value: String?) {
        if (value == null) subtitles.edit().remove(uri).apply() else subtitles.edit().putString(uri, value).apply()
    }
}
