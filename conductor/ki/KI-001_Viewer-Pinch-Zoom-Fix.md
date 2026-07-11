# KI-001: Viewer Pinch-to-Zoom & Pan
[CLAUDE.md](../../CLAUDE.md)

**File:** `feature-viewer/.../viewer/ui/ViewerScreen.kt`

## Gesture contract

The single-page viewer handles zoom and pan inside one `awaitPointerEventScope` loop on the current page. State: `itemScale` (1f–5f), `itemOffsetX/Y`, mirrored to the ViewModel via `UpdateZoom` so zoom survives page changes.

- **Pinch (2 fingers):** `calculateZoom()` scales about the touch centroid; `calculatePan()` moves the page.
- **Pan (1 finger, only while zoomed):** `calculatePan()` moves the page. The offset is applied only inside `if (itemScale > 1f)`, so an un-zoomed single-finger drag falls through to PageFlip as a page turn.
- **Border clamp:** offsets are coerced to `±maxTranslate`, where `maxTranslateX = (scaledImageWidth - containerWidth)/2` (same for Y). The image never pans past its own edges; when a dimension fits inside the container that axis stays centered.
- **Nav lock:** while `globalIsPinchZoomActive` (`globalZoomScale > 1.01f || isPinchGestureInProgress`), PageFlip drag/tap navigation is disabled and pointer changes are consumed, so panning never turns a page.

```kotlin
val pressedCount = changes.count { it.pressed }
val zoomFactor = if (pressedCount >= 2) event.calculateZoom() else 1f
val panDelta   = if (pressedCount >= 1) event.calculatePan()  else Offset.Zero
// ... applied only when itemScale > 1f, then coerceIn(-maxTranslate, +maxTranslate)
```

## Pinch lifecycle fix (regression guard)

**Problem:** After unpinch back to 1.0x, residual finger movement triggered page drag → unintended page turn + navigation lock.

**Root cause:** `globalIsPinchZoomActive` was derived from scale alone (`> 1.01f`), so it turned `false` while fingers were still on screen.

**Fix:** Track multi-touch contact lifecycle independently from scale.

```kotlin
var isPinchGestureInProgress by remember { mutableStateOf(false) }
val globalIsPinchZoomActive = globalZoomScale > 1.01f || isPinchGestureInProgress

val pressedCount = changes.count { it.pressed }
if (pressedCount >= 2 && !localPinchActive) { localPinchActive = true; isPinchGestureInProgress = true }
if (localPinchActive && pressedCount == 0) { localPinchActive = false; isPinchGestureInProgress = false; changes.forEach { it.consume() }; continue }
if (localPinchActive || itemScale > 1.01f) { changes.forEach { it.consume() } }
```

See [KI-002](KI-002_Viewer-Pinch-Zoom-NaN-Fix.md) for the NaN-offset corruption guard on the same handler.

**Last verified:** 2026-07-11
