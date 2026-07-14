package com.kula.shadowroutines.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.kula.shadowroutines.data.db.AppDatabase
import com.kula.shadowroutines.data.novelty.NoveltyStore
import com.kula.shadowroutines.data.repository.QuoteRepository
import com.kula.shadowroutines.data.repository.RoutineRepository

/**
 * Manual dependency container (no DI framework for v0). Built once in [ShadowRoutinesApp] and
 * read by the Compose layer via the Application instance.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "shadow-routines.db",
    ).build()

    private val noveltyDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { appContext.preferencesDataStoreFile("novelty") },
    )

    private val noveltyStore = NoveltyStore(noveltyDataStore)

    val quoteRepository = QuoteRepository(
        quoteDao = database.quoteDao(),
        noveltyStore = noveltyStore,
        appContext = appContext,
    )

    val routineRepository = RoutineRepository(database.routineDao())
}
