# 📋 Pro 模块生成与引入标准规范

## 概述

本文档定义了蓝河工具箱模块库的**模块生成标准**、**文档规范**和**引入流程**。通过遵循这些规范，可以将任何项目的功能模块化，并确保模块能够高质量地被其他项目复用。

## 🎯 核心目标

- **标准化生产**：统一的模块生成流程和质量标准
- **高质量复用**：确保模块的可复用性和可维护性
- **快速引入**：简化的模块引入流程和最佳实践
- **持续演进**：模块的版本管理和更新机制

## 📦 第一部分：模块生成规范

### 1.1 模块识别原则

#### 功能内聚性分析
```java
// ✅ 好的模块划分示例
// 网络请求相关功能应该放在一起
public class NetworkModule {
    - HttpClient 网络请求
    - CookieManager Cookie管理
    - CacheManager 缓存管理
    - RetryHandler 重试机制
}

// ❌ 不好的模块划分示例
// 不要将不相关的功能放在一起
public class Utils {
    - NetworkRequest 网络请求
    - ImageProcessing 图片处理
    - DatabaseHelper 数据库操作
    - FileOperations 文件操作
}
```

#### 依赖关系评估
```
低耦合模块 ✅
ModuleA → ModuleB (单向依赖)

高耦合模块 ❌
ModuleA ↔ ModuleB (双向依赖)
ModuleA → ModuleB → ModuleC → ModuleA (循环依赖)
```

### 1.2 模块架构设计

#### 标准模块结构模板
```bash
module_name/
├── src/main/java/com/hippo/ehviewer/modulename/
│   ├── ModuleNameManager.java          # 核心管理类
│   ├── ModuleNameConfig.java           # 配置类
│   ├── interfaces/                     # 公共接口
│   │   ├── IModuleNameCallback.java
│   │   └── IModuleNameService.java
│   ├── impl/                          # 实现类
│   │   ├── DefaultModuleNameManager.java
│   │   └── ModuleNameHelper.java
│   ├── utils/                         # 工具类
│   │   ├── ModuleNameUtils.java
│   │   └── ValidationUtils.java
│   ├── exception/                     # 异常类
│   │   ├── ModuleNameException.java
│   │   └── ValidationException.java
│   └── constants/                     # 常量定义
│       └── ModuleNameConstants.java
├── src/main/res/                       # 资源文件
│   ├── layout/                        # 布局文件
│   ├── values/                        # 值文件
│   ├── drawable/                      # 图片资源
│   └── xml/                           # XML配置
├── src/androidTest/                    # 仪器化测试
├── src/test/                          # 单元测试
├── proguard-rules.pro                 # 混淆规则
├── build.gradle                      # 构建配置
└── README.md                         # 模块文档
```

#### 核心管理类设计模式
```java
public class ModuleNameManager {

    // 单例模式实现
    private static volatile ModuleNameManager instance;

    public static ModuleNameManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ModuleNameManager.class) {
                if (instance == null) {
                    instance = new ModuleNameManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // 私有构造函数
    private ModuleNameManager(Context context) {
        init(context);
    }

    // 初始化方法
    private void init(Context context) {
        // 初始化逻辑
    }

    // 公共API方法
    public void performAction(Callback callback) {
        // 业务逻辑
    }
}
```

### 1.3 模块接口设计

#### 回调接口标准化
```java
// 统一的回调接口模板
public interface IModuleNameCallback<T> {

    /**
     * 操作成功回调
     * @param result 操作结果
     */
    void onSuccess(T result);

    /**
     * 操作失败回调
     * @param error 错误信息
     */
    void onFailure(ModuleNameException error);

    /**
     * 操作取消回调
     */
    void onCancel();

    /**
     * 进度更新回调
     * @param progress 进度值 (0-100)
     * @param message 进度消息
     */
    default void onProgress(int progress, String message) {
        // 默认空实现
    }
}
```

#### 配置接口标准化
```java
public interface IModuleNameConfig {

    /**
     * 获取超时时间
     * @return 超时时间(毫秒)
     */
    long getTimeout();

    /**
     * 获取重试次数
     * @return 重试次数
     */
    int getRetryCount();

    /**
     * 是否启用缓存
     * @return true启用缓存，false禁用缓存
     */
    boolean isCacheEnabled();

    /**
     * 获取缓存大小
     * @return 缓存大小(字节)
     */
    long getCacheSize();
}
```

### 1.4 异常处理规范

