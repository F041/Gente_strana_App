plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services") // Firebase services plugin
}

android {
    namespace = "com.gentestrana"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gentestrana"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true // Enable Jetpack Compose
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0" // Matches Compose version
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.0")
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("androidx.navigation:navigation-compose:2.7.0")
    implementation("androidx.compose.material:material-icons-extended:1.5.0")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("io.coil-kt:coil-compose:2.3.0")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation ("com.google.android.gms:play-services-auth:20.5.0")
    implementation ("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    implementation ("androidx.compose.foundation:foundation:1.6.0")
    implementation ("androidx.compose.material3:material3:1.2.0")
    implementation ("com.google.dagger:hilt-android:2.48")
    implementation ("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
}

