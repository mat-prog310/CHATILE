plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.chatile"
    compileSdk = 36
    layout.buildDirectory.set(file("C:/ChatileBuild/app"))

    defaultConfig {
        applicationId = "com.chatile"
        minSdk = 24
        targetSdk = 36
        versionCode = 9
        versionName = "1.3"
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${providers.gradleProperty("googleWebClientId").getOrElse("")}\"",
        )
        buildConfigField("String", "API_BASE_URL", "\"https://chatile.onrender.com\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    signingConfigs {
        create("release") {
            storeFile = file("release-key.jks")
            storePassword = "chatile123"
            keyAlias = "chatile"
            keyPassword = "chatile123"
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"https://chatile.onrender.com\"")
        }

        getByName("debug") {
            // Keep debug info for development
            isMinifyEnabled = false
            isShrinkResources = false
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:3000\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    bundle {
        language {
            enableSplit = false
        }
        density {
            enableSplit = false
        }
        abi {
            enableSplit = true
        }
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose BOM - manages all Compose versions
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))

    // Compose UI (using BOM versions)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Material Design 3 (primary design system)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")

    // Material 2 (needed for pullRefresh and other APIs still in use)
    implementation("androidx.compose.material:material")

    // Material Icons (from Material 3)
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.2")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.8.6")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Firebase (use BoM for version management)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // Google Play Services
    implementation("com.google.android.gms:play-services-ads-lite:23.3.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Billing
    implementation("com.android.billingclient:billing-ktx:8.0.0")

    // Credentials
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")

    // Google Sign-In
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")

    // Widgets
    implementation("androidx.glance:glance-appwidget:1.1.0")

    // ConstraintLayout for Compose (optional, for complex layouts)
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
}