#### 自定义异常体系
```java
// 基础异常类
public class ModuleNameException extends Exception {

    public static final int ERROR_UNKNOWN = 0;
    public static final int ERROR_NETWORK = 1;
    public static final int ERROR_TIMEOUT = 2;
    public static final int ERROR_AUTH = 3;

    private final int errorCode;

    public ModuleNameException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ModuleNameException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public static String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case ERROR_NETWORK:
                return "网络连接错误";
            case ERROR_TIMEOUT:
                return "请求超时";
            case ERROR_AUTH:
                return "认证失败";
            default:
                return "未知错误";
        }
    }
}
```

### 1.5 资源管理规范

#### 内存管理最佳实践
```java
public class ResourceManager implements AutoCloseable {

    private final List<Closeable> resources = new ArrayList<>();

    /**
     * 注册资源，在关闭时自动释放
     */
    public <T extends Closeable> T register(T resource) {
        resources.add(resource);
        return resource;
    }

    /**
     * 安全释放资源
     */
    public void release(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException e) {
                // 记录错误但不抛出异常
                Log.e(TAG, "Failed to close resource", e);
            }
        }
        resources.remove(resource);
    }

    @Override
    public void close() {
        for (Closeable resource : resources) {
            try {
                resource.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close resource during cleanup", e);
            }
        }
        resources.clear();
    }
}
```

## 📝 第二部分：文档生成规范

### 2.1 README.md 标准模板

#### 文档结构模板
```markdown
# 📦 模块名称

## 🎯 概述

简要描述模块的功能、特点和适用场景。

## ✨ 主要特性

- ✅ 特性1：详细说明
- ✅ 特性2：详细说明
- ✅ 特性3：详细说明

## 🚀 快速开始

### 基本使用

```java
// 初始化
ModuleNameManager manager = ModuleNameManager.getInstance(context);

// 基本使用示例
manager.performAction(new IModuleNameCallback<Result>() {
    @Override
    public void onSuccess(Result result) {
        // 处理成功结果
    }

    @Override
    public void onFailure(ModuleNameException error) {
        // 处理错误
    }
});
```

### 高级配置

```java
// 自定义配置
ModuleNameConfig config = new ModuleNameConfig.Builder()
    .setTimeout(30000)
    .setRetryCount(3)
    .enableCache(true)
    .build();

// 应用配置
manager.setConfig(config);
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `ModuleNameManager` | 核心管理类 |
| `ModuleNameConfig` | 配置类 |
| `IModuleNameCallback` | 回调接口 |

### 主要方法

#### ModuleNameManager

```java
// 执行主要操作
void performAction(IModuleNameCallback<T> callback)

// 获取配置
ModuleNameConfig getConfig()

// 设置配置
void setConfig(ModuleNameConfig config)

// 清理资源
void cleanup()
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `timeout` | `long` | `30000` | 请求超时时间(毫秒) |
| `retryCount` | `int` | `3` | 重试次数 |
| `cacheEnabled` | `boolean` | `true` | 是否启用缓存 |

## 📦 依赖项

```gradle
dependencies {
    // 核心依赖
    implementation 'com.example:module-name:1.0.0'

    // 可选依赖
    implementation 'com.squareup.okhttp3:okhttp:4.12.0' // 如果需要网络功能
}
```

## ⚠️ 注意事项

### 权限要求
```xml
<!-- 在AndroidManifest.xml中添加 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 兼容性
- **最低版本**: Android API 21 (Android 5.0)
- **目标版本**: Android API 34 (Android 14)
- **编译版本**: Android API 34

### 已知问题
- 问题1：解决方案
- 问题2：解决方案

## 🧪 测试

### 单元测试
```java
@Test
public void testPerformAction_Success() {
    // 测试代码
}
```

### 集成测试
```java
@RunWith(AndroidJUnit4.class)
public class ModuleNameIntegrationTest {
    // 集成测试代码
}
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情

## 📞 支持

- 📧 邮箱: support@example.com
- 📖 文档: [完整文档](https://docs.example.com)
- 🐛 问题跟踪: [GitHub Issues](https://github.com/example/repo/issues)
```

### 2.2 API文档规范

