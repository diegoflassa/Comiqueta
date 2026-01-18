package dev.diegoflassa.comiqueta.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.comiqueta.core.domain.usecase.comic.IGetCollectionStatsUseCase
import dev.diegoflassa.comiqueta.core.domain.model.CollectionStats
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getCollectionStatsUseCase: IGetCollectionStatsUseCase
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<StatisticsUIState>(StatisticsUIState.Success(CollectionStats()))
    val uiState = _uiState.asStateFlow()

    init {
        loadStats()
    }

    @OptIn(FlowPreview::class)
    private fun loadStats() {
        TimberLogger.logD("StatisticsViewModel", "DEBUG: loadStats called")
        viewModelScope.launch {
            getCollectionStatsUseCase()
                .debounce(500L)
                .catch { e ->
                    FirebaseCrashlytics.getInstance().recordException(e)
                    TimberLogger.logE("StatisticsViewModel", "DEBUG: Error loading stats", e)
                    _uiState.update { StatisticsUIState.Error(e.message ?: "Unknown error") }
                }
                .collect { stats ->
                    TimberLogger.logD("StatisticsViewModel", "DEBUG: Stats received: $stats")
                    _uiState.update { StatisticsUIState.Success(stats) }
                }
        }
    }
}
