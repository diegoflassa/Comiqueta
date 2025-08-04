package dev.diegoflassa.comiqueta.core.domain.usecase

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.comiqueta.core.data.worker.SafFolderScanWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class EnqueueSafFolderScanWorkerUseCaseTest {

    // Removemos os @Mock e deixamos como lateinit var
    private lateinit var mockWorkManager: WorkManager
    private lateinit var mockContext: Context

    private lateinit var enqueueSafFolderScanWorkerUseCase: EnqueueSafFolderScanWorkerUseCase

    private lateinit var workRequestCaptor: ArgumentCaptor<OneTimeWorkRequest>

    @Before
    fun setUp() {
        // Agora, criamos os mocks manualmente
        mockWorkManager = mock()
        mockContext = mock()
        workRequestCaptor = ArgumentCaptor.forClass(OneTimeWorkRequest::class.java)

        // Instanciamos o use case, passando o mockWorkManager explicitamente
        enqueueSafFolderScanWorkerUseCase = EnqueueSafFolderScanWorkerUseCase(mockWorkManager)
    }

    @Test
    fun `invoke should enqueue unique work with correct parameters`() {
        // Act
        enqueueSafFolderScanWorkerUseCase()

        // Assert
        // A verificação é a mesma, mas agora o mockWorkManager é uma instância controlada manualmente
        verify(mockWorkManager).enqueueUniqueWork(
            eq(SafFolderScanWorker::class.java.name),
            eq(ExistingWorkPolicy.KEEP),
            workRequestCaptor.capture()
        )

        val capturedRequest = workRequestCaptor.value
        assertThat(capturedRequest.workSpec.workerClassName).isEqualTo(SafFolderScanWorker::class.java.name)
    }
}
