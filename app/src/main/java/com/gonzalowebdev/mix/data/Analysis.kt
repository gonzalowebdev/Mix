package com.gonzalowebdev.mix.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Size
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

/** Resultado del análisis de una imagen: huella visual y nitidez. */
@Entity(tableName = "analysis")
data class MediaAnalysis(
    @PrimaryKey val uri: String,
    val hash: Long,
    val sharp: Float
)

@Dao
interface AnalysisDao {
    @Query("SELECT * FROM analysis")
    suspend fun all(): List<MediaAnalysis>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaAnalysis>)

    @Query("DELETE FROM analysis WHERE uri IN (:uris)")
    suspend fun delete(uris: List<String>)
}

/** Base aparte de la de listas, para no tocar nunca esas listas. */
@Database(entities = [MediaAnalysis::class], version = 1, exportSchema = false)
abstract class AnalysisDb : RoomDatabase() {
    abstract fun dao(): AnalysisDao

    companion object {
        fun create(context: Context): AnalysisDb =
            Room.databaseBuilder(context, AnalysisDb::class.java, "analysis.db").build()
    }
}

/** Análisis clásico (sin IA): hash perceptual para duplicados y varianza del laplaciano para nitidez. */
object MediaAnalyzer {

    private fun loadSmall(context: Context, file: MediaFile): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                context.contentResolver.loadThumbnail(file.uri, Size(160, 160), null)
            } else {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(file.uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                var sample = 1
                while (opts.outWidth / (sample * 2) >= 160 && opts.outHeight / (sample * 2) >= 160) sample *= 2
                val real = BitmapFactory.Options().apply { inSampleSize = sample }
                context.contentResolver.openInputStream(file.uri)?.use { BitmapFactory.decodeStream(it, null, real) }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun gray(px: Int): Float {
        val r = (px shr 16) and 0xFF
        val g = (px shr 8) and 0xFF
        val b = px and 0xFF
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    fun analyze(context: Context, file: MediaFile): MediaAnalysis? {
        val bmp = loadSmall(context, file) ?: return null
        try {
            // Huella perceptual (dHash 8x8)
            val tiny = Bitmap.createScaledBitmap(bmp, 9, 8, true)
            val t = IntArray(9 * 8)
            tiny.getPixels(t, 0, 9, 0, 0, 9, 8)
            var hash = 0L
            var bit = 0
            for (y in 0 until 8) {
                for (x in 0 until 8) {
                    if (gray(t[y * 9 + x]) > gray(t[y * 9 + x + 1])) hash = hash or (1L shl bit)
                    bit++
                }
            }
            if (tiny !== bmp) tiny.recycle()

            // Nitidez: varianza del laplaciano sobre 96x96
            val side = 96
            val small = Bitmap.createScaledBitmap(bmp, side, side, true)
            val px = IntArray(side * side)
            small.getPixels(px, 0, side, 0, 0, side, side)
            val g = FloatArray(side * side) { gray(px[it]) }
            var sum = 0.0
            var sumSq = 0.0
            var n = 0
            for (y in 1 until side - 1) {
                for (x in 1 until side - 1) {
                    val i = y * side + x
                    val lap = 4f * g[i] - g[i - 1] - g[i + 1] - g[i - side] - g[i + side]
                    sum += lap
                    sumSq += lap * lap
                    n++
                }
            }
            val mean = sum / n
            val variance = (sumSq / n - mean * mean).toFloat()
            if (small !== bmp) small.recycle()
            return MediaAnalysis(file.key, hash, variance)
        } catch (e: Exception) {
            return null
        } finally {
            bmp.recycle()
        }
    }
}

/** Agrupa imágenes casi idénticas. Cada grupo queda ordenado: la mejor primero. */
fun findDuplicateGroups(
    files: List<MediaFile>,
    analysis: Map<String, MediaAnalysis>,
    favorites: Set<String>
): List<List<MediaFile>> {
    val items = files.filter { !it.isVideo && analysis[it.key] != null }
    val n = items.size
    if (n < 2) return emptyList()
    val hashes = LongArray(n) { analysis.getValue(items[it].key).hash }
    val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var r = x
        while (parent[r] != r) {
            parent[r] = parent[parent[r]]
            r = parent[r]
        }
        return r
    }
    // Con 4 bandas de 16 bits, dos huellas a distancia <= 3 comparten al menos una banda
    for (band in 0 until 4) {
        val buckets = HashMap<Int, MutableList<Int>>()
        for (i in 0 until n) {
            val key = ((hashes[i] ushr (band * 16)) and 0xFFFFL).toInt()
            buckets.getOrPut(key) { ArrayList() }.add(i)
        }
        for (list in buckets.values) {
            if (list.size < 2 || list.size > 60) continue
            for (a in 0 until list.size) {
                for (b in a + 1 until list.size) {
                    if (java.lang.Long.bitCount(hashes[list[a]] xor hashes[list[b]]) <= 3) {
                        val ra = find(list[a])
                        val rb = find(list[b])
                        if (ra != rb) parent[ra] = rb
                    }
                }
            }
        }
    }
    val groups = HashMap<Int, MutableList<MediaFile>>()
    for (i in 0 until n) groups.getOrPut(find(i)) { ArrayList() }.add(items[i])
    return groups.values
        .filter { it.size >= 2 }
        .map { g ->
            g.sortedWith(
                compareByDescending<MediaFile> { it.key in favorites }
                    .thenByDescending { analysis[it.key]?.sharp ?: 0f }
                    .thenByDescending { it.size }
            )
        }
        .sortedByDescending { g -> g.sumOf { it.size } }
}
