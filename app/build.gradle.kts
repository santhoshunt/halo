plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.halo"
    // Compose BOM 2026.09.00, core 1.19.1 and lifecycle 2.11.0 require compiling against API 37.
    // targetSdk and minSdk stay at 36, so runtime behaviour on Android 16 is unchanged.
    compileSdk = 37

    defaultConfig {
        applicationId = "app.halo"
        minSdk = 36
        targetSdk = 36
        versionCode = providers.gradleProperty("haloVersionCode").getOrElse("1").toInt()
        versionName = providers.gradleProperty("haloVersionName").getOrElse("0.1.0-dev")
    }

    // Release signing comes from environment variables that the GitHub workflow fills from
    // repository secrets. Without HALO_KEYSTORE_PATH, the release build stays unsigned.
    val keystorePath = providers.environmentVariable("HALO_KEYSTORE_PATH").orNull
    if (keystorePath != null) {
        signingConfigs.create("release") {
            storeFile = file(keystorePath)
            storePassword = providers.environmentVariable("HALO_KEYSTORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("HALO_KEY_ALIAS").get()
            keyPassword = providers.environmentVariable("HALO_KEY_PASSWORD").get()
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            manifestPlaceholders["appLabel"] = "Halo dev"
        }
        release {
            isMinifyEnabled = false
            manifestPlaceholders["appLabel"] = "Halo"
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
