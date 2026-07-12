# Preview Standard

**Status:** Mandatory
**Scope:** Every Compose `@Preview` across the codebase. Load alongside `COMPOSE_RULES.md` when touching any screen or reusable composable.

> Companion specs: Compose rules → `COMPOSE_RULES.md`; test-friendliness → `INSTRUMENTED_TEST_STANDARD.md`.

---

## 1. Device Profiles (fixed — do not deviate)

Every state ships **two** previews, phone and tablet, with these exact configurations:

- **Phone:** `@Preview(name = "Xxx · State · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")` — Moto G100, 6.7", 1080×2520px @ 420dpi
- **Tablet:** `@Preview(name = "Xxx · State · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")` — Galaxy Tab A7, 10.4", 1200×2000px @ 240dpi

`Xxx` is the composable name; `State` is the preview's state (Idle/Loading/Empty/Error/Data/…). Both stack on **one** preview function per state — see §6 for why this is what drives grouping in Android Studio's Preview pane.

No generic device ids (`id:pixel_6`), no ad-hoc sizes.

> **Do NOT use `widthDp`/`heightDp` for these.** Those params are **dp**, not pixels — feeding raw pixel resolution (1080, 2520) into them renders a canvas ~2.6× too large. The `device = "spec:…px,dpi=…"` form takes physical pixels + density and lets Compose derive the correct dp canvas, so the preview matches the real device and keeps density-correct drawable/text scaling.

### 1.1 Light/Dark Theme Variant (optional axis)

Comiqueta ships a dark theme; components with meaningfully different dark-mode styling (custom colors/elevation, not just the automatic M3 palette swap) MAY add a **dark** variant alongside the mandatory phone+tablet pair, using `uiMode = Configuration.UI_MODE_NIGHT_YES` on the same device spec:

- **Phone (dark):** `@Preview(name = "Xxx · State · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)`

Light mode is the default (`uiMode` omitted) and is always mandatory per §1. Dark is opt-in per component, not per-state-mandatory — add it where the component has custom dark styling worth catching in review; skip it for components that only inherit `MaterialTheme.colorScheme`. When added, dark previews follow §6 grouping the same way: they either stack on the same function as their light phone/tablet pair (if the function covers the full state) or, if kept on a separate function, take the matching explicit `group`.

## 2. Theme Wrapper (MANDATORY)

Every `@Preview` MUST wrap its content in `ComiquetaTheme { ... }` — the real app theme. A preview without the wrapper renders against Compose's stock `MaterialTheme` (purple primary, Roboto) and misrepresents the component. This applies to every composable, including ones that read `MaterialTheme.colorScheme.*` directly.

## 3. Annotation Template

Every `@Preview` name MUST be prefixed `"Xxx · State · Device"` (append `· Dark` for the optional dark variant, §1.1). §6 explains why this drives Preview-pane grouping — no `group` parameter needed.

```kotlin
@Preview(name = "ComicCard · Default · Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "ComicCard · Default · Tablet", showBackground = true, locale = "en", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun ComicCardPreview() {
    ComiquetaTheme {
        ComicCard(state = previewStateComic)   // realistic fixture
    }
}

// Optional dark variant (§1.1) — separate function, only when dark styling is worth reviewing
@Preview(name = "ComicCard · Default · Phone · Dark", showBackground = true, locale = "en", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ComicCardDarkPreview() {
    ComiquetaTheme {
        ComicCard(state = previewStateComic)
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

## 6. Grouping (one function per state; `group` only as a fallback)

Android Studio's Preview pane auto-collapses every `@Preview` stacked on the **same function** into one section, headed by that function's name. This is sufficient by default — do **not** add the `group` parameter as a matter of course.

Default pattern, no `group` needed:
- **One preview function per state** (`ComicCardOwnedPreview`, `ComicCardDownloadingPreview`, `ComicCardLongTitlePreview`, …) — never combine two states in one function.
- Stack exactly the phone `@Preview` and the tablet `@Preview` (§1) on that function — that pair becomes the section's two rows.
- Name the function `XxxStatePreview` (PascalCase, no separators) so the pane header is scannable; the human-readable `"Xxx · State · Device"` string lives in each `@Preview`'s `name` (§1/§3), not in the function name.

**Only set `group = "Xxx · State"` when the phone/tablet pair for one state can't live on a single function** — e.g. a state needs a third device/variant beyond phone+tablet. In that case, every `@Preview` that belongs to the same state must set the same explicit `group` so they still cluster into one section despite living on different functions. If one function already covers the whole state, leave `group` unset — the automatic behavior already produces the correct clustering and an explicit group would just duplicate the header text.

## 7. Multiple States

Every component with more than one state ships a preview per state (idle/default, loading, empty, error, disabled). Previews double as test fixtures — they use the same `State` instances the instrumented tests use (see `INSTRUMENTED_TEST_STANDARD.md`).

## 8. Dialogs & Overlays

Render dialog/overlay previews over a scrim: wrap the content in a `Box(Modifier.fillMaxSize())` with a semi-transparent background so the preview matches the locked-screen presentation.

## 9. Reviewer Checklist

- [ ] Every `@Composable` has ≥ 1 `@Preview`.
- [ ] Phone + tablet preview per state, exact configs above (`device = "spec:…px,dpi=…"`, **never** `widthDp`/`heightDp`), including `showBackground = true`.
- [ ] `name = "Xxx · State · Device"` on every `@Preview` (§1/§3); one preview **function** per state with phone+tablet stacked on it, no `group` parameter unless one state spans multiple functions (§6).
- [ ] Dark variant (§1.1), if present, uses `uiMode = Configuration.UI_MODE_NIGHT_YES` on the same device spec and is named `"Xxx · State · Device · Dark"` — only added where dark styling is custom, not for every state by default.
- [ ] Content wrapped in `ComiquetaTheme { }`.
- [ ] Mock data realistic + localized (EN); short/long/very-long variants where text wraps.
- [ ] Multi-state components have a preview per state; idle is first.
- [ ] Previews live in a `// region Previews` block in the same file; no `*Previews.kt`.
- [ ] No generic device ids, no missing theme wrapper.
