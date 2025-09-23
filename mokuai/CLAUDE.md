[根目录](../CLAUDE.md) > **mokuai**

# 蓝河助手模块库文档

## 变更记录 (Changelog)

**最新扫描时间：** 2025-09-15 13:45:51
- 深度补捞：网络模块完整实现分析
- 新增模块库架构详情
- 完善网络管理器接口设计
- 补充数据库模块实现

## 模块职责

蓝河助手可复用功能模块库，采用模块化设计，为主应用和未来扩展提供标准化的功能组件。核心职责包括：

- **网络通信模块：** 统一的HTTP客户端和网络管理
- **数据库模块：** 数据持久化和迁移管理
- **Shizuku核心模块：** 权限管理和系统API封装
- **性能监控模块：** 系统性能数据采集

## 入口与启动

### 模块结构
```
mokuai/
├── core/                         # 核心模块
│   └── shizuku-manager/          # Shizuku权限管理核心
└── mokuai/
    └── modules/                  # 功能模块集合
        ├── network/              # 网络通信模块
        ├── database/             # 数据库模块
        ├── performance-monitor/  # 性能监控模块
        ├── memory-manager/       # 内存管理模块
        └── filesystem/           # 文件系统模块
```

### 初始化流程
```kotlin
// 模块库初始化
NetworkManager.getInstance(context)
DatabaseManager.getInstance(context)
PerformanceMonitor.init(context)
```

## 对外接口

### 网络模块接口

#### NetworkManager - 网络管理器
```kotlin
/**
 * 网络管理器 - 统一网络请求接口
 * 基于OkHttp实现，支持GET/POST/PUT/DELETE和文件下载
 */
public class NetworkManager {
    // 单例获取
    public static NetworkManager getInstance(@NonNull Context context)
    
    // HTTP方法
    public Call get(@NonNull String url, @Nullable INetworkCallback<String> callback)
    public Call post(@NonNull String url, @NonNull String body, @Nullable INetworkCallback<String> callback)
    public Call put(@NonNull String url, @NonNull String body, @Nullable INetworkCallback<String> callback)
    public Call delete(@NonNull String url, @Nullable INetworkCallback<String> callback)
    
    // 文件下载
    public Call download(@NonNull String url, @NonNull File destination, @Nullable INetworkCallback<File> callback)
    
    // 配置管理
    public void setConfig(@NonNull INetworkConfig config)
    public INetworkConfig getConfig()
}
```

#### INetworkCallback - 网络回调接口
```kotlin
public interface INetworkCallback<T> {
    void onSuccess(T result);
    void onFailure(NetworkException exception);
}
```

#### INetworkConfig - 网络配置接口
```kotlin
public interface INetworkConfig {
    long getConnectTimeout();
    long getReadTimeout(); 
    long getWriteTimeout();
    String getUserAgent();
}
```

### 数据库模块接口

#### DatabaseManager - 数据库管理器
```kotlin
/**
 * 数据库管理器 - 统一数据库操作接口
 */
public class DatabaseManager {
    public static DatabaseManager getInstance(@NonNull Context context)
    public void initDatabase()
    public void migrateDatabase(int fromVersion, int toVersion)
    public void backupDatabase(String backupPath)
    public void restoreDatabase(String backupPath)
}
```

## 关键依赖与配置

### 网络模块依赖
```kotlin
dependencies {
    // OkHttp网络框架
    implementation "com.squareup.okhttp3:okhttp:4.12.0"
    implementation "com.squareup.okhttp3:logging-interceptor:4.12.0"
    
    // JSON处理
    implementation "com.google.code.gson:gson:2.10.1"
    
    // 协程支持
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
}
```

### 模块配置
```kotlin
// NetworkConfig - 默认网络配置
public class NetworkConfig implements INetworkConfig {
    private static final long DEFAULT_CONNECT_TIMEOUT = 10000L;  // 10秒
    private static final long DEFAULT_READ_TIMEOUT = 30000L;     // 30秒  
    private static final long DEFAULT_WRITE_TIMEOUT = 30000L;    // 30秒
    private static final String DEFAULT_USER_AGENT = "LanheGongjuxiang/1.0";
}
```

## 数据模型

### 网络异常模型
```kotlin
/**
 * 网络异常类 - 统一异常处理
 */
public class NetworkException extends Exception {
    public static final int ERROR_NETWORK = 1000;     // 网络错误
    public static final int ERROR_SERVER = 2000;      // 服务器错误
    public static final int ERROR_TIMEOUT = 3000;     // 超时错误
    public static final int ERROR_PARSE = 4000;       // 解析错误
    
    private final int errorCode;
    private final String errorMessage;
}
```

### 网络工具类
```kotlin
/**
 * 网络工具类 - 辅助方法集合
 */
public class NetworkUtils {
    public static NetworkException createNetworkException(Exception e)
    public static boolean isNetworkAvailable(Context context)
    public static String getNetworkType(Context context)
    public static boolean isWifiConnected(Context context)
}
```

## 测试与质量

### 模块测试结构
```
network/src/
├── test/
│   └── java/com/hippo/ehviewer/module/network/
│       ├── NetworkManagerTest.java
│       ├── NetworkConfigTest.java
│       └── NetworkUtilsTest.java
└── androidTest/
    └── java/com/hippo/ehviewer/module/network/
        ├── NetworkManagerInstrumentedTest.java
        └── NetworkIntegrationTest.java
```

### 质量保证
- **单元测试覆盖率：** 目标80%以上
- **集成测试：** 网络请求端到端测试
- **Mock测试：** 模拟网络环境测试

## 常见问题 (FAQ)

### Q: 如何使用网络模块？
A: 获取NetworkManager实例，调用对应的HTTP方法，传入URL和回调接口即可。

### Q: 如何自定义网络配置？
A: 实现INetworkConfig接口，通过setConfig()方法设置自定义配置。

### Q: 网络异常如何处理？
A: 实现INetworkCallback接口的onFailure方法，根据NetworkException的错误码处理不同类型的异常。

### Q: 如何添加新的功能模块？
A: 在modules目录下创建新模块，遵循现有的接口设计规范，提供统一的API接口。

## 相关文件清单

### 网络模块文件
```
modules/network/src/main/java/com/hippo/ehviewer/module/network/
├── NetworkManager.java           # 网络管理器主类（407行）
├── NetworkConfig.java            # 默认网络配置
├── exception/
│   └── NetworkException.java     # 网络异常定义
├── interfaces/
│   ├── INetworkCallback.java     # 网络回调接口
│   └── INetworkConfig.java       # 网络配置接口
└── utils/
    └── NetworkUtils.java         # 网络工具类
```

### 数据库模块文件
```
modules/database/src/main/java/com/hippo/ehviewer/module/database/
├── DatabaseManager.java          # 数据库管理器
├── DatabaseConfig.java           # 数据库配置
├── DatabaseHelper.java           # 数据库助手
└── migration/
    └── MigrationHelper.java      # 迁移助手
```

### 构建配置文件
```
modules/
├── network/
│   └── build.gradle.kts          # 网络模块构建配置
├── database/
│   └── build.gradle.kts          # 数据库模块构建配置
└── performance-monitor/
    └── build.gradle.kts          # 性能监控模块构建配置
```

## 变更记录 (Changelog)

**2025-09-15 13:45:51**
- 完成网络模块深度分析
- 新增模块库完整架构文档
- 完善接口设计和实现细节
- 补充测试和质量保证规范