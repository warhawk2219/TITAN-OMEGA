package com.gipsy.data.local

import androidx.room.*
import com.gipsy.data.models.*
import kotlinx.coroutines.flow.Flow

// ── DAOs ──────────────────────────────────────────────────────

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getSessionMessages(sessionId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int = 20): List<Message>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getMessageCount(): Int
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_facts ORDER BY isPinned DESC, timestamp DESC")
    fun getAllFacts(): Flow<List<MemoryFact>>

    @Query("SELECT * FROM memory_facts WHERE isPinned = 1")
    fun getPinnedFacts(): Flow<List<MemoryFact>>

    @Query("SELECT * FROM memory_facts WHERE category = :category")
    fun getFactsByCategory(category: String): Flow<List<MemoryFact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFact(fact: MemoryFact): Long

    @Query("DELETE FROM memory_facts WHERE id = :id")
    suspend fun deleteFact(id: Long)

    @Query("DELETE FROM memory_facts WHERE isPinned = 0")
    suspend fun deleteNonPinnedFacts()

    @Query("DELETE FROM memory_facts")
    suspend fun deleteAllFacts()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ConversationSession>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: String): ConversationSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ConversationSession)

    @Update
    suspend fun updateSession(session: ConversationSession)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()
}

@Dao
interface ProtocolLogDao {
    @Query("SELECT * FROM protocol_log ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<ProtocolLog>>

    @Insert
    suspend fun insertLog(log: ProtocolLog)

    @Query("DELETE FROM protocol_log")
    suspend fun clearLogs()
}

// ── DATABASE ──────────────────────────────────────────────────

@Database(
    entities = [Message::class, MemoryFact::class, ConversationSession::class, ProtocolLog::class],
    version = 1,
    exportSchema = false
)
abstract class GipsyDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun sessionDao(): SessionDao
    abstract fun protocolLogDao(): ProtocolLogDao

    companion object {
        const val DATABASE_NAME = "gipsy_database"
    }
}
