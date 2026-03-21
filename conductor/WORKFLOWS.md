# Workflows: Comiqueta

## Build
```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK
./gradlew test                   # All unit tests
./gradlew :feature-viewer:test   # Module tests
./gradlew :feature-viewer:test --tests "...ViewerViewModelTest"  # Single class
./gradlew detekt                 # Static analysis
```

## Distribution
```bash
powershell -File ./appDistributionUploadDebug.ps1    # Firebase debug
powershell -File ./appDistributionUploadRelease.ps1  # Firebase release
```
