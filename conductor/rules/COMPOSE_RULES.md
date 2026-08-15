# Compose Rules

**Scope:** Compose code generation / modification. Load when touching any `@Composable` or screen file.

> Companion specs: previews → `PREVIEW_STANDARD.md`; test-friendliness → `INSTRUMENTED_TEST_STANDARD.md`.

---

## 1. Stability

- Mark every UI model passed as a `@Composable` parameter `@Immutable`. `data class`es with `val` only still need the annotation — the compiler does not infer it across modules.
- Use `@Stable` only when an interface/class has `var`s but contract guarantees stable snapshot equality.
- Lists / maps as parameters: use `kotlinx.collections.immutable` (`ImmutableList`) or wrap in an `@Immutable` holder. Never pass `List<T>` directly if you want skip.
- Lambdas in parameter position: ensure capture is stable. If a lambda captures a mutable holder, wrap in `remember(key) { ... }` or `rememberUpdatedState`.

## 2. State

- Primitive state APIs are mandatory: `mutableIntStateOf` / `mutableFloatStateOf` / `mutableLongStateOf` / `mutableDoubleStateOf`. Never `mutableStateOf(0)`.
- Hoist state. A `@Composable` that takes a `XxxUiState` and `(Event) -> Unit` callbacks is the correct shape. See `INSTRUMENTED_TEST_STANDARD.md` § Two-layer composition.
- `collectAsStateWithLifecycle()` always. Never `collectAsState()`.
- `rememberSaveable` **per field, never for a whole `XxxUIState` object** — and only for state the user would otherwise have to re-enter or re-find by hand. Save what the user paid for with their own time and what is still true after the process died: the current page index (as `PageCurlState` already does), a search query, the id of the comic or category being worked on, navigation arguments. **Never save** transient or externally-owned state — loading/scanning flags, dialog or bottom-sheet visibility, SAF-permission-granted or worker-running booleans, or an operation's result. Those describe what the dead process was *doing*, so restoring them shows a spinner nothing will dismiss, or a "scan in progress" flag whose real subject died with the process. Anything not explicitly saved must re-derive to its default. Recovering an interrupted scan is a persistence concern, not a saved-state one — see [`architecture.md` § Persistence & I/O](architecture.md).
- `derivedStateOf { ... }` for any computed value whose inputs change less often than the reads.
- **State derivation belongs in the ViewModel.** Mapping, filtering, sorting, grouping, formatting, and any `if` / `when` chain that turns domain data into what the screen renders is computed in the ViewModel and arrives as a ready field on `XxxUIState`. Wrapping it in `remember { }` fixes the recomposition cost but leaves it in the wrong layer — it is then reachable only through an instrumented test when it is plain unit-test material. `XxxScreen` reads its state and decides how to *look*, nothing more. (§12 already requires this for list data; it holds for every derived field.)

## 3. Recomposition

- Read state as late as possible: a parent that reads `uiState.title` to pass to a child forces the parent to recompose; pass `uiState` and let the child read.
- `LazyColumn` / `LazyRow` / `LazyVerticalGrid`: `items(items, key = { it.stableId })` mandatory.
- Side-effect launchers (`LaunchedEffect`, `DisposableEffect`) need stable, minimal keys. `Unit` only if the effect must outlive every recomposition.
- Never read `LocalConfiguration.current` inside a frequently recomposing scope — hoist to the screen root.
- Custom stateful Modifiers: use the `Modifier.Node` API (`androidx.compose.ui.node`). `Modifier.composed { ... }` only for legacy reasons, with a code comment justifying it.

## 4. Effects

| Need | API |
|---|---|
| Suspend coroutine tied to composition | `LaunchedEffect(key)` |
| Cleanup on leave / key-change | `DisposableEffect(key) { ... onDispose { ... } }` |
| Push state to a non-Compose system after composition | `SideEffect { ... }` |
| Produce a state from a non-Compose source | `produceState(initial, key) { ... }` |
| Call a fresh lambda inside a long-lived effect | `rememberUpdatedState(lambda)` |

- Never call a `suspend` function from a composition body — wrap in `LaunchedEffect`.
- Never start a coroutine in `rememberCoroutineScope()` unless triggered by a user gesture (click, gesture, focus).

## 5. Memory

