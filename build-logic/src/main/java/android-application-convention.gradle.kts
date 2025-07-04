import dev.diegoflassa.buildLogic.Configuracoes
import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
} else {
    println("WARNING: keystore.properties not found. Release builds may fail to sign.")
}

android {
    namespace = Configuracoes.APPLICATION_ID
    compileSdk = Configuracoes.COMPILE_SDK
    buildToolsVersion = Configuracoes.BUILD_TOOLS_VERSION

    defaultConfig {
        applicationId = Configuracoes.APPLICATION_ID
        minSdk = Configuracoes.MINIMUM_SDK
        targetSdk = Configuracoes.TARGET_SDK
        versionCode = Configuracoes.VERSION_CODE
        versionName = Configuracoes.VERSION_NAME
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        register("release") {
            if (keystoreProperties.getProperty("KEYSTORE_FILE") != null) {
                storeFile = rootProject.file(keystoreProperties.getProperty("KEYSTORE_FILE"))
                storePassword = keystoreProperties.getProperty("KEYSTORE_PASSWORD")
                keyAlias = keystoreProperties.getProperty("KEYSTORE_ALIAS")
                keyPassword = keystoreProperties.getProperty("KEY_PASSWORD")
                enableV3Signing = true
                enableV4Signing = true
            } else {
                println("INFO: Release signing config not fully set up due to missing keystore properties.")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro" // This file should be in each module that uses this convention (e.g., app/proguard-rules.pro)
            )
            // Apply signing config only if it's properly configured
            if (keystoreProperties.getProperty("KEYSTORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    ksp {
        arg("featureFlags", "STRONG_SKIPPING_MODE=ON")
    }

    packaging {
        resources {
            excludes += "META-INF/gradle/incremental.annotation.processors"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/LGPL2.1"
            excludes += "META-INF/ASL2.0"
        }
    }

    val globalBuildCount = Configuracoes.buildCount(project.rootDir)
    println("Calculated Global Build Count: $globalBuildCount")
    applicationVariants.all {
        val variant = this

        // APK renaming logic
        variant.outputs.all {
            val output = this
            val apkName = Configuracoes.buildAppName(
                variant.name,
                variant.versionName,
                globalBuildCount
            ) + ".apk"
            println("Set APK file name to: $apkName")
            val outputImpl = output as BaseVariantOutputImpl
            outputImpl.setOutputFileName(apkName)
        }

        // AAB renaming logic
        val capitalizedVariantName = variant.name.replaceFirstChar { it.uppercaseChar() }
        val bundleTaskName = "bundle${capitalizedVariantName}"
        tasks.named(bundleTaskName) {
            doLast {
                val outputBundleDir =
                    file("${rootProject.layout.buildDirectory.get().asFile}/apk/${variant.name}")

                val generatedAab =
                    outputBundleDir.listFiles { _, name -> name.endsWith(".aab") }?.firstOrNull()

                if (generatedAab != null && generatedAab.exists()) {
                    val newAabName = Configuracoes.buildAppName(
                        variant.name,
                        variant.versionName,
                        globalBuildCount
                    ) + ".aab"

                    val renamedFile = File(generatedAab.parentFile, newAabName)

                    println("Renaming AAB file for variant ${variant.name} to: ${renamedFile.name}")
                    val success = generatedAab.renameTo(renamedFile)
                    if (success) {
                        println("Set AAB file name to: $newAabName")
                    } else {
                        logger.warn("⚠️ Could not rename AAB file for variant ${variant.name}. From: ${generatedAab.absolutePath} To: ${renamedFile.absolutePath}")
                    }
                } else {
                    logger.warn("⚠️ No AAB file found in expected directory for variant ${variant.name}. Looked in: ${outputBundleDir.absolutePath}")
                }
            }
        }
    }
}
