package dev.diegoflassa.comiqueta.viewer.ui

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.diegoflassa.comiqueta.core.theme.ComiquetaThemeContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the viewer's gesture contract on a real device:
 *
 * - two fingers zoom,
 * - one finger turns the page while at 1x,
 * - one finger pans instead of turning the page once zoomed in.
 *
 * The third case is the regression this suite exists for: page navigation used to swallow the
 * single-finger drag, so a zoomed page could only be panned with two fingers.
 */
@RunWith(AndroidJUnit4::class)
class ViewerZoomPanGestureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val recordedIntents = mutableListOf<ViewerIntent>()

    private fun setViewerContent() {
        recordedIntents.clear()
        composeTestRule.setContent {
            var state by remember {
                mutableStateOf(
                    ViewerUIState(
                        comicTitle = "Test",
                        comicPath = Uri.parse("file:///test/comic.cbz"),
                        pageCount = PAGE_COUNT,
                        currentPage = 0,
                        isUiVisible = false,
                        loadedPages = (0 until PAGE_COUNT).associateWith {
                            ImageBitmap(PAGE_WIDTH_PX, PAGE_HEIGHT_PX)
                        }
                    )
                )
            }
            ComiquetaThemeContent {
                ViewerScreenContent(
                    uiState = state,
                    onIntent = { intent ->
                        recordedIntents += intent
                        // Stand in for the ViewModel so the screen observes its own updates,
                        // which is what re-enables/disables page navigation in production.
                        state = when (intent) {
                            is ViewerIntent.GoToPage -> state.copy(currentPage = intent.pageNumber)
                            is ViewerIntent.UpdateZoom -> state.copy(
                                zoomScale = intent.scale,
                                zoomOffsetX = intent.offsetX,
                                zoomOffsetY = intent.offsetY
                            )

                            else -> state
                        }
                    }
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    /** Spreads two fingers horizontally about the centre of the page. */
    private fun pinchOut() {
        composeTestRule.onRoot().performTouchInput {
            var left = center + Offset(-PINCH_START_GAP_PX, 0f)
            var right = center + Offset(PINCH_START_GAP_PX, 0f)
            down(0, left)
            down(1, right)
            repeat(PINCH_STEPS) {
                left += Offset(-PINCH_STEP_PX, 0f)
                right += Offset(PINCH_STEP_PX, 0f)
                updatePointerTo(0, left)
                updatePointerTo(1, right)
                move()
            }
            up(0)
            up(1)
        }
        composeTestRule.waitForIdle()
    }

    /** Drags a single finger horizontally from the centre of the page. */
    private fun singleFingerDrag(totalDx: Float) {
        composeTestRule.onRoot().performTouchInput {
            var position = center
            down(0, position)
            repeat(DRAG_STEPS) {
                position += Offset(totalDx / DRAG_STEPS, 0f)
                updatePointerTo(0, position)
                move()
            }
            up(0)
        }
        composeTestRule.waitForIdle()
    }

    private fun zoomIntents() = recordedIntents.filterIsInstance<ViewerIntent.UpdateZoom>()

    private fun pageIntents() = recordedIntents.filterIsInstance<ViewerIntent.GoToPage>()

    @Test
    fun twoFingerPinchZoomsThePage() {
        setViewerContent()

        pinchOut()

        val scale = zoomIntents().lastOrNull()?.scale
        assertTrue("expected a zoom, got scale=$scale", scale != null && scale > MIN_ZOOMED_SCALE)
    }

    @Test
    fun singleFingerDragTurnsThePageWhenNotZoomed() {
        setViewerContent()

        singleFingerDrag(-PAGE_TURN_DRAG_PX)

        assertTrue(
            "a single-finger swipe at 1x must still turn the page, intents=$recordedIntents",
            pageIntents().any { it.pageNumber == 1 }
        )
    }

    @Test
    fun singleFingerDragPansInsteadOfTurningThePageWhenZoomed() {
        setViewerContent()
        pinchOut()
        assertTrue("setup failed: page is not zoomed", (zoomIntents().lastOrNull()?.scale ?: 1f) > MIN_ZOOMED_SCALE)

        recordedIntents.clear()
        singleFingerDrag(-PAGE_TURN_DRAG_PX)

        val afterPan = zoomIntents().lastOrNull()
        assertTrue("one finger produced no pan at all, intents=$recordedIntents", afterPan != null)
        assertTrue(
            "the page must stay zoomed while panning, scale=${afterPan?.scale}",
            (afterPan?.scale ?: 1f) > MIN_ZOOMED_SCALE
        )
        assertTrue(
            "dragging left must move the page left, offsetX=${afterPan?.offsetX}",
            (afterPan?.offsetX ?: 0f) < -MIN_PAN_PX
        )
        assertTrue(
            "a zoomed single-finger drag must not turn the page, intents=$recordedIntents",
            pageIntents().isEmpty()
        )
    }

    private companion object {
        const val PAGE_COUNT = 3

        // Portrait page, taller than any phone viewport once scaled, so panning has room on both axes.
        const val PAGE_WIDTH_PX = 1000
        const val PAGE_HEIGHT_PX = 1500

        const val PINCH_START_GAP_PX = 40f
        const val PINCH_STEP_PX = 25f
        const val PINCH_STEPS = 12

        const val DRAG_STEPS = 10
        const val PAGE_TURN_DRAG_PX = 400f

        const val MIN_ZOOMED_SCALE = 1.5f
        const val MIN_PAN_PX = 10f
    }
}
