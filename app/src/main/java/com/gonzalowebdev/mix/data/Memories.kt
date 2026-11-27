package com.gonzalowebdev.mix.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

enum class MemoryKind { DAILY, THEME, LUCKY, OLD_IMAGES, OLD_VIDEOS, RECENT_PLAYED, RECENT_ADDED }

/** Una pastilla de Recuerdos: al tocarla se reproduce su lista. */
data class Memory(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: MemoryKind,
    val files: List<MediaFile>,
    val cover: MediaFile
)

object MemoryGenerator {

    private val es: Locale = Locale.forLanguageTag("es-AR")

    private fun monthName(date: LocalDate): String =
        date.month.getDisplayName(TextStyle.FULL, es)

    private fun season(date: LocalDate): String = when (date.monthValue) {
        12, 1, 2 -> "Verano"
        3, 4, 5 -> "Otoño"
        6, 7, 8 -> "Invierno"
        else -> "Primavera"
    }

    private fun build(id: String, title: String, subtitle: String, kind: MemoryKind, files: List<MediaFile>): Memory? {
        if (files.isEmpty()) return null
        val cover = files.firstOrNull { !it.isVideo } ?: files.first()
        return Memory(id, title, subtitle, kind, files, cover)
    }

    /** Fotos y videos aceptables: sin capturas ni basura de mensajería (salvo favoritos). */
    private fun goodMedia(media: List<MediaFile>, favorites: Set<String>): List<MediaFile> =
        media.filter { !it.isScreenshot && (!it.isMessagingJunk || it.key in favorites) }.ifEmpty { media }

    /** "Feeling lucky": 3 archivos al azar, distintos cada vez que se toca. */
    fun lucky(media: List<MediaFile>, favorites: Set<String>): Memory? =
        build("lucky", "Feeling lucky", "3 al azar", MemoryKind.LUCKY, goodMedia(media, favorites).shuffled().take(3))

    private class Theme(val name: String, val files: List<MediaFile>)

    private fun eventName(start: LocalDate, end: LocalDate): String {
        val days = ChronoUnit.DAYS.between(start, end).toInt() + 1
        var hasSat = false
        var hasSun = false
        var d = start
        while (!d.isAfter(end)) {
            if (d.dayOfWeek == DayOfWeek.SATURDAY) hasSat = true
            if (d.dayOfWeek == DayOfWeek.SUNDAY) hasSun = true
            d = d.plusDays(1)
        }
        return when {
            days == 1 -> "${start.dayOfMonth} de ${monthName(start)} de ${start.year}"
            days <= 4 && hasSat && hasSun -> if (days >= 3) "Fin de semana largo" else "Fin de semana"
            days <= 13 -> "Escapada de ${monthName(start)}"
            else -> "${season(start)} ${start.year}"
        }
    }

