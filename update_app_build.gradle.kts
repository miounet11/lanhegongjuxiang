plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    id("dagger.hilt.android.plugin")
    id("jacoco")
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
            keyAlias = project.property("RELEASE_KEY_ALIAS").toString())
            keyPassword = project.property("RELEASE_KEY_PASSWORD").toString())
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            testCoverageEnabled = true
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
    
    kotlinOptions {
        jvmTarget = "11"
    }
    
    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }

    // 测试配置
    testOptions {
        unitTests {
            includeAndroidResources = true
            all {
                // Jacoco配置
                jacoco {
                    includeNoLocationClasses = true
                    excludes = ['jdk.internal.*']
                }
            }
        }
        animationsDisabled = true
    }

    // Kapt配置
    kapt {
        correctErrorTypes = true
        arguments {
            arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
        }
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

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation("androidx.hilt:hilt-navigation-fragment:1.2.0")
    
    // Shizuku框架 - 系统级操作
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    // Android隐藏API绕过
    implementation(libs.hiddenapibypass)

    // 协程和异步操作
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime.ktx)
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

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
    testImplementation("com.google.dagger:hilt-android-testing:2.52")
    kaptTest("com.google.dagger:hilt-compiler:2.52")
    
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
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.52")
    kaptAndroidTest("com.google.dagger:hilt-compiler:2.52")
}
