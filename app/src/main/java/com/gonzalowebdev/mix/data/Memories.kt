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

/** Un recuerdo: una colección temática armada automáticamente. */
data class Memory(
    val id: String,
    val title: String,
    val subtitle: String,
    val files: List<MediaFile>,
    val cover: MediaFile,
    val pinned: Boolean = false
)

object MemoryGenerator {

    /** Por debajo de este valor una foto se considera borrosa. */
    const val BLUR_THRESHOLD = 18f

    private val es: Locale = Locale.forLanguageTag("es-AR")

    private fun monthName(date: LocalDate): String =
        date.month.getDisplayName(TextStyle.FULL, es)

    private fun season(date: LocalDate): String = when (date.monthValue) {
        12, 1, 2 -> "Verano"
        3, 4, 5 -> "Otoño"
        6, 7, 8 -> "Invierno"
        else -> "Primavera"
    }

    private fun sharpOf(f: MediaFile, analysis: Map<String, MediaAnalysis>): Float =
        analysis[f.key]?.sharp ?: 50f

    /** Fotos aceptables para un recuerdo: sin capturas, sin mensajería y sin borrosas. */
    private fun isGood(f: MediaFile, analysis: Map<String, MediaAnalysis>, favorites: Set<String>): Boolean {
        if (f.isScreenshot) return false
        val fav = f.key in favorites
        if (f.isMessagingJunk && !fav) return false
        if (f.isVideo || fav) return true
        val sharp = analysis[f.key]?.sharp ?: return true
        return sharp >= BLUR_THRESHOLD
    }

    /** Quita fotos casi idénticas seguidas, quedándose con la más nítida. */
    private fun dedupe(
        files: List<MediaFile>,
        analysis: Map<String, MediaAnalysis>,
        favorites: Set<String>
    ): List<MediaFile> {
        val kept = ArrayList<MediaFile>()
        for (f in files) {
            val h = analysis[f.key]?.hash
            if (f.isVideo || h == null) {
                kept += f
                continue
            }
            var similarIndex = -1
            val from = maxOf(0, kept.size - 8)
            for (i in kept.size - 1 downTo from) {
                val kh = analysis[kept[i].key]?.hash ?: continue
                if (java.lang.Long.bitCount(h xor kh) <= 6) {
                    similarIndex = i
                    break
                }
            }
            if (similarIndex < 0) {
                kept += f
            } else {
                val other = kept[similarIndex]
                val better = (f.key in favorites && other.key !in favorites) ||
                    (sharpOf(f, analysis) > sharpOf(other, analysis) + 5f && other.key !in favorites)
                if (better) kept[similarIndex] = f
            }
        }
        return kept
    }

    private fun pickCover(
        files: List<MediaFile>,
        analysis: Map<String, MediaAnalysis>,
        favorites: Set<String>,
        rnd: Random
    ): MediaFile {
        fun score(f: MediaFile): Float {
            var s = sharpOf(f, analysis).coerceAtMost(300f)
            if (f.key in favorites) s += 120f
            if (!f.isVideo) s += 40f
            return s + rnd.nextFloat() * 10f
        }
        return files.maxByOrNull { score(it) } ?: files.first()
    }

    private fun eventTitle(start: LocalDate, end: LocalDate, count: Int): Pair<String, String> {
        val days = ChronoUnit.DAYS.between(start, end).toInt() + 1
        val range = if (days == 1) {
            "${start.dayOfMonth} de ${monthName(start)} de ${start.year}"
        } else if (start.month == end.month) {
            "${start.dayOfMonth}–${end.dayOfMonth} de ${monthName(end)} de ${end.year}"
        } else {
            "${start.dayOfMonth} de ${monthName(start)} – ${end.dayOfMonth} de ${monthName(end)} de ${end.year}"
        }
        var hasSat = false
        var hasSun = false
        var d = start
        while (!d.isAfter(end)) {
            if (d.dayOfWeek == DayOfWeek.SATURDAY) hasSat = true
            if (d.dayOfWeek == DayOfWeek.SUNDAY) hasSun = true
            d = d.plusDays(1)
        }
        val title = when {
            days == 1 -> range
            days <= 4 && hasSat && hasSun -> if (days >= 3) "Fin de semana largo" else "Fin de semana"
            days <= 13 -> "Escapada de ${monthName(start)}"
            else -> "${season(start.plusDays((days / 2).toLong()))} ${start.plusDays((days / 2).toLong()).year}"
        }
        val subtitle = if (days == 1) "$count fotos y videos" else "$range · $count fotos y videos"
        return title to subtitle
    }

