apply(from = "./config/keystore.gradle.kts")

@Suppress("UNCHECKED_CAST")
val verifyKeystore = extra["verifyKeystore"] as () -> Unit

buildscript {
    dependencies {
        classpath(libs.google.services)
        classpath(libs.firebase.crashlytics.gradle)
        classpath(libs.perf.plugin)
    }
}

plugins {
    alias(libs.plugins.com.osacky.doctor)
    alias(libs.plugins.org.jetbrains.kotlinx.kover)
    alias(libs.plugins.com.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.com.google.devtools.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.perf) apply false
    alias(libs.plugins.compose.compiler) apply false
    //alias(libs.plugins.io.realm.kotlin) apply false
}

verifyKeystore()
