@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
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
include(":feature-core")
include(":feature-settings")
include(":feature-home")
include(":feature-categories")
include(":feature-viewer")
