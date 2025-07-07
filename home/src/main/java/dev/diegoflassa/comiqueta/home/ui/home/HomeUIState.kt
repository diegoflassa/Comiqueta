package dev.diegoflassa.comiqueta.home.ui.home

import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity

data class HomeUIState(
    val allComics: List<ComicEntity> = emptyList(),
    val latestComics: List<ComicEntity> = emptyList(),
    val favoriteComics: List<ComicEntity> = emptyList(),
    val unreadComics: List<ComicEntity> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val categories: List<String> = listOf(
        "All",
        "Action",
        "Sci-Fi",
        "Fantasy",
        "Horror",
        "Adventure",
        "Mystery"
    ),
    val showPermissionRationale: Boolean = false,
    val isFolderPermissionGranted: Boolean? = null
)