package dev.diegoflassa.comiqueta.viewer.ui

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ViewerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var getComicInfoUseCase: IGetComicInfoUseCase

    @Mock
    private lateinit var decodeComicPageUseCase: IDecodeComicPageUseCase

    @Mock
    private lateinit var getComicUseCase: IGetComicUseCase

    @Mock
    private lateinit var updateComicProgressUseCase: IUpdateComicProgressUseCase

    @Mock
    private lateinit var comicsRepository: IComicsRepository

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var dataStore: DataStore<Preferences>

    @Mock
    private lateinit var preferences: Preferences

    private lateinit var viewModel: IViewerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(dataStore.data).thenReturn(flowOf(preferences))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when dataStore emits preferences, uiState should be updated`() = runTest {
        // Arrange
        whenever(preferences[PreferencesKeys.WEBTOON_MODE]).thenReturn(true)
        whenever(preferences[PreferencesKeys.DOUBLE_PAGE_MODE]).thenReturn(true)
        whenever(preferences[PreferencesKeys.PAGE_FLIP_SOUND_ENABLED]).thenReturn(true)

        // Act
        viewModel = ViewerViewModel(
            getComicInfoUseCase,
            decodeComicPageUseCase,
            getComicUseCase,
            updateComicProgressUseCase,
            comicsRepository,
            application,
            dataStore
        )
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertThat(state.isWebtoonMode).isTrue()
        assertThat(state.isDoublePageMode).isTrue()
        assertThat(state.isPageFlipSoundEnabled).isTrue()
    }

    @Test
    fun `when preferences are false, uiState should reflect that`() = runTest {
        // Arrange
        whenever(preferences[PreferencesKeys.WEBTOON_MODE]).thenReturn(false)
        whenever(preferences[PreferencesKeys.DOUBLE_PAGE_MODE]).thenReturn(false)
        whenever(preferences[PreferencesKeys.PAGE_FLIP_SOUND_ENABLED]).thenReturn(false)

        // Act
        viewModel = ViewerViewModel(
            getComicInfoUseCase,
            decodeComicPageUseCase,
            getComicUseCase,
            updateComicProgressUseCase,
            comicsRepository,
            application,
            dataStore
        )
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertThat(state.isWebtoonMode).isFalse()
        assertThat(state.isDoublePageMode).isFalse()
        assertThat(state.isPageFlipSoundEnabled).isFalse()
    }
}
