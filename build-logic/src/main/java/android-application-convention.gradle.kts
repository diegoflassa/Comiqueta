import dev.diegoflassa.buildLogic.Configuracoes
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import java.io.FileInputStream
import java.util.Properties
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Get the names of the tasks Gradle was requested to run
val requestedTaskNames = gradle.startParameter.taskNames

// Determine if an assembleDebug or assembleRelease task is among them
val isAssembleTask = requestedTaskNames.any { taskName ->
    taskName.contains("assembleDebug", ignoreCase = true) ||
            taskName.contains("assembleRelease", ignoreCase = true) ||
            taskName.contains("bundleDebug", ignoreCase = true) ||
            taskName.contains("bundleRelease", ignoreCase = true)
}

plugins {
    //alias(libs.plugins.com.android.application)
    id("com.android.application")
    //alias(libs.plugins.com.google.devtools.ksp)
    id("com.google.devtools.ksp")
}

// Call the initialization method from Configuracoes.
Configuracoes.incrementBuildCount(rootProject.rootDir, isAssembleTask)

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
} else {
    println("WARNING: keystore.properties not found. Release builds may fail to sign.")
}

// Configure the main Android Application extension
configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = Configuracoes.APPLICATION_ID
    compileSdk = Configuracoes.COMPILE_SDK
    buildToolsVersion = Configuracoes.BUILD_TOOLS_VERSION

    println("Setted versionCode to: ${Configuracoes.VERSION_CODE}")
    println("Setted versionName to: ${Configuracoes.VERSION_NAME}")

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
                "proguard-rules.pro"
            )
            if (keystoreProperties.getProperty("KEYSTORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {}
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    // ksp block is NOT part of ApplicationExtension. It must be top-level.
    // Moving ksp block out of here.
    
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
}

// KSP configuration must be done via its own extension
configure<com.google.devtools.ksp.gradle.KspExtension> {
    arg("featureFlags", "STRONG_SKIPPING_MODE=ON")
}

// Configure application variants using AndroidComponentsExtension to avoid deprecation warnings
configure<ApplicationAndroidComponentsExtension> {
    onVariants { variant ->
        val variantName = variant.name
        // Use the global version name as variant.versionName access varies in new API
        val variantVersionName = Configuracoes.VERSION_NAME

        // Determine date-time suffix if in CI
        val dateTimeSuffix = if (System.getenv("CI") == "true") {
            val currentDateTime = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("dd_MM_yyyy-HH_mm")
            "-${currentDateTime.format(formatter)}"
        } else {
            "" // No suffix if not in CI
        }

        // Renaming APKs
        val capitalizedVariantName = variantName.replaceFirstChar { it.uppercaseChar() }
        val assembleTaskName = "assemble$capitalizedVariantName"

        try {
             tasks.named(assembleTaskName) {
                doLast {
                    val rootDir = rootProject.layout.buildDirectory.get().asFile
                    val outputApkDir = file("$rootDir/outputs/apk/$variantName")

                    val generatedApk = outputApkDir.listFiles { _, name -> name.endsWith(".apk") }
                            ?.firstOrNull()

                    if (generatedApk != null && generatedApk.exists()) {
                        val baseName = Configuracoes.buildAppName(variantName, variantVersionName)
                        val newApkName = "$baseName$dateTimeSuffix.apk"
                        val renamedFile = File(generatedApk.parentFile, newApkName)

                        println("Renaming APK file for variant $variantName to: ${renamedFile.name}")
                        val success = generatedApk.renameTo(renamedFile)
                        if (success) {
                             println("Set APK file name to: $newApkName")
                        } else {
                             logger.warn("Could not rename APK file for variant $variantName.")
                        }
                    } else {
                         // Fallback path
                         val legacyDir = file("$rootDir/apk/$variantName")
                         if (legacyDir.exists()) {
                             val generatedApkLegacy = legacyDir.listFiles { _, name -> name.endsWith(".apk") }?.firstOrNull()
                             if (generatedApkLegacy != null && generatedApkLegacy.exists()) {
                                 val baseName = Configuracoes.buildAppName(variantName, variantVersionName)
                                 val newApkName = "$baseName$dateTimeSuffix.apk"
                                 val renamedFile = File(generatedApkLegacy.parentFile, newApkName)
                                 generatedApkLegacy.renameTo(renamedFile)
                                 println("Set APK file name to: $newApkName (Legacy Dir)")
                             }
                         }
                    }
                }
            }
        } catch (e: Exception) {
            println("Task $assembleTaskName not found or configuring failed: ${e.message}")
        }

        // Renaming AABs
        val bundleTaskName = "bundle$capitalizedVariantName"
        try {
            tasks.named(bundleTaskName) {
                doLast {
                    val rootDir = rootProject.layout.buildDirectory.get().asFile
                    val outputBundleDir = file("$rootDir/outputs/bundle/$variantName")

                    val generatedAab = outputBundleDir.listFiles { _, name -> name.endsWith(".aab") }
                            ?.firstOrNull()

                    if (generatedAab != null && generatedAab.exists()) {
                        val baseName = Configuracoes.buildAppName(variantName, variantVersionName)
                        val newAabName = "$baseName$dateTimeSuffix.aab"
                        val renamedFile = File(generatedAab.parentFile, newAabName)

                        println("Renaming AAB file for variant $variantName to: ${renamedFile.name}")
                        val success = generatedAab.renameTo(renamedFile)
                        if (success) {
                            println("Set AAB file name to: $newAabName")
                        } else {
                            logger.warn("Could not rename AAB file for variant $variantName.")
                        }
                    } else {
                        // Fallback path
                        val legacyBundleDir = file("$rootDir/apk/$variantName")
                         if (legacyBundleDir.exists()) {
                             val generatedAabLegacy = legacyBundleDir.listFiles { _, name -> name.endsWith(".aab") }?.firstOrNull()
                             if (generatedAabLegacy != null && generatedAabLegacy.exists()) {
                                 val baseName = Configuracoes.buildAppName(variantName, variantVersionName)
                                 val newAabName = "$baseName$dateTimeSuffix.aab"
                                 val renamedFile = File(generatedAabLegacy.parentFile, newAabName)
                                 generatedAabLegacy.renameTo(renamedFile)
                                 println("Set AAB file name to: $newAabName (Legacy Dir)")
                             }
                        }
                    }
                }
            }
        } catch (e: Exception) {
             println("Task $bundleTaskName not found or configuring failed: ${e.message}")
        }
    }
}
