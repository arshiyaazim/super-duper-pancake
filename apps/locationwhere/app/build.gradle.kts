plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.googleKsp)
    alias(libs.plugins.googleHilt)
    id("com.google.gms.google-services")
}

fun String.asBuildConfigString(): String = "\"${replace("\\", "\\\\")}\""

val apiBaseUrl = (project.findProperty("API_BASE_URL") as String?) ?: "https://locationwhere.iamazim.com/"
val apiCertHost = (project.findProperty("API_CERT_HOST") as String?) ?: ""
val apiCertSha256 = (project.findProperty("API_CERT_SHA256") as String?) ?: ""
val encryptionPassphrase = (project.findProperty("ENCRYPTION_PASSPHRASE") as String?) ?: "secure_recording_passphrase_2025"
val encryptionIv = (project.findProperty("ENCRYPTION_IV") as String?) ?: "1234567890123456"

android {
    namespace = "com.example.location_where"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.location_where"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", apiBaseUrl.asBuildConfigString())
        buildConfigField("String", "API_CERT_HOST", apiCertHost.asBuildConfigString())
        buildConfigField("String", "API_CERT_SHA256", apiCertSha256.asBuildConfigString())
        buildConfigField("String", "ENCRYPTION_PASSPHRASE", encryptionPassphrase.asBuildConfigString())
        buildConfigField("String", "ENCRYPTION_IV", encryptionIv.asBuildConfigString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.material)
    implementation(libs.play.services.location)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Security
    implementation(libs.rootbeer)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
