import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import org.gradle.kotlin.dsl.project
import java.util.Properties

val firebaseAppDistributionProps = Properties()
// Try to read from project root first, then from app module directory as a fallback for CI
var firebasePropsFile = project.rootProject.file("firebase_app_distribution.properties")
if (!firebasePropsFile.exists() || !firebasePropsFile.isFile) {
    firebasePropsFile = project.file("firebase_app_distribution.properties") // Original path for local
}

if (firebasePropsFile.exists() && firebasePropsFile.isFile) {
    firebasePropsFile.inputStream().use {
        firebaseAppDistributionProps.load(it)
    }
} else {
    println("Warning: firebase_app_distribution.properties not found. App Distribution appId and testers might be missing for local builds.")
}

plugins {
    id("android-application-convention")
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.android.gradle.plugin)
    alias(libs.plugins.firebase.appdistribution.gradle)
}

// Configure Firebase App Distribution
firebaseAppDistribution {
    // Attempt to load appId and testers from properties file if it exists (for local convenience)
    if (firebasePropsFile.exists()) {
        appId = firebaseAppDistributionProps.getProperty("firebase.appdistribution.appId") ?: ""
        val configuredTesters = firebaseAppDistributionProps.getProperty("firebase.appdistribution.testers") ?: ""
        if (configuredTesters.isNotEmpty()) {
            testers = configuredTesters
            println("App Distribution: Using testers from firebase_app_distribution.properties: $configuredTesters")
        }
    } else {
        println("App Distribution: firebase_app_distribution.properties not found. appId and testers might need to be set via CI environment variables or plugin config.")
    }

    // This is crucial for CI: Read the service credentials file path from the environment variable
    // The environment variable FIREBASE_APP_DISTRO_SERVICE_CREDENTIALS_FILE is set in the GitHub Actions workflow
    val ciCredentialsFile = System.getenv("FIREBASE_APP_DISTRO_SERVICE_CREDENTIALS_FILE")
    if (ciCredentialsFile != null) {
        serviceCredentialsFile = ciCredentialsFile
        println("App Distribution: Using service credentials from CI environment variable: $ciCredentialsFile")
    } else {
        // Fallback for local builds if you have credentials at a fixed path locally and not using the env var
        // Example: val localCredentials = project.rootProject.file("path/to/local/service-account.json")
        // if (localCredentials.exists()) {
        //     serviceCredentialsFile = localCredentials.absolutePath
        //     println("App Distribution: Using local service credentials file: ${localCredentials.absolutePath}")
        // } else {
        println("App Distribution: CI environment variable FIREBASE_APP_DISTRO_SERVICE_CREDENTIALS_FILE not set, and no local fallback path configured for serviceCredentialsFile.")
        // }
    }

    // Default release notes, can be overridden per variant or by CI
    releaseNotes = "Debug test version from Gradle."

    // Example of per-variant configuration if needed later:
    // variantFilter {
    //     if (name.contains("debug", ignoreCase = true)) {
    //         // config for debug
    //         releaseNotes = "Debug build for testing."
    //     }
    //     if (name.contains("release", ignoreCase = true)) {
    //         // config for release
    //         releaseNotes = "New release version."
    //     }
    // }
}


kotlin {
    jvmToolchain(JavaVersion.VERSION_21.toString().toInt())
}

