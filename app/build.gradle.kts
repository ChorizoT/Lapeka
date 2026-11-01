plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "fr.birdywood.lapeka"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "fr.birdywood.lapeka"
        minSdk = 26
        targetSdk = 37
        versionCode = 26
        versionName = "2.3.1"
    }

    flavorDimensions += "version"
    productFlavors {
        create("full") {
            buildConfigField("Boolean", "IS_FOSS", "false")
            manifestPlaceholders["deepLinkHost"] = "auth"
        }
        create("foss") {
            applicationIdSuffix = ".foss"

            buildConfigField("Boolean", "IS_FOSS", "true")
            manifestPlaceholders["deepLinkHost"] = "disabled-deep-link"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        checkReleaseBuilds = true
    }


}

dependencies {
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.browser)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3.window.size.class1)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Background work
    implementation(libs.androidx.work.runtime.ktx)

    // Icon loading
    implementation(libs.coil.compose)

    // Core
    implementation(libs.androidx.core.ktx)

    // BirdyAuth
    implementation(libs.androidx.datastore.preferences)

}
