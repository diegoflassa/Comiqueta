package dev.diegoflassa.comiqueta.core.domain.usecase

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.diegoflassa.comiqueta.core.data.worker.SafFolderScanWorker
import javax.inject.Inject

/**
 * Use case to enqueue a one-time work request for [SafFolderScanWorker]
 * to scan persisted comic folders.
 */
class EnqueueSafFolderScanWorkerUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    operator fun invoke() {
        val workManager = WorkManager.getInstance(context)

        val scanWorkRequest = OneTimeWorkRequestBuilder<SafFolderScanWorker>()
            // You can add constraints here if needed, for example:
            // .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()

        // Enqueue the work as unique work to prevent multiple instances if already scheduled
        workManager.enqueueUniqueWork(
            SafFolderScanWorker::class.java.name, // Using class name as unique work name
            ExistingWorkPolicy.KEEP, // Or use .REPLACE if you want a new worker to start immediately, canceling the old one
            scanWorkRequest
        )
    }
}
