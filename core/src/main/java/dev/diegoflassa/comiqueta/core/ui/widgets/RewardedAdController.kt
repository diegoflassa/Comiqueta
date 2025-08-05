package dev.diegoflassa.comiqueta.core.ui.widgets

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger

private const val TAG = "RewardedAd"

/**
 * A Composable that loads a Rewarded Ad.
 *
 * @param activity The current Activity, required to show the ad.
 * @param adUnitId The Ad Unit ID for the rewarded ad.
 * @param enabled Whether ad loading and showing should be attempted.
 * @param adRequest The [AdRequest] to use for loading the ad. Defaults to a basic AdRequest.
 * @param onAdLoaded Callback invoked when the ad is successfully loaded.
 *                   The loaded [RewardedAd] is provided. The caller is responsible for
 *                   setting its [FullScreenContentCallback] and calling [RewardedAd.show].
 * @param onAdFailedToLoad Callback invoked when the ad fails to load.
 */
@Composable
fun LoadRewardedAd(
    activity: Activity,
    adUnitId: String,
    enabled: Boolean,
    adRequest: AdRequest = AdRequest.Builder().build(),
    onAdLoaded: (rewardedAd: RewardedAd) -> Unit,
    onAdFailedToLoad: (LoadAdError) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(enabled, adUnitId, activity) {
        if (!enabled || adUnitId.isBlank()) {
            TimberLogger.logI(TAG, "Rewarded ad loading disabled or adUnitId is blank.")
            return@LaunchedEffect
        }

        TimberLogger.logI(TAG, "Requesting Rewarded Ad: \$adUnitId")
        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    TimberLogger.logI(TAG, "Rewarded Ad loaded: \$adUnitId")
                    onAdLoaded(rewardedAd)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    TimberLogger.logE(TAG, "Rewarded Ad failed to load: \$adUnitId, Error: \${loadAdError.message}")
                    onAdFailedToLoad(loadAdError)
                }
            }
        )
    }
}
