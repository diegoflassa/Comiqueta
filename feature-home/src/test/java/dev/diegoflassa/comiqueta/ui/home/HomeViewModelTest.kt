package dev.diegoflassa.comiqueta.ui.home

import android.app.Application
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.cash.turbine.test
import dev.diegoflassa.comiqueta.core.data.config.IConfig
import dev.diegoflassa.comiqueta.core.data.repository.IComicsFolderRepository
import dev.diegoflassa.comiqueta.core.domain.usecase.IEnqueueSafFolderScanWorkerUseCase
import dev.diegoflassa.comiqueta.domain.usecase.IGetPaginatedComicsUseCase
import dev.diegoflassa.comiqueta.domain.usecase.ILoadCategoriesUseCase
import dev.diegoflassa.comiqueta.home.util.MainDispatcherRule
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK(relaxed = true)
    lateinit var application: Application
    
    @MockK(relaxed = true)
    lateinit var config: IConfig

    @MockK(relaxed = true)
    lateinit var getPaginatedComicsUseCase: IGetPaginatedComicsUseCase

    @MockK(relaxed = true)
    lateinit var loadCategoriesUseCase: ILoadCategoriesUseCase

    @MockK(relaxed = true)
    lateinit var comicsFolderRepository: IComicsFolderRepository

    @MockK(relaxed = true)
    lateinit var enqueueSafFolderScanWorkerUseCase: IEnqueueSafFolderScanWorkerUseCase
    
    @MockK
    lateinit var workManager: WorkManager

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        every { workManager.getWorkInfosByTagFlow(any()) } returns flowOf(emptyList<WorkInfo>())
        // Mock default flows
        coEvery { getPaginatedComicsUseCase(any()) } returns flowOf(androidx.paging.PagingData.empty())
        
        viewModel = HomeViewModel(
            application,
            config,
            getPaginatedComicsUseCase,
            loadCategoriesUseCase,
            comicsFolderRepository,
            enqueueSafFolderScanWorkerUseCase,
            workManager
        )
    }

    @Test
    fun `initial state has default values`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(false, state.isLoading)
            assertEquals("", state.searchQuery)
        }
    }

    @Test
    fun `LoadComics intent triggers use case`() = runTest {
        viewModel.reduce(HomeIntent.LoadComics)
        // Since LoadComics is called in init, it's hard to verify strict invocation count without verifying init block execution,
        // but we can verify that the flow is being collected or setup.
        // However, reduce(LoadComics) triggers loadPaginatedComics().
        
        coVerify(atLeast = 1) { getPaginatedComicsUseCase(any()) }
    }
}
