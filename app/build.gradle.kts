plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.kotlin.serialization)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.pixelpal.app"
    compileSdk = 35

    val localProps = java.util.Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            load(localPropertiesFile.inputStream())
        }
    }
    val geminiApiKey = localProps.getProperty("GEMINI_API_KEY")
        ?: providers.gradleProperty("GEMINI_API_KEY").orNull
        ?: providers.environmentVariable("GEMINI_API_KEY").orNull
        ?: ""

    defaultConfig {
        applicationId = "com.pixelpal.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    signingConfigs {
        create("release") {
            val keystorePath = providers.gradleProperty("RELEASE_STORE_FILE")
            if (keystorePath.isPresent && keystorePath.get().isNotBlank()) {
                storeFile = file(keystorePath.get())
                storePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD").getOrElse("")
                keyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS").getOrElse("")
                keyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD").getOrElse("")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xjsr305=strict")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE,LICENSE.txt,NOTICE,NOTICE.txt}"
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
            val hasReleaseStore = providers.gradleProperty("RELEASE_STORE_FILE")
                .map { it.isNotBlank() }
                .getOrElse(false)
            if (hasReleaseStore) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

dependencies {
    // AndroidX Core & Activity
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
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
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coil & Lottie
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.lottie.compose)

    // Gemini AI SDK
    implementation(libs.google.generativeai)

    // Firebase (BOM + Auth + Firestore)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Timber Logging
    implementation(libs.timber)

    // Kotlinx Coroutines & Flow
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // OkHttp (agent connector & web sockets)
    implementation(libs.okhttp)

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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn")
    }
}