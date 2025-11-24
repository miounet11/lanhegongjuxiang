#!/bin/bash

echo "🚀 应用模块化架构更新..."

# 备份重要文件
echo "📦 备份现有配置..."
cp app/build.gradle.kts app/build.gradle.kts.bak.$(date +%Y%m%d_%H%M%S)

# 应用新的app build.gradle.kts
echo "🔄 更新主应用配置..."
cp update_app_build.gradle.kts app/build.gradle.kts

# 应用项目级build.gradle.kts
echo "🔄 更新项目级配置..."
cp build.gradle.kts app/../build.gradle.kts

# 创建缺失的模块配置
echo "📝 创建模块配置文件..."

# 创建剩余模块的build.gradle.kts
for module in bookmark-manager download-manager image-helper notification password-manager proxy-selector security-manager text-extractor url-opener; do
    if [ ! -f "mokuai/mokuai/modules/$module/build.gradle.kts" ]; then
        cat > "mokuai/mokuai/modules/$module/build.gradle.kts" << EOF
plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "com.lanhe.module.${module//-/}"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    implementation(project(":mokuai:mokuai:core:common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}
EOF
        echo "✓ Created $module module config"
    fi
done

# 创建核心模块
for core in common shizuku-api data; do
    if [ ! -f "mokuai/mokuai/core/$core/build.gradle.kts" ]; then
        echo "Creating core module: $core"
        mkdir -p "mokuai/mokuai/core/$core/src/main/java/com/lanhe/core/$core"
    fi
done

echo "✅ 模块化架构更新完成！"
echo ""
echo "下一步："
echo "1. 运行 ./gradlew build 验证构建"
echo "2. 运行 ./gradlew test 运行测试"
echo "3. 查看 MODULE_USAGE_GUIDE.md 了解使用方法"
echo ""
echo "注意：首次构建可能需要下载额外的依赖项。"