    /**
     * Genera los recuerdos del día. Es estable durante el día (la semilla es la fecha)
     * y cambia al día siguiente.
     *
     * @param avoid ids que se mostraron ayer (no se repiten dos días seguidos)
     * @param hidden ids que la persona ocultó
     * @param pinned ids que la persona fijó (van primero y no se descartan)
     */
    fun generate(
        media: List<MediaFile>,
        favorites: Set<String>,
        today: LocalDate,
        analysis: Map<String, MediaAnalysis>,
        avoid: Set<String>,
        hidden: Set<String>,
        pinned: Set<String>
    ): List<Memory> {
        if (media.isEmpty()) return emptyList()
        val rnd = Random(today.toEpochDay())
        val zone = ZoneId.systemDefault()
        val good = media.filter { isGood(it, analysis, favorites) }
        val dated = good.filter { it.realDate }.sortedBy { it.dateTaken }

        fun dateOf(f: MediaFile): LocalDate =
            Instant.ofEpochMilli(f.dateTaken).atZone(zone).toLocalDate()

        val out = ArrayList<Memory>()
        fun add(id: String, title: String, subtitle: String, files: List<MediaFile>, min: Int) {
            val cleaned = dedupe(files, analysis, favorites)
            if (cleaned.size >= min) {
                out += Memory(id, title, subtitle, cleaned, pickCover(cleaned, analysis, favorites, rnd), id in pinned)
            }
        }

        // Un día como hoy (±3 días), agrupado por años atrás
        val byYearsAgo = HashMap<Int, MutableList<MediaFile>>()
        for (f in dated) {
            val d = dateOf(f)
            val yearsAgo = today.year - d.year
            if (yearsAgo >= 1) {
                val anniversary = d.withYear(today.year)
                if (abs(ChronoUnit.DAYS.between(anniversary, today)) <= 3) {
                    byYearsAgo.getOrPut(yearsAgo) { ArrayList() }.add(f)
                }
            }
        }
        for (years in byYearsAgo.keys.sorted()) {
            add(
                "otd-$years",
                "Un día como hoy",
                if (years == 1) "Hace 1 año" else "Hace $years años",
                byYearsAgo.getValue(years),
                4
            )
        }

        // Eventos reales: días seguidos con muchas fotos
        val clusters = ArrayList<MutableList<MediaFile>>()
        for (f in dated) {
            val last = clusters.lastOrNull()
            if (last != null && f.dateTaken - last.last().dateTaken <= 36L * 3600_000L) last += f
            else clusters += mutableListOf(f)
        }
        val events = clusters.filter {
            it.size >= 20 && dateOf(it.last()).isBefore(today.minusDays(14))
        }
        val eventPick = events.shuffled(rnd).take(4).toMutableList()
        for (c in events) {
            val id = "ev-${dateOf(c.first())}"
            if (id in pinned && c !in eventPick) eventPick += c
        }
        for (c in eventPick) {
            val start = dateOf(c.first())
            val (title, subtitle) = eventTitle(start, dateOf(c.last()), c.size)
            add("ev-$start", title, subtitle, c, 8)
        }

        // El mes pasado
        val lastMonth = today.minusMonths(1)
        add(
            "lm",
            "El mes pasado",
            monthName(lastMonth).replaceFirstChar { it.uppercase() } + " " + lastMonth.year,
            dated.filter {
                val d = dateOf(it)
                d.year == lastMonth.year && d.month == lastMonth.month
            },
            6
        )

        // Favoritos
        val favs = media.filter { it.key in favorites && !it.isScreenshot }
        if (favs.size >= 6) add("fav", "Tus favoritos", "Selección del día", favs.shuffled(rnd).take(30), 6)

        // Redescubrí
        val old = dated.filter { dateOf(it).isBefore(today.minusYears(1)) }
        if (old.size >= 20) add("red", "Redescubrí", "Del archivo", old.shuffled(rnd).take(30), 12)

        // Mezcla del día
        if (good.size >= 10) add("mix", "Mezcla del día", "Al azar", good.shuffled(rnd).take(40), 10)

        // Carpetas con mucho contenido
        val byFolder = good.groupBy { it.folderId }.filter { it.value.size >= 30 }
        for (id in byFolder.keys.toList().shuffled(rnd).take(2)) {
            val files = byFolder.getValue(id)
            add("fo-$id", "De " + files.first().folder, "Selección al azar", files.shuffled(rnd).take(30), 10)
        }

        // Ocultos fuera; los de ayer no se repiten (salvo los fijados); fijados primero
        return out
            .filter { it.id !in hidden && (it.id !in avoid || it.id in pinned) }
            .sortedByDescending { it.pinned }
    }
}
