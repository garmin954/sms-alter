plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.example.pulse"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.pulse"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val sFile = project.findProperty("RELEASE_KEYSTORE_FILE")
                ?: System.getenv("RELEASE_KEYSTORE_FILE")
            val sPass = project.findProperty("RELEASE_KEYSTORE_PASSWORD")
                ?: System.getenv("RELEASE_KEYSTORE_PASSWORD")
            val sAlias = project.findProperty("RELEASE_KEY_ALIAS")
                ?: System.getenv("RELEASE_KEY_ALIAS")
            val sKeyPass = project.findProperty("RELEASE_KEY_PASSWORD")
                ?: System.getenv("RELEASE_KEY_PASSWORD")

            if (sFile != null && (sFile as? String)?.isNotEmpty() == true) {
                storeFile = file(sFile as String)
                storePassword = sPass as String
                keyAlias = sAlias as String
                keyPassword = sKeyPass as String
            }
        }
    }

    buildTypes {
        release {
            val sf = project.findProperty("RELEASE_KEYSTORE_FILE")
                ?: System.getenv("RELEASE_KEYSTORE_FILE")
            if (sf != null && (sf as? String)?.isNotEmpty() == true) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity.ktx)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Lifecycle
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // OkHttp
    implementation(libs.okhttp)

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.json.test)
}
