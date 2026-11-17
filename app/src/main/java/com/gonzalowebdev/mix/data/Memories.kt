package com.gonzalowebdev.mix.data

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
    val cover: MediaFile
)

object MemoryGenerator {

    private val es: Locale = Locale.forLanguageTag("es-AR")

    private fun monthName(date: LocalDate): String =
        date.month.getDisplayName(TextStyle.FULL, es)

    private fun pickCover(files: List<MediaFile>, rnd: Random): MediaFile {
        val images = files.filter { !it.isVideo }
        return (if (images.isNotEmpty()) images else files).random(rnd)
    }

    private fun make(id: String, title: String, subtitle: String, files: List<MediaFile>, rnd: Random): Memory =
        Memory(id, title, subtitle, files, pickCover(files, rnd))

    /**
     * Genera los recuerdos del día. Es estable durante el día (la semilla es la fecha)
     * y cambia al día siguiente.
     */
    fun generate(media: List<MediaFile>, favorites: Set<String>, today: LocalDate): List<Memory> {
        if (media.isEmpty()) return emptyList()
        val rnd = Random(today.toEpochDay())
        val zone = ZoneId.systemDefault()
        val dated = media.filter { it.realDate }

        fun dateOf(f: MediaFile): LocalDate =
            Instant.ofEpochMilli(f.dateTaken).atZone(zone).toLocalDate()

        val out = ArrayList<Memory>()

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
            val files = byYearsAgo.getValue(years).sortedBy { it.dateTaken }
            if (files.size >= 4) {
                out += make(
                    "otd-$years",
                    "Un día como hoy",
                    if (years == 1) "Hace 1 año" else "Hace $years años",
                    files,
                    rnd
                )
            }
        }

        // Momentos: días con muchas fotos o videos
        val byDay = dated.groupBy { dateOf(it) }
        val eventDays = byDay.filter { (day, list) ->
            list.size >= 15 && day.isBefore(today.minusDays(14))
        }.keys.toList().shuffled(rnd).take(3)
        for (day in eventDays) {
            val files = byDay.getValue(day).sortedBy { it.dateTaken }
            out += make(
                "ev-$day",
                "${day.dayOfMonth} de ${monthName(day)} de ${day.year}",
                "${files.size} fotos y videos",
                files,
                rnd
            )
        }

        // El mes pasado
        val lastMonth = today.minusMonths(1)
        val lastMonthFiles = dated.filter {
            val d = dateOf(it)
            d.year == lastMonth.year && d.month == lastMonth.month
        }.sortedBy { it.dateTaken }
        if (lastMonthFiles.size >= 6) {
            out += make(
                "lm",
                "El mes pasado",
                monthName(lastMonth).replaceFirstChar { it.uppercase() } + " " + lastMonth.year,
                lastMonthFiles,
                rnd
            )
        }

        // Favoritos
        val favs = media.filter { it.key in favorites }
        if (favs.size >= 6) {
            out += make("fav", "Tus favoritos", "Selección del día", favs.shuffled(rnd).take(30), rnd)
        }

        // Redescubrí
        val old = dated.filter { dateOf(it).isBefore(today.minusYears(1)) }
        if (old.size >= 20) {
            out += make("red", "Redescubrí", "Del archivo", old.shuffled(rnd).take(30), rnd)
        }

        // Mezcla del día
        if (media.size >= 10) {
            out += make("mix", "Mezcla del día", "Al azar", media.shuffled(rnd).take(40), rnd)
        }

        // Carpetas con mucho contenido
        val byFolder = media.groupBy { it.folderId }.filter { it.value.size >= 30 }
        for (id in byFolder.keys.toList().shuffled(rnd).take(2)) {
            val files = byFolder.getValue(id)
            out += make("fo-$id", "De " + files.first().folder, "Selección al azar", files.shuffled(rnd).take(30), rnd)
        }

        return out
    }
}
