plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.lanhe.module.ui"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = false  // 禁用Compose以兼容Kotlin 2.0（使用ViewBinding代替）
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // 核心模块
    implementation(project(":mokuai:mokuai:core:common"))

    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // ViewBinding - 通过buildFeatures启用，不需要显式依赖

    // Lottie for animations
    implementation(libs.lottie)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