- No object allocation in composition body. Wrap in `remember`. Includes: `Paint()`, `Brush.linearGradient(...)`, formatters, regexes, `BigDecimal`, `Color(0x...)` arithmetic.
- Never put a `Context` / `Activity` / `View` in `remember { ... }`. Use `LocalContext.current.applicationContext` for app-scoped needs, `DisposableEffect` for activity-scoped.
- Listeners, sensors, broadcast receivers: registration + `onDispose { unregister }` inside `DisposableEffect`.
- Painters and image resources: `painterResource(R.drawable.x)` is already remembered by the lib — do not re-`remember`.

## 6. Logging inside Composables

- **No `Timber.d` inside a `@Composable` function body.** Recomposition runs the call N times per frame; logs become useless and spam logcat. Move debug logs to the `ViewModel` or to a `LaunchedEffect(...)`.
- `Timber.i` / `Timber.w` / `Timber.e` inside a composable are allowed only inside an effect block.

## 7. Strings, Colors, Dimens

- `stringResource(...)` for every user-facing text — `contentDescription` included.
- `MaterialTheme.colorScheme.*` — never hardcode `Color(0xFFxxxxxx)` inline in layouts.
- `dimensionResource(R.dimen.x)` only when the dimen is reused. Inline `12.dp` is fine for one-off layout.

## 8. Public Composable Signature

- `XxxScreen(viewModel: XxxViewModel = hiltViewModel(), ... callbacks ...)` — wiring only.
- `XxxScreenContent(uiState: XxxUiState, on...: () -> Unit, modifier: Modifier = Modifier)` — pure layout, no DI, no singletons. See `INSTRUMENTED_TEST_STANDARD.md` § Two-layer composition.
- `modifier: Modifier = Modifier` parameter is mandatory on every reusable composable and is **always the first optional parameter**, immediately after required ones.
- Modifier chain order: `size` → `padding` → `background` → `clip` → `border` → `clickable` / `pointerInput`.

## 9. Animations

- Prefer `animate*AsState` (`animateFloatAsState`, `animateColorAsState`, `animateDpAsState`) for single-value transitions.
- Use `Animatable` only when you need imperative control (cancel / snap / custom velocity).
- `updateTransition` for coordinated multi-value animations driven by a state machine.
- `AnimatedVisibility` for enter / exit; `Crossfade` for swapping leaf composables.
- Specify `animationSpec` explicitly for any animation longer than the default. Reuse specs as `private val` constants.
- Never animate inside a composition body without a state-driven trigger — animation `value` reads must be the last reads in the scope (recomposition pressure).

## 10. Gestures & Input

- `Modifier.clickable` / `combinedClickable` for taps. Provide `onClickLabel` for accessibility.
- `Modifier.pointerInput(key) { detectTapGestures(...) }` for custom gestures. Key on the dependency the handler captures.
- Focus: `FocusRequester` must be `remember`-ed. Request focus inside `LaunchedEffect(Unit)`.

## 11. Accessibility (Semantics)

- Every actionable `Icon` / `IconButton` has a non-null `contentDescription`.
- Decorative-only `Icon` / `Image` use `contentDescription = null`.
- Group related elements with `Modifier.semantics(mergeDescendants = true) { ... }`.
- Custom controls expose role: `Modifier.semantics { role = Role.Button }`.
- Test tags (`Modifier.testTag(...)`) are NOT a substitute for `contentDescription`. Both are needed; one for tests, one for users.
- Minimum touch target: 48dp × 48dp. Wrap small icons with `Modifier.minimumInteractiveComponentSize()` or explicit padding.
- **A clickable icon is an `IconButton`.** Never `Icon` + `Modifier.clickable` for an icon-only action: that makes the touch target the icon's own bounds — typically 24dp, half the minimum above — with no ripple and no `Role.Button` semantics, while `IconButton` gives all three for free. In the viewer this is worse than a missed tap: the tap-to-turn interaction claims the whole page area, so a tap that misses an undersized icon falls through and turns the page instead of doing what the user aimed at. The app is also read one-handed for long stretches, where thumb accuracy is at its worst. Applies to every icon-only affordance: close ×, back arrow, favourite toggle, overflow, clear-field. When the click really belongs to a larger row or card surface, put it on that surface and leave the icon decorative with `contentDescription = null` — never both.

