plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.numbux"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.numbux"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Compose helpers (setContent, enableEdgeToEdge extension, etc.)
    implementation("androidx.activity:activity-compose:1.8.0")
    // Core Activity APIs (where enableEdgeToEdge() is declared)
    implementation("androidx.activity:activity:1.8.0")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.appcompat:appcompat:1.6.1") // ← Esto es esencial
    implementation("com.google.android.material:material:1.11.0") // ← Tema Material3/XML
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation ("androidx.compose.material:material-icons-extended:1.4.0")
    implementation ("androidx.preference:preference-ktx:1.2.0")
    implementation ("com.google.firebase:firebase-database-ktx:20.2.2")
    implementation ("com.google.firebase:firebase-auth-ktx:22.2.0")
    implementation ("androidx.compose.material3:material3:1.1.0")
    implementation ("androidx.activity:activity-ktx:1.7.2")
    implementation ("androidx.activity:activity-compose:1.7.2")
    implementation ("androidx.compose.material:material-ripple:1.7.0")
    implementation ("androidx.compose.material:material-ripple:1.4.3")
    implementation ("androidx.compose.ui:ui:1.4.3")
    implementation ("androidx.compose.material3:material3:1.0.1")
    implementation ("androidx.compose.ui:ui:1.5.0")
    implementation ("androidx.compose.material3:material3:1.2.0")      // or material if you prefer
    implementation ("androidx.activity:activity-compose:1.7.2")
    debugImplementation ("androidx.compose.ui:ui-tooling-preview:1.5.0")
    debugImplementation ("androidx.compose.ui:ui-tooling:1.5.0")
}