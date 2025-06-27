package dev.diegoflassa.comiqueta.core.data.config

import android.content.Context
import dev.diegoflassa.comiqueta.core.R

class Config(val context: Context) : IConfig {
    override val enableADs by lazy {
        context.getString(R.string.ENABLE_ADS).toBoolean()
    }
    override val appAdsId by lazy {
        context.getString(R.string.APP_ADS_ID)
    }
    override val adMobId by lazy {
        context.getString(R.string.ADMOB_ID)
    }
    override val addBannerId by lazy {
        context.getString(R.string.ADD_BANNER_ID)
    }
    override val addInterstitialId by lazy {
        context.getString(R.string.ADD_INTERSTITIAL_ID)
    }
    override val adRewardedId by lazy {
        context.getString(R.string.ADD_REWARDED_ID)
    }
    override val adNativeId by lazy {
        context.getString(R.string.ADD_NATIVE_ID)
    }
    override val adAppOpenId by lazy {
        context.getString(R.string.ADD_APP_OPEN_ID)
    }
}
