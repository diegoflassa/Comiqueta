import dev.diegoflassa.buildLogic.Configuracoes
import org.gradle.api.JavaVersion

// Apply common plugins for an Android library
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // Add other common library plugins here if needed, e.g.:
    // id("org.jetbrains.kotlin.parcelize")
    // id("com.google.devtools.ksp") // If KSP is used in libraries
}

// Access the Android Library extension
// No need for a separate 'library()' extension function like in the Groovy plugin.
// The 'android' block is directly available after applying "com.android.library".
android {
    compileSdk = Configuracoes.COMPILE_SDK
    buildToolsVersion = Configuracoes.BUILD_TOOLS_VERSION
    // buildToolsVersion is generally not needed with modern AGP versions (it uses a suitable version based on compileSdk)
    // if (Configuracoes.BUILD_TOOLS_VERSION.isNotBlank()) { // Only set if explicitly defined and needed
    //    buildToolsVersion = Configuracoes.BUILD_TOOLS_VERSION
    // }

    defaultConfig {
        minSdk = Configuracoes.MINIMUM_SDK
        // targetSdk for libraries is typically not set in defaultConfig as it's set by the consuming app.
        // If you need to set it for testing the library standalone, you can.
        // targetSdk = Configuracoes.TARGET_SDK

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro") // Standard for libraries
        // multiDexEnabled for libraries is typically not needed, the app handles multidex.
        // If your library itself is very large and needs multidex for its own tests, you might enable it.
        // multiDexEnabled = true
    }

    buildTypes {
        getByName("release") { // Use getByName for existing types
            isMinifyEnabled = false // Libraries are typically not minified by default; the app does it.
            // Set to true if you want to ship a minified library (less common).
            // isShrinkResources = false // Resource shrinking is done by the app.
            // Proguard files for libraries usually focus on 'consumer-rules.pro'
            // to specify rules for apps consuming the library.
            // If the library itself needs internal proguard rules for its own release build (rare):
            // proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            isMinifyEnabled = false
            // isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21 // Recommended for modern AGP
        targetCompatibility = JavaVersion.VERSION_21 // Recommended for modern AGP
    }

    // For libraries, you often don't need 'packagingOptions' unless there are specific conflicts
    // to resolve that the library itself introduces.

    // If your library uses dataBinding or viewBinding:
    // buildFeatures {
    //    dataBinding = true
    //    viewBinding = true
    // }
}

// Common dependencies for your libraries can be added here
// dependencies {
//    implementation(libs.androidx.core.ktx)
//    // api(libs.some.logging.framework) // If all libraries expose this
// }