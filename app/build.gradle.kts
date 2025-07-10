@file:Suppress("UnstableApiUsage")

import org.gradle.kotlin.dsl.project

plugins {
    id("android-application-convention")
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.android.gradle.plugin)
}

kotlin {
    jvmToolchain(JavaVersion.VERSION_21.toString().toInt())
}

dependencies {
    //Modules
    implementation(project(":core"))
    implementation(project(":settings"))
    implementation(project(":home"))

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
    implementation(libs.com.google.firebase.crashlytics.ktx)
    implementation(libs.com.google.firebase.analytics.ktx)
    implementation(libs.com.google.firebase.perf.ktx)
    implementation(libs.com.google.firebase.config.ktx)

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

    //Play Core
    implementation(libs.com.google.android.play.app.update)

    //Startup
    implementation(libs.ax.startup.runtime)

    //Other
    implementation(libs.com.microsoft.clarity.compose)

    implementation(libs.com.google.auto.value)

    implementation(libs.play.services.ads)

    implementation(libs.io.coil.kt.coil.compose)
}
