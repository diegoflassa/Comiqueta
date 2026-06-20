# KI-002: Viewer Pinch-to-Zoom NaN State Corruption Fix

[CLAUDE.md](../../CLAUDE.md)

**Files:**
- `feature-viewer/src/main/java/dev/diegoflassa/comiqueta/viewer/ui/ViewerScreen.kt`
- `feature-viewer/src/main/java/dev/diegoflassa/comiqueta/viewer/ui/ViewerViewModel.kt`
- `feature-viewer/src/main/java/dev/diegoflassa/comiqueta/viewer/ui/anim/pageFlip/` (all gesture files)

**Problem:** After pinch-to-zoom gesture and returning to normal zoom level, the displayed page changes to the wrong page, and navigation (swipe left/right, tap navigation) becomes completely broken. The UI showed NaN offsets and zoom scale stuck above 1.0x.

**Root Cause:** IEEE 754 floating-point arithmetic vulnerability in centroid-based offset calculation.

When the user lifts fingers during a pinch-out gesture (not simultaneously), the `pressedCount` variable drops below 2 before reaching zero. On these intermediate frames:
1. `calculateCentroid(useCurrent = true)` returns `Offset.Unspecified` (NaN coordinates) because fewer than 2 fingers are pressed
2. The offset pivot adjustment formula executes: `itemOffsetX - (centroid.x - itemOffsetX) * (itemScale / oldLocalItemScale - 1)`
3. With `centroid.x = NaN` and scale delta ≈ 0, this evaluates as: `itemOffsetX - NaN * 0`
4. In IEEE 754, `NaN * 0 = NaN`, so the result becomes `itemOffsetX - NaN = NaN`
5. All subsequent offset calculations propagate the NaN value, poisoning the zoom state permanently

The NaN state remains even after the gesture completes because:
- Scale becomes stuck at ~1.2x (never reaches 1.0f due to NaN math)
- Condition `globalZoomScale > 1.01f` remains true, keeping `globalIsPinchZoomActive = true`
- This disables all page navigation gestures (drag and tap)
- No recovery mechanism existed to escape this corrupted state

**Fix:** Three-part mitigation strategy.

### Part 1: Centroid Guard (lines 484-505 in ViewerScreen.kt)
Only calculate centroid and apply pivot adjustments when at least 2 fingers are actively pressed. Add NaN sanity check before any offset math:

```kotlin
if (pressedCount >= 2) {
    val centroid = event.calculateCentroid(useCurrent = true)
    if (!centroid.x.isNaN() && !centroid.y.isNaN()) {
        val scaleDelta = itemScale / oldLocalItemScale - 1
        itemOffsetX -= (centroid.x - itemOffsetX) * scaleDelta
        itemOffsetY -= (centroid.y - itemOffsetY) * scaleDelta
    }
}
```

This prevents the centroid calculation from ever being used when `pressedCount < 2`, eliminating the source of NaN values.

### Part 2: Pinch End NaN Recovery (lines 410-433 in ViewerScreen.kt)
When pinch gesture ends (`pressedCount == 0`), perform explicit NaN state cleanup in addition to scale threshold snapping:

```kotlin
if (localPinchActive && pressedCount == 0) {
    localPinchActive = false
    isPinchGestureInProgress = false
    // Reset zoom if close to 1.0 OR if offsets are NaN (corrupted state)
    if (itemScale <= 1.05f || itemOffsetX.isNaN() || itemOffsetY.isNaN()) {
        itemScale = 1f
        itemOffsetX = 0f
        itemOffsetY = 0f
    }
    changes.forEach { it.consume() }
    continue
}
```

This serves as a safety net to recover from any NaN state that somehow propagates despite the centroid guard.

### Part 3: Increased Snap Threshold
Increased the zoom-to-1.0 snap threshold from `1.01f` to `1.05f`, allowing the state machine to recover from slightly zoomed states that might otherwise remain stuck.

### Additional Changes: UI Improvements & Log Cleanup
- Changed loading indicators from `CircularProgressIndicator()` to `CircularProgressIndicator(color = Color.White)` for better visibility during dark comic pages (lines 235, 615 in ViewerScreen.kt)
- Removed all `[PageNavFix]` debug logging statements from ViewerViewModel.kt and all pageFlip gesture files for production cleanliness

**Technical Explanation:**
This fix is rooted in understanding IEEE 754 floating-point semantics:
- `NaN` is contagious: any arithmetic operation with `NaN` produces `NaN` (except for some special cases like `NaN * 0`)
- However, `NaN * 0` specifically evaluates to `NaN`, not `0`
- This is the root of the poisoning: multiplying a non-zero offset by a near-zero scale delta still produces NaN when the offset is NaN

By guarding the centroid-dependent calculation and explicitly checking for NaN before execution, we eliminate the condition under which NaN can be introduced into the offset state.

**Impact:**
- Navigation is now reliably restored after pinch-to-zoom gestures complete
- Zoom state is properly reset when the gesture ends
- Recovery mechanism prevents permanent corruption even if edge cases occur
- Loading indicators now visible on dark pages

**Status:** Resolved — 2026-03-21

**Verification:**
The fix was validated through:
1. Log analysis showing NaN offsets appearing at the exact moment scale became stuck
2. Reproducing the bug scenario: pinch-out gesture followed by sequential finger release
3. Confirming navigation fully restored after applying the three-part fix
4. Removal of debug logs verified with grep to ensure no [PageNavFix] remains
