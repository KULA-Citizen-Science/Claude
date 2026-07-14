package com.kula.shadowroutines

import android.app.Application
import com.kula.shadowroutines.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ShadowRoutinesApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Seed the bundled corpus on first launch so rewards work immediately.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.quoteRepository.seedIfEmpty()
        }
    }
}
