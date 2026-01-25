package dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.config.PageCurlConfig
import dev.diegoflassa.comiqueta.viewer.ui.anim.pageFlip.config.rememberPageCurlConfig

/**
 * A Composable that provides a page-flipping animation effect for a given set of content pages.
 * This version of PageFlip manages page state based on simple integer indices.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param count The total number of pages.
 * @param state The state object to control and observe the PageFlip's current page and animation state.
 *              Defaults to a remembered [PageCurlState].
 * @param config The configuration for the page curl animation, including gesture interactions and physics.
 *               Defaults to a remembered [PageCurlConfig].
 * @param content A lambda that provides the Composable content for a given page index.
 */
@Composable
fun PageFlip(
    modifier: Modifier = Modifier,
    count: Int,
    state: PageCurlState = rememberPageCurlState(),
    config: PageCurlConfig = rememberPageCurlConfig(),
    content: @Composable (index: Int) -> Unit
) {
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier) {
        state.setup(count, constraints)

        val updatedCurrent by rememberUpdatedState(state.current)
        val internalState by rememberUpdatedState(state.internalState ?: return@BoxWithConstraints)

        val updatedConfig by rememberUpdatedState(config)

        val dragGestureModifier = when (val interaction = updatedConfig.dragInteraction) {
            is PageCurlConfig.GestureDragInteraction ->
                Modifier
                    .dragGesture(
                        dragInteraction = interaction,
                        state = internalState,
                        enabledForward = updatedConfig.dragForwardEnabled && updatedCurrent < state.max - 1,
                        enabledBackward = updatedConfig.dragBackwardEnabled && updatedCurrent > 0,
                        dragThreshold = updatedConfig.dragThreshold,
                        scope = scope,
                        onChange = { state.current = updatedCurrent + it }
                    )

            is PageCurlConfig.StartEndDragInteraction ->
                Modifier
                    .dragStartEnd(
                        dragInteraction = interaction,
                        state = internalState,
                        enabledForward = updatedConfig.dragForwardEnabled && updatedCurrent < state.max - 1,
                        enabledBackward = updatedConfig.dragBackwardEnabled && updatedCurrent > 0,
                        dragThreshold = updatedConfig.dragThreshold,
                        scope = scope,
                        onChange = { state.current = updatedCurrent + it }
                    )
        }

        Box(
            Modifier
                .then(dragGestureModifier)
                .tapGesture(
                    config = updatedConfig,
                    scope = scope,
                    onTapForward = state::next,
                    onTapBackward = state::prev,
                )
        ) {
            // Wrap in key to synchronize state updates
            key(updatedCurrent, internalState.forward.value, internalState.backward.value) {
                if (updatedCurrent + 1 < state.max) {
                    content(updatedCurrent + 1)
                }

                if (updatedCurrent < state.max) {
                    val forward = internalState.forward.value
                    Box(Modifier.drawCurl(updatedConfig, forward.top, forward.bottom)) {
                        content(updatedCurrent)
                    }
                }

                if (updatedCurrent > 0) {
                    val backward = internalState.backward.value
                    Box(Modifier.drawCurl(updatedConfig, backward.top, backward.bottom)) {
                        content(updatedCurrent - 1)
                    }
                }
            }
        }
    }
}

/**
 * A Composable that provides a page-flipping animation effect for a given set of content pages.
 * This overload allows for page state synchronization based on a stable [key] generated for each page index.
 * This is useful when the underlying data set might change in ways that affect page indices, allowing the
 * PageFlip to attempt to maintain the currently viewed item.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param count The total number of pages.
 * @param key A lambda that returns a stable, unique key for a given page index. Used to synchronize
 *            the current page if the [count] changes or the underlying data items are reordered.
 * @param state The state object to control and observe the PageFlip's current page and animation state.
 *              Defaults to a remembered [PageCurlState].
 * @param config The configuration for the page curl animation, including gesture interactions and physics.
 *               Defaults to a remembered [PageCurlConfig].
 * @param content A lambda that provides the Composable content for a given page index.
 */
@Composable
fun PageFlip(
    modifier: Modifier = Modifier,
    count: Int,
    key: (Int) -> Any,
    state: PageCurlState = rememberPageCurlState(),
    config: PageCurlConfig = rememberPageCurlConfig(),
    content: @Composable (index: Int) -> Unit
) {
    var lastKey by remember(state.current) { mutableStateOf(if (count > 0) key(state.current) else null) }

    remember(count) {
        val newKey = if (count > 0) key(state.current) else null
        if (newKey != lastKey) {
            val index = List(count, key).indexOf(lastKey).coerceIn(0, count - 1)
            lastKey = newKey
            state.current = index
        }
        count
    }

    PageFlip(
        count = count,
        state = state,
        config = config,
        content = content,
        modifier = modifier,
    )
}
