package dev.diegoflassa.comiqueta.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.diegoflassa.comiqueta.core.data.repository.ComicsFolderRepository
import dev.diegoflassa.comiqueta.core.domain.usecase.folder.AddMonitoredFolderUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.folder.GetMonitoredFoldersUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.folder.RemoveMonitoredFolderUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.permission.GetRelevantOsPermissionsUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCasesSettingsModule {

    @Provides
    @Singleton
    fun provideGetMonitoredFoldersUseCase(comicsFolderRepository: ComicsFolderRepository): GetMonitoredFoldersUseCase {
        return GetMonitoredFoldersUseCase(comicsFolderRepository)
    }

    @Provides
    @Singleton
    fun provideAddMonitoredFolderUseCase(comicsFolderRepository: ComicsFolderRepository): AddMonitoredFolderUseCase {
        return AddMonitoredFolderUseCase(comicsFolderRepository)
    }

    @Provides
    @Singleton
    fun provideRemoveMonitoredFolderUseCase(comicsFolderRepository: ComicsFolderRepository): RemoveMonitoredFolderUseCase {
        return RemoveMonitoredFolderUseCase(comicsFolderRepository)
    }

    @Provides
    @Singleton
    fun provideGetRelevantOsPermissionsUseCase(): GetRelevantOsPermissionsUseCase {
        return GetRelevantOsPermissionsUseCase()
    }
}
