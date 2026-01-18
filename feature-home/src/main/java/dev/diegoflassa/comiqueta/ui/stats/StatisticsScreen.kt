package dev.diegoflassa.comiqueta.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImportContacts
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.diegoflassa.comiqueta.core.domain.model.CollectionStats
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.core.ui.hiltActivityViewModel
import dev.diegoflassa.comiqueta.home.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    navigationViewModel: NavigationViewModel = hiltActivityViewModel(),
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigationViewModel.goBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(dev.diegoflassa.comiqueta.core.R.string.back_button_desc)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is StatisticsUIState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is StatisticsUIState.Success -> {
                    StatisticsContent(stats = state.stats)
                }
                is StatisticsUIState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsContent(stats: CollectionStats) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(ComiquetaTheme.dimen.paddingMedium.scaled()),
        verticalArrangement = Arrangement.spacedBy(ComiquetaTheme.dimen.paddingMedium.scaled())
    ) {
        item {
            StatSection(title = stringResource(R.string.stats_summary_title)) {
                StatRow(
                    label = stringResource(R.string.stats_total_comics),
                    value = stats.totalComics.toString(),
                    icon = Icons.Default.LibraryBooks
                )
                StatRow(
                    label = stringResource(R.string.stats_read_comics),
                    value = stats.readComics.toString(),
                    icon = Icons.Default.ImportContacts
                )
                StatRow(
                    label = stringResource(R.string.stats_in_progress_comics),
                    value = stats.inProgressComics.toString(),
                    icon = Icons.Default.MenuBook
                )
                StatRow(
                    label = stringResource(R.string.stats_unread_comics),
                    value = stats.unreadComics.toString(),
                    icon = Icons.Default.LibraryBooks
                )
                StatRow(
                    label = stringResource(R.string.stats_favorite_comics),
                    value = stats.favoriteComics.toString(),
                    icon = Icons.Default.Favorite
                )
                StatRow(
                    label = stringResource(R.string.stats_total_categories),
                    value = stats.totalCategories.toString(),
                    icon = Icons.Default.Category
                )
            }
        }

        item {
            StatSection(title = stringResource(R.string.stats_formats_title)) {
                StatRow(label = "CBZ", value = stats.cbzCount.toString())
                StatRow(label = "CBR", value = stats.cbrCount.toString())
                StatRow(label = "PDF", value = stats.pdfCount.toString())
            }
        }

        item {
            StatSection(title = stringResource(R.string.stats_last_scan_title)) {
                StatRow(
                    label = stringResource(R.string.stats_last_scan_files_seen),
                    value = stats.lastScanTotalFiles.toString(),
                    icon = Icons.Default.History
                )
                StatRow(
                    label = stringResource(R.string.stats_last_scan_processed),
                    value = stats.lastScanProcessedComics.toString(),
                    icon = Icons.Default.History
                )
            }
        }

        if (stats.topAuthors.isNotEmpty()) {
            item {
                StatSection(title = stringResource(R.string.stats_top_authors_title)) {
                    stats.topAuthors.forEach { author ->
                        StatRow(
                            label = author.name,
                            value = author.count.toString(),
                            icon = Icons.Default.Person
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = ComiquetaTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(ComiquetaTheme.dimen.paddingMedium.scaled())) {
            Text(
                text = title,
                style = ComiquetaTheme.typography.typography.titleMedium.scaled(),
                fontWeight = FontWeight.Bold,
                color = ComiquetaTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.paddingSmall.scaled()))
            HorizontalDivider(color = ComiquetaTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(ComiquetaTheme.dimen.paddingSmall.scaled()))
            content()
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ComiquetaTheme.dimen.paddingExtraSmall.scaled()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp.scaled()),
                tint = ComiquetaTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(ComiquetaTheme.dimen.paddingMedium.scaled()))
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = ComiquetaTheme.typography.typography.bodyMedium.scaled(),
            color = ComiquetaTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = ComiquetaTheme.typography.typography.bodyLarge.scaled(),
            fontWeight = FontWeight.Medium,
            color = ComiquetaTheme.colorScheme.onSurfaceVariant
        )
    }
}
