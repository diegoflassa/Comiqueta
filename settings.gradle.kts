@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        //maven {
        //    url = uri("https://androidx.dev/snapshots/builds/13508953/artifacts/repository")
        //}
    }
}
rootProject.name = "Comiqueta"
includeBuild("build-logic")
include(":app")
include(":core")
include(":feature-settings")
include(":feature-home")
include(":feature-categories")
include(":feature-viewer")
include(":feature-ads")
