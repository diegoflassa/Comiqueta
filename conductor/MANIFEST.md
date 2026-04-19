# COMICUETA_MANIFEST

[ARCH]
graph: :app -> :feature-* -> :core | :feature-ads (sibling)
modules:
  :app: MainActivity | NavDisplay | Hilt
  :core: /domain (models, usecases) | /data (Room, repos, SAF) | /di | /navigation | /theme | /ui
  :feature-*: /ui (MVI: State | Intent | Effect) | /domain (usecases) | /di
  build-logic: convention plugins
mvi_contract: XxxUIState (data) | XxxIntent (sealed) | XxxEffect (sealed/Channel) | IXxxViewModel (interface)
nav: Nav3 | type-safe | @Serializable keys @ core/navigation/Screen.kt
data: Room (ComicDatabase, ComicsDao, CategoryDao) | SAF (DocumentFile) | Repos (IComicsRepository) | Formats (CBZ, CBR, CB7, CBT, PDF)

[RULES]
ui_strings: ComiquetaTheme | res/strings.xml | Locales: [EN, PT, ES, DE]
logging: TimberLogger.logX(CLASS, "[TAG] msg") | NO Log.x
io: DocumentFile + runCatching + Dispatchers.IO | NO java.io.File (external)
domain: Pure Kotlin | NO Android deps | Always IXxxUseCase
build: Java 21 | KSP | Convention plugins @ build-logic/ | detekt via ./gradlew detekt

[WORKFLOWS]
build:
  - assemble: ./gradlew assembleDebug | assembleRelease
  - test: ./gradlew test | ./gradlew :module:test
  - analysis: ./gradlew detekt
dist:
  - firebase_debug: ./appDistributionUploadDebug.ps1
  - firebase_release: ./appDistributionUploadRelease.ps1
