plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.trackcui.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.trackcui.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.google.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)

    // Room & RecyclerView
    implementation(libs.android.room.runtime)
    implementation(libs.android.room.ktx)
    ksp(libs.android.room.compiler)
    implementation(libs.androidx.recyclerview)
}