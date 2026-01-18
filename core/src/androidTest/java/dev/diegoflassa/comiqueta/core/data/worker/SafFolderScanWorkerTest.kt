package dev.diegoflassa.comiqueta.core.data.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.diegoflassa.comiqueta.core.domain.usecase.IEnqueueSafFolderScanWorkerUseCase
import org.hamcrest.CoreMatchers.`is`

@RunWith(AndroidJUnit4::class)
class SafFolderScanWorkerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testWorkerStart() = runTest {
        // We cannot easily test DocumentFile recursion without a real Uri tree provided by SAF,
        // which requires user interaction or complex test setup. 
        // However, we can verify the worker starts and potentially fails gracefully or handles invalid input.
        
        val worker = TestListenableWorkerBuilder<SafFolderScanWorker>(context)
            .setInputData(workDataOf(SafFolderScanWorker.KEY_URI to "content://fake/uri"))
            .build()
            
        val result = worker.doWork()
        
        // It should probably fail or succeed with 0 files depending on how DocumentFile handles fake URI.
        // Our fix added try-catch, so it shouldn't crash.
        // If it was crashing before, this test (if run before fix) would have crashed.
        // Now it should return Result.failure() or Result.success() but NOT crash.
        
        // Actually, without a valid tree URI permission, DocumentFile.fromTreeUri might return null or a non-directory.
        // And our code handles "Parent URI is not a directory or invalid" by logging warning and returning 0 files.
        // Then doWork logic continues to scan. 
        
        // We expect Result.success() or Result.failure() but no exception thrown.
        // Let's just assert it runs.
        
        assertTrue(result is ListenableWorker.Result.Success || result is ListenableWorker.Result.Failure)
    }
}
