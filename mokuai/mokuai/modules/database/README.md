# 📦 数据库模块 (Database Module)

## 🎯 概述

蓝河工具箱数据库模块提供完整的数据存储和管理功能，包括SQLite数据库操作、数据迁移、查询优化等。该模块基于GreenDAO ORM框架和Chromium数据存储规范实现，支持复杂的数据关系和高效的查询操作。

## ✨ 主要特性

- ✅ **ORM数据映射**：自动对象关系映射
- ✅ **数据库迁移**：支持版本升级和数据迁移
- ✅ **事务管理**：支持复杂事务操作
- ✅ **查询优化**：高效的查询和索引支持
- ✅ **数据缓存**：内存缓存和磁盘缓存
- ✅ **并发安全**：线程安全的数据库操作
- ✅ **备份恢复**：数据库备份和恢复功能
- ✅ **数据导出**：支持多种格式的数据导出

## 🚀 快速开始

### 基本使用

```java
// 初始化数据库管理器
DatabaseManager manager = DatabaseManager.getInstance(context);

// 获取DAO实例
DownloadInfoDao downloadDao = manager.getDownloadInfoDao();
HistoryDao historyDao = manager.getHistoryDao();

// 插入数据
DownloadInfo downloadInfo = new DownloadInfo();
downloadInfo.setGid(12345L);
downloadInfo.setTitle("Sample Gallery");
downloadDao.insert(downloadInfo);

// 查询数据
List<DownloadInfo> downloads = downloadDao.queryBuilder()
    .where(DownloadInfoDao.Properties.Title.like("%sample%"))
    .list();

// 更新数据
downloadInfo.setState(DownloadInfo.STATE_FINISH);
downloadDao.update(downloadInfo);

// 删除数据
downloadDao.deleteByKey(12345L);
```

### 高级查询

```java
// 复杂查询条件
QueryBuilder<DownloadInfo> queryBuilder = downloadDao.queryBuilder();
queryBuilder.where(
    queryBuilder.and(
        DownloadInfoDao.Properties.State.eq(DownloadInfo.STATE_DOWNLOADING),
        DownloadInfoDao.Properties.Speed.gt(0)
    )
);
queryBuilder.orderDesc(DownloadInfoDao.Properties.Time);
queryBuilder.limit(20);

List<DownloadInfo> results = queryBuilder.list();

// 分页查询
int pageSize = 20;
int page = 1;
int offset = (page - 1) * pageSize;

List<DownloadInfo> pageResults = downloadDao.queryBuilder()
    .orderDesc(DownloadInfoDao.Properties.Time)
    .offset(offset)
    .limit(pageSize)
    .list();
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `DatabaseManager` | 数据库管理器核心类 |
| `DatabaseConfig` | 数据库配置类 |
| `MigrationManager` | 数据迁移管理器 |
| `BackupManager` | 数据备份管理器 |

### 主要DAO类

| DAO类 | 说明 |
|-------|------|
| `DownloadInfoDao` | 下载信息数据访问对象 |
| `HistoryDao` | 浏览历史数据访问对象 |
| `LocalFavoritesDao` | 本地收藏数据访问对象 |
| `QuickSearchDao` | 快速搜索数据访问对象 |
| `FilterDao` | 过滤器数据访问对象 |
| `BlackListDao` | 黑名单数据访问对象 |

### 主要方法

#### DatabaseManager

```java
// 获取单例实例
DatabaseManager getInstance(Context context)

// 获取DAO实例
<T> T getDao(Class<T> daoClass)

// 执行事务
void runInTransaction(Runnable runnable)

// 备份数据库
boolean backup(String backupPath)

// 恢复数据库
boolean restore(String backupPath)

// 清理数据库
void cleanup()

// 关闭数据库
void close()
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `databaseName` | `String` | `ehviewer.db` | 数据库文件名 |
| `databaseVersion` | `int` | `1` | 数据库版本号 |
| `maxConnectionPoolSize` | `int` | `5` | 最大连接池大小 |
| `enableWAL` | `boolean` | `true` | 启用WAL模式 |
| `enableForeignKeys` | `boolean` | `true` | 启用外键约束 |
| `cacheSize` | `long` | `10MB` | 缓存大小 |

## 📦 依赖项

```gradle
dependencies {
    // 核心依赖
    implementation 'org.greenrobot:greendao:3.3.0'
    implementation 'org.greenrobot:greendao-api:3.3.0'

    // Android核心库
    implementation 'androidx.sqlite:sqlite:2.3.1'

    // 蓝河工具箱数据库模块
    implementation 'com.hippo.ehviewer:database:1.0.0'
}
```

## ⚠️ 注意事项

### 权限要求
```xml
<!-- 在AndroidManifest.xml中添加 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

### 兼容性
- **最低版本**: Android API 21 (Android 5.0)
- **目标版本**: Android API 34 (Android 14)
- **编译版本**: Android API 34

### 已知问题
- 在低内存设备上可能需要调整缓存大小
- 大量数据操作时建议使用事务

## 🧪 测试

### 单元测试
```java
@Test
public void testDatabaseManager_InsertAndQuery_Success() {
    // Given
    DatabaseManager manager = DatabaseManager.getInstance(context);
    DownloadInfoDao dao = manager.getDownloadInfoDao();

    DownloadInfo info = new DownloadInfo();
    info.setGid(12345L);
    info.setTitle("Test Gallery");

    // When
    long id = dao.insert(info);
    DownloadInfo result = dao.load(id);

    // Then
    assertNotNull(result);
    assertEquals("Test Gallery", result.getTitle());
}
```

### 集成测试
```java
@RunWith(AndroidJUnit4::class)
public class DatabaseIntegrationTest {

    @Test
    public void testFullDatabaseFlow() {
        // 测试完整的数据库操作流程
        // 1. 初始化数据库
        // 2. 插入测试数据
        // 3. 执行复杂查询
        // 4. 更新数据
        // 5. 删除数据
        // 6. 清理资源
    }
}
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingDatabaseFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingDatabaseFeature'`)
4. 推送到分支 (`git push origin feature/AmazingDatabaseFeature`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情

## 📞 支持

- 📧 邮箱: support@ehviewer.com
- 📖 文档: [完整API文档](https://docs.ehviewer.com/database/)
- 🐛 问题跟踪: [GitHub Issues](https://github.com/ehviewer/ehviewer/issues)
- 💬 讨论: [GitHub Discussions](https://github.com/ehviewer/ehviewer/discussions)
