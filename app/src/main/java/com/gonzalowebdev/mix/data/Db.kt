package com.gonzalowebdev.mix.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)

@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId")]
)
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val uri: String,
    val name: String,
    val isVideo: Boolean,
    val durationMs: Long,
    val position: Int
) {
    fun toQueueItem() = QueueItem(uri, name, isVideo, durationMs)
}

data class PlaylistWithCount(
    @Embedded val playlist: PlaylistEntity,
    val itemCount: Int
)

@Dao
abstract class PlaylistDao {

    @Query(
        "SELECT p.*, (SELECT COUNT(*) FROM playlist_items WHERE playlistId = p.id) AS itemCount " +
            "FROM playlists p ORDER BY p.createdAt DESC"
    )
    abstract fun playlists(): Flow<List<PlaylistWithCount>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    abstract fun playlist(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :id ORDER BY position")
    abstract fun items(id: Long): Flow<List<PlaylistItemEntity>>

    @Query("SELECT * FROM playlists ORDER BY id")
    abstract suspend fun allPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist_items ORDER BY playlistId, position")
    abstract suspend fun allItems(): List<PlaylistItemEntity>

    @Insert
    abstract suspend fun insertPlaylist(p: PlaylistEntity): Long

    @Insert
    abstract suspend fun insertItems(items: List<PlaylistItemEntity>)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    abstract suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    abstract suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_items WHERE id = :id")
    abstract suspend fun deleteItem(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_items WHERE playlistId = :id")
    abstract suspend fun maxPosition(id: Long): Int

    @Query("UPDATE playlist_items SET position = :position WHERE id = :id")
    abstract suspend fun setPosition(id: Long, position: Int)

    @Transaction
    open suspend fun addItems(playlistId: Long, items: List<QueueItem>) {
        var pos = maxPosition(playlistId) + 1
        val entities = ArrayList<PlaylistItemEntity>()
        for (it in items) {
            entities += PlaylistItemEntity(
                playlistId = playlistId,
                uri = it.uri,
                name = it.name,
                isVideo = it.isVideo,
                durationMs = it.durationMs,
                position = pos++
            )
        }
        insertItems(entities)
    }

    @Transaction
    open suspend fun reorder(ids: List<Long>) {
        ids.forEachIndexed { index, id -> setPosition(id, index) }
    }
}

@Database(
    entities = [PlaylistEntity::class, PlaylistItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MixDb : RoomDatabase() {
    abstract fun dao(): PlaylistDao

    companion object {
        fun create(context: Context): MixDb =
            Room.databaseBuilder(context, MixDb::class.java, "mix.db").build()
    }
}
