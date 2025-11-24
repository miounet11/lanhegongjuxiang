plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "com.lanhe.module.textextractor"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":mokuai:mokuai:core:common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Apache POI for document extraction
    // TODO: POI requires minSdk 26+ due to MethodHandle usage
    // Commented out for Android 7.0+ compatibility
    // implementation("org.apache.poi:poi:5.2.3")
    // implementation("org.apache.poi:poi-ooxml:5.2.3")
}
