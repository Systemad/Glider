plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}
android {
    namespace = "nl.viter.glider"
    compileSdk = 37
    defaultConfig {
        applicationId = "nl.viter.glider"
        minSdk = 24
        targetSdk = 37
        versionCode = 50
        versionName = "3.0.0-dev"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.material3)
    implementation(libs.material.icons)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)
    implementation(libs.coroutines.android)
    implementation(libs.ktor)
    implementation(libs.serialization)
    implementation(libs.jsoup)
    debugImplementation(libs.compose.tooling)
    implementation(libs.datastore)
    implementation(libs.room)
    ksp(libs.room.compiler)
    implementation(libs.hilt)
    ksp(libs.hilt.compiler)
    implementation(libs.browser)
    implementation(libs.navigation.runtime)
    implementation(libs.navigation.ui)
    implementation(libs.coil)
    implementation(libs.coil.network)
    implementation(libs.materialkolor)
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }
