# 🚀 Main 底座项目

## 🎯 概述

Main底座是一个基于软件快速开发的Android项目模板，提供完整的模块化架构和最佳实践。通过Main底座，您可以快速引入蓝河工具箱的各个功能模块，构建自己的应用程序。

## ✨ 主要特性

- ✅ **模块化架构**：清晰的模块分离和依赖管理
- ✅ **快速开发**：预配置的开发环境和工具链
- ✅ **最佳实践**：遵循Android开发最佳实践
- ✅ **易于扩展**：灵活的插件化架构
- ✅ **高质量保证**：完整的测试和代码质量检查
- ✅ **文档完善**：详细的使用文档和API参考

## 📦 项目结构

```
main-base/
├── app/                          # 主应用模块
│   ├── src/main/
│   │   ├── AndroidManifest.xml   # 应用清单
│   │   ├── java/com/example/
│   │   │   ├── AppApplication.kt # 应用入口
│   │   │   ├── di/               # 依赖注入
│   │   │   ├── ui/               # UI层
│   │   │   ├── data/             # 数据层
│   │   │   └── business/         # 业务逻辑
│   │   └── res/                  # 资源文件
│   └── build.gradle.kts         # 应用构建配置
├── libraries/                    # 功能模块库
│   ├── network/                  # 网络模块
│   ├── database/                 # 数据库模块
│   ├── ui/                       # UI模块
│   ├── utils/                    # 工具模块
│   ├── settings/                 # 设置管理模块
│   ├── notification/             # 通知模块
│   ├── image/                    # 图片处理模块
│   └── filesystem/               # 文件系统模块
├── gradle.properties            # 全局配置
├── settings.gradle.kts          # 项目设置
└── build.gradle.kts             # 根构建文件
```

## 🚀 快速开始

### 1. 环境准备

确保您已安装以下环境：
- **JDK 11+**
- **Android Studio Arctic Fox+**
- **Android SDK API 21+**

### 2. 克隆项目

```bash
# 克隆Main底座项目
git clone https://github.com/ehviewer/main-base.git
cd main-base

# 复制gradle配置
cp gradle/wrapper/gradle-wrapper.properties.backup gradle/wrapper/gradle-wrapper.properties
```

### 3. 配置项目

```bash
# 配置本地.properties
echo "sdk.dir=/path/to/your/android/sdk" > local.properties

# 同步项目
./gradlew sync
```

### 4. 运行项目

```bash
# 构建项目
./gradlew build

# 运行调试版本
./gradlew installDebug

# 运行发布版本
./gradlew installRelease
```

## 🔧 模块引入

### 引入网络模块

在 `app/build.gradle.kts` 中添加：

```kotlin
dependencies {
    // 引入网络模块
    implementation(project(":libraries:network"))
}
```

在Application类中初始化：

```kotlin
class AppApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化网络模块
        NetworkManager.getInstance(this)
    }
}
```

使用网络模块：

```kotlin
// 发送GET请求
NetworkManager.getInstance(context)
    .get("https://api.example.com/data")
    .enqueue(object : INetworkCallback<String> {
        override fun onSuccess(result: String) {
            Log.d(TAG, "Response: $result")
        }

        override fun onFailure(error: Exception) {
            Log.e(TAG, "Error: $error")
        }
    })
```

### 引入数据库模块

```kotlin
dependencies {
    // 引入数据库模块
    implementation(project(":libraries:database"))
}
```

```kotlin
// 初始化数据库
val dbManager = DatabaseManager.getInstance(context)

// 获取DAO
val downloadDao = dbManager.getDao(DownloadInfoDao::class.java)

// 查询数据
val downloads = downloadDao.queryBuilder()
    .where(DownloadInfoDao.Properties.State.eq(DownloadInfo.STATE_FINISH))
    .list()
```

### 引入UI模块

```kotlin
dependencies {
    // 引入UI模块
    implementation(project(":libraries:ui"))
}
```

```kotlin
// 继承BaseActivity
class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 使用UI工具方法
        showMessage("Welcome to Main Base!")
    }
}
```

## 📋 配置说明

### 全局配置 (gradle.properties)

```properties
# Android配置
android.useAndroidX=true
android.enableJetifier=true

# Kotlin配置
kotlin.code.style=official

# 构建配置
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.caching=true
org.gradle.parallel=true

# 版本配置
versionCode=1
versionName=1.0.0
```

### 应用配置 (app/build.gradle.kts)

```kotlin
plugins {
    id("com.android.application")
    id("kotlin-android")
    kotlin("kapt")
}

android {
    namespace = "com.example.mainbase"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mainbase"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"https://api.dev.example.com\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://api.example.com\"")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    // Android核心库
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // 蓝河工具箱模块
    implementation(project(":libraries:network"))
    implementation(project(":libraries:database"))
    implementation(project(":libraries:ui"))
}
```

## 🧪 测试配置

### 单元测试

```kotlin
// app/src/test/java/com/example/mainbase/ExampleUnitTest.kt
class ExampleUnitTest {

    @Test
    fun networkManager_getInstance_shouldNotBeNull() {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()

        // When
        val networkManager = NetworkManager.getInstance(context)

        // Then
        assertNotNull(networkManager)
    }
}
```

### 仪器化测试

```kotlin
// app/src/androidTest/java/com/example/mainbase/ExampleInstrumentedTest.kt
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    fun useAppContext() {
        // Given
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        // When & Then
        assertEquals("com.example.mainbase", appContext.packageName)
    }
}
```

## 📊 构建和部署

### 构建变体

```bash
# 构建所有变体
./gradlew build

# 构建调试版本
./gradlew assembleDebug

# 构建发布版本
./gradlew assembleRelease

# 构建并安装调试版本
./gradlew installDebug
```

### 签名配置

在 `app/build.gradle.kts` 中配置：

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("path/to/keystore.jks")
            storePassword = "store_password"
            keyAlias = "key_alias"
            keyPassword = "key_password"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

## 🔍 代码质量检查

### 运行检查

```bash
# 运行所有检查
./gradlew check

# 运行Kotlin代码检查
./gradlew ktlintCheck

# 运行单元测试
./gradlew test

# 运行仪器化测试
./gradlew connectedAndroidTest
```

### 配置检查工具

```kotlin
// build.gradle.kts
plugins {
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

ktlint {
    version.set("0.50.0")
    android.set(true)
}

detekt {
    config = files("$projectDir/detekt-config.yml")
}
```

## 📚 开发文档

### 模块文档
- [网络模块文档](../modules/network/README.md)
- [数据库模块文档](../modules/database/README.md)
- [UI模块文档](../modules/ui/README.md)
- [工具模块文档](../modules/utils/README.md)

### API文档
- [完整API文档](https://docs.ehviewer.com/api/)
- [模块集成指南](https://docs.ehviewer.com/integration/)

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情

## 📞 支持

- 📧 邮箱: support@ehviewer.com
- 📖 文档: [完整文档](https://docs.ehviewer.com/main-base/)
- 🐛 问题跟踪: [GitHub Issues](https://github.com/ehviewer/ehviewer/issues)
- 💬 讨论: [GitHub Discussions](https://github.com/ehviewer/ehviewer/discussions)
