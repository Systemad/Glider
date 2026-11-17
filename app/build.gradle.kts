plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}
val releaseKeystore = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
android {
    namespace = "nl.viter.glider"
    compileSdk = 37
    defaultConfig {
        applicationId = "io.github.systemad.glider"
        minSdk = 31
        targetSdk = 37
        versionCode = 51
        versionName = "0.1-beta"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = providers.environmentVariable("ANDROID_STORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
            }
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig = signingConfigs.findByName("release")
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
