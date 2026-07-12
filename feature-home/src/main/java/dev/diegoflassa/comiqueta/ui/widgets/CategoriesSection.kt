package dev.diegoflassa.comiqueta.ui.widgets

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.diegoflassa.comiqueta.core.R
import dev.diegoflassa.comiqueta.core.domain.model.Category
import dev.diegoflassa.comiqueta.core.data.preferences.UserPreferencesKeys
import dev.diegoflassa.comiqueta.ui.home.ImmutableList
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.core.theme.tabSelectedText
import dev.diegoflassa.comiqueta.core.theme.tabUnselectedText
import dev.diegoflassa.comiqueta.core.ui.extensions.scaled
import kotlin.collections.indexOf

private const val tag = "CategoriesSection"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesSection(
    categories: ImmutableList<Category>,
    selectedCategory: Category?,
    onCategoryClicked: (Category) -> Unit
) {
    if (categories.isEmpty()) {
        return
    }

    val selectedTabIndex = remember(categories, selectedCategory) {
        categories.indexOf(selectedCategory).let {
            if (it == -1) 0 else it
        }
    }

    SecondaryTabRow(
        modifier = Modifier.padding(horizontal = ComiquetaTheme.dimen.tabHorizontalPadding),
        containerColor = ComiquetaTheme.colorScheme.background,
        selectedTabIndex = selectedTabIndex,
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = true),
                color = ComiquetaTheme.colorScheme.tabSelectedText,
                height = 2.dp.scaled()
            )
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
                            maxLines = 1
                        )
                    }
                })
        }

    }
}

// --- CategoriesSection Previews ---

val sampleCategoriesForPreview = listOf(
    Category(id = 0, name = "All long text to test", createdAt = 0L),
    Category(id = 1, name = "Action", createdAt = 0L),
    Category(id = 2, name = "Comedy", createdAt = 0L),
    Category(id = 3, name = "Sci-Fi", createdAt = 0L),
    Category(id = 4, name = "Fantasy", createdAt = 0L)
)

@Preview(name = "CategoriesSection · All Selected · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "CategoriesSection · All Selected · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun CategoriesSectionAllSelectedPreview() {
    ComiquetaThemeContent {
        Surface {
            CategoriesSection(
                categories = ImmutableList(sampleCategoriesForPreview),
                selectedCategory = sampleCategoriesForPreview.find { it.name == "All" },
                onCategoryClicked = {}
            )
        }
    }
}

@Preview(name = "CategoriesSection · All Selected · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoriesSectionAllSelectedDarkPreview() {
    ComiquetaThemeContent {
        Surface {
            CategoriesSection(
                categories = ImmutableList(sampleCategoriesForPreview),
                selectedCategory = sampleCategoriesForPreview.find { it.name == "All" },
                onCategoryClicked = {}
            )
        }
    }
}

@Preview(name = "CategoriesSection · Comedy Selected · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "CategoriesSection · Comedy Selected · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun CategoriesSectionComedySelectedPreview() {
    ComiquetaThemeContent {
        Surface {
            CategoriesSection(
                categories = ImmutableList(sampleCategoriesForPreview),
                selectedCategory = sampleCategoriesForPreview.find { it.name == "Comedy" },
                onCategoryClicked = {}
            )
        }
    }
}

@Preview(name = "CategoriesSection · Comedy Selected · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoriesSectionComedySelectedDarkPreview() {
    ComiquetaThemeContent {
        Surface {
            CategoriesSection(
                categories = ImmutableList(sampleCategoriesForPreview),
                selectedCategory = sampleCategoriesForPreview.find { it.name == "Comedy" },
                onCategoryClicked = {}
            )
        }
    }
}

@Preview(name = "CategoriesSection · No Selection (Defaults to First) · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "CategoriesSection · No Selection (Defaults to First) · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun CategoriesSectionNoSelectionPreview() {
    ComiquetaThemeContent {
        Surface {
            CategoriesSection(
                categories = ImmutableList(sampleCategoriesForPreview),
                selectedCategory = null,
                onCategoryClicked = {}
            )
        }
    }
}

@Preview(name = "CategoriesSection · No Selection (Defaults to First) · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoriesSectionNoSelectionDarkPreview() {
    ComiquetaThemeContent {
        Surface {
            CategoriesSection(
                categories = ImmutableList(sampleCategoriesForPreview),
                selectedCategory = null,
                onCategoryClicked = {}
            )
        }
    }
}

@Preview(name = "CategoriesSection · Empty List · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "CategoriesSection · Empty List · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun CategoriesSectionEmptyPreview() {
    ComiquetaThemeContent {
        Surface {
            CategoriesSection(
                categories = ImmutableList(emptyList()),
                selectedCategory = null,
                onCategoryClicked = {}
            )
        }
    }
}

@Preview(name = "CategoriesSection · Empty List · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoriesSectionEmptyDarkPreview() {
    ComiquetaThemeContent {
        Surface {
            CategoriesSection(
                categories = ImmutableList(emptyList()),
                selectedCategory = null,
                onCategoryClicked = {}
            )
        }
    }
}
