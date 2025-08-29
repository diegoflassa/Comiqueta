package dev.diegoflassa.comiqueta

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.data.timber.TimberManager
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.Clarity.getCurrentSessionUrl
import com.microsoft.clarity.ClarityConfig
import dev.diegoflassa.comiqueta.core.data.config.IConfig
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var config: IConfig

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    companion object {
        private val TAG = MyApplication::class.simpleName
    }

    override fun onCreate() {
        super.onCreate()
        inicializarClarity()
        FirebaseApp.initializeApp(this)
        TimberManager.inicializar(this)
        TimberLogger.logI(TAG, "onCreate")

        // Initialize WorkManager
        val hiltWorkManagerConfiguration = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
        WorkManager.initialize(this, hiltWorkManagerConfiguration)
    }

    private fun inicializarClarity() {
        if (config.clarityId.isNotEmpty()) {
            val clarityConfig = ClarityConfig(config.clarityId)
            Clarity.initialize(applicationContext, clarityConfig)
            Clarity.setOnSessionStartedCallback { session ->
                associateClarityWithCrashlytics()
            }
            TimberLogger.logI(
                TAG,
                "Microsoft Clarity initialized with Project ID: ${config.clarityId}"
            )
        }
    }

    private fun associateClarityWithCrashlytics(){
        try {
            val claritySessionUrl: String? = getCurrentSessionUrl()

            if (!claritySessionUrl.isNullOrEmpty()) {
                FirebaseCrashlytics.getInstance().setCustomKey("clarity_session_url", claritySessionUrl)
                TimberLogger.logI(TAG, "Clarity Session URL logged to Crashlytics: $claritySessionUrl")
            } else {
                TimberLogger.logW(TAG, "Clarity Session URL was null or empty after initialization. Not logging to Crashlytics.")
            }
        } catch (ex: Exception) {
            TimberLogger.logE(TAG, "Error during Clarity initialization or logging Session URL to Crashlytics", ex)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
