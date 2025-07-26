package dev.diegoflassa.comiqueta.home.ui.home

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags
import dev.diegoflassa.comiqueta.home.ui.enums.ViewMode

data class HomeUIState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedCategory: CategoryEntity? = null,
    val viewMode: ViewMode = ViewMode.GRID,
    val flags: Set<ComicFlags> = emptySet(),
    val categories: List<CategoryEntity> = emptyList(),
    val isLegacyPermissionGranted: Boolean = false
)

