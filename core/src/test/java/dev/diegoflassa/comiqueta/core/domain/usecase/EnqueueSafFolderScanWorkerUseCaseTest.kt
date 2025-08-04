package dev.diegoflassa.comiqueta.core.domain.usecase

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.comiqueta.core.data.worker.SafFolderScanWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class EnqueueSafFolderScanWorkerUseCaseTest {

    private lateinit var mockWorkManager: WorkManager

    private lateinit var enqueueSafFolderScanWorkerUseCase: EnqueueSafFolderScanWorkerUseCase

    private lateinit var kWorkRequestCaptor: KArgumentCaptor<OneTimeWorkRequest>

    @Before
    fun setUp() {
        mockWorkManager = Mockito.mock(WorkManager::class.java)
        kWorkRequestCaptor = argumentCaptor<OneTimeWorkRequest>()
        enqueueSafFolderScanWorkerUseCase = EnqueueSafFolderScanWorkerUseCase(mockWorkManager)
    }

    @Test
    fun `invoke should enqueue unique work with correct parameters`() {
        enqueueSafFolderScanWorkerUseCase()

        verify(mockWorkManager).enqueueUniqueWork(
            eq(SafFolderScanWorker::class.java.name),
            eq(ExistingWorkPolicy.KEEP),
            kWorkRequestCaptor.capture()
        )

        val capturedRequest = kWorkRequestCaptor.firstValue
        assertThat(capturedRequest).isNotNull()
        assertThat(capturedRequest.workSpec.workerClassName).isEqualTo(SafFolderScanWorker::class.java.name)
    }
}
