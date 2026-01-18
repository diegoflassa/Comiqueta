package dev.diegoflassa.comiqueta.core.domain.model

data class CollectionStats(
    val totalComics: Int = 0,
    val readComics: Int = 0,
    val inProgressComics: Int = 0,
    val unreadComics: Int = 0,
    val favoriteComics: Int = 0,
    val totalCategories: Int = 0,
    val cbzCount: Int = 0,
    val cbrCount: Int = 0,
    val pdfCount: Int = 0,
    val lastScanTotalFiles: Int = 0,
    val lastScanProcessedComics: Int = 0,
    val topAuthors: List<AuthorStat> = emptyList()
)

data class AuthorStat(
    val name: String,
    val count: Int
)
