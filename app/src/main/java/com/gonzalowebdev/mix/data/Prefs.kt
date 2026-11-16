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

    var muted: Boolean
        get() = sp.getBoolean("muted", true)
        set(v) = sp.edit().putBoolean("muted", v).apply()

    var typeFilter: String
        get() = sp.getString("typeFilter", "ALL") ?: "ALL"
        set(v) = sp.edit().putString("typeFilter", v).apply()

    var folderFilter: String?
        get() = sp.getString("folderFilter", null)
        set(v) {
            if (v == null) sp.edit().remove("folderFilter").apply() else sp.edit().putString("folderFilter", v).apply()
        }

    var favoritesOnly: Boolean
        get() = sp.getBoolean("favoritesOnly", false)
        set(v) = sp.edit().putBoolean("favoritesOnly", v).apply()

    var sortField: String
        get() = sp.getString("sortField", "RANDOM") ?: "RANDOM"
        set(v) = sp.edit().putString("sortField", v).apply()

    var sortAsc: Boolean
        get() = sp.getBoolean("sortAsc", false)
        set(v) = sp.edit().putBoolean("sortAsc", v).apply()

    /** true = se muestran todas las carpetas; false = solo las elegidas en selectedFolders */
    var useAllFolders: Boolean
        get() = sp.getBoolean("useAllFolders", true)
        set(v) = sp.edit().putBoolean("useAllFolders", v).apply()

    var selectedFolders: Set<String>
        get() = (sp.getStringSet("selectedFolders", emptySet()) ?: emptySet()).toSet()
        set(v) = sp.edit().putStringSet("selectedFolders", v).apply()

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
