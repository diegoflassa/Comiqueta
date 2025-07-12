plugins {
    id("android-library-convention")
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.android.gradle.plugin)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "dev.diegoflassa.comiqueta.core"
}

kotlin {
    jvmToolchain(JavaVersion.VERSION_21.toString().toInt())
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    //Common
    implementation(libs.ui.text.google.fonts)

    //Common Testing
    testImplementation(libs.junit)
    testImplementation(libs.ax.test.ext.junit.ktx)
    androidTestImplementation(libs.ax.test.runner)
    androidTestImplementation(libs.ax.test.uiautomator)
    androidTestImplementation(libs.ax.benchmark.macro.junit4)
    androidTestImplementation(libs.ax.test.rules)
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
    //androidTestImplementation(libs.org.jetbrains.kotlinx.coroutines.test)
    debugImplementation(libs.ax.compose.ui.test.manifest)
    debugImplementation(libs.ax.compose.ui.tooling)

    //Compose Navigation 3
    implementation(libs.ax.navigation3.runtime)
    implementation(libs.ax.navigation3.ui)

    //Firebase
    implementation(platform(libs.com.google.firebase.bom))
    implementation(libs.com.google.firebase.crashlytics.ktx)

    //Timber
    implementation(libs.com.jakewharton.timber)

    //Room
    implementation(libs.ax.room.runtime)
    ksp(libs.ax.room.compiler)
    implementation(libs.ax.room.ktx)
    //Room Testing
    androidTestImplementation(libs.ax.room.testing)

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

    //Worker
    implementation(libs.ax.work.runtime.ktx)

    //Documentfile
    implementation(libs.ax.documentfile)

    //DataStore
    implementation(libs.ax.datastore.preferences)

    //App Search
    implementation(libs.ax.appsearch)
    implementation(libs.ax.appsearch.compiler)
    implementation(libs.ax.appsearch.local.storage)

    //Splashscreen
    implementation(libs.ax.core.splashscreen)

    //Apache Commons Compress
    implementation(libs.org.apache.commons.compress)

    //Rar File
    implementation(libs.org.github.junrar)

    //Other
    implementation(libs.com.microsoft.clarity.compose)
}
