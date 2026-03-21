# Rules: Comiqueta

## Coding
- UI: `ComiquetaTheme` tokens only. No hardcoded colors/dimensions.
- Strings: `res/strings.xml` only. Locales: EN, PT, ES, DE.
- Logging: `TimberLogger.logX(CLASS, "[TAG] msg")`. Never `Log.x`.
- IO: `DocumentFile` + `runCatching` + `Dispatchers.IO`. Never `java.io.File` for external storage.
- Domain: Pure Kotlin. Zero Android deps in `/domain`.
- UseCases: Always define `IXxxUseCase` interface alongside implementation.
- Workers: `@HiltWorker`.
- Nav: Nav3 type-safe. `@Serializable` keys in `core/navigation/Screen.kt`.

## Build
- Convention plugins in `build-logic/` — apply `android-library-convention` or `android-application-convention`.
- Java 21 toolchain, KSP for annotation processing.
- Static analysis: `./gradlew detekt`
