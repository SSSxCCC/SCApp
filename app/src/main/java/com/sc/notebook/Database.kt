package com.sc.notebook

import androidx.room.*
import java.io.Serializable

@Database(entities = [Note::class], version = 1)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}

@Entity
data class Note(
    @PrimaryKey val id: Long,
    val title: String,
    val content: String,
) : Serializable

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(vararg note: Note)

    @Update
    suspend fun update(vararg note: Note)

    @Delete
    suspend fun delete(vararg note: Note)

    @Query("SELECT * FROM Note")
    suspend fun getAll(): List<Note>
}