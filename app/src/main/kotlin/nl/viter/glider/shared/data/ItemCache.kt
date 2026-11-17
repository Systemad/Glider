package nl.viter.glider.shared.data

import android.content.Context
import androidx.room.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Entity(tableName = "items")
data class CachedItem(@PrimaryKey val id: Long, val json: String, val fetchedAt: Long)

@Entity(tableName = "feeds")
data class CachedFeed(@PrimaryKey val kind: String, val ids: String)

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun get(id: Long): CachedItem?

    @Upsert suspend fun put(item: CachedItem)

    @Query("SELECT * FROM feeds WHERE kind = :kind")
    suspend fun feed(kind: String): CachedFeed?

    @Upsert suspend fun put(feed: CachedFeed)
}

@Database(entities = [CachedItem::class, CachedFeed::class], version = 2, exportSchema = true)
abstract class GliderDatabase : RoomDatabase() {
    abstract fun items(): ItemDao
}

@Singleton
class ItemCache @Inject constructor(@ApplicationContext context: Context) {
    val dao = Room.databaseBuilder(
        context,
        GliderDatabase::class.java,
        "glider.db"
    ).addMigrations(object : androidx.room.migration.Migration(1, 2) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `feeds` (`kind` TEXT NOT NULL, " +
                    "`ids` TEXT NOT NULL, PRIMARY KEY(`kind`))"
            )
        }
    }).build().items()
}
