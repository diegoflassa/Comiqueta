package dev.diegoflassa.comiqueta.ui.stats

import dev.diegoflassa.comiqueta.core.domain.model.CollectionStats

sealed interface StatisticsUIState {
    data object Loading : StatisticsUIState
    data class Success(val stats: CollectionStats) : StatisticsUIState
    data class Error(val message: String) : StatisticsUIState
}
