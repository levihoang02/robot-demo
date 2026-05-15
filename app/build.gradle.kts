plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {

    namespace = "com.example.test"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {

        applicationId = "com.example.test"

        minSdk = 24

        targetSdk = 36

        versionCode = 1

        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "ELEVENLABS_API_KEY",
            "\"sk_fbb622d64d5baa0d32a997c0f309a3bbf2c41c628ac93713\""
        )
    }

    buildTypes {

        release {

            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(
        "androidx.activity:activity-compose:1.9.0"
    )

    implementation(
        platform(
            "androidx.compose:compose-bom:2024.06.00"
        )
    )

    implementation("androidx.compose.ui:ui")

    implementation(
        "androidx.compose.ui:ui-tooling-preview"
    )

    implementation(
        "androidx.compose.material3:material3"
    )

    implementation("androidx.compose.material:material-icons-extended")

    implementation(
        "androidx.lifecycle:lifecycle-runtime-ktx:2.8.4"
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1"
    )

    implementation(
        "com.airbnb.android:lottie-compose:6.4.0"
    )

    implementation("com.github.mik3y:usb-serial-for-android:3.10.0")

    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )

    testImplementation(libs.junit)

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}