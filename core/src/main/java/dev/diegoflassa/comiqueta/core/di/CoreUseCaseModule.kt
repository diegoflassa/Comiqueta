package dev.diegoflassa.comiqueta.core.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import dev.diegoflassa.comiqueta.core.domain.usecase.EnqueueSafFolderScanWorkerUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.IEnqueueSafFolderScanWorkerUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.folder.AddMonitoredFolderUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.folder.GetMonitoredFoldersUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.folder.IAddMonitoredFolderUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.folder.IGetMonitoredFoldersUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.folder.IRemoveMonitoredFolderUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.folder.RemoveMonitoredFolderUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.permission.GetRelevantOsPermissionsUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.permission.IGetRelevantOsPermissionsUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.comic.GetComicUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.comic.IGetComicUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.comic.IUpdateComicProgressUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.comic.UpdateComicProgressUseCase

@Module
@InstallIn(ViewModelComponent::class)
abstract class CoreUseCaseModule {

    @Binds
    @ViewModelScoped
    abstract fun bindAddMonitoredFolderUseCase(
        addMonitoredFolderUseCase: AddMonitoredFolderUseCase
    ): IAddMonitoredFolderUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindGetMonitoredFoldersUseCase(
        getMonitoredFoldersUseCase: GetMonitoredFoldersUseCase
    ): IGetMonitoredFoldersUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindRemoveMonitoredFolderUseCase(
        removeMonitoredFolderUseCase: RemoveMonitoredFolderUseCase
    ): IRemoveMonitoredFolderUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindGetRelevantOsPermissionsUseCase(
        getRelevantOsPermissionsUseCase: GetRelevantOsPermissionsUseCase
    ): IGetRelevantOsPermissionsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindEnqueueSafFolderScanWorkerUseCase(
        enqueueSafFolderScanWorkerUseCase: EnqueueSafFolderScanWorkerUseCase
    ): IEnqueueSafFolderScanWorkerUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindGetComicUseCase(
        getComicUseCase: GetComicUseCase
    ): IGetComicUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindUpdateComicProgressUseCase(
        updateComicProgressUseCase: UpdateComicProgressUseCase
    ): IUpdateComicProgressUseCase

    companion object {
        @Provides
        @ViewModelScoped
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
            return WorkManager.getInstance(context)
        }
    }
}