#### JavaDoc 标准格式
```java
/**
 * 用户管理器
 *
 * <p>该类负责用户的登录、注册、信息管理等功能。</p>
 *
 * <h2>基本用法</h2>
 * <pre>{@code
 * UserManager manager = UserManager.getInstance(context);
 * manager.login("username", "password", callback);
 * }</pre>
 *
 * <h2>线程安全</h2>
 * <p>该类的所有公共方法都是线程安全的。</p>
 *
 * @author 开发者姓名
 * @version 1.0.0
 * @since 2024-01-01
 * @see User
 * @see UserCallback
 * @deprecated 从2.0.0版本开始，使用 {@link NewUserManager} 替代
 */
public class UserManager {

    /**
     * 用户登录
     *
     * <p>该方法执行异步登录操作，在操作完成后通过回调接口返回结果。</p>
     *
     * @param username 用户名，必须非空且长度在3-20字符之间
     * @param password 密码，必须非空且符合密码复杂度要求
     * @param callback 登录结果回调接口，不能为null
     *
     * @throws IllegalArgumentException 当用户名或密码不符合要求时抛出
     * @throws NullPointerException 当callback为null时抛出
     *
     * @return 登录任务的唯一标识符，可用于取消操作
     *
     * @since 1.0.0
     * @see #cancelLogin(String)
     */
    public String login(String username, String password, UserCallback callback) {
        // 实现代码
    }

    /**
     * 取消登录操作
     *
     * @param loginId 登录任务标识符，由 {@link #login(String, String, UserCallback)} 返回
     * @return true如果成功取消操作，false如果操作已完成或不存在
     *
     * @since 1.0.0
     */
    public boolean cancelLogin(String loginId) {
        // 实现代码
    }
}
```

### 2.3 示例代码规范

#### 完整示例
```java
package com.example.demo;

import com.hippo.ehviewer.modulename.ModuleNameManager;
import com.hippo.ehviewer.modulename.ModuleNameConfig;
import com.hippo.ehviewer.modulename.IModuleNameCallback;

/**
 * 完整的模块使用示例
 */
public class ModuleNameDemo {

    private final ModuleNameManager manager;

    public ModuleNameDemo(Context context) {
        // 初始化管理器
        this.manager = ModuleNameManager.getInstance(context);

        // 配置模块
        configureModule();
    }

    /**
     * 配置模块参数
     */
    private void configureModule() {
        ModuleNameConfig config = new ModuleNameConfig.Builder()
            .setTimeout(30000L)           // 30秒超时
            .setRetryCount(3)             // 重试3次
            .enableCache(true)            // 启用缓存
            .setCacheSize(50 * 1024 * 1024) // 50MB缓存
            .build();

        manager.setConfig(config);
    }

    /**
     * 执行异步操作
     */
    public void performAsyncOperation() {
        manager.performAction(new IModuleNameCallback<Result>() {

            @Override
            public void onSuccess(Result result) {
                // 处理成功结果
                Log.d(TAG, "Operation successful: " + result.toString());
                updateUI(result);
            }

            @Override
            public void onFailure(ModuleNameException error) {
                // 处理错误
                Log.e(TAG, "Operation failed", error);
                showError(error);
            }

            @Override
            public void onCancel() {
                // 处理取消
                Log.i(TAG, "Operation cancelled");
                showCancelled();
            }

            @Override
            public void onProgress(int progress, String message) {
                // 更新进度
                Log.d(TAG, "Progress: " + progress + "% - " + message);
                updateProgress(progress, message);
            }
        });
    }

    /**
     * 批量操作示例
     */
    public void performBatchOperations(List<String> items) {
        for (String item : items) {
            manager.performAction(item, new IModuleNameCallback<Result>() {
                @Override
                public void onSuccess(Result result) {
                    // 处理单个结果
                }

                @Override
                public void onFailure(ModuleNameException error) {
                    // 处理单个错误
                }
            });
        }
    }

    /**
     * 资源清理
     */
    public void cleanup() {
        manager.cleanup();
    }
}
```

## 🔗 第三部分：模块引入规范

### 3.1 项目结构规划

#### 推荐的项目结构
```bash
your-project/
├── app/                          # 主应用模块
│   ├── src/main/
│   │   ├── AndroidManifest.xml   # 应用清单
│   │   ├── java/com/example/
│   │   │   ├── AppApplication.kt # 应用入口
│   │   │   ├── di/               # 依赖注入
│   │   │   ├── ui/               # UI层
│   │   │   ├── data/             # 数据层
│   │   │   ├── business/         # 业务逻辑
│   │   │   └── utils/            # 工具类
│   │   └── res/                  # 资源文件
├── libraries/                    # 模块库目录
│   ├── network/                  # 网络模块
│   ├── database/                 # 数据库模块
│   ├── image/                    # 图片处理模块
│   ├── settings/                 # 设置管理模块
│   └── utils/                    # 工具类模块
├── gradle.properties            # 全局配置
├── settings.gradle.kts          # 项目设置
└── build.gradle.kts             # 根构建文件
```

### 3.2 依赖管理

#### settings.gradle.kts 配置
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 如果模块库在远程仓库
        maven {
            url = uri("https://maven.example.com/releases")
        }
    }
}

