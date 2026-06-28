plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

fun String.asBuildConfigString(): String = "\"${replace("\\", "\\\\")}""

val defaultBackendUrl =
    (project.findProperty("SMSGATEWAY_BACKEND_URL") as String?) ?: ""
val defaultGatewaySecret =
    (project.findProperty("SMSGATEWAY_GATEWAY_SECRET") as String?) ?: ""
val defaultAdminNumber =
    (project.findProperty("SMSGATEWAY_ADMIN_NUMBER") as String?) ?: ""

android {
    namespace = "com.locationwhere.smsgateway"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.locationwhere.smsgateway"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "DEFAULT_BACKEND_URL", defaultBackendUrl.asBuildConfigString())
        buildConfigField("String", "DEFAULT_GATEWAY_SECRET", defaultGatewaySecret.asBuildConfigString())
        buildConfigField("String", "DEFAULT_ADMIN_NUMBER", defaultAdminNumber.asBuildConfigString())
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    
    // OkHttp
    implementation(libs.okhttp)
    // JSON
    implementation(libs.json)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    // Security
    implementation(libs.security.crypto)
    
    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Logging
    implementation(libs.timber)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}