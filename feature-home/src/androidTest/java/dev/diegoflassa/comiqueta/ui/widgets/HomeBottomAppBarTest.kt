package dev.diegoflassa.comiqueta.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeBottomAppBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bodyAtReferenceWidth_is56DpHigh() {
        composeRule.setContent {
            HomeBottomAppBarUnderTest(
                width = 360.dp,
                windowSize = IntSize(width = 360, height = 640)
            )
        }

        composeRule.onNodeWithTag(HomeBottomAppBarTestTags.BODY).assertHeightIsEqualTo(56.dp)
    }

    @Test
    fun equalWidthsWithDifferentWindowHeights_keepTheSame56DpBodyHeight() {
        composeRule.setContent {
            HomeBottomAppBarUnderTest(
                width = 360.dp,
                windowSize = IntSize(width = 360, height = 640)
            )
            HomeBottomAppBarUnderTest(
                width = 360.dp,
                windowSize = IntSize(width = 360, height = 1280)
            )
        }

        val bodies = composeRule.onAllNodesWithTag(HomeBottomAppBarTestTags.BODY)
        bodies[0].assertHeightIsEqualTo(56.dp)
        bodies[1].assertHeightIsEqualTo(56.dp)
    }

    @Test
    fun bodyHeight_scalesWithWidthOnly() {
        val cases = listOf(
            320f to 56f * 320f / 360f,
            360f to 56f,
            411f to 56f * 411f / 360f,
            600f to 56f * 600f / 360f,
            800f to 56f * 800f / 360f,
        )
        composeRule.setContent {
            Column {
                cases.forEach { (width, _) ->
                    HomeBottomAppBarUnderTest(
                        width = width.dp,
                        windowSize = IntSize(width = width.toInt(), height = 640)
                    )
                }
            }
        }
        val bodies = composeRule.onAllNodesWithTag(HomeBottomAppBarTestTags.BODY)
        cases.forEachIndexed { index, (_, expectedHeight) ->
            bodies[index].assertHeightIsEqualTo(expectedHeight.dp)
        }
    }

    @Test
    fun densityChanges_preserveDpGeometry() {
        val densities = listOf(1f, 1.5f, 2.625f, 3f)
        composeRule.setContent {
            Column {
                densities.forEach { density ->
                    HomeBottomAppBarUnderTest(
                        width = 360.dp,
                        windowSize = IntSize(width = 360, height = 640),
                        density = density
                    )
                }
            }
        }
        val bodies = composeRule.onAllNodesWithTag(HomeBottomAppBarTestTags.BODY)
        val actions = composeRule.onAllNodesWithTag(HomeBottomAppBarTestTags.ACTION)
        densities.indices.forEach { index ->
            bodies[index].assertHeightIsEqualTo(56.dp)
            actions[index].assertWidthIsEqualTo(52.dp)
            actions[index].assertHeightIsEqualTo(52.dp)
        }
    }

    @Test
    fun action_isCircularAtReferenceWidth() {
        composeRule.setContent {
            HomeBottomAppBarUnderTest(
                width = 360.dp,
                windowSize = IntSize(width = 360, height = 640)
            )
        }

        composeRule.onNodeWithTag(HomeBottomAppBarTestTags.ACTION).assertWidthIsEqualTo(52.dp)
        composeRule.onNodeWithTag(HomeBottomAppBarTestTags.ACTION).assertHeightIsEqualTo(52.dp)
    }
}

@Composable
private fun HomeBottomAppBarUnderTest(
    width: Dp,
    windowSize: IntSize,
    density: Float = 1f
) {
    val windowInfo = object : WindowInfo {
        override val isWindowFocused = true
        override val containerSize = windowSize
    }

    CompositionLocalProvider(
        LocalDensity provides Density(density = density),
        LocalWindowInfo provides windowInfo
    ) {
        ComiquetaThemeContent {
            Layout(
                content = { HomeBottomAppBar(onIntent = {}) },
                modifier = Modifier.width(width)
            ) { measurables, constraints ->
                val placeable = measurables.single().measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
        }
    }
}