// 包含主应用和所有模块
include(":app")
include(":libraries:network")
include(":libraries:database")
include(":libraries:image")
include(":libraries:settings")
include(":libraries:utils")
```

#### 模块依赖配置模板
```kotlin
// libraries/network/build.gradle.kts
plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "com.hippo.ehviewer.network"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Android标准库
    implementation("androidx.core:core-ktx:1.12.0")

    // 网络库
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON处理
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")

    // 依赖注入
    implementation("javax.inject:javax.inject:1")

    // 其他蓝河工具箱模块（如果需要）
    implementation(project(":libraries:utils"))
}
```

#### 主应用依赖配置
```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

android {
    // Android配置
}

dependencies {
    // 蓝河工具箱模块库
    implementation(project(":libraries:network"))
    implementation(project(":libraries:database"))
    implementation(project(":libraries:image"))
    implementation(project(":libraries:settings"))
    implementation(project(":libraries:utils"))

    // 依赖注入框架
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")

    // 其他依赖...
}
```

### 3.3 应用初始化

#### Application类配置
```kotlin
@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化所有模块
        initModules()

        // 配置全局设置
        configureGlobalSettings()
    }

    private fun initModules() {
        try {
            // 按依赖顺序初始化模块

            // 1. 初始化工具模块（无依赖）
            Utils.init(this)

            // 2. 初始化设置模块（依赖工具模块）
            SettingsManager.init(this)

            // 3. 初始化网络模块（依赖工具和设置模块）
            NetworkClient.init(this)

            // 4. 初始化数据库模块（依赖工具模块）
            DatabaseManager.init(this)

            // 5. 初始化图片模块（依赖网络和设置模块）
            ImageLoader.init(this)

            Log.i(TAG, "All modules initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize modules", e)
            // 可以选择重启应用或显示错误界面
        }
    }

    private fun configureGlobalSettings() {
        // 配置全局设置
        val settings = SettingsManager.getInstance(this)

        // 设置应用级别配置
        settings.putBoolean("debug_mode", BuildConfig.DEBUG)
        settings.putString("app_version", BuildConfig.VERSION_NAME)
    }
}
```

#### 依赖注入配置
```kotlin
// di/AppModule.kt
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNetworkClient(@ApplicationContext context: Context): NetworkClient {
        return NetworkClient.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideDatabaseManager(@ApplicationContext context: Context): DatabaseManager {
        return DatabaseManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return ImageLoader.getInstance(context)
    }
}
```

### 3.4 模块生命周期管理

#### Activity生命周期管理
```kotlin
abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var networkClient: NetworkClient
    protected lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取模块实例
        networkClient = NetworkClient.getInstance(this)
        settingsManager = SettingsManager.getInstance(this)

        // 初始化UI
        initViews()

        // 加载数据
        loadData()
    }

    override fun onResume() {
        super.onResume()
        // 重新连接网络或其他资源
        networkClient.resume()
    }

    override fun onPause() {
        super.onPause()
        // 暂停网络请求或其他资源
        networkClient.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
        cleanupResources()
    }

    protected abstract fun initViews()
    protected abstract fun loadData()
    protected abstract fun cleanupResources()
}
```

#### ViewModel生命周期管理
```kotlin
abstract class BaseViewModel : ViewModel() {

    protected val networkClient: NetworkClient by lazy {
        NetworkClient.getInstance(getApplication())
    }

    protected val databaseManager: DatabaseManager by lazy {
        DatabaseManager.getInstance(getApplication())
    }

    // 统一的错误处理
    protected fun handleError(error: Exception) {
        when (error) {
            is NetworkException -> handleNetworkError(error)
            is DatabaseException -> handleDatabaseError(error)
            else -> handleGenericError(error)
        }
    }

    protected open fun handleNetworkError(error: NetworkException) {
        // 处理网络错误
        when (error.errorCode) {
            NetworkException.ERROR_TIMEOUT -> {
                // 处理超时错误
            }
            NetworkException.ERROR_CONNECTION -> {
                // 处理连接错误
            }
        }
    }

    protected open fun handleDatabaseError(error: DatabaseException) {
        // 处理数据库错误
    }

    protected open fun handleGenericError(error: Exception) {
        // 处理通用错误
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel销毁时的清理工作
    }
}
```

## 🧪 第四部分：质量保证规范

### 4.1 代码质量检查

#### 静态代码分析配置
```kotlin
// build.gradle.kts
plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-kapt")
    // 代码质量检查插件
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

// 配置KtLint
ktlint {
    version.set("0.50.0")
    android.set(true)
    outputToConsole.set(true)
    outputColorName.set("RED")
}

