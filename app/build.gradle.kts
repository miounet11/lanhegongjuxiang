plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("jacoco")
    // 移除Hilt插件：id("dagger.hilt.android.plugin") - 使用手动DI代替
}

android {
    namespace = "com.lanhe.gongjuxiang"
    compileSdk = 36

    lint {
        baseline = file("lint-baseline.xml")
        warningsAsErrors = false
        abortOnError = false
    }

    defaultConfig {
        applicationId = "com.lanhe.gongjuxiang"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Room schema export
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
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
        debug {
            isDebuggable = true
            enableAndroidTestCoverage = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
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
    
    buildFeatures {
        viewBinding = true
        dataBinding = false  // 禁用DataBinding，避免Kotlin版本冲突
        buildConfig = true
    }

    // 测试配置
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
        animationsDisabled = true
    }


    // 打包选项
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/gradle/incremental.annotation.processors"
        }
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

    // Dependency Injection - 使用手动DI而不是Hilt，以避免编译问题
    // implementation(libs.hilt.android)
    // kapt(libs.hilt.compiler)
    // implementation("androidx.hilt:hilt-navigation-fragment:1.2.0")
    
    // Shizuku框架 - 系统级操作
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    // Android隐藏API绕过
    implementation(libs.hiddenapibypass)

    // 协程和异步操作
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime.ktx)
    // implementation("androidx.hilt:hilt-work:1.2.0") // 移除Hilt集成

    // 数据存储 - 使用Room
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
    // 移除: ksp(libs.glide.compiler) - 禁用编译器避免注解处理

    // Advanced UI Components for premium experience
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    implementation("androidx.transition:transition:1.5.1")
    implementation("androidx.interpolator:interpolator:1.0.0")

    // Advanced RecyclerView animations
    implementation("jp.wasabeef:recyclerview-animators:4.0.2")

    // Fluent System Icons - Microsoft官方图标库
    implementation("com.microsoft.design:fluent-system-icons:1.1.307@aar")

    // Analytics & Monitoring
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    debugImplementation(libs.leakcanary)

    // Memory Management
    implementation("com.facebook.fresco:fresco:3.1.3")

    // Network Optimization
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")

    // Storage & File Management
    implementation("commons-io:commons-io:2.15.1")
    implementation("org.apache.commons:commons-compress:1.25.0")

    // ===== Chromium文件管理功能依赖 =====
    // WebView增强
    implementation("androidx.webkit:webkit:1.10.0")
    // TODO: 恢复此依赖当GMS更新可用时
    // implementation("com.google.android.gms:play-services-safe-browsing:18.0.1")

    // JavaScript引擎 (用于文件管理脚本)
    implementation("org.mozilla:rhino:1.7.14")

    // 文件类型检测和MIME处理
    // TODO: Tika requires minSdk 26+ due to MethodHandle usage
    // Commented out for Android 7.0+ compatibility
    // implementation("org.apache.tika:tika-core:2.9.1")
    // implementation("org.apache.tika:tika-parsers:2.9.1")

    // 高级文件操作
    // TODO: 恢复此依赖当JitPack访问恢复时
    // implementation("com.github.hzy3774:AndroidFilePicker:1.0.3")
    implementation("me.zhanghai.android.fastscroll:library:1.3.0")

    // PDF文档支持
    // TODO: 替换为更新的PDF查看库，当前库可用性问题
    // implementation("com.github.barteksc:androidpdfviewer:3.2.0-beta.1")

    // 图片查看和编辑
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("jp.wasabeef:glide-transformations:4.3.0")

    // 媒体播放
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation("com.google.android.exoplayer:extension-okhttp:2.19.1")

    // Game Mode & FPS
    implementation("jp.co.cyberagent.android:gpuimage:2.1.0")

    // AI & Machine Learning
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // Enhanced Image Processing
    implementation(libs.glide.transformations)

    // Biometric & Security
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)

    // System Features
    implementation("androidx.window:window:1.3.0")

    // ===== 蓝河助手模块库集成 =====
    // 核心模块
    implementation(project(":mokuai:mokuai:core:common"))
    implementation(project(":mokuai:mokuai:core:shizuku-api"))
    implementation(project(":mokuai:mokuai:core:data"))

    // 功能模块
    implementation(project(":mokuai:mokuai:modules:network"))
    implementation(project(":mokuai:mokuai:modules:performance-monitor"))
    implementation(project(":mokuai:mokuai:modules:memory-manager"))
    implementation(project(":mokuai:mokuai:modules:filesystem"))
    implementation(project(":mokuai:mokuai:modules:database"))
    implementation(project(":mokuai:mokuai:modules:analytics"))
    implementation(project(":mokuai:mokuai:modules:crash"))
    implementation(project(":mokuai:mokuai:modules:bookmark-manager"))
    implementation(project(":mokuai:mokuai:modules:download-manager"))
    implementation(project(":mokuai:mokuai:modules:image-helper"))
    implementation(project(":mokuai:mokuai:modules:notification"))
    implementation(project(":mokuai:mokuai:modules:password-manager"))
    implementation(project(":mokuai:mokuai:modules:proxy-selector"))
    implementation(project(":mokuai:mokuai:modules:security-manager"))
    implementation(project(":mokuai:mokuai:modules:text-extractor"))
    implementation(project(":mokuai:mokuai:modules:ui"))
    implementation(project(":mokuai:mokuai:modules:url-opener"))
    implementation(project(":mokuai:mokuai:modules:ad-blocker"))

    // 测试依赖
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation("org.robolectric:robolectric:4.10.3")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.test.espresso:espresso-core:3.5.1")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.room:room-testing:2.7.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    // 移除Hilt测试依赖：testImplementation("com.google.dagger:hilt-android-testing:2.52")
    // kaptTest("com.google.dagger:hilt-compiler:2.52")

    // Android测试依赖
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-web:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("androidx.room:room-testing:2.7.0")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    // 移除Hilt测试依赖：androidTestImplementation("com.google.dagger:hilt-android-testing:2.52")
    // kaptAndroidTest("com.google.dagger:hilt-compiler:2.52")

    // 强制使用Kotlin 2.0.21版本 - 与KSP完全兼容
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common:2.0.21")
}

configurations.all {
    resolutionStrategy {
        // 强制使用Kotlin 2.0.21版本
        force("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:2.0.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.21")
        force("org.jetbrains.kotlin:kotlin-reflect:2.0.21")
    }
}
