buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        val composeBomVersion = "2024.09.00"
        val hiltVersion = "2.51.1"
        val kotlinVersion = "2.0.0"
        classpath("com.android.tools.build:gradle:8.5.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:$kotlinVersion")
        classpath("com.google.dagger:hilt-android-gradle-plugin:$hiltVersion")
        classpath("androidx.compose:compose-bom:$composeBomVersion")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}