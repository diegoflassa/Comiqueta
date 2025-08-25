package dev.diegoflassa.comiqueta.core.ui.widgets

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.ui.extensions.hasHeightBeenSet

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var adViewInstance: AdView? = null

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .background(ComiquetaTheme.colorScheme.background)
            .let { currentModifier ->
                if (currentModifier.hasHeightBeenSet().not()) {
                    currentModifier.height(50.dp)
                } else {
                    currentModifier
                }
            },
        factory = { ctx ->
            AdView(ctx).apply {
                adViewInstance = this

                var activity: Activity? = null
                var contextWrapper: Context? = ctx
                while (contextWrapper is ContextWrapper) {
                    if (contextWrapper is Activity) {
                        activity = contextWrapper
                        break
                    }
                    contextWrapper = contextWrapper.baseContext
                }

                val adSize = activity?.let { currentActivity ->
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        currentActivity,
                        AdSize.FULL_WIDTH
                    )
                } ?: AdSize.BANNER

                setAdSize(adSize)
                setAdUnitId(adUnitId)

                loadAd(AdRequest.Builder().build())
            }
        },
        update = {
            /* Called when the view is recomposed. Can update AdRequest if needed. */
        }
    )

    DisposableEffect(
        key1 = lifecycleOwner,
        key2 = adUnitId
    ) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adViewInstance?.resume()
                Lifecycle.Event.ON_PAUSE -> adViewInstance?.pause()
                Lifecycle.Event.ON_DESTROY -> adViewInstance?.destroy()
                else -> {
                    /* Do nothing for other events */
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            adViewInstance?.destroy()
            adViewInstance = null
        }
    }
}
