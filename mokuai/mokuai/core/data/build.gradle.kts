plugins {
    id("com.android.library")
    id("kotlin-android")
    // 移除KSP和KAPT - 使用SharedPreferences代替Room
}

android {
    namespace = "com.lanhe.core.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        languageVersion = "2.0"
        apiVersion = "2.0"
    }
}

dependencies {
    // Core modules
    implementation(project(":mokuai:mokuai:core:common"))

    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Room Database - 仅运行时库，无编译器（避免注解处理问题）
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // 移除: ksp(libs.androidx.room.compiler)  - 使用SharedPreferences代替

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // 强制使用Kotlin 2.0.21版本 - 与KSP兼容
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common:2.0.21")

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

configurations.all {
    resolutionStrategy {
        // 强制使用Kotlin 2.0.21版本
        force("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:2.0.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.21")
    }
}
