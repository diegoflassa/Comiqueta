package dev.diegoflassa.comiqueta.core.data.preferences

import androidx.datastore.preferences.core.intPreferencesKey

object PreferencesKeys {
    val VIEWER_PAGES_TO_PRELOAD_AHEAD =
        intPreferencesKey(UserPreferencesKeys.COMIC_VIEWER_PRELOAD_PAGES_AHEAD)

    const val DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD = 1
}