// 配置Detekt
detekt {
    toolVersion = "1.23.4"
    config = files("$projectDir/detekt-config.yml")
    buildUponDefaultConfig = true
    parallel = true
}
```

#### Detekt配置模板
```yaml
# detekt-config.yml
build:
  maxIssues: 0
  excludeCorrectable: false
  weights:
    complexity: 2
    formatting: 1
    LongParameterList: 1
    MethodOverloading: 1
    TooManyFunctions: 1

complexity:
  active: true
  ComplexInterface:
    active: true
    threshold: 10
  ComplexMethod:
    active: true
    threshold: 15
  LargeClass:
    active: true
    threshold: 600
  LongMethod:
    active: true
    threshold: 60
  LongParameterList:
    active: true
    threshold: 6
  TooManyFunctions:
    active: true
    threshold: 10

naming:
  active: true
  ClassNaming:
    active: true
  FunctionNaming:
    active: true
  VariableNaming:
    active: true

style:
  active: true
  MagicNumber:
    active: true
    ignoreNumbers:
      - '-1'
      - '0'
      - '1'
      - '2'
  MaxLineLength:
    active: true
    maxLineLength: 120
  WildcardImport:
    active: true
```

### 4.2 测试覆盖率要求

#### 单元测试标准
```kotlin
// 核心业务类测试覆盖率 >= 80%
// 工具类测试覆盖率 >= 90%
// 异常处理路径必须有测试用例

class ModuleNameManagerTest {

    private lateinit var manager: ModuleNameManager
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        manager = ModuleNameManager.getInstance(mockContext)
    }

    @Test
    fun `performAction with valid input should succeed`() {
        // Given
        val input = "valid_input"
        val expectedResult = "expected_result"

        // When
        val result = manager.performAction(input)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `performAction with null input should throw exception`() {
        // When & Then
        manager.performAction(null)
    }

    @Test
    fun `performAction with network error should retry and fail`() {
        // Given
        val input = "network_error_input"

        // Mock network failure
        whenever(mockNetworkClient.sendRequest(any())).thenThrow(IOException("Network error"))

        // When & Then
        assertThrows(ModuleNameException::class.java) {
            manager.performAction(input)
        }

        // Verify retry attempts
        verify(mockNetworkClient, times(3)).sendRequest(any())
    }
}
```

#### 集成测试标准
```kotlin
@RunWith(AndroidJUnit4::class)
class ModuleNameIntegrationTest {

    @Rule
    @JvmField
    val activityRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun testFullUserFlow() {
        // 完整的用户流程测试
        // 1. 用户登录
        // 2. 执行操作
        // 3. 验证结果
        // 4. 清理数据
    }
}
```

#### UI测试标准
```kotlin
@RunWith(AndroidJUnit4::class)
class ModuleNameUITest {

    @Rule
    @JvmField
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testButtonClickTriggersAction() {
        // Given
        val scenario = activityRule.scenario

        // When
        onView(withId(R.id.action_button)).perform(click())

        // Then
        onView(withId(R.id.result_text)).check(matches(isDisplayed()))
    }
}
```

### 4.3 性能基准测试

#### 性能测试模板
```kotlin
class PerformanceTest {

    @Test
    fun `module initialization should complete within 100ms`() {
        val startTime = System.nanoTime()

        // 执行初始化
        ModuleNameManager.init(context)

        val endTime = System.nanoTime()
        val durationMs = (endTime - startTime) / 1_000_000.0

        assertTrue("Initialization took $durationMs ms, should be < 100ms", durationMs < 100.0)
    }

    @Test
    fun `performAction should handle 100 concurrent requests within 1s`() {
        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(100)
        val results = ConcurrentHashMap<String, Boolean>()

        // 提交100个并发任务
        for (i in 1..100) {
            executor.submit {
                try {
                    manager.performAction("test_input_$i")
                    results["task_$i"] = true
                } catch (e: Exception) {
                    results["task_$i"] = false
                } finally {
                    latch.countDown()
                }
            }
        }

        // 等待所有任务完成，最多等待1秒
        val completed = latch.await(1, TimeUnit.SECONDS)

        // 验证结果
        assertTrue("All tasks should complete within 1 second", completed)
        assertEquals("All 100 tasks should succeed", 100, results.size)
        results.values.forEach { assertTrue("All tasks should succeed", it) }
    }
}
```

## 📊 第五部分：版本管理规范

### 5.1 版本号规范

#### 语义化版本格式
```
主版本号.次版本号.修订号[-预发布版本][+构建元数据]
```

**版本号组成部分**：
- **主版本号**：破坏性变更（breaking changes）
- **次版本号**：新增功能（features）
- **修订号**：修复bug（bug fixes）
- **预发布版本**：alpha、beta、rc等
- **构建元数据**：构建信息

#### 版本号示例
```kotlin
// 稳定版本
1.0.0          // 第一个稳定版本
1.1.0          // 向后兼容的新功能
1.1.1          // bug修复

