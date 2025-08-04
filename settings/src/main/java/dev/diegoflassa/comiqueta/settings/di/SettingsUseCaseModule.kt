package dev.diegoflassa.comiqueta.settings.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dev.diegoflassa.comiqueta.core.domain.usecase.permission.GetRelevantOsPermissionsUseCase
import dev.diegoflassa.comiqueta.settings.domain.usecase.RefreshPermissionDisplayStatusUseCase

@Module
@InstallIn(ViewModelComponent::class)
object SettingsUseCaseModule {

    @Provides
    @ViewModelScoped
    fun provideRefreshPermissionDisplayStatusUseCase(
        getRelevantOsPermissionsUseCase: GetRelevantOsPermissionsUseCase
    ): RefreshPermissionDisplayStatusUseCase {
        return RefreshPermissionDisplayStatusUseCase(getRelevantOsPermissionsUseCase)
    }
}
