package dev.diegoflassa.comiqueta.home.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.PrimaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.data.preferences.UserPreferencesKeys
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
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
            if (tabPositions.isNotEmpty() && selectedTabIndex >= 0 && selectedTabIndex < tabPositions.size) {
                val currentTabPosition = tabPositions[selectedTabIndex]
                val currentTextWidth = textWidths[selectedTabIndex]?.scaled() ?: 0.dp
                TimberLogger.logI(tag, "Got size[$selectedTabIndex]: $currentTextWidth")

                if (currentTextWidth > 0.dp) {
                    val mainIndicatorHeight = 2.dp.scaled()
                    val mainIndicatorColor = ComiquetaTheme.colorScheme.tabSelectedText

                    PrimaryIndicator(
                        modifier = Modifier
                            .tabIndicatorOffset(currentTabPosition)
                            .width(currentTextWidth.scaled())
                            .wrapContentSize(Alignment.BottomStart)
                            .padding(start = 16.dp),
                        height = mainIndicatorHeight,
                        color = mainIndicatorColor
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
                stringResource(id = dev.diegoflassa.comiqueta.core.R.string.all)
            } else {
                category.name
            }

            Tab(
                selected = selectedTabIndex == index,
                onClick = { onCategoryClicked(category) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier
                                .wrapContentWidth()
                                .onSizeChanged { intSize ->
                                    TimberLogger.logI(
                                        tag, "Setted size[$index]: ${intSize.width}"
                                    )
                                    textWidths[index] = with(density) { intSize.width.toDp() }
                                }
                                .align(Alignment.CenterStart),
                            text = categoryText,
                            style = ComiquetaTheme.typography.tabText,
                            color = if (selectedTabIndex == index) ComiquetaTheme.colorScheme.tabSelectedText else ComiquetaTheme.colorScheme.tabUnselectedText,
                            textAlign = TextAlign.Start,
                            maxLines = 1)
                    }
                })
        }

    }
}
