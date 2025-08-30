package dev.diegoflassa.comiqueta.categories.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dev.diegoflassa.comiqueta.core.domain.usecase.category.AddCategoryUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.category.DeleteCategoryUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.category.GetCategoriesUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.category.IAddCategoryUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.category.IDeleteCategoryUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.category.IGetCategoriesUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.category.IUpdateCategoryUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.category.UpdateCategoryUseCase

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewerDomainModule {

    @Binds
    @ViewModelScoped
    abstract fun bindGetCategoriesUseCase(
        getCategoriesUseCase: GetCategoriesUseCase
    ): IGetCategoriesUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindAddCategoryUseCase(
        addCategoryUseCase: AddCategoryUseCase
    ): IAddCategoryUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindUpdateCategoryUseCase(
        updateCategoryUseCase: UpdateCategoryUseCase
    ): IUpdateCategoryUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindDeleteCategoryUseCase(
        deleteCategoryUseCase: DeleteCategoryUseCase
    ): IDeleteCategoryUseCase
}
