package dev.diegoflassa.comiqueta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.AndroidEntryPoint
import dev.diegoflassa.comiqueta.core.data.extensions.modoDebugHabilitado
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.ui.hiltActivityViewModel
import dev.diegoflassa.comiqueta.navigation.NavDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var consentInformation: ConsentInformation
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private val tag = "MainActivityAds"

    var showAds by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar)
        enableEdgeToEdge()

        configureAdRequestFlags()
        requestConsentInfo()

        setContent {
            val navigationViewModel: NavigationViewModel = hiltActivityViewModel()
            ComiquetaThemeContent {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ComiquetaTheme.colorScheme.background
                ) {
                    NavDisplay(modifier = Modifier, navigationViewModel = navigationViewModel)
                }

                BackHandler {
                    navigationViewModel.goBack()
                }
            }
        }
    }

    private fun configureAdRequestFlags() {
        val requestConfigurationBuilder = RequestConfiguration.Builder()
        // Replaces the deprecated setTagForChildDirectedTreatment/setTagForUnderAgeOfConsent int
        // pair: play-services-ads 25.x folds both into one AgeRestrictedTreatment enum, and
        // UNSPECIFIED is what the two former UNSPECIFIED constants meant together.
        requestConfigurationBuilder.setAgeRestrictedTreatment(AgeRestrictedTreatment.UNSPECIFIED)
        requestConfigurationBuilder.setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG)
        val requestConfiguration = requestConfigurationBuilder.build()
        MobileAds.setRequestConfiguration(requestConfiguration)
        TimberLogger.logD(tag, "[Comiqueta][Main] AdMob RequestConfiguration set.")
    }

    private fun requestConsentInfo() {
        val debugSettings = if (modoDebugHabilitado()) {
            ConsentDebugSettings.Builder(this)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                //.addTestDeviceHashedId("YOUR_TEST_DEVICE_HASHED_ID_FROM_LOGCAT")
                .build()
        } else {
            null
        }
        val params = ConsentRequestParameters.Builder().also {
            if (debugSettings != null) {
                it.setConsentDebugSettings(debugSettings)
            }
        }.build()

        lifecycleScope.launch {
            consentInformation = withContext(Dispatchers.IO) {
                UserMessagingPlatform.getConsentInformation(this@MainActivity)
            }
            consentInformation.requestConsentInfoUpdate(
                this@MainActivity,
                params,
                {
                    TimberLogger.logI(
                        tag,
                        "[Comiqueta][Main] Consent info updated. Status: ${consentInformation.consentStatus}, CanRequestAds: ${consentInformation.canRequestAds()}"
                    )
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(this@MainActivity) { loadAndShowError ->
                        if (loadAndShowError != null) {
                            TimberLogger.logE(
                                tag,
                                "[Comiqueta][Main] Consent form load/show error: ${loadAndShowError.message}"
                            )
                        } else {
                            TimberLogger.logI(tag, "[Comiqueta][Main] Consent form shown (or not required).")
                        }
                        initializeMobileAdsSdkIfNeeded()
                    }
                },
                { requestConsentError ->
                    TimberLogger.logE(
                        tag,
                        "[Comiqueta][Main] Consent info update error: ${requestConsentError.message}"
                    )
                    initializeMobileAdsSdkIfNeeded()
                }
            )
            // Initial check in case UMP doesn't need to show a form and consent is already there.
            // The callbacks above are the primary triggers.
            if (::consentInformation.isInitialized && consentInformation.canRequestAds() &&
                (consentInformation.consentStatus == ConsentInformation.ConsentStatus.OBTAINED ||
                        consentInformation.consentStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED)
            ) {
                initializeMobileAdsSdkIfNeeded()
            }
        }
    }

    private fun initializeMobileAdsSdkIfNeeded() {
        if (::consentInformation.isInitialized && consentInformation.canRequestAds()) {
            if (isMobileAdsInitializeCalled.compareAndSet(false, true)) {
                // MobileAds.initialize() blocks for ~1s on first run (it loads the GMS
                // dynamite module and DroidGuard). Calling it on the main thread cost ~84
                // dropped frames at startup, so it goes to a background thread as Google
                // recommends. The completion callback still returns on the main thread.
                lifecycleScope.launch(Dispatchers.IO) {
                    MobileAds.initialize(applicationContext) { initializationStatus ->
                        TimberLogger.logI(
                            tag,
                            "[Comiqueta][Main] MobileAds initialized. Status: ${initializationStatus.adapterStatusMap}"
                        )
                        showAds = true
                    }
                }
            } else {
                // SDK already initialized, consent is still valid
                showAds = true
                TimberLogger.logD(
                    tag,
                    "[Comiqueta][Main] MobileAds already initialized and consent valid. Showing ads."
                )
            }
        } else {
            TimberLogger.logW(
                tag,
                "[Comiqueta][Main] Cannot request ads. Consent not obtained or SDK not ready. Ads hidden."
            )
            showAds = false
        }
    }
}