dependencies {
    //Modules
    implementation(project(":core"))
    implementation(project(":feature-home"))
    implementation(project(":feature-settings"))
    implementation(project(":feature-categories"))
    implementation(project(":feature-viewer"))

    // Common
    implementation(libs.ax.core.ktx)
    implementation(libs.com.google.android.material)

    //Common Testing
    testImplementation(libs.junit)
    testImplementation(libs.ax.test.ext.junit.ktx)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.ax.test.ext.junit.ktx)

    //Compose
    implementation(platform(libs.ax.compose.bom))
    implementation(libs.ax.compose.ui)
    implementation(libs.ax.compose.ui.graphics)
    implementation(libs.ax.compose.ui.tooling)
    implementation(libs.ax.compose.ui.tooling.preview)
    implementation(libs.ax.compose.ui.viewbinding)
    implementation(libs.ax.compose.runtime.livedata)
    implementation(libs.ax.compose.runtime.rxjava3)
    implementation(libs.ax.compose.material3)
    implementation(libs.ax.constraintlayout.compose)
    implementation(libs.ax.compose.material.icons.core)
    implementation(libs.ax.compose.material.icons.extended)
    implementation(libs.ax.activity.compose)
    implementation(libs.ax.lifecycle.viewmodel.compose)
    implementation(libs.ax.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    //Compose Testing
    androidTestImplementation(platform(libs.ax.compose.bom))
    androidTestImplementation(libs.ax.compose.ui.test)
    androidTestImplementation(libs.ax.compose.ui.test.junit4)
    androidTestImplementation(libs.org.mockito.android)
    debugImplementation(libs.ax.compose.ui.test.manifest)
    debugImplementation(libs.ax.compose.ui.tooling)

    //Compose Navigation 3
    implementation(libs.ax.navigation3.runtime)
    implementation(libs.ax.navigation3.ui)
    implementation(libs.ax.navigation3.viewmodel)
    //implementation(libs.ax.navigation3.adaptive)

    //Firebase
    implementation(platform(libs.com.google.firebase.bom))
    implementation(libs.com.google.firebase.crashlytics)
    implementation(libs.com.google.firebase.analytics)
    implementation(libs.com.google.firebase.perf)
    implementation(libs.com.google.firebase.config)
    implementation(libs.com.google.firebase.appcheck)
    implementation(libs.com.google.firebase.appcheck.playintegrity)

    //Timber
    implementation(libs.com.jakewharton.timber)

    //Dagger & Hilt
    implementation(libs.com.google.dagger.hilt.android)
    ksp(libs.com.google.dagger.hilt.android.compiler)
    implementation(libs.ax.hilt.common)
    ksp(libs.ax.hilt.compiler)
    implementation(libs.ax.hilt.navigation.compose)
    implementation(libs.ax.hilt.work)
    //Dagger & Hilt Testing
    testImplementation(libs.com.google.dagger.hilt.android.testing)
    kspTest(libs.com.google.dagger.hilt.android.compiler)
    androidTestImplementation(libs.com.google.dagger.hilt.android.testing)
    kspAndroidTest(libs.com.google.dagger.hilt.android.compiler)

    //OkHttp
    implementation(platform(libs.com.squareup.okhttp3.bom))
    implementation(libs.com.squareup.okhttp3)
    implementation(libs.com.squareup.okhttp3.logging.interceptor)

    //Moshi
    implementation(libs.com.squareup.moshi.kotlin)
    ksp(libs.com.squareup.moshi.kotlin.codegen)

    //Retrofit 2
    implementation(libs.com.squareup.retrofit2.retrofit)
    implementation(libs.com.squareup.retrofit2.adapter.rxjava3)
    implementation(libs.com.squareup.retrofit2.converter.moshi)
    implementation(libs.com.squareup.retrofit2.converter.gson)

    //Lifecycle
    implementation(libs.ax.lifecycle.runtime.ktx)
    implementation(libs.ax.lifecycle.common)
    implementation(libs.ax.lifecycle.common.java8)
    implementation(libs.ax.lifecycle.viewmodel.savedstate)
    implementation(libs.ax.lifecycle.livedata.ktx)
    implementation(libs.ax.lifecycle.viewmodel.ktx)
    implementation(libs.ax.lifecycle.extensions)

    //RecyclerView
    implementation(libs.ax.recyclerview)
    implementation(libs.ax.recyclerview.selection)

    //Worker
    implementation(libs.ax.work.runtime.ktx)

    //SwipeRefreshLayout
    implementation(libs.ax.swiperefreshlayout)

    //DataStore
    implementation(libs.ax.datastore.preferences)

    //App Search
    implementation(libs.ax.appsearch)
    implementation(libs.ax.appsearch.compiler)
    implementation(libs.ax.appsearch.local.storage)

    //Splashscreen
    implementation(libs.ax.core.splashscreen)

    //App Update
    implementation(libs.com.google.android.play.app.update)

    //Ads
    implementation(libs.play.services.ads.api)

    //Startup
    implementation(libs.ax.startup.runtime)

    //Other
    implementation(libs.com.microsoft.clarity.compose)

    implementation(libs.com.google.auto.value)

    implementation(libs.io.coil.kt.coil.compose)
}
