package dev.diegoflassa.comiqueta.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.diegoflassa.comiqueta.core.data.database.ComicDatabase
import dev.diegoflassa.comiqueta.core.data.database.dao.ComicsDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Or another appropriate component
object DatabaseModule {

    @Provides
    @Singleton
    fun provideComicDatabase(@ApplicationContext context: Context): ComicDatabase {
        return ComicDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideComicsDao(database: ComicDatabase): ComicsDao {
        return database.comicsDao()
    }
}
    