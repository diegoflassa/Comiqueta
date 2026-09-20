# KI-06: Home Bottom Bar Proportional Geometry

**Scope:** Home bottom bar, circular add-folder action, and cutout — `feature-home` widgets and `core` theme shape
**Last verified:** 2026-09-20
**Reflects code:** `652303d`

## Problem

The Home bar must keep the 360 × 56 design proportions at every window width. Global `Dp.scaled()` uses window height and a 360 × 640 reference, so equal widths with different heights (or tablets) deform the bar, squeeze the cutout into a 90 dp slot, and place the plus button with a separate Scaffold FAB offset.

## Root Cause / Rules

1. Body width `W` is the bar's local max width in dp, taken from Compose constraints before horizontal safe-area padding. Split-screen `W` is the app window, not the display.
2. Scale `s = W / 360`. Body height is `56 * s`. Circle diameter `D = 52 * s`, center X is `W / 2`, center Y is `12 * s` from the body top, protrusion is `14 * s`. Occupied height is body + protrusion. `D` and center Y stay provisional until a vector or visual calibration replaces them; changing them updates tokens and tests together.
3. One uniform `s` applies to X and Y. Do not use window height, device-name branches, or global `Scaling.kt` / `Dp.scaled()` for this geometry.
4. The background path reuses the original 360-wide curve (cutout from 131.79 to 228.84, source body ~55.7), extends the flat bottom to 56, and scales uniformly. `ComiquetaShapes.bottomBarShape` is that path. The bar is full window width; the central assembly does not stretch to fill extra width.
5. `HomeBottomAppBar` owns the background and the circular action. There is no separate Scaffold FAB, `2.2f` offset, or preview-only FAB. Banner height and system insets are not part of `s`. The Scaffold `bottomBar` slot consumes the occupied height once; list tail spacers do not add another whole bar.
6. Navigation labels use bar-local `sp * s` once and still honor Android `fontScale`. Hardcoded `contentDescription`s are forbidden; destinations use the existing `bottom_nav_*` strings.
7. Add-folder is a lifecycle-bound guard (`HomeAddFolderGuard`): claim with compare-and-set, hold through picker/permission, release on select, cancel, or permission denial. Destination taps use a short widget bounce lock.

## Fix / Implementation

| File | Role |
|---|---|
| `feature-home/.../ui/widgets/HomeBottomBarGeometry.kt` | Scalar contract from `W` |
| `feature-home/.../ui/widgets/HomeBottomAppBar.kt` | Full-width bar + circular action + test tags |
| `core/.../theme/Shapes.kt` | Uniform 360 × 56 `bottomBarShape` |
| `feature-home/.../ui/home/HomeScreen.kt` | Route, launchers, effects |
| `feature-home/.../ui/home/HomeScreenContent.kt` | Runtime layout |
| `feature-home/.../ui/home/HomePreviewContent.kt` | Preview layout using the same bottom component |
| `feature-home/.../ui/widgets/ComicsContent.kt` | Runtime list/grid |
| `feature-home/.../ui/widgets/ComicsPreviewContent.kt` | Preview list/grid |
| `feature-home/.../ui/home/HomeAddFolderGuard.kt` | In-memory add-folder claim/release |

Expected body heights: 320 → 49.778; 360 → 56; 411 → 63.933; 600 → 93.333; 800 → 124.444.

## Validation

- [x] JVM `HomeBottomBarGeometryTest` — scalars at 320/360/411/600/800 (`:feature-home:testDebugUnitTest`)
- [x] Instrumented `HomeBottomAppBarTest` — all five geometry tests green on Moto G100 (`0075942121`, Android 12)
- [x] Instrumented `HomeScreenTest` — one integrated action, 56 dp body at 360 (same device)
- [x] `HomeViewModelTest` add-folder guard — second click ignored until cancel
- [ ] Pixel-perfect silhouette still needs original vector or visual approval; D/center Y remain raster-provisional
- [ ] Extremely narrow windows where five non-overlapping 48 dp targets cannot fit are unsupported until a design decision
