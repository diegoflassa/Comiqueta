@file:Suppress("unused")

package dev.diegoflassa.buildLogic

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler

fun DependencyHandler.implementation(dependency: String) {
    add("implementation", dependency)
}

fun DependencyHandler.implementation(dependency: Dependency) {
    add("implementation", dependency)
}

fun DependencyHandler.androidTest(dependency: String) {
    add("androidTest", dependency)
}

fun DependencyHandler.androidTest(dependency: Dependency) {
    add("androidTest", dependency)
}

fun DependencyHandler.androidTestImplementation(dependency: String) {
    add("androidTestImplementation", dependency)
}

fun DependencyHandler.androidTestImplementation(dependency: Dependency) {
    add("androidTestImplementation", dependency)
}

fun DependencyHandler.testImplementation(dependency: String) {
    add("testImplementation", dependency)
}

fun DependencyHandler.testImplementation(dependency: Dependency) {
    add("testImplementation", dependency)
}

fun DependencyHandler.debugImplementation(dependency: String) {
    add("debugImplementation", dependency)
}

fun DependencyHandler.debugImplementation(dependency: Dependency) {
    add("debugImplementation", dependency)
}

fun DependencyHandler.ksp(dependency: String) {
    add("ksp", dependency)
}

fun DependencyHandler.ksp(dependency: Dependency) {
    add("ksp", dependency)
}

fun DependencyHandler.kspTest(dependency: String) {
    add("kspTest", dependency)
}

fun DependencyHandler.kspTest(dependency: Dependency) {
    add("kspTest", dependency)
}

fun DependencyHandler.kspAndroidTest(dependency: String) {
    add("kspAndroidTest", dependency)
}

fun DependencyHandler.kspAndroidTest(dependency: Dependency) {
    add("kspAndroidTest", dependency)
}