// 预发布版本
2.0.0-alpha    // 内部测试版本
2.0.0-beta.1   // 公开测试版本
2.0.0-rc.1     // 候选发布版本

// 构建版本
1.0.0+build.1  // 带构建信息的版本
1.0.0+20240101 // 带日期的版本
```

### 5.2 版本发布流程

#### 发布前检查清单
```bash
# 1. 代码质量检查
./gradlew ktlintCheck                    # Kotlin代码风格检查
./gradlew detekt                         # 静态代码分析
./gradlew test                           # 运行所有测试
./gradlew testCoverage                   # 检查测试覆盖率

# 2. 构建检查
./gradlew clean build                   # 完整构建
./gradlew assembleRelease               # 发布版本构建

# 3. 文档检查
./gradlew dokkaHtml                     # 生成API文档
./gradlew checkDocumentation            # 检查文档完整性
```

#### 版本发布脚本
```bash
#!/bin/bash
# release.sh

set -e  # 遇到错误立即退出

# 参数检查
if [ $# -ne 1 ]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 1.2.0"
    exit 1
fi

VERSION=$1

# 验证版本号格式
if ! [[ $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?(\+[a-zA-Z0-9.]+)?$ ]]; then
    echo "Invalid version format: $VERSION"
    echo "Expected format: x.y.z[-pre-release][+build]"
    exit 1
fi

echo "Releasing version $VERSION"

# 更新版本号
echo "version=$VERSION" > version.properties

# 提交版本更改
git add version.properties
git commit -m "Release version $VERSION"

# 创建标签
git tag -a "v$VERSION" -m "Release version $VERSION"

# 推送到远程仓库
git push origin main
git push origin "v$VERSION"

echo "Version $VERSION released successfully!"
```

### 5.3 变更日志管理

#### CHANGELOG.md 格式
```markdown
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- New feature description

### Changed
- Change description

### Deprecated
- Deprecated feature description

### Removed
- Removed feature description

### Fixed
- Bug fix description

### Security
- Security fix description

## [1.2.0] - 2024-01-15

### Added
- Add new authentication module
- Add support for OAuth2 login flow
- Add automatic token refresh mechanism

### Changed
- Update network timeout from 30s to 60s
- Improve error handling in database operations

### Fixed
- Fix memory leak in image loader
- Fix race condition in concurrent downloads

## [1.1.0] - 2024-01-01

### Added
- Initial release of the module library
- Basic network, database, and UI components
- Comprehensive documentation and examples

### Changed
- Migrate from Java to Kotlin
- Update all dependencies to latest versions

### Fixed
- Fix compatibility issues with Android 14
- Fix memory leaks in background tasks
```

#### 自动生成变更日志
```bash
#!/bin/bash
# generate_changelog.sh

# 使用git log生成变更日志
generate_changelog() {
    local tag1=$1
    local tag2=$2

    echo "## [$tag2] - $(date +%Y-%m-%d)"
    echo ""

    # 提取新增功能
    echo "### Added"
    git log --pretty=format:"%s" $tag1..$tag2 | grep -i "^feat:" | sed 's/^feat: /- /' || echo "- No new features"
    echo ""

    # 提取修复
    echo "### Fixed"
    git log --pretty=format:"%s" $tag1..$tag2 | grep -i "^fix:" | sed 's/^fix: /- /' || echo "- No bug fixes"
    echo ""

    # 提取变更
    echo "### Changed"
    git log --pretty=format:"%s" $tag1..$tag2 | grep -i "^refactor:\|^perf:" | sed 's/^.*: /- /' || echo "- No changes"
    echo ""
}

# 获取最新的两个标签
LATEST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
PREVIOUS_TAG=$(git describe --tags --abbrev=0 HEAD~1 2>/dev/null || echo "")

if [ -n "$LATEST_TAG" ] && [ -n "$PREVIOUS_TAG" ]; then
    generate_changelog $PREVIOUS_TAG $LATEST_TAG
else
    echo "Unable to generate changelog: insufficient tags"
fi
```

### 5.4 依赖版本管理

#### 版本锁定文件
```gradle
// versions.gradle.kts
object Versions {
    // Android
    const val compileSdk = 34
    const val minSdk = 21
    const val targetSdk = 34

    // Kotlin
    const val kotlin = "1.9.10"

    // AndroidX
    const val coreKtx = "1.12.0"
    const val appcompat = "1.6.1"
    const val constraintlayout = "2.1.4"

    // Networking
    const val okhttp = "4.12.0"
    const val retrofit = "2.9.0"

    // Database
    const val room = "2.6.1"

    // Image Processing
    const val glide = "4.16.0"

    // Dependency Injection
    const val hilt = "2.48"

    // Testing
    const val junit = "4.13.2"
    const val espresso = "3.5.1"
    const val mockito = "5.8.0"
}
```

#### 依赖声明模板
```kotlin
// build.gradle.kts
dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:${Versions.coreKtx}")
    implementation("androidx.appcompat:appcompat:${Versions.appcompat}")
    implementation("androidx.constraintlayout:constraintlayout:${Versions.constraintlayout}")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:${Versions.okhttp}")
    implementation("com.squareup.okhttp3:logging-interceptor:${Versions.okhttp}")
    implementation("com.squareup.retrofit2:retrofit:${Versions.retrofit}")
    implementation("com.squareup.retrofit2:converter-gson:${Versions.retrofit}")

    // Database
    implementation("androidx.room:room-runtime:${Versions.room}")
    implementation("androidx.room:room-ktx:${Versions.room}")
    kapt("androidx.room:room-compiler:${Versions.room}")

    // Image Processing
    implementation("com.github.bumptech.glide:glide:${Versions.glide}")
    kapt("com.github.bumptech.glide:compiler:${Versions.glide}")

    // Dependency Injection
    implementation("com.google.dagger:hilt-android:${Versions.hilt}")
    kapt("com.google.dagger:hilt-compiler:${Versions.hilt}")

    // Testing
    testImplementation("junit:junit:${Versions.junit}")
    testImplementation("org.mockito:mockito-core:${Versions.mockito}")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:${Versions.espresso}")
}
```

## 🎯 第六部分：最佳实践和案例

### 6.1 模块设计最佳实践

#### 单一职责原则应用
```java
// ✅ 好的设计：每个模块职责单一
public interface INetworkClient {
    void get(String url, NetworkCallback callback);
    void post(String url, Object data, NetworkCallback callback);
}

