package dev.diegoflassa.comiqueta.core.di

import androidx.work.WorkManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { WorkManager.getInstance(androidContext()) }
}