package dev.diegoflassa.comiqueta.ui.widgets

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.theme.headerSelectedIcon
import dev.diegoflassa.comiqueta.core.theme.headerUnselectedIcon
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import dev.diegoflassa.comiqueta.home.R
import dev.diegoflassa.comiqueta.ui.enums.ViewMode


@Composable
fun SectionHeader(
    modifier: Modifier = Modifier,
    title: String,
    isExpanded: Boolean,
    showGridListOption: Boolean = false,
    onHeaderClick: () -> Unit,
    currentViewMode: ViewMode,
    onViewTypeChange: ((ViewMode) -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onHeaderClick)
            .padding(
                start = 16.dp.scaled(),
                end = 8.dp.scaled(),
                top = 16.dp.scaled(),
                bottom = 8.dp.scaled()
            )
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            textAlign = TextAlign.Start,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )

        IconButton(
            onClick = onHeaderClick
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(if (isExpanded) R.string.home_section_collapse else R.string.home_section_expand)
            )
        }

        if (showGridListOption) {

            Spacer(modifier = Modifier.width(8.dp.scaled()))

            IconButton(onClick = {
                if (currentViewMode != ViewMode.LIST) {
                    onViewTypeChange?.invoke(ViewMode.LIST)
                }
            }) {
                Icon(
                    painter = painterResource(id = dev.diegoflassa.comiqueta.core.R.drawable.ic_list),
                    contentDescription = stringResource(R.string.view_as_list_description),
                    modifier = Modifier.size(21.dp),
                    tint = if (currentViewMode == ViewMode.LIST) MaterialTheme.colorScheme.headerSelectedIcon else MaterialTheme.colorScheme.headerUnselectedIcon
                )
            }

            Spacer(modifier = Modifier.width(8.dp.scaled()))

            IconButton(onClick = {
                if (currentViewMode != ViewMode.GRID) {
                    onViewTypeChange?.invoke(ViewMode.GRID)
                }
            }
            ) {
                Icon(
                    painter = painterResource(id = dev.diegoflassa.comiqueta.core.R.drawable.ic_grid),
                    contentDescription = stringResource(R.string.view_as_grid_description),
                    modifier = Modifier.size(21.dp),
                    tint = if (currentViewMode == ViewMode.GRID) ComiquetaTheme.colorScheme.headerSelectedIcon else ComiquetaTheme.colorScheme.headerUnselectedIcon
                )
            }
        }
    }
}

// --- SectionHeader Previews ---
@PreviewScreenSizes
@Preview(
    name = "SectionHeader - Expanded, Grid, View Options - Dark",
    group = "SectionHeader",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SectionHeaderExpandedGridViewOptionsPreview() {
    ComiquetaThemeContent {
        Surface {
            SectionHeader(
                title = "Latest Comics",
                isExpanded = true,
                onHeaderClick = {},
                showGridListOption = true,
                currentViewMode = ViewMode.GRID,
                onViewTypeChange = {}
            )
        }
    }
}

@PreviewScreenSizes
@Preview(
    name = "SectionHeader - Collapsed, List, View Options - Dark",
    group = "SectionHeader",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SectionHeaderCollapsedListViewOptionsPreview() {
    ComiquetaThemeContent {
        Surface {
            SectionHeader(
                title = "Favorite Comics",
                isExpanded = false,
                onHeaderClick = {},
                showGridListOption = true,
                currentViewMode = ViewMode.LIST,
                onViewTypeChange = {}
            )
        }
    }
}

@PreviewScreenSizes
@Preview(
    name = "SectionHeader - Expanded, No View Options - Dark",
    group = "SectionHeader",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SectionHeaderExpandedNoViewOptionsPreview() {
    ComiquetaThemeContent {
        Surface {
            SectionHeader(
                title = "All Comics",
                isExpanded = true,
                onHeaderClick = {},
                showGridListOption = false,
                currentViewMode = ViewMode.LIST,
                onViewTypeChange = {}
            )
        }
    }
}

@PreviewScreenSizes
@Preview(
    name = "SectionHeader - Collapsed, No View Options - Dark",
    group = "SectionHeader",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SectionHeaderCollapsedNoViewOptionsPreview() {
    ComiquetaThemeContent {
        Surface {
            SectionHeader(
                title = "Search Results",
                isExpanded = false,
                onHeaderClick = {},
                showGridListOption = false,
                currentViewMode = ViewMode.GRID,
                onViewTypeChange = {}
            )
        }
    }
}
