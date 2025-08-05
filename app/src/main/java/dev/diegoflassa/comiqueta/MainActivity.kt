package dev.diegoflassa.comiqueta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration // Added
import com.google.android.ump.ConsentDebugSettings // Added
import com.google.android.ump.ConsentForm // Added
import com.google.android.ump.ConsentInformation // Added
import com.google.android.ump.ConsentRequestParameters // Added
import com.google.android.ump.UserMessagingPlatform // Added
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import dagger.hilt.android.AndroidEntryPoint
import dev.diegoflassa.comiqueta.core.data.config.IConfig
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.ui.hiltActivityViewModel
import dev.diegoflassa.comiqueta.navigation.NavDisplay
import java.util.concurrent.atomic.AtomicBoolean // Added for ensuring one-time init
import javax.inject.Inject
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger // Assuming you have this

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private val tag = MainActivity::class.simpleName
    }

    @Inject
    lateinit var config: IConfig

    private lateinit var consentInformation: ConsentInformation

    // Helper to ensure MobileAds.initialize() is called only once.
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar)
        enableEdgeToEdge()

        // 1. Configure AdMob request flags (before UMP if flags influence consent params)
        configureAdRequestFlags()
        // 2. Start UMP consent flow
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
            }

            BackHandler {
                navigationViewModel.goBack()
            }
        }
        inicializarClarity()
    }

    private fun configureAdRequestFlags() {
        val requestConfigurationBuilder = RequestConfiguration.Builder()

        // TODO: **CRITICAL** Determine and set the correct values based on your app's content and audience.
        // See: https://developers.google.com/admob/android/targeting#child-directed_setting
        requestConfigurationBuilder.setTagForChildDirectedTreatment(
            RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        )

        // TODO: **CRITICAL** Determine and set the correct value.
        // See: https://developers.google.com/admob/android/targeting#users_under_the_age_of_consent
        requestConfigurationBuilder.setTagForUnderAgeOfConsent(
            RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
        )

        // TODO: Set an appropriate content rating for ads. "G", "PG", "T", "MA".
        // For a general comic app, "PG" or "T" might be appropriate.
        requestConfigurationBuilder.setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG)

        val requestConfiguration = requestConfigurationBuilder.build()
        MobileAds.setRequestConfiguration(requestConfiguration)
        TimberLogger.logD(tag, "AdMob RequestConfiguration set.")
    }

    private fun requestConsentInfo() {
        // For testing, you can force geography and add test device IDs.
        // REMOVE setDebugGeography and addTestDeviceHashedId for PRODUCTION.
        val debugSettings = ConsentDebugSettings.Builder(this)
            // .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            // .addTestDeviceHashedId("YOUR_TEST_DEVICE_HASHED_ID_FROM_LOGCAT") // Replace!
            .build()

        val params = ConsentRequestParameters.Builder()
            // .setConsentDebugSettings(debugSettings) // Uncomment for testing
            // If you've already determined TFUA status before UMP, you can set it here too.
            // .setTagForUnderAgeOfConsent(false) // Example
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                // Consent information updated successfully.
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
                        // Handle error, but still try to initialize ads if permissible
                    } else {
                        TimberLogger.logI(tag, "Consent form shown (or not required).")
                    }

                    // Consent has been gathered (or was already up-to-date).
                    // Initialize MobileAds if needed and allowed.
                    initializeMobileAdsSdkIfNeeded()
                }
            },
            { requestConsentError ->
                TimberLogger.logE(tag, "Consent info update error: ${requestConsentError.message}")
                initializeMobileAdsSdkIfNeeded()
            }
        )

        // If consent is already gathered from a previous session, MobileAds can be initialized sooner.
        // However, it's generally safer to initialize after the UMP flow confirms `canRequestAds`.
        if (consentInformation.canRequestAds()) {
            initializeMobileAdsSdkIfNeeded()
        }
    }

    private fun initializeMobileAdsSdkIfNeeded() {
        if (consentInformation.canRequestAds()) {
            // Ensure MobileAds.initialize() is called only once.
            if (isMobileAdsInitializeCalled.compareAndSet(false, true)) {
                MobileAds.initialize(this) { initializationStatus ->
                    TimberLogger.logI(
                        tag,
                        "MobileAds initialized. Status: ${initializationStatus.adapterStatusMap}"
                    )
                    // Ads can now be loaded.
                    // Example: loadBannerAd(), loadInterstitialAd(), etc.
                }
            }
        } else {
            TimberLogger.logW(
                tag,
                "Cannot request ads. Consent not obtained or not applicable. MobileAds not initialized."
            )
            FirebaseCrashlytics.getInstance()
                .recordException(Exception("Cannot request ads. Consent not obtained or not applicable. MobileAds not initialized."))
        }
    }

    private fun inicializarClarity() {
        if (config.clarityId.isNotEmpty()) {
            val clarityConfig = ClarityConfig(config.clarityId)
            Clarity.initialize(applicationContext, clarityConfig)
        }
    }

    // TODO: Add a way for users to change/revoke their consent.
    // E.g., in a settings screen:

}
