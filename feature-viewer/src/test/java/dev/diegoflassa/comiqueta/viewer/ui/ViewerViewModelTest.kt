package dev.diegoflassa.comiqueta.viewer.ui

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.comiqueta.core.data.preferences.PreferencesKeys
import dev.diegoflassa.comiqueta.core.data.repository.IComicsRepository
import dev.diegoflassa.comiqueta.core.domain.usecase.comic.IGetComicUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.comic.IUpdateComicProgressUseCase
import dev.diegoflassa.comiqueta.viewer.domain.usecase.IDecodeComicPageUseCase
import dev.diegoflassa.comiqueta.viewer.domain.usecase.IGetComicInfoUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the settings half of `ViewerViewModel.init`: the reading-mode flags it mirrors out of
 * DataStore, and the clamping applied to the page-preload count.
 *
 * Runs under Robolectric because `ViewerUIState` defaults `comicPath` to `Uri.EMPTY`, which is null
 * in the stub `android.jar` — the state cannot even be constructed on a bare JVM. The SDK is pinned
 * one below `compileSdk` because Robolectric ships no runtime for API 37 yet.
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ViewerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getComicInfoUseCase: IGetComicInfoUseCase = mock()
    private val decodeComicPageUseCase: IDecodeComicPageUseCase = mock()
    private val getComicUseCase: IGetComicUseCase = mock()
    private val updateComicProgressUseCase: IUpdateComicProgressUseCase = mock()
    private val comicsRepository: IComicsRepository = mock()
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val dataStore: DataStore<Preferences> = mock()
    private val preferences: Preferences = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(dataStore.data).thenReturn(flowOf(preferences))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Built per test: `init` reads DataStore immediately, so stubs have to be in place first. */
    private fun createViewModel(): IViewerViewModel = ViewerViewModel(
        getComicInfoUseCase,
        decodeComicPageUseCase,
        getComicUseCase,
        updateComicProgressUseCase,
        comicsRepository,
        application,
        dataStore
    )

    @Test
    fun `stored reading modes are mirrored into the state`() = runTest(testDispatcher) {
        whenever(preferences[PreferencesKeys.MANGA_MODE]).thenReturn(true)
        whenever(preferences[PreferencesKeys.WEBTOON_MODE]).thenReturn(true)
        whenever(preferences[PreferencesKeys.DOUBLE_PAGE_MODE]).thenReturn(true)
        whenever(preferences[PreferencesKeys.PAGE_FLIP_SOUND_ENABLED]).thenReturn(true)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isMangaMode).isTrue()
        assertThat(state.isWebtoonMode).isTrue()
        assertThat(state.isDoublePageMode).isTrue()
        assertThat(state.isPageFlipSoundEnabled).isTrue()
    }

    @Test
    fun `preferences set to false are honoured rather than defaulted back on`() =
        runTest(testDispatcher) {
            whenever(preferences[PreferencesKeys.MANGA_MODE]).thenReturn(false)
            whenever(preferences[PreferencesKeys.WEBTOON_MODE]).thenReturn(false)
            whenever(preferences[PreferencesKeys.DOUBLE_PAGE_MODE]).thenReturn(false)
            whenever(preferences[PreferencesKeys.PAGE_FLIP_SOUND_ENABLED]).thenReturn(false)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.isMangaMode).isFalse()
            assertThat(state.isWebtoonMode).isFalse()
            assertThat(state.isDoublePageMode).isFalse()
            assertThat(state.isPageFlipSoundEnabled).isFalse()
        }

    @Test
    fun `an unwritten preference reads as off on a fresh install`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isMangaMode).isFalse()
        assertThat(state.isWebtoonMode).isFalse()
        assertThat(state.isDoublePageMode).isFalse()
        assertThat(state.isPageFlipSoundEnabled).isFalse()
        assertThat(state.pagesToPreloadLogic)
            .isEqualTo(ViewerUIState.DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD)
    }

    @Test
    fun `an oversized preload setting is clamped before it reaches the cache`() =
        runTest(testDispatcher) {
            // The preload count sizes an LruCache of decoded bitmaps. An unclamped 99 would size it
            // at 199 full-page bitmaps, which is an OOM on a real device.
            whenever(preferences[PreferencesKeys.VIEWER_PAGES_TO_PRELOAD_AHEAD]).thenReturn(99)

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.pagesToPreloadLogic).isEqualTo(5)
        }

    @Test
    fun `a negative preload setting is floored at zero`() = runTest(testDispatcher) {
        whenever(preferences[PreferencesKeys.VIEWER_PAGES_TO_PRELOAD_AHEAD]).thenReturn(-3)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // A negative window would make the cache-size arithmetic collapse to a non-positive value.
        assertThat(viewModel.uiState.value.pagesToPreloadLogic).isEqualTo(0)
    }
}
