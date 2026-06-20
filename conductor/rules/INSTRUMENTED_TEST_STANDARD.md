# Instrumented-Test Standard

**Status:** 📋 Mandatory
**Applies to:** Every `@Composable` `*Screen.kt` and every reusable widget in `:core:ui` / `feature/.../presentation/components/`
**Goal:** Any screen must be addressable, drivable, and observable from a Compose UI test (`createComposeRule()` / `createAndroidComposeRule<>()`) without modifying production code.

---

## 1. Two-Layer Composition (MANDATORY)

Every screen MUST be split in two composables:

| Composable | Responsibility | Test target |
|---|---|---|
| `XxxScreen(viewModel, onNavigate…)` | Wires `viewModel.uiState`, `navEvents`, lifecycle. **No layout.** | Robolectric / instrumented with fake VM |
| `XxxScreenContent(uiState, on…: () -> Unit, modifier: Modifier = Modifier)` | All layout & interactions. Stateless. | Pure Compose UI test — no Hilt, no VM |

Rules:
- `*ScreenContent` MUST be `private` or `internal` (never call from app code other than `XxxScreen` + `@Preview`).
- `*ScreenContent` MUST accept a `modifier: Modifier = Modifier` as the **last** parameter so tests can inject `Modifier.testTag(...)` from outside if needed.
- Every callback is a typed lambda (`(String) -> Unit`), never a `viewModel::method` reference inside `*ScreenContent`.
- **No** `hiltViewModel()`, `LocalContext.current`-based singleton lookups, or `remember { SomeManager() }` inside `*ScreenContent`.

---

## 2. Test Tags (MANDATORY)

Every interactive or assertable node MUST carry a stable `Modifier.testTag(...)`.

### 2.1 Tag constants

Each screen MUST declare a `XxxScreenTestTags` object next to the screen file (same package, same file is fine):

```kotlin
object LoginScreenTestTags {
    const val ROOT = "login_screen"
    const val USERNAME_FIELD = "login_username_field"
    const val PASSWORD_FIELD = "login_password_field"
    const val SUBMIT_BUTTON = "login_submit_button"
    const val ERROR_DIALOG = "login_error_dialog"
}
```

Rules:
- `snake_case`, prefixed by the screen's short name (`login_`, `cart_`).
- One `ROOT` tag per screen on the outermost `Box`/`Column`.
- Every actionable button, field, dialog, overlay, and any `Text`/`Image` an assertion needs MUST have a tag.
- Tags are constants — never inline string literals at the call site.

### 2.2 Shared widgets

`:core:ui` widgets MUST accept `modifier: Modifier = Modifier` and apply it to their root node so the screen can attach its own `testTag`. They MUST NOT hardcode internal tags.

---

## 3. Semantics & Accessibility (MANDATORY)

A test driver should be able to find a node by role + text **without** falling back to `onAllNodes`.

- Every `Icon`, `IconButton`, `Image` that is interactive or carries meaning MUST set a non-null `contentDescription` (user-facing copy).
- Decorative-only icons MAY use `contentDescription = null`, but the parent control MUST still expose its purpose via its own text/label.
- Modal dialogs and loading overlays MUST set `Modifier.semantics { contentDescription = "<purpose>" }` OR carry a tag on the root.
- Custom click handlers on raw `Box`/`Modifier.clickable` MUST set `Modifier.semantics { role = Role.Button; contentDescription = "..." }`.

---

## 4. State Inputs (MANDATORY)

`*ScreenContent` MUST be deterministic given its inputs. Forbidden inside `*ScreenContent`:

- `System.currentTimeMillis()`, `Date()`, `Instant.now()`, `Random.*` — pass values in via the UiState.
- `LocalContext.current.getString(...)` — strings must come from the UiState (preferred) or be inlined; if a `stringResource(...)` is needed, that's fine because tests can override resources, but anything depending on `Context` services is not.
- Reading from singletons — these belong in the ViewModel; their relevant fields enter via `UiState`.
- Side-effecting work (network, DB, WorkManager) — only in ViewModel/use case.

Local UI-only state (`var query by remember { mutableStateOf("") }`) is allowed but discouraged when it's part of the contract being tested. If a test needs to assert on it, hoist it into the UiState and expose a callback.

---

## 5. Navigation & One-Shot Events

- Navigation MUST be triggered via `(...) -> Unit` callbacks injected into `XxxScreen` (`onNavigateToCart`, `onSuccess`). Never call `NavController.navigate(...)` from inside the screen.
- One-shot events (`Channel<NavEvent>.receiveAsFlow()`) MUST be collected in `XxxScreen` only, not in `*ScreenContent`. This keeps `*ScreenContent` pure and replayable in a test.

---

## 6. Previews ↔ Tests

Every preview state MUST be reachable from `*ScreenContent` with a single `UiState` value. If a preview needs constructor gymnastics, the screen is not test-friendly — refactor.

Test files for screens live in:
`feature/<name>/src/androidTest/java/.../presentation/<Screen>InstrumentedTest.kt`

One test class per screen. At minimum, instrument: idle render, primary action invokes the callback with the right args, each error/loading state renders the expected node.

---

## 7. Forbidden Patterns

| ❌ Forbidden | ✅ Required |
|---|---|
| `XxxScreen(viewModel)` containing all layout | Split: `XxxScreen` (wiring) + `XxxScreenContent` (layout) |
| Inline `Modifier.testTag("login_button")` | `Modifier.testTag(LoginScreenTestTags.SUBMIT_BUTTON)` |
| `IconButton(onClick = ...) { Icon(..., contentDescription = null) }` for an actionable icon | Provide a meaningful `contentDescription` |
| `hiltViewModel()` inside `*ScreenContent` | Inject the ViewModel into `XxxScreen` only |
| `NavController` reference inside the screen | `onNavigateToX: () -> Unit` parameter |
| Local `private fun` widget inside the screen file | Extract to `presentation/components/` |