public interface IDatabaseManager {
    void save(Object data);
    Object findById(long id);
}

public interface ISettingsManager {
    void putString(String key, String value);
    String getString(String key, String defaultValue);
}

// ❌ 不好的设计：职责混乱
public class Utils {
    // 网络功能
    public void sendRequest(String url) { /* ... */ }

    // 数据库功能
    public void saveData(Object data) { /* ... */ }

    // 设置功能
    public void saveSetting(String key, String value) { /* ... */ }

    // 文件操作
    public void writeFile(String path, String content) { /* ... */ }
}
```

#### 依赖倒置原则应用
```java
// ✅ 好的设计：依赖抽象
public class UserService {

    private final INetworkClient networkClient;
    private final IDatabaseManager databaseManager;

    public UserService(INetworkClient networkClient, IDatabaseManager databaseManager) {
        this.networkClient = networkClient;
        this.databaseManager = databaseManager;
    }

    public void login(String username, String password, UserCallback callback) {
        // 使用抽象接口，不依赖具体实现
        networkClient.post("/login", new LoginRequest(username, password),
            new NetworkCallback<LoginResponse>() {
                @Override
                public void onSuccess(LoginResponse response) {
                    databaseManager.save(response.getUser());
                    callback.onSuccess(response.getUser());
                }

                @Override
                public void onFailure(Exception error) {
                    callback.onFailure(error);
                }
            });
    }
}

// ❌ 不好的设计：依赖具体实现
public class UserService {

    private final OkHttpClient httpClient;  // 直接依赖具体实现
    private final SQLiteDatabase database;  // 直接依赖具体实现

    public UserService(OkHttpClient httpClient, SQLiteDatabase database) {
        this.httpClient = httpClient;
        this.database = database;
    }
}
```

### 6.2 错误处理最佳实践

#### 统一的错误处理策略
```java
public class ErrorHandler {

    public static void handleError(Throwable error, ErrorCallback callback) {
        if (error instanceof NetworkException) {
            handleNetworkError((NetworkException) error, callback);
        } else if (error instanceof DatabaseException) {
            handleDatabaseError((DatabaseException) error, callback);
        } else if (error instanceof ValidationException) {
            handleValidationError((ValidationException) error, callback);
        } else {
            handleGenericError(error, callback);
        }
    }

