package dev.diegoflassa.comiqueta.viewer.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dev.diegoflassa.comiqueta.viewer.domain.usecase.DecodeComicPageUseCase
import dev.diegoflassa.comiqueta.viewer.domain.usecase.GetComicInfoUseCase

@Module
@InstallIn(ViewModelComponent::class)
object ViewerUseCaseModule {

    @Provides
    @ViewModelScoped
    fun provideGetComicInfoUseCase(application: Application): GetComicInfoUseCase {
        return GetComicInfoUseCase(application)
    }

    @Provides
    @ViewModelScoped
    fun provideDecodeComicPageUseCase(application: Application): DecodeComicPageUseCase {
        return DecodeComicPageUseCase(application)
    }
}
