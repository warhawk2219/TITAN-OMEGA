package com.ghost.memory

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,       // preferences, people, commands, notes, events
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY timestamp DESC")
    fun getByCategory(category: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE `key` LIKE '%' || :query || '%' OR value LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity)

    @Delete
    suspend fun delete(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    @Query("DELETE FROM memories")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun getCount(): Int
}

@Database(entities = [MemoryEntity::class], version = 1, exportSchema = false)
abstract class GhostDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile private var INSTANCE: GhostDatabase? = null

        fun getInstance(context: Context): GhostDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    GhostDatabase::class.java,
                    "ghost.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class MemoryManager(context: Context) {
    private val db = GhostDatabase.getInstance(context)
    private val dao = db.memoryDao()

    suspend fun remember(category: String, key: String, value: String) {
        dao.insert(MemoryEntity(category = category, key = key, value = value))
    }

    suspend fun recall(query: String): List<MemoryEntity> = dao.search(query)

    fun getAllFlow() = dao.getAllMemories()

    fun getByCategoryFlow(category: String) = dao.getByCategory(category)

    suspend fun forget(memory: MemoryEntity) = dao.delete(memory)

    suspend fun forgetCategory(category: String) = dao.deleteByCategory(category)

    suspend fun forgetAll() = dao.deleteAll()

    suspend fun getCount() = dao.getCount()
}
