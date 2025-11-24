#!/bin/bash

# 定义模块列表
declare -A modules=(
    ["ad-blocker"]="com.lanhe.module.adblocker"
    ["bookmark-manager"]="com.lanhe.module.bookmark"
    ["download-manager"]="com.lanhe.module.download"
    ["image-helper"]="com.lanhe.module.image"
    ["notification"]="com.lanhe.module.notification"
    ["password-manager"]="com.lanhe.module.password"
    ["proxy-selector"]="com.lanhe.module.proxy"
    ["security-manager"]="com.lanhe.module.security"
    ["text-extractor"]="com.lanhe.module.text"
    ["url-opener"]="com.lanhe.module.urlopener"
)

# 为每个模块创建build.gradle.kts
for module in "${!modules[@]}"; do
    namespace="${modules[$module]}"
    
    cat > "mokuai/mokuai/modules/$module/build.gradle.kts" << EOF
plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
}

android {
    namespace = "$namespace"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        targetSdk = 36
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
    // 核心模块
    implementation(project(":mokuai:mokuai:core:common"))
    
    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
EOF

    echo "Created build.gradle.kts for module: $module"
done
