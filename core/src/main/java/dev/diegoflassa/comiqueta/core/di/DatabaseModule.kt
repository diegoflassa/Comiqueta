package dev.diegoflassa.comiqueta.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.diegoflassa.comiqueta.core.data.database.ComicDatabase
import dev.diegoflassa.comiqueta.core.data.database.dao.ComicDao
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
    @Singleton // Or another appropriate scope if ComicDatabase is not a Singleton
    fun provideComicDao(database: ComicDatabase): ComicDao {
        return database.comicDao() // Ensure comicDao() is a method in your ComicDatabase class
    }
}
    