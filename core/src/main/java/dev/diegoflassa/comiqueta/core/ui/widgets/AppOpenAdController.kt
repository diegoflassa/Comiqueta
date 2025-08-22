package dev.diegoflassa.comiqueta.core.ui.widgets

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger

private const val TAG = "AppOpenAd"

/**
 * A Composable that loads an App Open Ad.
 *
 * The actual showing of the App Open Ad is typically handled by an Application class
 * or a main Activity that implements Application.ActivityLifecycleCallbacks to determine
 * when the app comes to the foreground.
 *
 * @param activity The current Activity context, required to show the ad.
 * @param adUnitId The Ad Unit ID for the App Open ad.
 * @param enabled Whether ad loading should be attempted.
 * @param adRequest The [AdRequest] to use for loading the ad. Defaults to a basic AdRequest.
 * @param onAdLoaded Callback invoked when the ad is successfully loaded.
 *                     The loaded [AppOpenAd] is provided. The caller is responsible for
 *                     setting its [FullScreenContentCallback] and calling [AppOpenAd.show].
 * @param onAdFailedToLoad Callback invoked when the ad fails to load.
 */
@Composable
fun LoadAppOpenAd(
    activity: Activity,
    adUnitId: String,
    enabled: Boolean,
    adRequest: AdRequest = AdRequest.Builder().build(),
    onAdLoaded: (appOpenAd: AppOpenAd) -> Unit,
    onAdFailedToLoad: (LoadAdError) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(enabled, adUnitId, activity) {
        if (!enabled || adUnitId.isBlank()) {
            TimberLogger.logI(TAG, "App Open ad loading disabled or adUnitId is blank.")
            return@LaunchedEffect
        }

        TimberLogger.logI(TAG, "Requesting App Open Ad: \$adUnitId")
        AppOpenAd.load(
            context,
            adUnitId,
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(appOpenAd: AppOpenAd) {
                    TimberLogger.logI(TAG, "App Open Ad loaded: \$adUnitId")
                    onAdLoaded(appOpenAd)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    TimberLogger.logE(TAG, "App Open Ad failed to load: \$adUnitId, Error: \${loadAdError.message}")
                    onAdFailedToLoad(loadAdError)
                }
            }
        )
    }
}
