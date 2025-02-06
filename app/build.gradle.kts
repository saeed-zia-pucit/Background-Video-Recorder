plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.intervalrecorder"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.intervalrecorder"
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "1.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isDebuggable= false
            isMinifyEnabled = false
            proguardFiles (getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.camera.core)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.video)
    implementation(libs.camera.view)
    implementation(libs.androidx.camera.camera2)
    implementation("io.github.farimarwat:anrspy:1.3") {
        exclude(group = "com.google.guava", module = "listenablefuture")
    }
    implementation("com.google.guava:guava:31.1-android")
//    implementation ("androidx.camera:camera-camera2:1.4.1")

//
//    implementation (libs.androidx.camera.core.v141)
//    implementation (libs.androidx.camera.lifecycle.v141)
//    implementation (libs.androidx.camera.video.v141)
//    implementation (libs.androidx.camera.view.v141)
    //
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
}