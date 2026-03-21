plugins {
    id("android-library-convention")
    id("com.android.library")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "dev.diegoflassa.comiqueta.ads"
}

dependencies {
    //Modules
    implementation(project(":core"))

    //Compose
    implementation(platform(libs.ax.compose.bom))
    implementation(libs.ax.compose.ui)
    implementation(libs.ax.compose.material3)
    implementation(libs.ax.activity.compose)
    implementation(libs.ax.lifecycle.viewmodel.compose)

    //Dagger & Hilt
    implementation(libs.com.google.dagger.hilt.android)
    ksp(libs.com.google.dagger.hilt.android.compiler)
    ksp(libs.ax.hilt.compiler)

    //Ads
    implementation(libs.play.services.ads.api)
}
