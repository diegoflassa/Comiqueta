import dev.diegoflassa.buildLogic.Configuracoes
import org.gradle.api.JavaVersion

// Apply common plugins for an Android library
plugins {
    //alias(libs.plugins.android.library)
    id("com.android.library")
    //alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.android")
    //alias(libs.plugins.kotlin.parcelize)
    // id("org.jetbrains.kotlin.parcelize")
    //alias(libs.plugins.com.google.devtools.ksp)
    // id("com.google.devtools.ksp")
}

// Access the Android Library extension
// No need for a separate 'library()' extension function like in the Groovy plugin.
// The 'android' block is directly available after applying "com.android.library".
android {
    compileSdk = Configuracoes.COMPILE_SDK
    buildToolsVersion = Configuracoes.BUILD_TOOLS_VERSION
    // if (Configuracoes.BUILD_TOOLS_VERSION.isNotBlank()) {
    //    buildToolsVersion = Configuracoes.BUILD_TOOLS_VERSION
    // }

    defaultConfig {
        minSdk = Configuracoes.MINIMUM_SDK
        // targetSdk for libraries is typically not set in defaultConfig as it's set by the consuming app.
        // If you need to set it for testing the library standalone, you can.
        // targetSdk = Configuracoes.TARGET_SDK

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
        getByName("debug") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
