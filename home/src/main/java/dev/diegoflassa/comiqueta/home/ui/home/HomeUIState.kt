package dev.diegoflassa.comiqueta.home.ui.home

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity

data class HomeUIState(
    val allComics: List<ComicEntity> = emptyList(),
    val latestComics: List<ComicEntity> = emptyList(),
    val favoriteComics: List<ComicEntity> = emptyList(),
    val unreadComics: List<ComicEntity> = emptyList(),
    val continueReadingComics: List<ComicEntity> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedCategory: CategoryEntity = CategoryEntity(1, "All"),
    val categories: List<CategoryEntity> = listOf(
        CategoryEntity(1, "All"),
        CategoryEntity(2, "Action"),
        CategoryEntity(3, "Horror"),
        CategoryEntity(4, "Fantasy")
    ),
    val showPermissionRationale: Boolean = false,
    val isFolderPermissionGranted: Boolean? = null,
    val viewMode: ViewMode = ViewMode.LIST,
    val error: String? = null
)