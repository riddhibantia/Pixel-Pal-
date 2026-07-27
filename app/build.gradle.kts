plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.kotlin.serialization)
    kotlin("kapt")
}

android {
    namespace = "com.pixelpal.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pixelpal.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf("-Xjsr305=strict")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE,LICENSE.txt,NOTICE,NOTICE.txt}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
        }
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // AndroidX Core & Activity
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Material3 & Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    kapt(libs.hilt.compiler)
    kapt(libs.androidx.hilt.compiler)

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Coil Image Loading (with animated WebP support)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    // Timber Logging
    implementation(libs.timber)

    // Kotlinx Coroutines & Flow
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.tooling.preview)
}

kapt {
    correctErrorTypes = true
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn")
    }
}