# Preview Standard

**Status:** Mandatory
**Scope:** Every Compose `@Preview` across the codebase. Load alongside `COMPOSE_RULES.md` when touching any screen or reusable composable.

> Companion specs: Compose rules → `COMPOSE_RULES.md`; test-friendliness → `INSTRUMENTED_TEST_STANDARD.md`.

---

## 1. Device Profiles (fixed — do not deviate)

Every state ships **two** previews, phone and tablet, with these exact configurations:

- **Phone:** `@Preview(name = "Xxx · Phone", widthDp = 411, heightDp = 960, locale = "en")` — 1080×2520px @ 420dpi
- **Tablet:** `@Preview(name = "Xxx · Tablet", widthDp = 800, heightDp = 1333, locale = "en")` — 1200×2000px @ 240dpi

No generic device ids (`id:pixel_6`), no ad-hoc sizes.

## 2. Theme Wrapper (MANDATORY)

Every `@Preview` MUST wrap its content in `ComiquetaTheme { ... }` — the real app theme. A preview without the wrapper renders against Compose's stock `MaterialTheme` (purple primary, Roboto) and misrepresents the component. This applies to every composable, including ones that read `MaterialTheme.colorScheme.*` directly.

## 3. Annotation Template

```kotlin
@Preview(name = "Comic · Phone", widthDp = 411, heightDp = 960, locale = "en")
@Preview(name = "Comic · Tablet", widthDp = 800, heightDp = 1333, locale = "en")
@Composable
private fun ComicCardPreview() {
    ComiquetaTheme {
        ComicCard(state = previewStateComic)   // realistic fixture
    }
}
```

## 4. Placement

- Previews live in the screen/component file only, inside a `// region Previews … // endregion` block at the bottom. No separate `*Previews.kt`.
- State fixtures (`private val previewStateXxx`) are declared just above the preview functions, in the same region.
- **Default/idle state is the first preview.** Order: idle → loading → empty → error → success/content. Extra states follow.

## 5. Mock Data

- Realistic and representative, in the app's primary locale (EN; PT-BR / ES / DE also shipped). Never `"Item"`, `"Text"`, `"0.00"`.
- Test text-length variation: short, long (wrapping), very long (truncation / overflow).
- Examples: `"The Amazing Spider-Man #42"`, `"Saga, Vol. 1"`, `"Watchmen"`, `"42 pages · CBZ"`.

## 6. Multiple States

Every component with more than one state ships a preview per state (idle/default, loading, empty, error, disabled). Previews double as test fixtures — they use the same `State` instances the instrumented tests use (see `INSTRUMENTED_TEST_STANDARD.md`).

## 7. Dialogs & Overlays

Render dialog/overlay previews over a scrim: wrap the content in a `Box(Modifier.fillMaxSize())` with a semi-transparent background so the preview matches the locked-screen presentation.

## 8. Reviewer Checklist

- [ ] Every `@Composable` has ≥ 1 `@Preview`.
- [ ] Phone + tablet preview per state, exact configs above.
- [ ] Content wrapped in `ComiquetaTheme { }`.
- [ ] Mock data realistic + localized (EN); short/long/very-long variants where text wraps.
- [ ] Multi-state components have a preview per state; idle is first.
- [ ] Previews live in a `// region Previews` block in the same file; no `*Previews.kt`.
- [ ] No generic device ids, no missing theme wrapper.
