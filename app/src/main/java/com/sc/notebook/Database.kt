package com.sc.notebook

import androidx.room.*

@Database(entities = [Note::class], version = 1)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}

@Entity
data class Note(
    @PrimaryKey val id: Long,
    val title: String,
    val content: String,
)

@Dao
interface NoteDao {
    @Insert
    fun insert(vararg note: Note)

    @Update
    fun update(vararg note: Note)

    @Delete
    fun delete(vararg note: Note)

    @Query("SELECT * FROM Note")
    fun getAll(): List<Note>
}