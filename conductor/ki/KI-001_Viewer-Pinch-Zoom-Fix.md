# KI-001: Viewer Pinch-to-Zoom Fix
[CLAUDE.md](../../CLAUDE.md)

**File:** `feature-viewer/.../viewer/ui/ViewerScreen.kt`

**Problem:** After unpinch back to 1.0x, residual finger movement triggered page drag → unintended page turn + navigation lock.

**Root cause:** `globalIsPinchZoomActive` was derived from scale alone (`> 1.01f`), so it turned `false` while fingers were still on screen.

**Fix:** Track multi-touch contact lifecycle independently from scale.

```kotlin
// State
var isPinchGestureInProgress by remember { mutableStateOf(false) }
val globalIsPinchZoomActive = globalZoomScale > 1.01f || isPinchGestureInProgress

// In awaitPointerEventScope
val pressedCount = changes.count { it.pressed }
if (pressedCount >= 2 && !localPinchActive) { localPinchActive = true; isPinchGestureInProgress = true }
if (localPinchActive && pressedCount == 0) { localPinchActive = false; isPinchGestureInProgress = false; changes.forEach { it.consume() }; continue }
val zoomFactor = if (pressedCount >= 2) event.calculateZoom() else 1f
val panDelta  = if (pressedCount >= 2) event.calculatePan()  else Offset.Zero
if (localPinchActive || itemScale > 1.01f) { changes.forEach { it.consume() } }
```

**Last verified:** 2026-06-23
