package dev.diegoflassa.comiqueta.ui.widgets

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.diegoflassa.comiqueta.core.R
import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.data.preferences.UserPreferencesKeys
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.theme.tabSelectedText
import dev.diegoflassa.comiqueta.core.theme.tabUnselectedText
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import kotlin.collections.indexOf
import kotlin.collections.set

private const val tag = "CategoriesSection"

@Composable
fun CategoriesSection(
    categories: List<CategoryEntity>,
    selectedCategory: CategoryEntity?,
    onCategoryClicked: (CategoryEntity) -> Unit
) {
    if (categories.isEmpty()) {
        return
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val textWidths = remember { mutableStateMapOf<Int, Dp>() }
    val density = LocalDensity.current

    TabRow(
        modifier = Modifier.padding(horizontal = ComiquetaTheme.dimen.tabHorizontalPadding),
        containerColor = ComiquetaTheme.colorScheme.background,
        selectedTabIndex = selectedTabIndex,
        indicator = { tabPositions ->
            selectedTabIndex = categories.indexOf(selectedCategory).let {
                if (it == -1) 0 else it
            }

            if (tabPositions.isNotEmpty() && selectedTabIndex in tabPositions.indices) {
                val currentTabPosition = tabPositions[selectedTabIndex]
                val currentTextWidth = textWidths[selectedTabIndex]?.scaled() ?: 0.dp

                if (currentTextWidth > 0.dp) {
                    val mainIndicatorHeight = 2.dp.scaled()
                    val mainIndicatorColor = ComiquetaTheme.colorScheme.tabSelectedText

                    Box(
                        Modifier
                            .wrapContentSize(Alignment.BottomStart)
                            .padding(start = currentTabPosition.left + 16.dp)
                            .width(currentTextWidth)
                            .background(mainIndicatorColor)
                            .height(mainIndicatorHeight)
                    )
                }
            } else {
                Box(Modifier)
            }
        }) {
        categories.forEachIndexed { index, category ->
            val categoryText = if (category.name.equals(
                    UserPreferencesKeys.DEFAULT_CATEGORY_ALL, ignoreCase = true
                )
            ) {
                stringResource(id = R.string.all)
            } else {
                category.name
            }

            Tab(
                selected = selectedTabIndex == index,
                onClick = { onCategoryClicked(category) },
                text = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            modifier = Modifier.wrapContentWidth(),
                            text = categoryText,
                            style = ComiquetaTheme.typography.tabText,
                            color = if (selectedTabIndex == index)
                                ComiquetaTheme.colorScheme.tabSelectedText
                            else
                                ComiquetaTheme.colorScheme.tabUnselectedText,
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            onTextLayout = { textLayoutResult ->
                                val lastVisibleLine = textLayoutResult.lineCount - 1
                                val lastVisibleCharIndex = textLayoutResult.getLineEnd(lastVisibleLine, visibleEnd = true)

                                val lastCharRight = textLayoutResult.getHorizontalPosition(lastVisibleCharIndex - 1, usePrimaryDirection = true)

                                val visibleWidth = with(density) { lastCharRight.toDp() }
                                TimberLogger.logI(tag, "Visible width[$index]: $visibleWidth")
                                textWidths[index] = visibleWidth
                            }
                        )
                    }
                })
        }

    }
}

// --- CategoriesSection Previews ---

val sampleCategoriesForPreview = listOf(
    CategoryEntity(id = 0, name = "All"),
    CategoryEntity(id = 1, name = "Action"),
    CategoryEntity(id = 2, name = "Comedy"),
    CategoryEntity(id = 3, name = "Sci-Fi"),
    CategoryEntity(id = 4, name = "Fantasy")
)

@PreviewScreenSizes
@Preview(
    name = "CategoriesSection - All Selected - Dark",
    group = "CategoriesSection",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun CategoriesSectionAllSelectedPreview() {
    ComiquetaThemeContent {
        Surface {
            CategoriesSection(
                categories = sampleCategoriesForPreview,
                selectedCategory = sampleCategoriesForPreview.find { it.name == "All" },
                onCategoryClicked = {}
            )
        }
    }
}

@PreviewScreenSizes
@Preview(
    name = "CategoriesSection - Comedy Selected - Dark",
    group = "CategoriesSection",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun CategoriesSectionComedySelectedPreview() {
    ComiquetaThemeContent {
        Surface {
            CategoriesSection(
                categories = sampleCategoriesForPreview,
                selectedCategory = sampleCategoriesForPreview.find { it.name == "Comedy" },
                onCategoryClicked = {}
            )
        }
    }
}

@PreviewScreenSizes
@Preview(
    name = "CategoriesSection - No Selection (Defaults to First) - Dark",
    group = "CategoriesSection",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun CategoriesSectionNoSelectionPreview() {
    ComiquetaThemeContent {
        Surface {
            CategoriesSection(
                categories = sampleCategoriesForPreview,
                selectedCategory = null,
                onCategoryClicked = {}
            )
        }
    }
}

@PreviewScreenSizes
@Preview(
    name = "CategoriesSection - Empty List - Dark",
    group = "CategoriesSection",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun CategoriesSectionEmptyPreview() {
    ComiquetaThemeContent {
        Surface {
            CategoriesSection(
                categories = emptyList(),
                selectedCategory = null,
                onCategoryClicked = {}
            )
        }
    }
}
