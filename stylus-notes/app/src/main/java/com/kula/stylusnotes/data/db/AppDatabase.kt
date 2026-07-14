package com.kula.stylusnotes.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kula.stylusnotes.data.model.NoteEntity
import com.kula.stylusnotes.data.model.StrokeEntity

@Database(entities = [NoteEntity::class, StrokeEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stylus-notes.db"
                ).build().also { instance = it }
            }
    }
}
