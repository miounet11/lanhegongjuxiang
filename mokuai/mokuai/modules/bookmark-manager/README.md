# 🔖 书签管理模块 (Bookmark Manager Module)

## 🎯 概述

蓝河工具箱书签管理模块提供完整的书签存储、管理和同步功能，支持书签分类、搜索、导入导出等高级特性。基于Chromium书签系统规范实现。

## ✨ 主要特性

- ✅ **书签存储**：本地SQLite数据库存储书签信息
- ✅ **书签分类**：支持书签分组和标签管理
- ✅ **快速搜索**：支持书签标题和URL的快速搜索
- ✅ **访问统计**：记录访问频率和时间
- ✅ **图标缓存**：缓存网站favicon图标
- ✅ **导入导出**：支持HTML格式的书签导入导出
- ✅ **云同步**：支持书签数据云端同步
- ✅ **备份恢复**：书签数据的备份和恢复功能

## 🚀 快速开始

### 初始化书签管理器

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化书签管理器
        BookmarkManager.initialize(this);
    }
}
```

### 添加书签

```java
// 创建书签信息
BookmarkInfo bookmark = new BookmarkInfo();
bookmark.setTitle("蓝河工具箱官网");
bookmark.setUrl("https://ehviewer.com");
bookmark.setCategory("技术网站");

// 添加书签
long bookmarkId = BookmarkManager.getInstance()
    .addBookmark(bookmark);

// 检查添加结果
if (bookmarkId > 0) {
    Toast.makeText(context, "书签添加成功", Toast.LENGTH_SHORT).show();
}
```

### 搜索书签

```java
// 搜索书签
List<BookmarkInfo> bookmarks = BookmarkManager.getInstance()
    .searchBookmarks("蓝河工具箱");

// 显示搜索结果
for (BookmarkInfo bookmark : bookmarks) {
    Log.d(TAG, "找到书签: " + bookmark.getTitle());
}
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `BookmarkManager` | 书签管理器核心类 |
| `BookmarkInfo` | 书签信息数据类 |
| `BookmarkCategory` | 书签分类数据类 |
| `BookmarkSyncManager` | 书签同步管理器 |

### 主要方法

#### BookmarkManager

```java
// 添加书签
long addBookmark(BookmarkInfo bookmark)

// 删除书签
boolean deleteBookmark(long bookmarkId)

// 更新书签
boolean updateBookmark(BookmarkInfo bookmark)

// 获取所有书签
List<BookmarkInfo> getAllBookmarks()

// 搜索书签
List<BookmarkInfo> searchBookmarks(String query)

// 获取书签分类
List<BookmarkCategory> getCategories()

// 导入书签
boolean importBookmarks(String htmlContent)

// 导出书签
String exportBookmarks()

// 同步书签
void syncBookmarks(SyncCallback callback)
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enableSync` | `boolean` | `true` | 是否启用云同步 |
| `syncFrequency` | `long` | `3600000` | 同步频率(毫秒) |
| `maxBookmarks` | `int` | `1000` | 最大书签数量 |
| `enableIconCache` | `boolean` | `true` | 是否启用图标缓存 |
| `iconCacheSize` | `long` | `10485760` | 图标缓存大小(字节) |

## 📦 依赖项

```gradle
dependencies {
    // 蓝河工具箱书签管理模块
    implementation 'com.hippo.ehviewer:bookmark-manager:1.0.0'
}
```

## 🧪 测试

### 书签操作测试
```java
@Test
public void testBookmarkManager_addAndRetrieve_shouldWorkCorrectly() {
    // Given
    BookmarkManager manager = BookmarkManager.getInstance();
    BookmarkInfo bookmark = createTestBookmark();

    // When
    long id = manager.addBookmark(bookmark);
    BookmarkInfo retrieved = manager.getBookmarkById(id);

    // Then
    assertTrue(id > 0);
    assertNotNull(retrieved);
    assertEquals(bookmark.getTitle(), retrieved.getTitle());
}
```

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
