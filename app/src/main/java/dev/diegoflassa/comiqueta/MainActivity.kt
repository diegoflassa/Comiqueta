package dev.diegoflassa.comiqueta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import dagger.hilt.android.AndroidEntryPoint
import dev.diegoflassa.comiqueta.core.data.config.IConfig
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.ui.hiltActivityViewModel
import dev.diegoflassa.comiqueta.navigation.NavDisplay
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var config: IConfig

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
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavDisplay(modifier = Modifier, navigationViewModel = navigationViewModel)
                }

                BackHandler {
                    navigationViewModel.goBack()
                }
            }
        }
        inicializarClarity()
    }

    private fun configureAdRequestFlags() {
        val requestConfigurationBuilder = RequestConfiguration.Builder()
        requestConfigurationBuilder.setTagForChildDirectedTreatment(
            RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )
        requestConfigurationBuilder.setTagForUnderAgeOfConsent(
            RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
        )
        requestConfigurationBuilder.setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG)
        val requestConfiguration = requestConfigurationBuilder.build()
        MobileAds.setRequestConfiguration(requestConfiguration)
        TimberLogger.logD(tag, "AdMob RequestConfiguration set.")
    }

    private fun requestConsentInfo() {
        val debugSettings = ConsentDebugSettings.Builder(this)
            // .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            // .addTestDeviceHashedId("YOUR_TEST_DEVICE_HASHED_ID_FROM_LOGCAT")
            .build()
        val params = ConsentRequestParameters.Builder()
            // .setConsentDebugSettings(debugSettings) // Uncomment for testing
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                TimberLogger.logI(
                    tag,
                    "Consent info updated. Status: ${consentInformation.consentStatus}, CanRequestAds: ${consentInformation.canRequestAds()}"
                )
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this@MainActivity) { loadAndShowError ->
                    if (loadAndShowError != null) {
                        TimberLogger.logE(
                            tag,
                            "Consent form load/show error: ${loadAndShowError.message}"
                        )
                    } else {
                        TimberLogger.logI(tag, "Consent form shown (or not required).")
                    }
                    initializeMobileAdsSdkIfNeeded()
                }
            },
            { requestConsentError ->
                TimberLogger.logE(tag, "Consent info update error: ${requestConsentError.message}")
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

    private fun initializeMobileAdsSdkIfNeeded() {
        if (::consentInformation.isInitialized && consentInformation.canRequestAds()) {
            if (isMobileAdsInitializeCalled.compareAndSet(false, true)) {
                MobileAds.initialize(this) { initializationStatus ->
                    TimberLogger.logI(
                        tag,
                        "MobileAds initialized. Status: ${initializationStatus.adapterStatusMap}"
                    )
                    showAds = true // Update Compose state
                }
            } else {
                // SDK already initialized, consent is still valid
                showAds = true
                TimberLogger.logD(
                    tag,
                    "MobileAds already initialized and consent valid. Showing ads."
                )
            }
        } else {
            TimberLogger.logW(
                tag,
                "Cannot request ads. Consent not obtained or SDK not ready. Ads hidden."
            )
            showAds = false // Update Compose state
        }
    }

    private fun inicializarClarity() {
        if (config.clarityId.isNotEmpty()) {
            val clarityConfig = ClarityConfig(config.clarityId)
            Clarity.initialize(applicationContext, clarityConfig)
        }
    }
}
