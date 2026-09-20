package dev.diegoflassa.comiqueta.ui.home

import android.app.Application
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.data.worker.SafFolderScanWorker
import dev.diegoflassa.comiqueta.home.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun CoroutineScope.observeHomeScanWorker(
    workManager: WorkManager,
    applicationContext: Application,
    uiState: MutableStateFlow<HomeUIState>,
    loadPaginatedComics: () -> Unit,
) {
    launch {
        workManager
            .getWorkInfosByTagFlow(SafFolderScanWorker.TAG)
            .collectLatest { workInfos ->
                val workInfo = workInfos.firstOrNull { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    ?: workInfos.firstOrNull { it.state.isFinished }

                val progress = workInfo?.progress?.getInt(SafFolderScanWorker.KEY_PROGRESS, 0) ?: 0
                val currentComicName = workInfo?.progress?.getString(SafFolderScanWorker.KEY_CURRENT_COMIC_NAME)
                val processedComicsCount = workInfo?.progress?.getInt(SafFolderScanWorker.KEY_PROCESSED_COMICS_COUNT, 0) ?: 0
                val scanTotalFiles = workInfo?.progress?.getInt(SafFolderScanWorker.KEY_TOTAL_FILES_COUNT, 0) ?: 0
                val scanProcessedFiles = workInfo?.progress?.getInt(SafFolderScanWorker.KEY_PROCESSED_FILES_COUNT, 0) ?: 0

                uiState.update {
                    it.copy(
                        isScanningFolders = workInfo?.state == WorkInfo.State.RUNNING || workInfo?.state == WorkInfo.State.ENQUEUED,
                        scanProgress = if (workInfo?.state == WorkInfo.State.RUNNING) progress else if (it.scanFinished) 100 else 0,
                        currentComicName = if (workInfo?.state == WorkInfo.State.RUNNING) currentComicName
                            ?: it.currentComicName else if (it.scanFinished) it.currentComicName else null,
                        processedComicsCount = if (workInfo?.state == WorkInfo.State.RUNNING) processedComicsCount else if (it.scanFinished) it.processedComicsCount else 0,
                        scanTotalFiles = if (workInfo?.state == WorkInfo.State.RUNNING) scanTotalFiles else if (it.scanFinished) it.scanTotalFiles else 0,
                        scanProcessedFiles = if (workInfo?.state == WorkInfo.State.RUNNING) scanProcessedFiles else if (it.scanFinished) it.scanProcessedFiles else 0
                    )
                }

                if (workInfo != null && workInfo.state.isFinished && !uiState.value.scanFinished) {
                    uiState.update { it.copy(scanFinished = true) }
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            TimberLogger.logD("HomeViewModel", "[Comiqueta][Home] Scan SUCCEEDED. Refreshing.")
                            val message = applicationContext.getString(R.string.scan_completed)
                            uiState.update { it.copy(scanResultMessage = message) }
                            loadPaginatedComics()
                        }

                        WorkInfo.State.FAILED -> {
                            val errorMessage =
                                workInfo.outputData.getString(SafFolderScanWorker.KEY_ERROR_MESSAGE)
                            uiState.update {
                                it.copy(
                                    scanResultMessage = errorMessage
                                        ?: applicationContext.getString(R.string.scan_failed)
                                )
                            }
                        }

                        else -> {}
                    }
                }
            }
    }
}
