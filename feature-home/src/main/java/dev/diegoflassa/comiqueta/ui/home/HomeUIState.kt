package dev.diegoflassa.comiqueta.ui.home

import dev.diegoflassa.comiqueta.core.domain.model.Category
import dev.diegoflassa.comiqueta.core.data.enums.ComicFlags
import dev.diegoflassa.comiqueta.ui.enums.BottomNavItems
import dev.diegoflassa.comiqueta.ui.enums.ViewMode

data class HomeUIState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedCategory: Category? = null,
    val viewMode: ViewMode = ViewMode.GRID,
    val flags: Set<ComicFlags> = emptySet(),
    val categories: ImmutableList<Category> = ImmutableList(emptyList()),
    val generalStoragePermissionGranted: Boolean = false, // Renamed from isLegacyPermissionGranted
    val currentBottomNavItem: BottomNavItems = BottomNavItems.HOME,
    val isScanningFolders: Boolean = false,
    val scanProgress: Int = 0,
    val currentComicName: String? = null,
    val isScanProgressMinimized: Boolean = false,
    val processedComicsCount: Int = 0,
    val scanTotalFiles: Int = 0,
    val scanProcessedFiles: Int = 0,
    val scanFinished: Boolean = false,
    val scanResultMessage: String? = null
)

@androidx.compose.runtime.Immutable
data class ImmutableList<T>(val items: List<T> = emptyList()) : List<T> by items


