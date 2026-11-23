package com.gonzalowebdev.mix.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val tag: String, val build: Int, val url: String)

sealed class UpdateResult {
    data class Available(val info: UpdateInfo) : UpdateResult()
    object UpToDate : UpdateResult()
    object Error : UpdateResult()
}

/** Consulta la última versión publicada en GitHub (única conexión a internet de la app). */
object UpdateChecker {
    private const val URL_RELEASES = "https://api.github.com/repos/gonzalowebdev/Mix/releases?per_page=1"

    suspend fun check(currentBuild: Int): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val conn = URL(URL_RELEASES).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val arr = JSONArray(text)
            if (arr.length() == 0) {
                UpdateResult.UpToDate
            } else {
                val release = arr.getJSONObject(0)
                val tag = release.getString("tag_name")
                val build = Regex("\\d+").find(tag)?.value?.toIntOrNull() ?: 0
                val assets = release.getJSONArray("assets")
                val url = if (assets.length() > 0) {
                    assets.getJSONObject(0).getString("browser_download_url")
                } else {
                    release.getString("html_url")
                }
                if (build > currentBuild) UpdateResult.Available(UpdateInfo(tag, build, url)) else UpdateResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateResult.Error
        }
    }
}
