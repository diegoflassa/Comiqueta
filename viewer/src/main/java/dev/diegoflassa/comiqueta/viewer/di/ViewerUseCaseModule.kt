package dev.diegoflassa.comiqueta.viewer.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dev.diegoflassa.comiqueta.viewer.domain.usecase.DecodeComicPageUseCase
import dev.diegoflassa.comiqueta.viewer.domain.usecase.GetComicInfoUseCase
import dev.diegoflassa.comiqueta.viewer.domain.usecase.IDecodeComicPageUseCase
import dev.diegoflassa.comiqueta.viewer.domain.usecase.IGetComicInfoUseCase

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewerDomainModule {

    @Binds
    @ViewModelScoped
    abstract fun bindGetComicInfoUseCase(
        getComicInfoUseCase: GetComicInfoUseCase
    ): IGetComicInfoUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindDecodeComicPageUseCase(
        decodeComicPageUseCase: DecodeComicPageUseCase
    ): IDecodeComicPageUseCase
}
