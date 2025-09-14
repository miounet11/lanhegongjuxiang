// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.4" apply false
    id("com.android.library") version "8.1.4" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
    id("com.google.dagger.hilt.android.plugin") version "2.48" apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.4" apply false
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}

// 配置所有项目的通用设置
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// 子项目配置
subprojects {
    afterEvaluate {
        if (this.plugins.hasPlugin("com.android.application") ||
            this.plugins.hasPlugin("com.android.library")) {

            configure<com.android.build.gradle.BaseExtension> {
                compileSdkVersion(34)

                defaultConfig {
                    minSdk = 21
                    targetSdk = 34
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                }

                // 配置Kotlin
                if (this is com.android.build.gradle.LibraryExtension ||
                    this is com.android.build.gradle.AppExtension) {
                    kotlinOptions {
                        jvmTarget = "11"
                    }
                }
            }
        }
    }
}

// 代码质量检查任务
task("checkAll") {
    dependsOn("ktlintCheck", "detekt")
}

// 运行所有测试
task("testAll") {
    dependsOn(subprojects.map { "${it.name}:test" })
}

// 构建所有模块
task("buildAll") {
    dependsOn(subprojects.map { "${it.name}:build" })
}

// 生成所有API文档
task("dokkaAll") {
    dependsOn(subprojects.map { "${it.name}:dokkaHtml" })
}