    /**
     * Genera las pastillas del día. La semilla es la fecha: el mix del día y los Mix 1, 2 y 3
     * se mantienen durante el día y cambian al siguiente.
     */
    fun generate(
        media: List<MediaFile>,
        favorites: Set<String>,
        today: LocalDate,
        recent: List<String>
    ): List<Memory> {
        if (media.isEmpty()) return emptyList()
        val seed = today.toEpochDay()
        val zone = ZoneId.systemDefault()
        val good = goodMedia(media, favorites)
        val dated = good.filter { it.realDate }

        fun dateOf(f: MediaFile): LocalDate =
            Instant.ofEpochMilli(f.dateTaken).atZone(zone).toLocalDate()

        val out = ArrayList<Memory>()

        // 1. Mix del día: 20 archivos al azar que cambian cada día
        build("daily", "Mix del día", "20 al azar", MemoryKind.DAILY, good.shuffled(Random(seed)).take(20))?.let { out += it }

        // 2-4. Mix 1, 2 y 3: selecciones temáticas del día
        val rnd = Random(seed + 1)
        val themes = ArrayList<() -> Theme?>()

        themes += {
            val list = dated.filter {
                val d = dateOf(it)
                d.year < today.year && abs(ChronoUnit.DAYS.between(d.withYear(today.year), today)) <= 3
            }
            if (list.size >= 6) Theme("Un día como hoy", list) else null
        }
        themes += {
            val list = good.filter { it.key in favorites }
            if (list.size >= 6) Theme("Tus favoritos", list) else null
        }
        themes += {
            val clusters = ArrayList<MutableList<MediaFile>>()
            for (f in dated.sortedBy { it.dateTaken }) {
                val last = clusters.lastOrNull()
                if (last != null && f.dateTaken - last.last().dateTaken <= 36L * 3600_000L) last += f
                else clusters += mutableListOf(f)
            }
            val events = clusters.filter { it.size >= 15 && dateOf(it.last()).isBefore(today.minusDays(14)) }
            val pick = events.randomOrNull(rnd)
            if (pick != null) Theme(eventName(dateOf(pick.first()), dateOf(pick.last())), pick) else null
        }
        themes += {
            val folders = good.groupBy { it.folder }.filter { it.value.size >= 15 }
            val pick = folders.entries.toList().randomOrNull(rnd)
            if (pick != null) Theme("De ${pick.key}", pick.value) else null
        }
        themes += {
            val list = good.filter { it.isVideo }
            if (list.size >= 8) Theme("Solo videos", list) else null
        }
        themes += {
            val list = dated.filter {
                val h = Instant.ofEpochMilli(it.dateTaken).atZone(zone).hour
                h >= 20 || h < 5
            }
            if (list.size >= 10) Theme("De noche", list) else null
        }
        themes += {
            val ago = today.minusYears(1)
            val list = dated.filter { val d = dateOf(it); d.year == ago.year && d.month == ago.month }
            if (list.size >= 8) Theme(monthName(ago).replaceFirstChar { it.uppercase() } + " " + ago.year, list) else null
        }
        themes += {
            val s = season(today)
            val list = dated.filter { val d = dateOf(it); d.year < today.year && season(d) == s }
            if (list.size >= 15) Theme("${s}s de otros años", list) else null
        }

        val picked = ArrayList<Theme>()
        for (t in themes.shuffled(rnd)) {
            if (picked.size >= 3) break
            val theme = t() ?: continue
            if (picked.none { it.name == theme.name }) picked += theme
        }
        for (i in 0 until 3) {
            val theme = picked.getOrNull(i)
            val files = theme?.files?.shuffled(Random(seed + 20 + i))?.take(25)
                ?: good.shuffled(Random(seed + 30 + i)).take(25)
            build("mix${i + 1}", "Mix ${i + 1}", theme?.name ?: "Al azar", MemoryKind.THEME, files)?.let { out += it }
        }

        // 5. Feeling lucky
        lucky(media, favorites)?.let { out += it }

        // 6-7. Imágenes y videos para recordar: lo más viejo de la biblioteca
        fun oldest(list: List<MediaFile>, salt: Long): List<MediaFile> {
            val ordered = list.filter { it.realDate }.ifEmpty { list }.sortedBy { it.dateTaken }
            val pool = ordered.take(maxOf(30, ordered.size / 4))
            return pool.shuffled(Random(seed + salt)).take(20)
        }
        build("old-img", "Imágenes para recordar", "Del archivo", MemoryKind.OLD_IMAGES, oldest(good.filter { !it.isVideo }, 5))?.let { out += it }
        build("old-vid", "Videos para recordar", "Del archivo", MemoryKind.OLD_VIDEOS, oldest(good.filter { it.isVideo }, 6))?.let { out += it }

        // 8. Últimas reproducidas
        val byKey = media.associateBy { it.key }
        build("recent-played", "Últimas reproducidas", "Lo último que viste", MemoryKind.RECENT_PLAYED, recent.mapNotNull { byKey[it] }.take(50))?.let { out += it }

        // 9. Últimas agregadas
        build("recent-added", "Últimas agregadas", "Lo más nuevo", MemoryKind.RECENT_ADDED, media.sortedByDescending { it.dateAdded }.take(40))?.let { out += it }

        return out
    }
}
