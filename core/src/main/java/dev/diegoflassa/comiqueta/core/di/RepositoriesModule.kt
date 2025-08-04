package dev.diegoflassa.comiqueta.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.diegoflassa.comiqueta.core.data.repository.CategoryRepository
import dev.diegoflassa.comiqueta.core.data.repository.IComicsFolderRepository
import dev.diegoflassa.comiqueta.core.data.repository.ComicsFolderRepository
import dev.diegoflassa.comiqueta.core.data.repository.ComicsRepository
import dev.diegoflassa.comiqueta.core.data.repository.IComicsRepository
import dev.diegoflassa.comiqueta.core.domain.repository.ICategoryRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoriesModule {

    @Binds
    @Singleton
    abstract fun bindComicsRepository(comicsRepository: ComicsRepository): IComicsRepository

    @Binds
    @Singleton
    abstract fun bindComicsFolderRepository(impl: ComicsFolderRepository): IComicsFolderRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(categoryRepositoryImpl: CategoryRepository): ICategoryRepository
}
