package dev.diegoflassa.comiqueta.di

import dagger.Binds // Import Binds
import dagger.Module
// import dagger.Provides // No longer needed if all are @Binds
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
// import dev.diegoflassa.comiqueta.core.data.repository.CategoryRepository // No longer directly needed here for providing
import dev.diegoflassa.comiqueta.core.domain.repository.ICategoryRepository // Ensure this is the one used by LoadCategoriesUseCase
import dev.diegoflassa.comiqueta.domain.usecase.GetPaginatedComicsUseCase
import dev.diegoflassa.comiqueta.domain.usecase.IGetPaginatedComicsUseCase
import dev.diegoflassa.comiqueta.domain.usecase.LoadCategoriesUseCase
import dev.diegoflassa.comiqueta.domain.usecase.ILoadCategoriesUseCase // Import the new interface

@Module
@InstallIn(ViewModelComponent::class)
abstract class HomeUseCaseModule { // Changed to abstract class

    @Binds
    @ViewModelScoped // ViewModelScoped can be applied to @Binds
    abstract fun bindGetPaginatedComicsUseCase(
        getPaginatedComicsUseCase: GetPaginatedComicsUseCase
    ): IGetPaginatedComicsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindLoadCategoriesUseCase(
        loadCategoriesUseCase: LoadCategoriesUseCase
    ): ILoadCategoriesUseCase
}
