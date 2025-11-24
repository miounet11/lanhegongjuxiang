# 蓝河助手模块化架构使用指南

## 概述

蓝河助手采用模块化架构设计，将功能拆分为独立的模块，便于维护、测试和复用。

## 模块架构

```
mokuai/
├── core/                     # 核心模块
│   ├── common/               # 通用功能和接口
│   ├── shizuku-api/         # Shizuku API封装
│   └── data/                # 数据共享和管理
└── modules/                  # 功能模块
    ├── network/             # 网络通信
    ├── performance-monitor/ # 性能监控
    ├── memory-manager/      # 内存管理
    ├── filesystem/          # 文件系统
    ├── database/            # 数据库
    ├── analytics/           # 分析统计
    ├── crash/               # 崩溃处理
    ├── ui/                  # UI组件
    └── ...                  # 其他模块
```

## 集成步骤

### 1. 在主应用中集成模块

在 `app/build.gradle.kts` 中添加所需模块：

```kotlin
dependencies {
    // 核心模块（必需）
    implementation(project(":mokuai:mokuai:core:common"))
    implementation(project(":mokuai:mokuai:core:data"))
    
    // 功能模块
    implementation(project(":mokuai:mokuai:modules:network"))
    implementation(project(":mokuai:mokuai:modules:performance-monitor"))
    implementation(project(":mokuai:mokuai:modules:memory-manager"))
    // 添加更多模块...
}
```

### 2. 使用Hilt注入模块

在 `Application` 类中初始化模块：

```kotlin
@HiltAndroidApp
class LanheApplication : Application() {
    @Inject lateinit var moduleRegistry: ModuleRegistry
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化所有模块
        lifecycleScope.launch {
            val results = moduleRegistry.initializeAllModules()
            results.forEach { (name, result) ->
                if (result.isSuccess) {
                    Log.d("Module", "Successfully initialized $name")
                } else {
                    Log.e("Module", "Failed to initialize $name", result.exceptionOrNull())
                }
            }
        }
    }
}
```

### 3. 在Activity/Fragment中使用模块

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    // 注入网络模块
    @Inject lateinit var networkApi: NetworkModuleApi
    
    // 注入性能监控模块
    @Inject lateinit var performanceApi: PerformanceMonitorApi
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 使用网络模块
        lifecycleScope.launch {
            val result = networkApi.request<String>(
                NetworkRequestConfig(
                    url = "https://api.example.com/data",
                    method = HttpMethod.GET,
                    responseType = String::class.java
                )
            )
            
            result.onSuccess { data ->
                // 处理成功响应
            }.onFailure { error ->
                // 处理错误
            }
        }
        
        // 使用性能监控
        performanceApi.getCpuUsage().collect { cpuUsage ->
            // 更新UI显示CPU使用率
        }
    }
}
```

## 创建新模块

### 1. 创建模块目录

```bash
mkdir -p mokuai/mokuai/modules/your-module/src/main/java/com/lanhe/module/yourmodule
```

### 2. 创建 build.gradle.kts

```kotlin
plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.lanhe.module.yourmodule"
    compileSdk = 36
    
    defaultConfig {
        minSdk = 24
        targetSdk = 36
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":mokuai:mokuai:core:common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
```

### 3. 实现模块API

```kotlin
@Singleton
class YourModuleApiImpl @Inject constructor(
    private val dataStore: ModuleDataStore
) : YourModuleApi {
    
    override fun getModuleName(): String = "your-module"
    
    override fun getModuleVersion(): String = "1.0.0"
    
    override suspend fun initialize(): Result<Unit {
        // 初始化逻辑
        return Result.success(Unit)
    }
    
    override suspend fun cleanup(): Result<Unit {
        // 清理逻辑
        return Result.success(Unit)
    }
    
    // 实现你的模块功能...
}
```

### 4. 配置Hilt模块

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class YourModuleModule {
    
    @Binds
    @IntoMap
    @ClassKey(YourModuleApi::class)
    abstract fun bindYourModuleApi(
        yourModuleApiImpl: YourModuleApiImpl
    ): ModuleApi
}
```

### 5. 在 settings.gradle.kts 中注册模块

```kotlin
include(":mokuai:mokuai:modules:your-module")
```

## 最佳实践

1. **模块解耦**：每个模块应该独立工作，避免直接依赖其他模块的实现
2. **接口定义**：使用API接口定义模块的公共接口
3. **依赖注入**：使用Hilt管理模块依赖关系
4. **数据共享**：使用ModuleDataStore进行模块间数据共享
5. **异步操作**：所有耗时操作都应该使用挂起函数
6. **错误处理**：使用Result类型处理错误
7. **测试覆盖**：每个模块都应该有单元测试

## 构建命令

```bash
# 构建所有模块
./gradlew build

# 构建特定模块
./gradlew :mokuai:mokuai:modules:network:build

# 运行所有测试
./gradlew test

# 运行模块测试
./gradlew :mokuai:mokuai:modules:network:test

# 生成测试覆盖率报告
./gradlew jacocoTestReport
```

## 问题排查

1. **编译错误**：检查所有模块的namespace是否正确
2. **依赖冲突**：使用 `./gradlew dependencies` 查看依赖树
3. **模块未找到**：确保模块已添加到 `settings.gradle.kts`
4. **Hilt注入失败**：检查Hilt模块配置是否正确
