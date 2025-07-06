package dev.diegoflassa.comiqueta.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.diegoflassa.comiqueta.core.data.database.dao.ComicsDao
import dev.diegoflassa.comiqueta.core.data.repository.ComicsFolderRepository
import dev.diegoflassa.comiqueta.core.data.repository.ComicsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoriesModule {

    @Provides
    @Singleton
    fun provideComicsRepository(comicsDao: ComicsDao): ComicsRepository {
        return ComicsRepository(comicsDao)
    }

    @Provides
    @Singleton
    fun provideComicsFolderRepository(@ApplicationContext context: Context): ComicsFolderRepository {
        return ComicsFolderRepository(context)
    }
}
