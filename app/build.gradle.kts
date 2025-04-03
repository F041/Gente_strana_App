plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    // Firebase services plugin
}

android {
    namespace = "com.gentestrana"
    compileSdk = 35


    packaging {
        resources {
            // Esclusioni per file di testo e metadati
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/LICENSE")
            excludes.add("META-INF/NOTICE")
            excludes.add("META-INF/io.netty.versions.properties")
            excludes.add("META-INF/INDEX.LIST")
        }

        jniLibs {
            // Esclusioni specifiche per file .so (JNI libraries)
            excludes.add("libnetty*.so")
            excludes.add("libnetty-*.so")
        }
    }

    defaultConfig {
        applicationId = "com.gentestrana"
        minSdk = 24
        targetSdk = 35
        versionCode = 13
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    buildFeatures {
        compose = true
        buildConfig = true
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
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.compose.material:material-icons-extended")

    testImplementation(libs.junit)
    implementation(libs.androidx.ui.tooling.preview)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.0")
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.0")

    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation ("com.google.android.gms:play-services-auth:20.5.0")
    implementation ("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    implementation ("androidx.compose.foundation:foundation")
    implementation ("androidx.compose.material3:material3")
    implementation ("com.google.dagger:hilt-android:2.48")
    implementation ("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation ("com.google.accompanist:accompanist-swiperefresh:0.32.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.32.0")
    implementation ("com.google.accompanist:accompanist-pager:0.30.1")
    // before 28.0
    implementation ("com.google.mlkit:translate:17.0.2")
    implementation ("com.google.mlkit:language-id:17.0.5")

    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.google.firebase:firebase-analytics")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("androidx.navigation:navigation-compose:2.7.5")

    implementation ("com.android.billingclient:billing-ktx:6.0.1")

    testImplementation ("org.mockito:mockito-core:5.3.1")  // Per creare oggetti "finti" (mock)
    testImplementation ("org.mockito.kotlin:mockito-kotlin:4.1.0") // Estensioni Kotlin per Mockito
    testImplementation ("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3") // Test per coroutine
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.0")
    testImplementation ("org.robolectric:robolectric:4.5")
}