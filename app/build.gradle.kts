import java.text.SimpleDateFormat
import java.util.Date

val currentDate = Date()
val sdf = SimpleDateFormat("yyyyMMdd")

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "tw.lospot.kin.call"
    compileSdk = 34
    defaultConfig {
        applicationId = "tw.lospot.kin.call"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "2.1_${sdf.format(currentDate)}"
        setProperty("archivesBaseName", "${rootProject.name}-$versionName")
    }
    buildTypes {
        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
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
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    val lifecycleVersion = "2.7.0"
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")

    implementation("androidx.activity:activity-compose:1.8.2")

    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
