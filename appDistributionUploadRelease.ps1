# appDistributionUploadDebug.ps1

# This script first builds the debug version of the Android application
# and then uploads it to Firebase App Distribution.

Write-Host "Starting debug build and App Distribution upload..."

# Step 1: Build the debug version of the application
# Using 'gradlew' (Gradle Wrapper) ensures that Gradle is used from the project's wrapper,
# which is generally recommended for consistency across different environments.
Write-Host "Building debug APK..."
./gradlew assembleRelease

# Check if the build was successful
if ($LASTEXITCODE -ne 0) {
    Write-Error "Debug build failed. Aborting App Distribution upload."
    exit 1 # Exit with an error code
}

Write-Host "Debug build successful. Proceeding with App Distribution upload."

# Step 2: Upload the debug APK to Firebase App Distribution
./gradlew appDistributionUploadRelease

# Check if the upload was successful
if ($LASTEXITCODE -ne 0) {
    Write-Error "App Distribution upload for debug failed."
    exit 1 # Exit with an error code
}

Write-Host "Debug APK successfully uploaded to Firebase App Distribution."
