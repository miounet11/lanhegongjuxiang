plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.lanhe.gongjuxiang"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lanhe.gongjuxiang"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(project.property("RELEASE_STORE_FILE").toString())
            storePassword = project.property("RELEASE_STORE_PASSWORD").toString()
            keyAlias = project.property("RELEASE_KEY_ALIAS").toString()
            keyPassword = project.property("RELEASE_KEY_PASSWORD").toString()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }

    // KSP配置
    kapt {
        correctErrorTypes = true
    }
}

dependencies {
    // Core AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.viewpager2)

    // Architecture Components
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Navigation Component
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Shizuku框架 - 系统级操作
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    // Android隐藏API绕过
    implementation(libs.hiddenapibypass)

    // 协程和异步操作
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime.ktx)

    // 数据存储
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // 网络请求
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // 动画库
    implementation(libs.lottie)

    // 权限管理
    implementation(libs.dexter)

    // UI增强库
    implementation(libs.shimmer)
    implementation(libs.androidx.swiperefreshlayout)

    // 图片处理库 (Glide)
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    // Advanced UI Components for premium experience
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    implementation("androidx.transition:transition:1.5.1")
    implementation("androidx.interpolator:interpolator:1.0.0")

    // Haptic feedback is included in core Android SDK
    // Photo zoom functionality is handled by Glide transformations

    // Advanced RecyclerView animations
    implementation("jp.wasabeef:recyclerview-animators:4.0.2")

    // Fluent System Icons - Microsoft官方图标库
    implementation("com.microsoft.design:fluent-system-icons:1.1.307@aar")

    // Analytics & Monitoring
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    debugImplementation(libs.leakcanary)

    // Performance Optimization Libraries
    implementation("com.github.markzhai:AndroidPerformanceMonitor:0.9.5") // BlockCanary for UI performance
    implementation("com.tencent.matrix:matrix-android-lib:2.1.0") // Matrix APM platform
    implementation("com.squareup.leakcanary:leakcanary-android:2.14") // Memory leak detection
    implementation("com.github.moduth:blockcanary-android:1.5.0") // UI blocking detection

    // Memory Management
    implementation("com.github.YahooArchive:memory-leaks:1.0") // Memory leak detection utilities
    implementation("com.facebook.fresco:fresco:3.1.3") // Image memory management

    // Performance Monitoring
    implementation("com.tencent.mm.hardcoder:hardcoder:1.2.1") // CPU/GPU optimization
    implementation("io.nlopez.smartlocation:library:3.3.3") // Location optimization

    // Network Optimization
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0") // DNS over HTTPS
    implementation("com.github.yyued:SVGAPlayer-Android:2.7.1") // Optimized animations

    // Storage & File Management
    implementation("commons-io:commons-io:2.15.1") // File utilities
    implementation("org.apache.commons:commons-compress:1.25.0") // Compression utilities

    // Game Mode & FPS
    implementation("com.github.markzhai:AndroidPerformanceMonitor:0.9.5") // FPS monitoring
    implementation("com.github.cats-oss:android-gpuimage:2.1.0") // GPU processing

    // AI & Machine Learning
    implementation("org.tensorflow:tensorflow-lite:2.14.0") // TensorFlow Lite for AI suggestions
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4") // TensorFlow support

    // Advanced System Control
    implementation("eu.chainfire:libsuperuser:1.1.0.201907261845") // Root operations
    implementation("com.topjohnwu.superuser:core:5.0.3") // Modern root operations

    // Advanced Features
    implementation(libs.play.review)
    implementation(libs.play.review.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.dynamic.animation)
    implementation(libs.androidx.transition)

    // Enhanced Image Processing
    implementation(libs.glide.transformations)
    // Photo view is replaced with Glide's zoom functionality

    // Biometric & Security
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)

    // System Features - AppWidget is included in core Android SDK
    implementation("androidx.window:window:1.3.0")

    // 蓝河工具箱模块库集成
    implementation(project(":mokuai:mokuai:modules:network"))
    implementation(project(":mokuai:mokuai:modules:performance-monitor"))
    implementation(project(":mokuai:mokuai:modules:memory-manager"))
    implementation(project(":mokuai:mokuai:modules:filesystem"))

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
