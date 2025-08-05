package dev.diegoflassa.comiqueta.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.diegoflassa.comiqueta.core.data.repository.IComicsFolderRepository
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
    fun provideGetMonitoredFoldersUseCase(comicsFolderRepository: IComicsFolderRepository): GetMonitoredFoldersUseCase {
        return GetMonitoredFoldersUseCase(comicsFolderRepository)
    }

    @Provides
    @Singleton
    fun provideAddMonitoredFolderUseCase(comicsFolderRepository: IComicsFolderRepository): AddMonitoredFolderUseCase {
        return AddMonitoredFolderUseCase(comicsFolderRepository)
    }

    @Provides
    @Singleton
    fun provideRemoveMonitoredFolderUseCase(comicsFolderRepository: IComicsFolderRepository): RemoveMonitoredFolderUseCase {
        return RemoveMonitoredFolderUseCase(comicsFolderRepository)
    }

    @Provides
    @Singleton
    fun provideGetRelevantOsPermissionsUseCase(): GetRelevantOsPermissionsUseCase {
        return GetRelevantOsPermissionsUseCase()
    }
}
