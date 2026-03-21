import com.android.build.api.dsl.LibraryExtension
import dev.diegoflassa.buildLogic.Configuracoes

// Apply common plugins for an Android library
plugins {
    //alias(libs.plugins.android.library)
    id("com.android.library")
    //alias(libs.plugins.kotlin.parcelize)
    // id("org.jetbrains.kotlin.parcelize")
    //alias(libs.plugins.com.google.devtools.ksp)
    id("com.google.devtools.ksp")
}

// Access the Android Library extension using the new API interface
configure<LibraryExtension> {
    compileSdk = Configuracoes.COMPILE_SDK
    // buildToolsVersion might not be available in LibraryExtension interface or deprecated?
    // In AGP 8+, buildToolsVersion is often optional/determined by plugin.
    // If needed: (this as? com.android.build.gradle.LibraryExtension)?.buildToolsVersion = ...
    // But mostly we can skip it or set it if the DSL allows.
    // buildToolsVersion = Configuracoes.BUILD_TOOLS_VERSION

    defaultConfig {
        minSdk = Configuracoes.MINIMUM_SDK

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
