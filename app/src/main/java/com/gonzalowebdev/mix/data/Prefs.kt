package com.gonzalowebdev.mix.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

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

    /** true = el feed se desliza en vertical (estilo TikTok) */
    var feedVertical: Boolean
        get() = sp.getBoolean("feedVertical", true)
        set(v) = sp.edit().putBoolean("feedVertical", v).apply()

    /** Modo feed: sin avance automático; los videos se repiten en bucle */
    var feedMode: Boolean
        get() = sp.getBoolean("feedMode", false)
        set(v) = sp.edit().putBoolean("feedMode", v).apply()

    /** Movimiento lento en las imágenes de la presentación (apagado por defecto) */
    var kenBurns: Boolean
        get() = sp.getBoolean("kenBurns", false)
        set(v) = sp.edit().putBoolean("kenBurns", v).apply()

    /** 0 = apagado, 1 = horizontal, 2 = vertical, 3 = ambos */
    var mirror: Int
        get() = sp.getInt("mirror", 0)
        set(v) = sp.edit().putInt("mirror", v).apply()

    /** 0 = apagado, 1 = TV de tubo, 2 = VHS */
    var tvMode: Int
        get() = sp.getInt("tvMode", 0)
        set(v) = sp.edit().putInt("tvMode", v).apply()

    var tvStatic: Boolean
        get() = sp.getBoolean("tvStatic", false)
        set(v) = sp.edit().putBoolean("tvStatic", v).apply()

    var ambient: Boolean
        get() = sp.getBoolean("ambient", false)
        set(v) = sp.edit().putBoolean("ambient", v).apply()

    var hiddenMemories: Set<String>
        get() = (sp.getStringSet("hiddenMemories", emptySet()) ?: emptySet()).toSet()
        set(v) = sp.edit().putStringSet("hiddenMemories", v).apply()

    var pinnedMemories: Set<String>
        get() = (sp.getStringSet("pinnedMemories", emptySet()) ?: emptySet()).toSet()
        set(v) = sp.edit().putStringSet("pinnedMemories", v).apply()

    /** Día en que se generaron los recuerdos y qué ids se mostraron hoy / ayer. */
    var memDay: String
        get() = sp.getString("memDay", "") ?: ""
        set(v) = sp.edit().putString("memDay", v).apply()

    var memIds: Set<String>
        get() = (sp.getStringSet("memIds", emptySet()) ?: emptySet()).toSet()
        set(v) = sp.edit().putStringSet("memIds", v).apply()

    var memPrev: Set<String>
        get() = (sp.getStringSet("memPrev", emptySet()) ?: emptySet()).toSet()
        set(v) = sp.edit().putStringSet("memPrev", v).apply()

    var recentSearches: List<String>
        get() = (sp.getString("recentSearches", "") ?: "").split("\n").filter { it.isNotBlank() }
        set(v) = sp.edit().putString("recentSearches", v.joinToString("\n")).apply()

    /** Estilos visuales guardados (JSON). */
    var stylesJson: String
        get() = sp.getString("stylesJson", "[]") ?: "[]"
        set(v) = sp.edit().putString("stylesJson", v).apply()

    var autoUpdateCheck: Boolean
        get() = sp.getBoolean("autoUpdateCheck", false)
        set(v) = sp.edit().putBoolean("autoUpdateCheck", v).apply()

    var lastUpdateCheck: Long
        get() = sp.getLong("lastUpdateCheck", 0L)
        set(v) = sp.edit().putLong("lastUpdateCheck", v).apply()

    /** Copia de todos los ajustes (para el respaldo). */
    fun dumpJson(): JSONObject {
        val out = JSONObject()
        for ((k, v) in sp.all) {
            val e = JSONObject()
            when (v) {
                is Boolean -> { e.put("t", "b"); e.put("v", v) }
                is Int -> { e.put("t", "i"); e.put("v", v) }
                is Long -> { e.put("t", "l"); e.put("v", v) }
                is Float -> { e.put("t", "f"); e.put("v", v.toDouble()) }
                is String -> { e.put("t", "z"); e.put("v", v) }
                is Set<*> -> { e.put("t", "s"); e.put("v", JSONArray(v.map { it.toString() })) }
                else -> continue
            }
            out.put(k, e)
        }
        return out
    }

    fun restoreJson(json: JSONObject) {
        val ed = sp.edit()
        val keys = json.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val e = json.getJSONObject(k)
            when (e.getString("t")) {
                "b" -> ed.putBoolean(k, e.getBoolean("v"))
                "i" -> ed.putInt(k, e.getInt("v"))
                "l" -> ed.putLong(k, e.getLong("v"))
                "f" -> ed.putFloat(k, e.getDouble("v").toFloat())
                "z" -> ed.putString(k, e.getString("v"))
                "s" -> {
                    val arr = e.getJSONArray("v")
                    ed.putStringSet(k, (0 until arr.length()).map { arr.getString(it) }.toSet())
                }
            }
        }
        ed.apply()
    }

    var tipsSeen: Boolean
        get() = sp.getBoolean("tipsSeen", false)
        set(v) = sp.edit().putBoolean("tipsSeen", v).apply()

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
