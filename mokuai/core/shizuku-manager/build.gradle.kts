plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
    // 代码质量检查插件
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

android {
    namespace = "com.lanhe.module.shizuku"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 启用JUnit5
        testOptions {
            unitTests.isIncludeAndroidResources = true
        }
    }

    buildTypes {
        debug {
            buildConfigField("Boolean", "DEBUG_MODE", "true")
            buildConfigField("String", "LOG_TAG", "\"ShizukuManager\"")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            buildConfigField("Boolean", "DEBUG_MODE", "false")
            buildConfigField("String", "LOG_TAG", "\"ShizukuManager\"")
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
        buildConfig = true
        viewBinding = false
        dataBinding = false
    }

    // 测试配置
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // ===============================
    // Android 标准库
    // ===============================
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // ===============================
    // Shizuku 框架支持
    // ===============================
    implementation("dev.rikka.shizuku:api:13.1.0")
    implementation("dev.rikka.shizuku:provider:13.1.0")

    // 隐藏API绕过（可选）
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")

    // ===============================
    // Kotlin 协程支持
    // ===============================
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ===============================
    // 依赖注入框架
    // ===============================
    implementation("javax.inject:javax.inject:1")

    // ===============================
    // 测试依赖
    // ===============================
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-android:5.8.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.robolectric:robolectric:4.10.3")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")

    // ===============================
    // 调试依赖（仅debug模式）
    // ===============================
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}

// ===============================
// KtLint 配置
// ===============================
ktlint {
    version.set("0.50.0")
    android.set(true)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)
    enableExperimentalRules.set(true)
}

// ===============================
// Detekt 配置
// ===============================
detekt {
    toolVersion = "1.23.4"
    config = files("$projectDir/detekt-config.yml")
    buildUponDefaultConfig = true
    parallel = true
    ignoreFailures = false
}

// ===============================
// 任务配置
// ===============================
tasks.register("checkCodeQuality") {
    group = "verification"
    description = "Run all code quality checks"

    dependsOn("ktlintCheck", "detekt")
}

tasks.register("generateDocumentation") {
    group = "documentation"
    description = "Generate module documentation"

    dependsOn("dokkaHtml")
}

tasks.register("runAllTests") {
    group = "verification"
    description = "Run all tests (unit and instrumentation)"

    dependsOn("test", "connectedAndroidTest")
}

tasks.register("publishToLocal") {
    group = "publishing"
    description = "Publish module to local Maven repository"

    dependsOn("publishToMavenLocal")
}

// ===============================
// 自定义任务
// ===============================
tasks.register("validateModule") {
    group = "validation"
    description = "Validate module structure and configuration"

    doLast {
        println("=== 模块验证报告 ===")

        // 检查必要的文件
        val requiredFiles = listOf(
            "src/main/java/com/lanhe/module/shizuku/ShizukuManager.java",
            "src/main/java/com/lanhe/module/shizuku/interfaces/IShizukuManager.java",
            "src/main/java/com/lanhe/module/shizuku/exception/ShizukuException.java",
            "README.md",
            "build.gradle.kts",
            "proguard-rules.pro"
        )

        var allPresent = true
        requiredFiles.forEach { file ->
            val fileObj = file(file)
            if (fileObj.exists()) {
                println("✅ $file")
            } else {
                println("❌ $file (缺失)")
                allPresent = false
            }
        }

        if (allPresent) {
            println("\n🎉 模块验证通过！所有必要文件都存在。")
        } else {
            println("\n❌ 模块验证失败！请检查缺失的文件。")
            throw GradleException("Module validation failed")
        }
    }
}

// ===============================
// 版本信息
// ===============================
val moduleVersion = "1.0.0"
val moduleName = "ShizukuManager"
val moduleDescription = "Shizuku权限管理系统模块"

// 在构建时输出模块信息
tasks.whenTaskAdded { task ->
    if (task.name == "assemble") {
        task.doFirst {
            println("""
                |================================
                | 构建模块: $moduleName
                | 版本: $moduleVersion
                | 描述: $moduleDescription
                | 构建时间: ${java.time.LocalDateTime.now()}
                |================================
            """.trimMargin())
        }
    }
}
