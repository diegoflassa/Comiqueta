package dev.diegoflassa.comiqueta.core.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

object PreferencesKeys {
    val VIEWER_PAGES_TO_PRELOAD_AHEAD =
        intPreferencesKey(UserPreferencesKeys.COMIC_VIEWER_PRELOAD_PAGES_AHEAD)

    val MANGA_MODE = booleanPreferencesKey(UserPreferencesKeys.MANGA_MODE)

    val WEBTOON_MODE = booleanPreferencesKey(UserPreferencesKeys.WEBTOON_MODE)

    val DOUBLE_PAGE_MODE = booleanPreferencesKey(UserPreferencesKeys.DOUBLE_PAGE_MODE)

    val PAGE_FLIP_SOUND_ENABLED = booleanPreferencesKey(UserPreferencesKeys.PAGE_FLIP_SOUND_ENABLED)

    val LAST_SCAN_TOTAL_FILES = intPreferencesKey(UserPreferencesKeys.LAST_SCAN_TOTAL_FILES)
    val LAST_SCAN_PROCESSED_COMICS = intPreferencesKey(UserPreferencesKeys.LAST_SCAN_PROCESSED_COMICS)

    const val DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD = 1
}