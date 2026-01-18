package dev.diegoflassa.comiqueta.core.domain.usecase

import androidx.work.Constraints
import androidx.work.Data // Import Data
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
    override operator fun invoke(uriString: String?): UUID {
        // Create input data for the worker
        val inputDataBuilder = Data.Builder()
        uriString?.let {
            inputDataBuilder.putString(SafFolderScanWorker.KEY_FOLDER_URI, it)
        }
        val inputData = inputDataBuilder.build()

        val scanWorkRequest = OneTimeWorkRequestBuilder<SafFolderScanWorker>()
            .setInputData(inputData)
            .addTag(SafFolderScanWorker.TAG)
            .build()

        // Using a unique work name strategy: 
        // - For a specific URI, the name includes a hash of the URI to allow specific scans to be replaced.
        // - For a general scan (uriString is null), a constant name is used.
        val uniqueWorkName = if (uriString != null) {
            "${SafFolderScanWorker.TAG}_${uriString.hashCode()}"
        } else {
            SafFolderScanWorker.TAG
        }

        dev.diegoflassa.comiqueta.core.data.timber.TimberLogger.logI(
            SafFolderScanWorker.TAG,
            "Enqueuing unique work: $uniqueWorkName for request: ${scanWorkRequest.id}"
        )

        workManager.enqueueUniqueWork(
            uniqueWorkName, 
            ExistingWorkPolicy.REPLACE,
            scanWorkRequest
        )
        return scanWorkRequest.id
    }
}