## 12. Lists, Paging, Large Data

- `LazyColumn` / `LazyRow` / `LazyVerticalGrid` for any list > ~10 items.
- Stable keys mandatory: `items(items, key = { it.id })`. Without keys, scroll position and animations break on data change.
- `contentType` parameter for heterogeneous lists — lets Compose reuse view types.
- Pre-compute heavy mapping (sort / filter / group) in the ViewModel; the composable receives the ready list.
- Paging: use the official `androidx.paging.compose` `LazyPagingItems` API. Never roll a manual paginator inside a `@Composable`.

## 13. Navigation Hand-off

- Navigation is triggered via injected `(...) -> Unit` callbacks (per `INSTRUMENTED_TEST_STANDARD.md`). The composable never holds a `NavController`.
- One-shot events (snackbar, toast, navigate-once) flow via `Channel<Event>` collected in `XxxScreen` only — never in `XxxScreenContent`.
- `LaunchedEffect(uiState.someTrigger)` for navigation-on-state — set the trigger back to `null` / `Idle` after handling so it doesn't re-fire on config change.

## 14. Performance Verification

- Enable Compose Compiler Metrics (`-P kotlin.compiler.metricsReport=true`) on demand to inspect stability / skip counts.
- Profile recomposition with Android Studio's Layout Inspector → Recomposition Counts.
- Strong-Skipping (Compose 1.7+) is on by default — confirm `@Composable` functions with stable params skip when params don't change.
- Frame-time budget: aim for < 16ms; profile on the lowest-end target device.

## 15. Forbidden

- `viewModel()` / `hiltViewModel()` inside nested composables — only at screen entry.
- `GlobalScope` from a `@Composable`.
- `runBlocking` in composition.
- Mutating state during composition (setting a `MutableState` from a child without an event handler).
- `Box` for a layout that is really a `Column` / `Row` with alignment.
- A clickable `Box` / `Row` / `Column` hand-built to replace a Material 3 component that would have worked — see [CORE_RULES §9](CORE_RULES.md).
- `Icon` + `Modifier.clickable` for an icon-only action — use `IconButton` (§11).
- Nested `LazyColumn` inside `LazyColumn` — use a single lazy list with `item { }` + `items { }`.
- Hardcoded `Color(0x...)`, hardcoded user-facing strings, hardcoded `contentDescription`.

## 16. AI Output Style (when generating Compose for this repo)

- One composable per file. Shared/reusable composables go in the core UI module's `components/`; feature-local components stay in their feature module.
- No comments unless behaviour is non-obvious (see `ai_behavior.md` §6 Human-Voice Comments).
- Functions ≤ 80 lines. Split if larger.
- No deprecated APIs.

## 17. UI States & Theme Fidelity

**Theme fidelity is non-negotiable.**

- **No hard-coded colors.** Every color reference must resolve to a `MaterialTheme.colorScheme` role. Raw `Color(0x...)` arithmetic belongs inside `ComiquetaTheme` color builders, never in layout code.
- **No hard-coded strings.** Every user-visible string must be in `strings.xml`. Hard-coded strings in Compose are a detekt error.
- **Every screen needs all 4 states:** loading (shimmer skeleton), empty (illustration + CTA), error (inline banner + retry), and success (content). Missing states are incomplete screens. All 4 states must be reachable from `State` alone — no internal `remember` flags that hide a state from tests.
- **Touch targets ≥ 48×48dp.** Use `Modifier.minimumInteractiveComponentSize()` or explicit `sizeIn(minWidth=48.dp, minHeight=48.dp)`.
- **Transitions:** tab switch → `fadeThrough`; list→detail → `sharedAxisX`; FAB→form → `containerTransform`. No abrupt snaps.
- **Accessibility:** every interactive element needs `contentDescription` or `semantics {}`. Color alone must never be the sole differentiator.
- **Font scale:** test at 1.3× — primary content must not be truncated or overflow.
- **Previews:** full spec in `PREVIEW_STANDARD.md`. Every screen ships `@Preview`s for each of the 4 states using the same `State` instances tests use — if the preview compiles, the test setup compiles.

See `PREVIEW_STANDARD.md` for the `@Preview` device/theme contract and `INSTRUMENTED_TEST_STANDARD.md` for the stateless screen + Route split and `testTag` conventions.
