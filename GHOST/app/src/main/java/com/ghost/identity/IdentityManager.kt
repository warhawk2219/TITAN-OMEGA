package com.ghost.identity

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class PersonTier { OWNER, FAMILY, FRIENDS, STRANGER }

@Entity(tableName = "persons")
data class PersonProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val gender: String,
    val relationship: String,
    val tier: String = PersonTier.FRIENDS.name,
    val faceEmbedding: ByteArray? = null,
    val voicePrint: ByteArray? = null,
    val photoPath: String = "",
    val notes: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface PersonDao {
    @Query("SELECT * FROM persons ORDER BY name ASC")
    fun getAllPersons(): Flow<List<PersonProfile>>

    @Query("SELECT * FROM persons WHERE tier = :tier")
    fun getByTier(tier: String): Flow<List<PersonProfile>>

    @Query("SELECT * FROM persons WHERE name LIKE '%' || :name || '%'")
    suspend fun searchByName(name: String): List<PersonProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(person: PersonProfile): Long

    @Update
    suspend fun update(person: PersonProfile)

    @Delete
    suspend fun delete(person: PersonProfile)

    @Query("SELECT * FROM persons WHERE id = :id")
    suspend fun getById(id: Long): PersonProfile?

    @Query("DELETE FROM persons")
    suspend fun deleteAll()
}

@Database(entities = [PersonProfile::class], version = 1, exportSchema = false)
abstract class IdentityDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao

    companion object {
        @Volatile private var INSTANCE: IdentityDatabase? = null
        fun getInstance(context: Context): IdentityDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    IdentityDatabase::class.java,
                    "ghost_identity.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class IdentityManager(private val context: Context) {
    private val db = IdentityDatabase.getInstance(context)
    private val dao = db.personDao()

    suspend fun savePerson(
        name: String,
        gender: String,
        relationship: String,
        tier: PersonTier,
        photoPath: String = "",
        faceEmbedding: ByteArray? = null,
        voicePrint: ByteArray? = null
    ): Long {
        val person = PersonProfile(
            name = name,
            gender = gender,
            relationship = relationship,
            tier = tier.name,
            faceEmbedding = faceEmbedding,
            voicePrint = voicePrint,
            photoPath = photoPath
        )
        return dao.insert(person)
    }

    suspend fun updateProfile(person: PersonProfile) = dao.update(person)

    suspend fun deletePerson(person: PersonProfile) = dao.delete(person)

    fun getAllFlow() = dao.getAllPersons()

    suspend fun searchByName(name: String) = dao.searchByName(name)

    suspend fun getTierForPerson(name: String): PersonTier {
        val persons = dao.searchByName(name)
        val person = persons.firstOrNull() ?: return PersonTier.STRANGER
        return try {
            PersonTier.valueOf(person.tier)
        } catch (e: Exception) {
            PersonTier.STRANGER
        }
    }

    suspend fun deleteAll() = dao.deleteAll()
}

// ===== FALLBACK AUTHENTICATION =====
class FallbackAuthManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("ghost_auth", Context.MODE_PRIVATE)

    // Three-layer auth for owner
    fun verifyOwnerPasscode(input: String): Boolean {
        val stored = prefs.getString("owner_passcode", "") ?: ""
        return input == stored
    }

    fun verifyOwnerVoicePhrase(input: String): Boolean {
        val stored = prefs.getString("owner_voice_phrase", "") ?: ""
        return input.equals(stored, ignoreCase = true)
    }

    fun verifySecurityAnswer(input: String): Boolean {
        val stored = prefs.getString("security_answer", "") ?: ""
        return input.equals(stored, ignoreCase = true)
    }

    // Family — two layers
    fun verifyFamilyPasscode(input: String, personId: Long): Boolean {
        val stored = prefs.getString("family_passcode_$personId", "") ?: ""
        return input == stored
    }

    fun verifyFamilyVoicePhrase(input: String, personId: Long): Boolean {
        val stored = prefs.getString("family_voice_$personId", "") ?: ""
        return input.equals(stored, ignoreCase = true)
    }

    // Friends — one layer
    fun verifyFriendPasscode(input: String, personId: Long): Boolean {
        val stored = prefs.getString("friend_passcode_$personId", "") ?: ""
        return input == stored
    }

    // Warhawk nuclear code
    fun verifyNuclearCode(input: String): Boolean {
        return input.uppercase() == "WARHAWK"
    }

    fun setOwnerPasscode(code: String) {
        prefs.edit().putString("owner_passcode", code).apply()
    }

    fun setOwnerVoicePhrase(phrase: String) {
        prefs.edit().putString("owner_voice_phrase", phrase).apply()
    }

    fun setSecurityAnswer(answer: String) {
        prefs.edit().putString("security_answer", answer).apply()
    }
}
