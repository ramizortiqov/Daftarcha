plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
//     alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp) // <-- ДОБАВЬТЕ ЭТУ СТРОКУ
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.daftarcha"
    compileSdk = 34 // РЕКОМЕНДУЕТСЯ: 34 (стабильный Android 14)

    defaultConfig {
        applicationId = "com.example.daftarcha"
        minSdk = 28
        targetSdk = 34 // РЕКОМЕНДУЕТСЯ: 34 (стабильный Android 14)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debugConfig")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // РЕКОМЕНДУЕТСЯ: 1.8 для лучшей совместимости
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        // РЕКОМЕНДУЕТСЯ: 1.8 для лучшей совместимости
        jvmTarget = "1.8"
    }

    // Оставляем ТОЛЬКО ОДИН блок buildFeatures
    buildFeatures {
        compose = true
    }

    composeOptions {
        // Указываем версию компилятора, которую мы добавили в TOML
        kotlinCompilerExtensionVersion = libs.versions.kotlinComposeCompiler.get()
    }

    // ЛИШНИЙ БЛОК buildFeatures УДАЛЕН
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // --- ДОБАВЬТЕ ЭТОТ БЛОК ---

    // Room (База данных)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler) // Используем ksp, а не implementation

    // ViewModel (Логика)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt (Dependency Injection)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler) // Используем ksp
    implementation(libs.hilt.navigation.compose)

    // Navigation (Переключение экранов)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    // Firebase (Firestore)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    // --- КОНЕЦ БЛОКА ---

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}