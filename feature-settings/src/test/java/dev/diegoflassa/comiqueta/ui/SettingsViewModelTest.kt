package dev.diegoflassa.comiqueta.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.comiqueta.core.data.preferences.PreferencesKeys
import dev.diegoflassa.comiqueta.core.data.repository.IComicsRepository
import dev.diegoflassa.comiqueta.core.domain.usecase.IEnqueueSafFolderScanWorkerUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.folder.IAddMonitoredFolderUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.folder.IGetMonitoredFoldersUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.folder.IRemoveMonitoredFolderUseCase
import dev.diegoflassa.comiqueta.core.domain.usecase.permission.IGetRelevantOsPermissionsUseCase
import dev.diegoflassa.comiqueta.domain.usecase.IRefreshPermissionDisplayStatusUseCase
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
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var getMonitoredFoldersUseCase: IGetMonitoredFoldersUseCase

    @Mock
    private lateinit var addMonitoredFolderUseCase: IAddMonitoredFolderUseCase

    @Mock
    private lateinit var removeMonitoredFolderUseCase: IRemoveMonitoredFolderUseCase

    @Mock
    private lateinit var getRelevantOsPermissionsUseCase: IGetRelevantOsPermissionsUseCase

    @Mock
    private lateinit var refreshPermissionDisplayStatusUseCase: IRefreshPermissionDisplayStatusUseCase

    @Mock
    private lateinit var comicsRepository: IComicsRepository

    @Mock
    private lateinit var enqueueSafFolderScanWorkerUseCase: IEnqueueSafFolderScanWorkerUseCase

    @Mock
    private lateinit var dataStore: DataStore<Preferences>

    @Mock
    private lateinit var preferences: Preferences

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(dataStore.data).thenReturn(flowOf(preferences))
        whenever(getMonitoredFoldersUseCase.invoke()).thenReturn(flowOf(emptyList()))
        whenever(getRelevantOsPermissionsUseCase.invoke()).thenReturn(flowOf(emptyList()))
        
        viewModel = SettingsViewModel(
            context,
            getMonitoredFoldersUseCase,
            addMonitoredFolderUseCase,
            removeMonitoredFolderUseCase,
            getRelevantOsPermissionsUseCase,
            refreshPermissionDisplayStatusUseCase,
            comicsRepository,
            enqueueSafFolderScanWorkerUseCase,
            dataStore
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when dataStore emits reading mode preferences, uiState should be updated`() = runTest {
        // Arrange
        whenever(preferences[PreferencesKeys.WEBTOON_MODE]).thenReturn(true)
        whenever(preferences[PreferencesKeys.DOUBLE_PAGE_MODE]).thenReturn(true)
        whenever(preferences[PreferencesKeys.PAGE_FLIP_SOUND_ENABLED]).thenReturn(true)

        // Act - Re-init to collect new flow
        viewModel = SettingsViewModel(
            context,
            getMonitoredFoldersUseCase,
            addMonitoredFolderUseCase,
            removeMonitoredFolderUseCase,
            getRelevantOsPermissionsUseCase,
            refreshPermissionDisplayStatusUseCase,
            comicsRepository,
            enqueueSafFolderScanWorkerUseCase,
            dataStore
        )
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertThat(state.isWebtoonMode).isTrue()
        assertThat(state.isDoublePageView).isTrue()
        assertThat(state.isPageFlipSoundEnabled).isTrue()
    }
}
