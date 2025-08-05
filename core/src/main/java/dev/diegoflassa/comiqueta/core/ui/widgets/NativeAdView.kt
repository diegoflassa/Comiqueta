package dev.diegoflassa.comiqueta.core.ui.widgets

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView as GoogleNativeAdView // Alias to avoid confusion
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger

private const val TAG = "NativeAdWidget"

/**
 * A Composable that loads and displays a Native Ad.
 *
 * You MUST provide an XML layout (via [nativeAdLayoutResId]) for the ad's presentation.
 * This layout should contain a `com.google.android.gms.ads.nativead.NativeAdView` as its root
 * or a primary child, and include views for assets like headline, body, icon, etc.
 * The resource IDs for these internal views must be passed to this composable.
 *
 * @param adUnitId The Ad Unit ID for the native ad.
 * @param enabled Whether ad loading and showing should be attempted.
 * @param nativeAdLayoutResId The resource ID of your XML layout for the native ad (e.g., `R.layout.your_native_ad_layout`).
 * @param headlineViewResId The resource ID of the TextView for the ad headline (e.g., `R.id.ad_headline`).
 * @param bodyViewResId The resource ID of the TextView for the ad body (e.g., `R.id.ad_body`).
 * @param callToActionViewResId The resource ID of the Button/TextView for the ad call to action (e.g., `R.id.ad_call_to_action`).
 * @param iconViewResId The resource ID of the ImageView for the ad icon (e.g., `R.id.ad_icon`).
 * @param mediaViewResId Optional resource ID of the MediaView for the ad media content (e.g., `R.id.ad_media`).
 * @param starRatingViewResId Optional resource ID of the RatingBar for the ad star rating (e.g., `R.id.ad_stars`).
 * @param advertiserViewResId Optional resource ID of the TextView for the ad advertiser (e.g., `R.id.ad_advertiser`).
 * @param modifier Modifier for this composable.
 * @param adRequest The [AdRequest] to use for loading the ad. Defaults to a basic AdRequest.
 * @param nativeAdOptions Optional [NativeAdOptions] to customize the ad request.
 * @param onAdFailedToLoad Callback invoked when the ad fails to load.
 * @param onAdImpression Optional callback for when an ad impression is recorded.
 * @param onAdClicked Optional callback for when an ad is clicked.
 */
