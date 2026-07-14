package com.kula.shadowroutines.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kula.shadowroutines.data.db.entity.ChecklistItemEntity
import com.kula.shadowroutines.data.db.entity.QuoteEntity
import com.kula.shadowroutines.data.db.entity.QuoteFtsEntity
import com.kula.shadowroutines.data.db.entity.RoutineEntity

@Database(
    entities = [
        RoutineEntity::class,
        ChecklistItemEntity::class,
        QuoteEntity::class,
        QuoteFtsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun quoteDao(): QuoteDao
}
