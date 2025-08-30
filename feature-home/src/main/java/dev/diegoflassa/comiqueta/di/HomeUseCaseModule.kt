package dev.diegoflassa.comiqueta.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dev.diegoflassa.comiqueta.domain.usecase.GetPaginatedComicsUseCase
import dev.diegoflassa.comiqueta.domain.usecase.IGetPaginatedComicsUseCase
import dev.diegoflassa.comiqueta.domain.usecase.LoadCategoriesUseCase
import dev.diegoflassa.comiqueta.domain.usecase.ILoadCategoriesUseCase

@Module
@InstallIn(ViewModelComponent::class)
abstract class HomeUseCaseModule {

    @Binds
    @ViewModelScoped
    abstract fun bindGetPaginatedComicsUseCase(
        getPaginatedComicsUseCase: GetPaginatedComicsUseCase
    ): IGetPaginatedComicsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindLoadCategoriesUseCase(
        loadCategoriesUseCase: LoadCategoriesUseCase
    ): ILoadCategoriesUseCase
}
