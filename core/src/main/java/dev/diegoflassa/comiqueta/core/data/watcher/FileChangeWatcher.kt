package dev.diegoflassa.comiqueta.core.data.watcher

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore

class FileChangeWatcher(
    private val context: Context,
    private val onChange: () -> Unit
) {

    private val handler = Handler(Looper.getMainLooper())
    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            onChange()
        }
    }

    fun start() {
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
    }

    fun stop() {
        context.contentResolver.unregisterContentObserver(observer)
    }
}

