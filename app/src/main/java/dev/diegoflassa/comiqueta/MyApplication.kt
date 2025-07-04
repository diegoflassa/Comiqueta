package dev.diegoflassa.comiqueta

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory // Ensure this import is present
import androidx.work.Configuration // Ensure this import is present
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.data.timber.TimberManager
import javax.inject.Inject // Ensure this import is present

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider { // Implement Configuration.Provider

    @Inject // Inject HiltWorkerFactory
    lateinit var workerFactory: HiltWorkerFactory

    companion object {
        private val TAG = MyApplication::class.simpleName
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize TimberManager using the application context
        TimberManager.inicializar(this)
        TimberLogger.logI(TAG, "onCreate")

        val hiltWorkManagerConfiguration = Configuration.Builder()
            .setWorkerFactory(workerFactory) // workerFactory is already @Injected
            .build()
        WorkManager.initialize(this, hiltWorkManagerConfiguration)
    }

    // Correct way to implement Configuration.Provider by overriding the property
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}