package dev.diegoflassa.comiqueta.ui.home

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import dev.diegoflassa.comiqueta.ui.widgets.HomeBottomAppBarTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun previewHost_exposesOneIntegratedActionAnd56DpBody() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f)) {
                ComiquetaThemeContent {
                    HomeScreenContentForPreview(
                        modifier = Modifier.width(360.dp),
                        comics = emptyList(),
                        latestComics = emptyList(),
                        favoriteComics = emptyList(),
                        uiState = HomeUIState(isLoading = false),
                        onIntent = {}
                    )
                }
            }
        }

        composeRule.onAllNodesWithTag(HomeBottomAppBarTestTags.ACTION).assertCountEquals(1)
        composeRule.onNodeWithTag(HomeBottomAppBarTestTags.BODY).assertHeightIsEqualTo(56.dp)
    }
}
