package dev.diegoflassa.comiqueta.ui.home

import android.app.Application
import androidx.paging.PagingData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.comiqueta.core.data.config.IConfig
import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags
import dev.diegoflassa.comiqueta.core.data.repository.IComicsFolderRepository
import dev.diegoflassa.comiqueta.core.data.worker.SafFolderScanWorker
import dev.diegoflassa.comiqueta.core.domain.model.Category
import dev.diegoflassa.comiqueta.core.domain.usecase.IEnqueueSafFolderScanWorkerUseCase
import dev.diegoflassa.comiqueta.domain.usecase.IGetPaginatedComicsUseCase
import dev.diegoflassa.comiqueta.domain.usecase.ILoadCategoriesUseCase
import dev.diegoflassa.comiqueta.domain.usecase.PaginatedComicsParams
import dev.diegoflassa.comiqueta.home.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Covers the reducer wiring that the Home screen depends on: what the initial state looks like,
 * and which filter set each intent forwards to the paging use case.
 *
 * This suite used to be written against MockK, which is not in the version catalogue, so it never
 * compiled. It now uses mockito-kotlin like every other suite in the repo.
 */
@ExperimentalCoroutinesApi
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val application: Application = mock()
    private val config: IConfig = mock()
    private val getPaginatedComicsUseCase: IGetPaginatedComicsUseCase = mock()
    private val loadCategoriesUseCase: ILoadCategoriesUseCase = mock()
    private val comicsFolderRepository: IComicsFolderRepository = mock()
    private val enqueueSafFolderScanWorkerUseCase: IEnqueueSafFolderScanWorkerUseCase = mock()
    private val workManager: WorkManager = mock()

    private val categories = MutableStateFlow<List<Category>>(emptyList())

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        whenever(loadCategoriesUseCase.invoke()).thenReturn(categories)
        // The scan observer runs from init; an empty tag flow is the "no scan in flight" case.
        whenever(workManager.getWorkInfosByTagFlow(SafFolderScanWorker.TAG))
            .thenReturn(flowOf(emptyList<WorkInfo>()))
        whenever(getPaginatedComicsUseCase.invoke(any()))
            .thenReturn(flowOf(PagingData.empty()))

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
    fun `init settles on an idle state without querying comics`() =
        runTest(mainDispatcherRule.testDispatcher) {
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.searchQuery).isEmpty()
            assertThat(state.error).isNull()
            assertThat(state.isScanningFolders).isFalse()
            assertThat(state.scanProgress).isEqualTo(0)
            // Loading the list is the screen's call, not the ViewModel's: init only wires up the
            // category and scan observers. A stray query here would double-load on every rotation.
            verify(getPaginatedComicsUseCase, never()).invoke(any())
        }

    @Test
    fun `categories emitted by the use case reach the state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val loaded = listOf(
                Category(id = 1L, name = "DC Comics", createdAt = 1_000L),
                Category(id = 2L, name = "Marvel", createdAt = 2_000L)
            )
            categories.value = loaded
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.categories).containsExactlyElementsIn(loaded).inOrder()
        }

    @Test
    fun `LoadComics queries the main grid plus the two shelves`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.reduce(HomeIntent.LoadComics)
            advanceUntilIdle()

            val captor = argumentCaptor<PaginatedComicsParams>()
            verify(getPaginatedComicsUseCase, times(3)).invoke(captor.capture())
            // One intent fans out into three queries: the main grid, which carries the screen's own
            // filters, and the Latest and Favorites shelves, which are always unfiltered. Pinning
            // all three catches a shelf being dropped or accidentally inheriting the grid filters.
            assertThat(captor.allValues).containsExactly(
                PaginatedComicsParams(categoryId = null, flags = emptySet(), searchQuery = ""),
                PaginatedComicsParams(flags = setOf(ComicFlags.NEW)),
                PaginatedComicsParams(flags = setOf(ComicFlags.FAVORITE))
            )
        }

    @Test
    fun `SearchComics stores the term and re-queries with it`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.reduce(HomeIntent.SearchComics("batman"))
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.searchQuery).isEqualTo("batman")

            val captor = argumentCaptor<PaginatedComicsParams>()
            verify(getPaginatedComicsUseCase, times(3)).invoke(captor.capture())
            // The main grid has to carry the new term. Updating only the state would leave the grid
            // showing the unfiltered library while the search box says otherwise. The shelves stay
            // unfiltered on purpose: they are carousels, not search results.
            assertThat(captor.allValues).containsExactly(
                PaginatedComicsParams(categoryId = null, flags = emptySet(), searchQuery = "batman"),
                PaginatedComicsParams(flags = setOf(ComicFlags.NEW)),
                PaginatedComicsParams(flags = setOf(ComicFlags.FAVORITE))
            )
        }

    @Test
    fun `repeating the same search term does not re-query`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.reduce(HomeIntent.SearchComics("batman"))
            advanceUntilIdle()
            viewModel.reduce(HomeIntent.SearchComics("batman"))
            advanceUntilIdle()

            // Every keystroke reaches the reducer; without this guard an unchanged term would
            // restart paging and visibly reset the user's scroll position. Three calls is one
            // round of loading (grid + two shelves), so the second search did nothing.
            verify(getPaginatedComicsUseCase, times(3)).invoke(any())
        }

    @Test
    fun `ToggleFlag adds the flag then removes it on the second toggle`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.uiState.test {
                assertThat(awaitItem().flags).isEmpty()

                viewModel.reduce(HomeIntent.ToggleFlag(ComicFlags.FAVORITE))
                advanceUntilIdle()
                assertThat(viewModel.uiState.value.flags)
                    .containsExactly(ComicFlags.FAVORITE)

                viewModel.reduce(HomeIntent.ToggleFlag(ComicFlags.FAVORITE))
                advanceUntilIdle()
                assertThat(viewModel.uiState.value.flags).isEmpty()

                cancelAndIgnoreRemainingEvents()
            }
        }
}
