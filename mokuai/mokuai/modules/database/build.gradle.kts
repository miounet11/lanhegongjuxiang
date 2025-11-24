plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
}

android {
    namespace = "com.lanhe.module.database"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        
        // Room schema export
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
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
    
}

dependencies {
    // 核心模块
    implementation(project(":mokuai:mokuai:core:common"))
    
    // Android Core
    implementation(libs.androidx.core.ktx)
    
    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    
    // SQLite
    implementation("androidx.sqlite:sqlite:2.5.0")
    implementation("androidx.sqlite:sqlite-ktx:2.5.0")
    
    // Database inspection
    // TODO: Stetho已弃用，考虑使用Android Studio Database Inspector
    // debugImplementation("com.facebook.stetho:stetho:4.6.0")
    
    // Testing
    testImplementation(libs.junit)
    testImplementation("androidx.room:room-testing:2.7.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // GreenDAO - Legacy ORM, replaced by Room for modern development
    // Version 3.3.0 not available in standard repositories
    // Use Room instead (see Room dependencies above)
    // implementation("org.greenrobot:greenrobot-greendao:3.3.0")
}
