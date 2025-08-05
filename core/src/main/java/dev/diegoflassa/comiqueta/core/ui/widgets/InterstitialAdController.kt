package dev.diegoflassa.comiqueta.core.ui.widgets

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger

private const val TAG = "InterstitialAd"

/**
 * A Composable that loads an Interstitial Ad.
 *
 * @param activity The current Activity, required to show the ad.
 * @param adUnitId The Ad Unit ID for the interstitial ad.
 * @param enabled Whether ad loading and showing should be attempted.
 * @param adRequest The [AdRequest] to use for loading the ad. Defaults to a basic AdRequest.
 * @param onAdLoaded Callback invoked when the ad is successfully loaded.
 *                     The loaded [InterstitialAd] is provided. The caller is responsible for
 *                     setting its [FullScreenContentCallback] and calling [InterstitialAd.show].
 * @param onAdFailedToLoad Callback invoked when the ad fails to load.
 */
@Composable
fun LoadInterstitialAd(
    activity: Activity,
    adUnitId: String,
    enabled: Boolean,
    adRequest: AdRequest = AdRequest.Builder().build(),
    onAdLoaded: (interstitialAd: InterstitialAd) -> Unit,
    onAdFailedToLoad: (LoadAdError) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(enabled, adUnitId, activity) {
        if (!enabled || adUnitId.isBlank()) {
            TimberLogger.logI(TAG, "Interstitial ad loading disabled or adUnitId is blank.")
            return@LaunchedEffect
        }

        TimberLogger.logI(TAG, "Requesting Interstitial Ad: \$adUnitId")
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    TimberLogger.logI(TAG, "Interstitial Ad loaded: \$adUnitId")
                    onAdLoaded(interstitialAd)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    TimberLogger.logE(TAG, "Interstitial Ad failed to load: \$adUnitId, Error: \${loadAdError.message}")
                    onAdFailedToLoad(loadAdError)
                }
            }
        )
    }
}
