package dev.diegoflassa.comiqueta.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dev.diegoflassa.comiqueta.domain.usecase.IRefreshPermissionDisplayStatusUseCase
import dev.diegoflassa.comiqueta.domain.usecase.RefreshPermissionDisplayStatusUseCase

@Module
@InstallIn(ViewModelComponent::class)
abstract class SettingsUseCaseModule {

    @Binds
    @ViewModelScoped
    abstract fun bindRefreshPermissionDisplayStatusUseCase(
        refreshPermissionDisplayStatusUseCase: RefreshPermissionDisplayStatusUseCase
    ): IRefreshPermissionDisplayStatusUseCase

}