import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import java.util.Date
import java.text.SimpleDateFormat

val currentDate = Date()
val sdf = SimpleDateFormat("yyyyMMdd")
val kotlinVersion = getKotlinPluginVersion()
val coreVersion = "1.8.0"
val composeVersion = "1.2.1"
val navVersion = "2.5.0"
val lifecycleVersion = "2.5.0"
val activityVersion = "1.5.0"

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "tw.lospot.kin.call"
    compileSdk = 33
    defaultConfig {
        applicationId = "tw.lospot.kin.call"
        minSdk = 24
        targetSdk = 33
        versionCode = 3
        versionName = "2.0_${sdf.format(currentDate)}"
    }
    buildTypes {
        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    productFlavors {
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.0"
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    implementation("androidx.core:core-ktx:$coreVersion")
    implementation("androidx.navigation:navigation-compose:$navVersion")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")

    implementation("androidx.activity:activity-ktx:$activityVersion")
    implementation("androidx.activity:activity-compose:$activityVersion")

    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.material:material:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    implementation("androidx.compose.foundation:foundation-layout:$composeVersion")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
}
