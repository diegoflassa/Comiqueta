package dev.diegoflassa.comiqueta.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
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
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Covers what `SettingsViewModel.init` promises the screen: the reading-mode preferences it mirrors
 * out of DataStore, the monitored folders it loads, and the permission rows it seeds.
 *
 * Deliberately plain JUnit. The ViewModel only reaches Android through `ContextCompat`, which falls
 * through to `Context.checkPermission` — something a mock answers fine — so this suite needs no
 * Robolectric runtime.
 */
@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val context: Context = mock()
    private val getMonitoredFoldersUseCase: IGetMonitoredFoldersUseCase = mock()
    private val addMonitoredFolderUseCase: IAddMonitoredFolderUseCase = mock()
    private val removeMonitoredFolderUseCase: IRemoveMonitoredFolderUseCase = mock()
    private val getRelevantOsPermissionsUseCase: IGetRelevantOsPermissionsUseCase = mock()
    private val refreshPermissionDisplayStatusUseCase: IRefreshPermissionDisplayStatusUseCase =
        mock()
    private val comicsRepository: IComicsRepository = mock()
    private val enqueueSafFolderScanWorkerUseCase: IEnqueueSafFolderScanWorkerUseCase = mock()
    private val dataStore: DataStore<Preferences> = mock()
    private val preferences: Preferences = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(dataStore.data).thenReturn(flowOf(preferences))
        whenever(getMonitoredFoldersUseCase.invoke()).thenReturn(flowOf(emptyList()))
        whenever(getRelevantOsPermissionsUseCase.invoke()).thenReturn(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Built per test rather than in [setUp]: `init` reads every stub straight away, so each test has
     * to finish arranging before the ViewModel exists.
     */
    private fun createViewModel() = SettingsViewModel(
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

    @Test
    fun `reading mode preferences are mirrored into the state`() = runTest(testDispatcher) {
        whenever(preferences[PreferencesKeys.WEBTOON_MODE]).thenReturn(true)
        whenever(preferences[PreferencesKeys.DOUBLE_PAGE_MODE]).thenReturn(true)
        whenever(preferences[PreferencesKeys.PAGE_FLIP_SOUND_ENABLED]).thenReturn(true)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isWebtoonMode).isTrue()
        assertThat(state.isDoublePageView).isTrue()
        assertThat(state.isPageFlipSoundEnabled).isTrue()
    }

    @Test
    fun `an unset preference falls back to off rather than to null`() = runTest(testDispatcher) {
        // Nothing stubbed: DataStore returns null for keys that were never written, which is the
        // state of a fresh install. Every toggle must read as off, not crash.
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isWebtoonMode).isFalse()
        assertThat(state.isDoublePageView).isFalse()
        assertThat(state.isPageFlipSoundEnabled).isFalse()
        assertThat(state.viewerPagesToPreloadAhead)
            .isEqualTo(PreferencesKeys.DEFAULT_VIEWER_PAGES_TO_PRELOAD_AHEAD)
    }

    @Test
    fun `the preload count is taken from DataStore when one was stored`() = runTest(testDispatcher) {
        whenever(preferences[PreferencesKeys.VIEWER_PAGES_TO_PRELOAD_AHEAD]).thenReturn(4)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.viewerPagesToPreloadAhead).isEqualTo(4)
    }

    @Test
    fun `monitored folders are loaded and the loading flag is cleared`() = runTest(testDispatcher) {
        val folders = listOf<Uri>(mock(), mock())
        whenever(getMonitoredFoldersUseCase.invoke()).thenReturn(flowOf(folders))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.comicsFolders).containsExactlyElementsIn(folders).inOrder()
        // The screen shows a spinner while this is true; leaving it set strands the user on it.
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `each relevant OS permission is seeded with the grant the system actually reports`() =
        runTest(testDispatcher) {
            val granted = Manifest.permission.READ_EXTERNAL_STORAGE
            val denied = Manifest.permission.READ_MEDIA_IMAGES
            whenever(getRelevantOsPermissionsUseCase.invoke()).thenReturn(listOf(granted, denied))
            whenever(context.checkPermission(eq(granted), any(), any()))
                .thenReturn(PackageManager.PERMISSION_GRANTED)
            whenever(context.checkPermission(eq(denied), any(), any()))
                .thenReturn(PackageManager.PERMISSION_DENIED)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val statuses = viewModel.uiState.value.permissionDisplayStatuses
            assertThat(statuses.keys).containsExactly(granted, denied)
            // A row seeded as granted when it is not sends the user to a settings screen they do
            // not need, and the reverse hides the prompt they do need.
            assertThat(statuses.getValue(granted).isGranted).isTrue()
            assertThat(statuses.getValue(denied).isGranted).isFalse()
            // Rationale is only knowable from an Activity, so the seed must not claim otherwise.
            assertThat(statuses.values.map { it.shouldShowRationale }).containsExactly(false, false)
        }
}
