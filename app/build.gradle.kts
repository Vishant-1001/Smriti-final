plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "app.smriti"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.smriti"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-m1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Only ship the languages SMRITI actually provides.
        resourceConfigurations += listOf("en", "ta")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                // FitValidationTest measures text with java.awt (the :fit model's measurer).
                it.systemProperty("java.awt.headless", "true")
                for (k in listOf("fit.validation", "fit.capture", "fit.seeds", "fit.out", "fit.v6out")) System.getProperty(k)?.let { v -> it.systemProperty(k, v) }
            }
        }
    }
    bundle {
        // Tamil/English are switched inside the app, so both must always be installed.
        language { enableSplit = false }
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

roborazzi {
    outputDir.set(file("../docs/screenshots/roborazzi"))
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    // SMRITI Fit core (pure Kotlin): chooses the radio screen's layout from explicit needs, behind FitFlags.
    implementation(project(":fit"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.junit)
    // Test-only: validates the :fit geometry model against real Compose rendering (FIT_PROTOCOL.md F5).
    testImplementation(project(":fit-bench"))

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
}