    private static void handleNetworkError(NetworkException error, ErrorCallback callback) {
        switch (error.getErrorCode()) {
            case NetworkException.ERROR_TIMEOUT:
                callback.onRetryableError("网络请求超时，请重试", error);
                break;
            case NetworkException.ERROR_NO_CONNECTION:
                callback.onRetryableError("网络连接失败，请检查网络设置", error);
                break;
            case NetworkException.ERROR_SERVER:
                callback.onNonRetryableError("服务器错误，请稍后重试", error);
                break;
            default:
                callback.onUnknownError("网络错误", error);
        }
    }

    private static void handleDatabaseError(DatabaseException error, ErrorCallback callback) {
        switch (error.getErrorCode()) {
            case DatabaseException.ERROR_DISK_FULL:
                callback.onNonRetryableError("存储空间不足", error);
                break;
            case DatabaseException.ERROR_CORRUPTION:
                callback.onNonRetryableError("数据库损坏，请联系客服", error);
                break;
            default:
                callback.onRetryableError("数据库操作失败，请重试", error);
        }
    }

    private static void handleValidationError(ValidationException error, ErrorCallback callback) {
        callback.onValidationError(error.getMessage(), error);
    }

    private static void handleGenericError(Throwable error, ErrorCallback callback) {
        callback.onUnknownError("发生未知错误，请重试", error);
    }
}

public interface ErrorCallback {
    void onRetryableError(String message, Throwable error);
    void onNonRetryableError(String message, Throwable error);
    void onValidationError(String message, Throwable error);
    void onUnknownError(String message, Throwable error);
}
```

### 6.3 性能优化最佳实践

#### 内存管理
```java
public class MemoryManager {

    private static final long MAX_MEMORY_CACHE_SIZE = Runtime.getRuntime().maxMemory() / 8;
    private final LruCache<String, Bitmap> memoryCache = new LruCache<String, Bitmap>((int) MAX_MEMORY_CACHE_SIZE) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount();
        }

        @Override
        protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
            if (oldValue != null && !oldValue.isRecycled()) {
                oldValue.recycle();
            }
        }
    };

    public void put(String key, Bitmap bitmap) {
        if (getReferenceCount(key) == 0) {
            memoryCache.put(key, bitmap);
        }
    }

    public Bitmap get(String key) {
        Bitmap bitmap = memoryCache.get(key);
        if (bitmap != null && bitmap.isRecycled()) {
            memoryCache.remove(key);
            return null;
        }
        return bitmap;
    }

    public void trimMemory(int level) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            memoryCache.evictAll();
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            memoryCache.trimToSize(memoryCache.size() / 2);
        }
    }
}
```

#### 异步处理优化
```java
public class OptimizedTaskExecutor {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int MAX_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE_TIME = 30;

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
        CPU_COUNT,
        MAX_POOL_SIZE,
        KEEP_ALIVE_TIME,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(),
        new PriorityThreadFactory(),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    public void execute(Runnable command) {
        executor.execute(command);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static class PriorityThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final ThreadGroup group = Thread.currentThread().getThreadGroup();

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(group, r, "ModuleTask-" + threadNumber.getAndIncrement(), 0);
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.setDaemon(false);
            return thread;
        }
    }
}
```

---

## 📋 总结

本文档提供了完整的**Pro模块生成与引入标准规范**，涵盖了：

### 📦 **模块生成规范**
- 模块识别和功能内聚性分析
- 标准化的模块架构设计
- 统一的接口设计和异常处理
- 完整的资源管理规范

### 📝 **文档生成规范**
- 标准化的README.md模板
- 完整的API文档规范
- 详细的使用示例和最佳实践

### 🔗 **模块引入规范**
- 标准化的项目结构规划
- 统一的依赖管理和配置
- 规范化的应用初始化流程
- 完整的生命周期管理

### 🧪 **质量保证规范**
- 全面的代码质量检查配置
- 严格的测试覆盖率要求
- 标准化的性能基准测试

### 📊 **版本管理规范**
- 语义化的版本号规范
- 完整的版本发布流程
- 规范的变更日志管理
- 统一的依赖版本管理

### 🎯 **最佳实践**
- 单一职责和依赖倒置原则应用
- 统一的错误处理策略
- 内存管理和异步处理优化

通过遵循这些规范，可以确保：
- **高质量**：模块具有统一的代码质量和测试覆盖
- **高可复用性**：模块可以轻松被其他项目集成
- **易维护性**：标准化的结构和文档便于维护
- **高扩展性**：模块化设计支持灵活扩展

这个规范将成为蓝河工具箱模块库持续发展和高质量产出的重要保障！ 🚀
