package dev.diegoflassa.comiqueta.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.diegoflassa.comiqueta.core.data.config.IConfig
import dev.diegoflassa.comiqueta.core.data.config.Config
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Or another appropriate component
object ConfigModule {

    @Provides
    @Singleton
    fun provideIConfig(@ApplicationContext context: Context): IConfig {
        return Config(context)
    }
}
