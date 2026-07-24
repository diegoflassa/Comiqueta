# Comiqueta

Comiqueta is a premium comics viewer application for Android, designed for a modern and immersive reading experience with support for multiple archive formats.

## ✨ Features

### 📖 Reading Experience
- **Progress Persistence**: Never lose your place. Comiqueta automatically remembers the last page you read for every comic.
- **Immersive Mode**: Read without distractions. System bars are automatically hidden to maximize screen real estate.
- **Thumbnail Navigation**: Quickly jump to any page using the integrated thumbnail navigation bar.
- **Manga Mode (RTL)**: Full support for Right-to-Left reading directions for manga fans.
- **Pinch-to-Zoom**: Smooth zooming and panning for detailed artwork with gesture-based navigation.
- **Fluid Page Turns**: Beautiful page-curl animations for a natural reading feel.

### 📂 Library Management
- **Smart Organization**: Easily track your progress with visual indicators on comic covers and list items.
- **Metadata Extraction**: Automatic support for `ComicInfo.xml` ensures your library has accurate titles and details.
- **Archive Support**: Read your favorite comics in **CBZ**, **CBR**, **CB7**, **CBT**, and **PDF** formats.
- **External Storage Access**: Full support for SAF (Storage Access Framework) to organize comics across device storage.

## 🛠️ Tech Stack

- **Kotlin** 2.3.20 — Modern, expressive language for Android development.
- **Jetpack Compose** (2026.03.00) — Declarative UI framework with Material 3.
- **Kotlin Coroutines** 1.10.2 — Structured concurrency for smooth, non-blocking operations.
- **Hilt** 2.59.2 — Dependency injection framework for modular architecture.
- **Room** 2.8.4 — Local persistence with SQLite database and migrations.
- **Navigation Compose 3** 1.0.1 — Type-safe, modular navigation.
- **Firebase** (Crashlytics, Analytics, Performance, Remote Config) — Monitoring, analytics, and configuration management.
- **Timber** 5.0.1 — Logging abstraction layer.
- **Coil** 2.7.0 — Image loading and caching.
- **Apache Commons Compress** 1.28.0 — Archive extraction utilities.
- **JunRAR** 7.5.8 — RAR file format support.
- **PDF Renderer** — Built-in PDF rendering via PdfRenderer.

**Build Tools:**
- Gradle 9.1.0
- Java 21 toolchain
- KSP 2.3.3 for annotation processing
- Android Gradle Plugin 9.1.0

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug or later
- **Java 21** (required for toolchain)
- **Gradle 9.1.0** (included via wrapper)

### Build Instructions

1. **Clone and open the project:**
   ```bash
   git clone <repository-url>
   cd Comiqueta
   ```

2. **Sync Gradle:**
   Open the project in Android Studio and let Gradle sync automatically.

3. **Build Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   APK output: `app/build/outputs/apk/debug/app-debug.apk`

4. **Build Release APK:**
   ```bash
   ./gradlew assembleRelease
   ```
   _Note: Requires signing configuration in `local.properties` or via build arguments._

### Run on Device/Emulator

- **From Android Studio:** Run > Run 'app'
- **Via Gradle:**
  ```bash
  ./gradlew installDebug
  ```

## 🧪 Testing

### Run All Tests
```bash
./gradlew test
```

### Run Module Tests
```bash
./gradlew :feature-viewer:test
./gradlew :core:test
```

### Run Specific Test Class
```bash
./gradlew :feature-viewer:test --tests "...ViewerViewModelTest"
```

### Code Quality Checks

**Static Analysis (Detekt):**
```bash
./gradlew detekt
```

**Lint Analysis:**
```bash
./gradlew lint
```

**Code Coverage (Kover):**
```bash
./gradlew koverHtmlReport
```
Report output: `build/reports/kover/html/index.html`

## 📐 Architecture

Comiqueta follows **Clean Architecture** with **MVI (Model-View-Intent)** pattern per feature module:

