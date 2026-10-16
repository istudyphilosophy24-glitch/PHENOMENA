package com.phenomena.study.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notes")
data class StudyNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lessonId: String, val heading: String, val body: String, val createdAt: Long = System.currentTimeMillis()
)
@Entity(tableName = "progress")
data class LessonProgress(
    @PrimaryKey val lessonId: String,
    val completed: Boolean = false,
    val bookmarked: Boolean = false,
    val lastOpened: Long = System.currentTimeMillis()
)
@Entity(tableName = "imports")
data class ImportedDocument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val content: String, val format: String, val addedAt: Long = System.currentTimeMillis()
)
@Dao
interface StudyDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC") fun notes(): Flow<List<StudyNote>>
    @Query("SELECT * FROM progress") fun progress(): Flow<List<LessonProgress>>
    @Query("SELECT * FROM imports ORDER BY addedAt DESC") fun imports(): Flow<List<ImportedDocument>>
    @Insert suspend fun addNote(note: StudyNote)
    @Query("DELETE FROM notes WHERE id = :id") suspend fun deleteNote(id: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun updateProgress(item: LessonProgress)
    @Insert suspend fun addImport(doc: ImportedDocument)
    @Query("DELETE FROM imports WHERE id = :id") suspend fun deleteImport(id: Long)
}
@Database(entities = [StudyNote::class, LessonProgress::class, ImportedDocument::class], version = 1, exportSchema = false)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun dao(): StudyDao
    companion object {
        @Volatile private var instance: StudyDatabase? = null
        fun get(context: android.content.Context): StudyDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, StudyDatabase::class.java, "phenomena.db").build().also { instance = it }
        }
    }
}
