package dev.diegoflassa.comiqueta.ui.home

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags
import dev.diegoflassa.comiqueta.ui.enums.BottomNavItems
import dev.diegoflassa.comiqueta.ui.enums.ViewMode

data class HomeUIState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedCategory: CategoryEntity? = null,
    val viewMode: ViewMode = ViewMode.GRID,
    val flags: Set<ComicFlags> = emptySet(),
    val categories: List<CategoryEntity> = emptyList(),
    val generalStoragePermissionGranted: Boolean = false, // Renamed from isLegacyPermissionGranted
    val currentBottomNavItem: BottomNavItems = BottomNavItems.HOME,
    val isScanningFolders: Boolean = false // Added
)