**Module Structure:**
- **`:app`** — Entry point (`MainActivity`), navigation routing, application setup via Hilt.
- **`:core`** — Shared domain, data layer, DI container, navigation, theme, reusable UI components.
- **`:feature-viewer`** — Core reading experience: gestures, animations, page management.
- **`:feature-ads`** — Google AdMob integration for monetization.
- **`:build-logic`** — Gradle convention plugins for shared build configuration.

**MVI Contract (per feature):**
- `XxxUIState` — Immutable state data class
- `XxxIntent` — Sealed class for user actions
- `XxxEffect` — Sealed class for one-shot side effects via Channel
- `IXxxViewModel` — Interface (enables fakes for testing)
- `XxxViewModel : ViewModel(), IXxxViewModel` — Hilt-injected implementation

**Data Layer:**
- **Room Database** with migrations and schema versioning
- **SAF (Storage Access Framework)** for external storage access
- **Repository pattern** for data abstraction
- **Flow-based reactive updates**

For detailed architecture documentation, see [architecture.md](conductor/rules/architecture.md).

## 📝 Development Guidelines

See [CORE_RULES.md](conductor/rules/CORE_RULES.md) for coding standards:

- **UI:** Use `ComiquetaTheme` tokens only — no hardcoded colors or dimensions.
- **Strings:** All user-facing strings in `res/strings.xml` only. Locales: EN, PT, ES, DE.
- **Logging:** Use `TimberLogger.logX(CLASS, "[TAG] msg")` — never `Log.x`.
- **IO:** Always use `DocumentFile` + `runCatching` + `Dispatchers.IO` for external storage.
- **Domain Layer:** Pure Kotlin with zero Android dependencies.
- **Use Cases:** Always define `IXxxUseCase` interface alongside implementation.
- **Navigation:** Nav3 with `@Serializable` keys in `core/navigation/Screen.kt`.

## 📚 Documentation

- **[Architecture](conductor/rules/architecture.md)** — Module graph, MVI pattern, navigation, data layer, key files.
- **[Rules](conductor/rules/CORE_RULES.md)** — Coding standards, build configuration, quality gates.
- **[Workflows](conductor/workflows/INDEX.md)** — Build, test, and distribution commands.

### Knowledge Items
- [KI-01: Viewer Pinch-to-Zoom Fix](conductor/knowledge/KI-01-VIEWER-PINCH-ZOOM-FIX.md) — Multi-touch gesture lifecycle fix
- [KI-02: Viewer Pinch-to-Zoom NaN State Corruption Fix](conductor/knowledge/KI-02-VIEWER-PINCH-ZOOM-NAN-FIX.md) — IEEE 754 floating-point edge case handling
- [KI-03: Token Audit & Pruning](conductor/knowledge/KI-03-TOKEN-AUDIT-AND-PRUNING.md) — Context optimization strategies

## 🔧 Troubleshooting

**Build fails with "Java 21 not found":**
- Install Java 21 or update `JAVA_HOME` environment variable
- Android Studio: File > Project Structure > SDK Location > Update JDK location

**APK crashes on startup:**
- Check logcat: `./gradlew logcat` or via Android Studio
- Ensure Firebase configuration (google-services.json) is present in `:app` module

**Navigation not working:**
- Verify `@Serializable` annotations on navigation keys in `core/navigation/Screen.kt`
- Check `NavDisplay.kt` for missing route mappings

## 📦 Distribution

### Firebase App Distribution (Debug)
```bash
powershell -File ./appDistributionUploadDebug.ps1
```

### Firebase App Distribution (Release)
```bash
powershell -File ./appDistributionUploadRelease.ps1
```

## 📄 License

This project is proprietary software. All rights reserved.

## 👨‍💻 Contributing

Follow the coding standards in [CORE_RULES.md](conductor/rules/CORE_RULES.md) and ensure all tests pass before submitting changes.
