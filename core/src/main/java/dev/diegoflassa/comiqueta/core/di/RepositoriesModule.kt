package dev.diegoflassa.comiqueta.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.diegoflassa.comiqueta.core.data.database.dao.ComicsDao
import dev.diegoflassa.comiqueta.core.data.database.dao.ComicsFoldersDao // Corrected import
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
    fun provideComicsFolderRepository(comicsFolderDao: ComicsFoldersDao): ComicsFolderRepository {
        return ComicsFolderRepository(comicsFolderDao)
    }
}