@Composable
fun NativeAdView(
    modifier: Modifier = Modifier,
    adUnitId: String,
    enabled: Boolean,
    @LayoutRes nativeAdLayoutResId: Int,
    @IdRes headlineViewResId: Int,
    @IdRes bodyViewResId: Int,
    @IdRes callToActionViewResId: Int,
    @IdRes iconViewResId: Int,
    @IdRes mediaViewResId: Int? = null,
    @IdRes starRatingViewResId: Int? = null,
    @IdRes advertiserViewResId: Int? = null,
    adRequest: AdRequest = AdRequest.Builder().build(),
    nativeAdOptions: NativeAdOptions? = null,
    onAdFailedToLoad: ((LoadAdError) -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var nativeAdState by remember { mutableStateOf<NativeAd?>(null) }
    var adLoader by remember { mutableStateOf<AdLoader?>(null) }

    LaunchedEffect(enabled, adUnitId, nativeAdLayoutResId) {
        if (!enabled || adUnitId.isBlank()) {
            TimberLogger.logI(TAG, "Native ad loading disabled or adUnitId is blank.")
            nativeAdState = null // Clear previous ad if disabled
            return@LaunchedEffect
        }

        TimberLogger.logI(TAG, "Requesting Native Ad: \$adUnitId")

        val loader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad: NativeAd ->
                TimberLogger.logI(TAG, "Native Ad loaded: \$adUnitId")
                nativeAdState?.destroy() // Destroy any old ad before assigning new one
                nativeAdState = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    TimberLogger.logE(TAG, "Native Ad failed to load: \$adUnitId, Error: \${loadAdError.message}")
                    nativeAdState = null
                    onAdFailedToLoad?.invoke(loadAdError)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    TimberLogger.logI(TAG, "Native Ad impression recorded: \$adUnitId")
                    onAdImpression?.invoke()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    TimberLogger.logI(TAG, "Native Ad clicked: \$adUnitId")
                    onAdClicked?.invoke()
                }
            })

        nativeAdOptions?.let {
            loader.withNativeAdOptions(it)
        }

        adLoader = loader.build()
        adLoader?.loadAd(adRequest)
    }

    DisposableEffect(Unit) {
        onDispose {
            TimberLogger.logD(TAG, "Disposing NativeAdView, destroying ad: \$adUnitId")
            nativeAdState?.destroy()
            nativeAdState = null
        }
    }

    if (enabled && nativeAdState != null) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = {
                val adView = LayoutInflater.from(it).inflate(nativeAdLayoutResId, null) as GoogleNativeAdView
                adView
            },
            update = { adView ->
                nativeAdState?.let { currentNativeAd ->
                    // Assign views using the passed-in resource IDs
                    adView.headlineView = adView.findViewById(headlineViewResId)
                    adView.bodyView = adView.findViewById(bodyViewResId)
                    adView.callToActionView = adView.findViewById(callToActionViewResId)
                    adView.iconView = adView.findViewById(iconViewResId)

                    mediaViewResId?.let { adView.mediaView = adView.findViewById(it) }
                    starRatingViewResId?.let { adView.starRatingView = adView.findViewById(it) }
                    advertiserViewResId?.let { adView.advertiserView = adView.findViewById(it) }

                    // Populate the views
                    (adView.headlineView as? TextView)?.text = currentNativeAd.headline
                    (adView.bodyView as? TextView)?.text = currentNativeAd.body
                    (adView.callToActionView as? TextView)?.text = currentNativeAd.callToAction // Can be Button or TextView
                    (adView.iconView as? ImageView)?.setImageDrawable(currentNativeAd.icon?.drawable)

                    adView.mediaView?.let {
                        it.setImageScaleType(ImageView.ScaleType.CENTER_CROP) // Example, adjust as needed
                        it.mediaContent = (currentNativeAd.mediaContent ?: return@let)
                    }

                    if (currentNativeAd.starRating == null) {
                        adView.starRatingView?.visibility = View.INVISIBLE
                    } else {
                        (adView.starRatingView as? RatingBar)?.rating = currentNativeAd.starRating!!.toFloat()
                        adView.starRatingView?.visibility = View.VISIBLE
                    }

                    if (currentNativeAd.advertiser == null) {
                        adView.advertiserView?.visibility = View.INVISIBLE
                    } else {
                        (adView.advertiserView as? TextView)?.text = currentNativeAd.advertiser
                        adView.advertiserView?.visibility = View.VISIBLE
                    }
                    
                    // Register the NativeAd object with the NativeAdView.
                    adView.setNativeAd(currentNativeAd)
                }
            }
        )
    } else if (enabled) {
        // Optional: Show a placeholder or loading indicator while the ad loads
        Box(modifier = modifier.padding(8.dp), contentAlignment = Alignment.Center) {
            // Text("Loading Native Ad...") // Placeholder Text
        }
    }
}

/*
Example of what your R.layout.your_native_ad_layout (e.g., in app module) might contain:

<com.google.android.gms.ads.nativead.NativeAdView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="8dp">

        <ImageView
            android:id="@+id/ad_icon" <!-- This ID would be passed as iconViewResId -->
            android:layout_width="40dp"
            android:layout_height="40dp"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            android:contentDescription="Ad icon" />

        <TextView
            android:id="@+id/ad_headline" <!-- This ID would be passed as headlineViewResId -->
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:textSize="16sp"
            android:textStyle="bold"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toEndOf="@id/ad_icon"
            app:layout_constraintTop_toTopOf="@id/ad_icon" />

        <com.google.android.gms.ads.nativead.MediaView
            android:id="@+id/ad_media" <!-- This ID would be passed as mediaViewResId -->
            android:layout_width="0dp"
            android:layout_height="120dp"
            android:layout_marginTop="8dp"
            app:layout_constraintStart_toStartOf="@id/ad_icon"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toBottomOf="@id/ad_icon" />

        <TextView
            android:id="@+id/ad_body" <!-- This ID would be passed as bodyViewResId -->
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:textSize="12sp"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="@id/ad_icon"
            app:layout_constraintTop_toBottomOf="@id/ad_media" />

        <TextView
            android:id="@+id/ad_advertiser"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="10sp"
            android:layout_marginTop="8dp"
            app:layout_constraintStart_toStartOf="@id/ad_icon"
            app:layout_constraintTop_toBottomOf="@id/ad_body" />
            
        <RatingBar
            android:id="@+id/ad_stars"
            style="?android:attr/ratingBarStyleSmall"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:isIndicator="true"
            android:numStars="5"
            android:stepSize="0.5"
            android:layout_marginStart="8dp"
            app:layout_constraintBottom_toBottomOf="@id/ad_advertiser"
            app:layout_constraintStart_toEndOf="@id/ad_advertiser"
            app:layout_constraintTop_toTopOf="@id/ad_advertiser" />

        <Button
            android:id="@+id/ad_call_to_action" <!-- This ID would be passed as callToActionViewResId -->
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="12sp"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toBottomOf="@id/ad_body" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</com.google.android.gms.ads.nativead.NativeAdView>
*/
