package dev.diegoflassa.comiqueta.core.domain.usecase

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.diegoflassa.comiqueta.core.data.worker.SafFolderScanWorker
import java.util.UUID
import javax.inject.Inject

/**
 * Use case to enqueue a one-time work request for [SafFolderScanWorker]
 * to scan persisted comic folders.
 */
class EnqueueSafFolderScanWorkerUseCase @Inject constructor(
    private val workManager: WorkManager
) : IEnqueueSafFolderScanWorkerUseCase {
    override operator fun invoke(): UUID {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val scanWorkRequest = OneTimeWorkRequestBuilder<SafFolderScanWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            SafFolderScanWorker::class.java.name,
            ExistingWorkPolicy.KEEP,
            scanWorkRequest
        )
        return scanWorkRequest.id
    }
}
