package dev.diegoflassa.comiqueta.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dev.diegoflassa.comiqueta.core.data.repository.CategoryRepository
import dev.diegoflassa.comiqueta.core.data.repository.IComicsRepository
import dev.diegoflassa.comiqueta.domain.usecase.GetPaginatedComicsUseCase
import dev.diegoflassa.comiqueta.domain.usecase.LoadCategoriesUseCase

@Module
@InstallIn(ViewModelComponent::class)
object HomeUseCaseModule {

    @Provides
    @ViewModelScoped
    fun provideGetPaginatedComicsUseCase(IComicsRepository: IComicsRepository): GetPaginatedComicsUseCase {
        return GetPaginatedComicsUseCase(IComicsRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideLoadCategoriesUseCase(categoryRepository: CategoryRepository): LoadCategoriesUseCase {
        return LoadCategoriesUseCase(categoryRepository)
    }
}