# gong ju lan 功能清单文档

> **文档版本**: 2.0.0
> **生成时间**: 2025-11-11
> **涵盖范围**: 除工具栏(Gallery)功能外的所有核心模块

---

## 目录

1. [浏览器引擎](#1-浏览器引擎)
2. [文件管理](#2-文件管理)
3. [下载管理](#3-下载管理)
4. [用户脚本系统](#4-用户脚本系统)
5. [网络管理](#5-网络管理)
6. [数据库与缓存](#6-数据库与缓存)
7. [应用更新](#7-应用更新)
8. [统计分析](#8-统计分析)
9. [设置管理](#9-设置管理)
10. [安全与隐私](#10-安全与隐私)
11. [辅助模块](#11-辅助模块)

---

## 1. 浏览器引擎

### 功能描述

gongjulan内置了功能完整的Web浏览器，支持双引擎(腾讯X5 + 原生WebView)渲染，提供类似Chrome的浏览体验。

### 核心功能

#### 1.1 多引擎支持
- **X5 WebView引擎**
  - 基于腾讯X5 SDK (版本44286)
  - 支持硬件加速、多线程渲染
  - QUIC、HTTP/2、WebSocket协议支持
  - 16MB瓦片缓存优化

- **原生WebView引擎**
  - Android系统原生WebView
  - 自动降级机制(X5不可用时)
  - GPU配置优化

#### 1.2 多标签页管理
- 标签页创建、切换、关闭
- 标签页后台加载
- 标签页状态保存与恢复
- 标签页历史记录

#### 1.3 WebView增强特性
- 资源拦截与优化
- 广告拦截集成
- 用户脚本自动注入
- Hosts DNS解析拦截
- 下载拦截与管理

#### 1.4 性能优化
- WebView预加载池
- LRU缓存管理
- 内存动态调整
- 缓存拦截链优化

### 技术方案

```
浏览器引擎架构:
┌─────────────────────────────────────┐
│   BrowserActivity (浏览器主界面)     │
└──────────────┬──────────────────────┘
               │
       ┌───────┴───────┐
       │               │
  X5Manager      CompatibleManager
       │               │
       ├─ X5初始化      ├─ 兼容性修复
       ├─ 硬件加速      ├─ 自动降级
       └─ 性能配置      └─ GPU优化
               │
       ┌───────┴──────────────────────┐
       │                              │
  EnhancedWebViewClient         TabManager
       │                              │
       ├─ 资源拦截                     ├─ 标签管理
       ├─ 脚本注入                     ├─ 状态保存
       └─ Hosts解析                   └─ 历史记录
```

**关键技术**:
- **WebView池**: 预创建和复用WebView实例
- **缓存策略**: HTTP缓存 + 静态资源缓存
- **内存管理**: 基于设备内存的动态调整
- **性能监控**: WebView性能实时监控

### 实现目的

1. **提供完整的Web浏览体验** - 用户可在应用内浏览任何网页
2. **优化渲染性能** - X5引擎提供更好的兼容性和性能
3. **增强用户体验** - 多标签页、脚本支持等现代浏览器特性
4. **集成下载管理** - 无缝对接应用下载系统

---

## 2. 文件管理

### 功能描述

提供完整的文件浏览和管理功能，支持多媒体预览、APK安装等操作。

### 核心功能

#### 2.1 文件浏览
- 文件和目录浏览
- 文件搜索和过滤
- 文件排序(名称、大小、时间)
- 存储空间显示
- 快速索引支持

#### 2.2 文件操作
- 复制、移动文件
- 删除、重命名文件
- 文件属性查看
- 压缩包解析和提取

#### 2.3 文件预览
- 图片预览(JPG, PNG, GIF, WebP)
- 文本预览(TXT, JSON, XML)
- 视频播放(MP4, MKV, AVI)
- 音频播放(MP3, AAC, FLAC)

#### 2.4 APK安装
- APK文件检测和验证
- 安装引导和权限申请
- 安装进度跟踪
- 后台静默安装(支持的系统)

#### 2.5 权限管理
- Android 6.0+ 运行时权限
- Android 10+ Scoped Storage适配
- Android 11+ MANAGE_EXTERNAL_STORAGE
- Storage Access Framework (SAF)

### 技术方案

```
文件管理架构:
┌────────────────────────────────────┐
│  FileManagerActivity (文件管理器)   │
└──────────────┬─────────────────────┘
               │
       ┌───────┴────────┬──────────────┐
       │                │              │
  FileIndexer      UniFile        MediaPlayer
       │                │              │
       ├─ 索引建立       ├─ 本地文件    ├─ 视频播放
       ├─ 快速搜索       ├─ ContentPro  ├─ 音频播放
       └─ 缓存管理       ├─ SAF支持     └─ 控制界面
                        └─ USB/SD卡
               │
       ┌───────┴──────────────────┐
       │                          │
  ApkInstaller              PermissionManager
       │                          │
       ├─ APK验证                 ├─ 权限检查
       ├─ 安装引导                 ├─ 权限申请
       └─ 进度跟踪                 └─ Uri权限
```

**关键技术**:
- **UniFile抽象层**: 统一文件访问接口(File/ContentProvider/SAF)
- **MediaStore集成**: Android多媒体数据库
- **Storage Access Framework**: Android 10+存储访问
- **FileIndexer**: 文件快速索引和搜索

### 实现目的

1. **完整的文件管理** - 提供类似文件管理器的功能
2. **多媒体支持** - 预览和播放各种格式文件
3. **APK安装便利** - 简化APK安装流程
4. **权限适配** - 适配不同Android版本的存储权限

---

## 3. 下载管理

### 功能描述

强大的后台下载管理系统,支持多任务并发、断点续传、智能调度。

### 核心功能

#### 3.1 下载任务管理
- 添加下载任务
- 暂停/恢复下载
- 取消下载
- 删除下载记录
- 下载进度跟踪

#### 3.2 并发控制
- 多任务并发下载(默认3个)
- 下载队列管理
- 优先级排序
- 智能调度算法

#### 3.3 断点续传
- 下载进度保存
- 中断后恢复下载
- 文件完整性校验
- 临时文件管理

#### 3.4 网络自适应
- 带宽自动检测
- 下载速率限制
- 网络变化响应
- WiFi/移动网络策略

#### 3.5 种子下载
- Torrent文件支持
- Magnet链接支持
- DHT协议支持
- Peer管理

#### 3.6 后台服务
- 前台服务保活
- 通知栏进度显示
- 下载失败自动重试
- 电池优化适配

### 技术方案

```
下载管理架构:
┌──────────────────────────────────────┐
│   DownloadManager (下载管理核心)      │
└──────────────┬───────────────────────┘
               │
       ┌───────┴────────┬──────────────┬──────────────┐
       │                │              │              │
  DownloadService  EnhancedDLMgr  StateMgr      TorrentMgr
       │                │              │              │
       ├─ 后台服务       ├─ 智能调度    ├─ 状态跟踪    ├─ 种子下载
       ├─ 通知显示       ├─ 网络自适应  ├─ 事件通知    ├─ Magnet
       ├─ 自动重试       ├─ 带宽限制    ├─ 持久化      └─ DHT协议
       └─ 保活机制       └─ 优先级队列  └─ 状态恢复
               │
       ┌───────┴──────────────────┐
       │                          │
  SpiderQueen           SmartFileProcessor
       │                          │
       ├─ 图片列表获取             ├─ 文件处理
       ├─ 逐页下载                ├─ 自动重命名
       └─ 进度更新                └─ 压缩包解压

下载流程:
添加任务 → 入队等待 → 获取列表 → 逐页下载 → 文件处理 → 完成通知
```

**关键技术**:
- **多任务队列**: LinkedList + SparseArray数据结构
- **状态机**: 等待/下载/暂停/完成/失败状态管理
- **SpiderQueen**: 爬虫引擎,获取下载资源列表
- **断点续传**: 文件分片和进度持久化
- **GreenDAO持久化**: 下载记录数据库存储

### 实现目的

1. **高效下载** - 支持多任务并发和断点续传
2. **智能管理** - 网络自适应和智能调度
3. **后台稳定** - 前台服务保证下载不被杀死
4. **用户友好** - 通知栏进度和便捷控制

---

## 4. 用户脚本系统

### 功能描述

Tampermonkey兼容的用户脚本引擎,支持20+预置增强脚本和用户自定义脚本。

### 核心功能

#### 4.1 脚本管理
- 加载和管理用户脚本
- 脚本启用/禁用
- 脚本市场(内置脚本库)
- 脚本执行日志
- 脚本版本管理

#### 4.2 脚本执行
- JavaScript代码注入
- WebView执行环境
- 脚本沙箱隔离
- 匹配规则解析
- 运行时机控制(document-start/end/idle)

#### 4.3 GM API支持
- `GM_getValue()` - 获取存储值
- `GM_setValue()` - 设置存储值
- `GM_deleteValue()` - 删除存储值
- `GM_xmlhttpRequest()` - HTTP请求
- `GM_openInTab()` - 打开新标签页
- `GM_notification()` - 显示通知

#### 4.4 预置增强脚本(20+)
- **视频解析**: 支持多个视频网站
- **广告拦截**: 移除常见广告
- **功能增强**: 界面优化、快捷键
- **下载助手**: 批量下载工具
- **翻译助手**: 页面翻译功能

### 技术方案

```
用户脚本架构:
┌──────────────────────────────────────┐
│   UserScriptManager (脚本管理器)      │
└──────────────┬───────────────────────┘
               │
       ┌───────┴────────┬──────────────┬──────────────┐
       │                │              │              │
  ScriptInjector   UserScriptParser  ScriptStorage  X5ScriptInj
       │                │              │              │
       ├─ WebView注入   ├─ 元数据解析  ├─ 文件管理    ├─ X5适配
       ├─ JS执行        ├─ 匹配规则    ├─ 配置保存    ├─ 兼容处理
       └─ 沙箱隔离      └─ 依赖解析    └─ 版本管理    └─ 性能优化

脚本格式:
// ==UserScript==
// @name         脚本名称
// @version      1.0.0
// @description  功能描述
// @match        https://example.com/*
// @run-at       document-end
// @grant        GM_getValue
// ==/UserScript==
```

**关键技术**:
- **Greasemonkey格式**: 标准用户脚本格式
- **WebView#evaluateJavascript()**: JavaScript执行
- **GM对象模拟**: Tampermonkey API兼容
- **脚本匹配**: URL模式匹配算法

### 实现目的

1. **增强浏览体验** - 通过脚本扩展浏览器功能
2. **用户自定义** - 允许用户添加自己的脚本
3. **功能扩展** - 视频解析、广告拦截等实用功能
4. **兼容性** - Tampermonkey脚本可直接使用

---

## 5. 网络管理

### 功能描述

完整的网络层管理,包括DNS解析、带宽控制、连接管理等。

### 核心功能

#### 5.1 网络监控
- 网络可用性检测
- 网络类型识别(WiFi/Mobile/VPN)
- 网络变化事件监听
- 带宽检测

#### 5.2 DNS管理
- **内置Hosts配置**
  - e-hentai.org: 104.20.18.168, 104.20.19.168, 172.67.2.238等
  - repo.e-hentai.org: 104.20.18.168等
  - forums.e-hentai.org: 104.20.18.168等
- **远程Hosts更新**
- **DNS over HTTPS (DoH)**
- **自定义DNS解析**

#### 5.3 带宽管理
- 带宽限制
- 流量监控
- 自适应码率调整
- 下载速率限制

#### 5.4 连接管理
- 连接超时配置(连接10s, 读10s, 写10s, 总30s)
- 指数退避重试
- 最大重试次数限制
- 快速失败检测
- 熔断机制
- 动态超时调整

#### 5.5 资源拦截
- HTTP请求拦截
- 请求头修改
- 响应修改
- 广告资源过滤

#### 5.6 SSL/TLS安全
- TLS 1.2+支持
- 现代加密套件
- 证书固定(Certificate Pinning)
- 自定义信任管理

### 技术方案

```
网络管理架构:
┌──────────────────────────────────────┐
│      EhApplication (网络配置入口)     │
└──────────────┬───────────────────────┘
               │
       ┌───────┴────────┬──────────────┬──────────────┐
       │                │              │              │
  OkHttpClient    ImageOkHttpClient  NetworkMonitor   EhHosts
       │                │              │              │
       ├─ 通用HTTP       ├─ 图片加载    ├─ 网络检测    ├─ DNS解析
       ├─ 50MB缓存      ├─ 图片优化    ├─ 类型识别    ├─ Hosts映射
       ├─ 拦截器链      ├─ 缓存策略    └─ 变化监听    └─ 远程更新
       └─ Cookie管理
               │
       ┌───────┴──────────────────┬──────────────┐
       │                          │              │
  BandwidthManager    ConnectionRetryMgr  ResourceInterceptor
       │                          │              │
       ├─ 带宽限制                 ├─ 重试策略    ├─ 请求拦截
       ├─ 流量监控                 ├─ 熔断机制    ├─ 头部修改
       └─ 码率调整                 └─ 超时管理    └─ 资源过滤

拦截器链:
Application Interceptor → Network Interceptor → CallServerInterceptor
├─ ResourceInterceptor      ├─ CacheInterceptor
└─ ErrorHandlingInterceptor └─ ConnectInterceptor
```

**关键技术**:
- **OkHttp 3.14.7**: HTTP客户端库
- **EhHosts**: 自定义DNS解析器(实现Dns接口)
- **EhCookieStore**: Cookie持久化存储
- **EhSSLSocketFactory**: SSL/TLS安全配置
- **ConnectionPool**: 连接池管理和Keep-Alive

### 实现目的

1. **网络可达性** - DNS解析优化,确保能访问目标网站
2. **性能优化** - 带宽管理和连接复用提升性能
3. **稳定性** - 重试机制和超时管理保证稳定
4. **安全性** - SSL/TLS和证书固定保证安全

---

## 6. 数据库与缓存

### 功能描述

基于GreenDAO的数据持久化层,提供高效的数据存储和缓存管理。

### 核心功能

#### 6.1 数据库管理
- 数据库初始化和版本管理
- 事务管理
- 数据库迁移
- Dao类统一入口

#### 6.2 数据表结构

| 表名 | 用途 | 主键 | 说明 |
|------|------|------|------|
| `downloads` | 下载记录 | gid | 工具栏下载信息 |
| `galleries` | 工具栏缓存 | gid | 工具栏详细信息 |
| `history` | 浏览历史 | gid | 用户浏览记录 |
| `bookmarks` | 书签 | id | 用户书签 |
| `quick_search` | 快速搜索 | id | 搜索历史 |
| `filter` | 过滤规则 | id | 内容过滤 |
| `local_favorites` | 本地收藏 | gid | 本地收藏夹 |

#### 6.3 缓存管理
- **MemoryCache**: LRU内存缓存
- **DiskCache**: 磁盘缓存(50MB HTTP缓存)
- **AdaptiveCacheManager**: 基于设备内存的动态缓存大小
- **WebViewCache**: WebView缓存优化
- **ImageCache**: 图片专用缓存

#### 6.4 数据模型
- **DownloadInfo**: 下载记录实体
- **GalleryInfo**: 工具栏信息实体
- **HistoryInfo**: 历史记录实体
- **BookmarkInfo**: 书签实体

### 技术方案

```
数据库架构:
┌──────────────────────────────────────┐
│          EhDB (数据库管理核心)        │
└──────────────┬───────────────────────┘
               │
       ┌───────┴────────┬──────────────┬──────────────┐
       │                │              │              │
  DaoSession      DownloadInfoDao  GalleryInfoDao  HistoryDao
       │                │              │              │
       ├─ 事务管理       ├─ 下载CRUD    ├─ 工具栏CRUD    ├─ 历史CRUD
       ├─ 数据库连接     ├─ 状态查询    ├─ 缓存查询    ├─ 时间排序
       └─ 版本控制       └─ 批量操作    └─ 批量加载    └─ 清理操作
               │
       ┌───────┴──────────────────┬──────────────┐
       │                          │              │
  MSQLiteOpenHelper     AdaptiveCacheManager  MemoryCache
       │                          │              │
       ├─ 创建表结构               ├─ 缓存大小    ├─ LRU算法
       ├─ 版本升级                ├─ 内存检测    ├─ 容量限制
       └─ 数据迁移                └─ 动态调整    └─ 命中统计

缓存层次:
┌─────────────────────────────────┐
│   Memory Cache (LRU)            │  ← 第一层:内存缓存
├─────────────────────────────────┤
│   Database Cache (GreenDAO)     │  ← 第二层:数据库缓存
├─────────────────────────────────┤
│   Disk Cache (50MB)             │  ← 第三层:磁盘缓存
└─────────────────────────────────┘
```

**关键技术**:
- **GreenDAO 3.0.0**: 高性能ORM框架
- **SQLiteOpenHelper**: 数据库版本管理
- **LRU Cache**: 最近最少使用缓存算法
- **自适应内存**: 基于ActivityManager.getMemoryClass()动态调整

### 实现目的

1. **数据持久化** - 保存下载、历史、收藏等数据
2. **性能优化** - 多层缓存提升访问速度
3. **内存管理** - 自适应缓存避免OOM
4. **数据迁移** - 支持版本升级和数据平滑迁移

---

## 7. 应用更新

### 功能描述

自动检查和安装应用更新,支持强制更新和可选更新。

### 核心功能

#### 7.1 更新检查
- 自动检查(每24小时)
- 手动检查(设置→关于)
- 版本比较
- 更新间隔控制

#### 7.2 更新策略
- **强制更新**: 立即下载安装,无法跳过
- **可选更新**: 提示并允许跳过
- **灰度发布**: 支持渠道控制(规划中)

#### 7.3 更新流程
1. 从远程获取更新信息(JSON格式)
2. 比较版本号(versionCode)
3. 显示更新对话框
4. 下载新版本APK
5. 引导用户安装

#### 7.4 更新对话框
- 更新内容显示(changelog)
- 下载进度显示
- 强制/可选更新UI区分
- 安装提示

### 技术方案

```
更新管理架构:
┌──────────────────────────────────────┐
│      AppUpdater (更新管理器)          │
└──────────────┬───────────────────────┘
               │
       ┌───────┴────────┬──────────────┐
       │                │              │
  更新检查逻辑      UpdateDialog   APK下载器
       │                │              │
       ├─ 版本比较       ├─ 信息显示    ├─ 下载APK
       ├─ 间隔控制       ├─ 进度显示    ├─ 校验文件
       └─ JSON解析      └─ 安装引导    └─ 保存本地

更新JSON格式:
{
  "version": "2.0.0.10",           // 版本名称
  "versionCode": 200010,           // 版本号
  "fileDownloadUrl": "https://...", // 下载链接
  "mustUpdate": false,             // 是否强制更新
  "updateContent": "更新内容",      // 更新说明
  "title": "新版本可用",            // 标题
  "content": "详细描述"             // 详细内容
}

更新流程:
启动检查 → 获取JSON → 版本比较 → 显示对话框 → 下载APK → 引导安装
```

**关键技术**:
- **OkHttp**: 网络请求获取更新信息
- **JSON解析**: 解析更新配置
- **DownloadManager**: APK下载
- **PackageInstaller**: APK安装

### 实现目的

1. **及时更新** - 确保用户使用最新版本
2. **自动化** - 减少用户手动更新的麻烦
3. **灵活控制** - 强制/可选更新满足不同场景
4. **用户体验** - 清晰的更新提示和进度显示

---

## 8. 统计分析

### 功能描述

基于Firebase Analytics的用户行为分析和渠道统计。

### 核心功能

#### 8.1 Firebase Analytics集成
- Google Play Services检测
- Firebase初始化
- 用户属性设置
- 事件追踪

#### 8.2 收集数据
- **设备信息**: 语言、国家、设备型号
- **应用事件**: 屏幕访问、用户行为
- **用户属性**: 用户ID、首次打开时间

#### 8.3 渠道统计
- 渠道来源识别(CHANNEL_CODE)
- 安装渠道追踪
- 渠道统计分析
- 自动重试机制

#### 8.4 用户行为分析
- 工具栏浏览追踪
- 下载操作统计
- 搜索行为分析
- 用户交互追踪

#### 8.5 隐私保护
- 用户可禁用分析统计
- Google Play Services不可用时自动禁用
- 不收集敏感用户数据
- 数据加密传输

### 技术方案

```
统计分析架构:
┌──────────────────────────────────────┐
│      Analytics (统计分析入口)         │
└──────────────┬───────────────────────┘
               │
       ┌───────┴────────┬──────────────┬──────────────┐
       │                │              │              │
  FirebaseAnalytics ChannelTracker UserBehaviorAnalyzer AnalyticsTracker
       │                │              │              │
       ├─ 事件追踪       ├─ 渠道识别    ├─ 行为追踪    ├─ 性能指标
       ├─ 用户属性       ├─ 安装统计    ├─ 用户旅程    ├─ 自定义事件
       └─ 屏幕追踪       └─ 自动重试    └─ 交互分析    └─ 转化追踪

隐私控制:
Settings.getEnableAnalytics() → true/false
├─ true:  启用统计
└─ false: 禁用所有统计
```

**关键技术**:
- **Firebase Analytics 22.4.0**: Google分析平台
- **CHANNEL_CODE**: 渠道标识(gradle构建参数)
- **Bundle**: 事件参数传递
- **UserProperty**: 用户属性设置

### 实现目的

1. **数据驱动** - 了解用户行为指导产品优化
2. **渠道分析** - 评估不同渠道的用户质量
3. **性能监控** - 发现和解决性能问题
4. **隐私友好** - 用户可控制数据收集

---

## 9. 设置管理

### 功能描述

全局设置管理系统,包括账户、界面、浏览器、隐私等各类设置。

### 核心功能

#### 9.1 账户设置
- 用户名/密码
- Cookie管理
- 登录状态

#### 9.2 界面设置
- **主题系统**
  - Light主题
  - Dark主题
  - 自动切换(跟随系统)
  - 自定义颜色
- **语言设置**: 支持中文、英语、日语等10+语言
- **夜间模式**: 手动/自动切换

#### 9.3 浏览器设置
- JavaScript启用/禁用
- Cookie管理
- 缓存管理
- User-Agent自定义
- 字体大小调整
- DOM Storage控制

#### 9.4 网络设置
- 内置Hosts启用/禁用
- DNS缓存控制
- 代理设置
- 超时配置

#### 9.5 隐私设置
- 统计分析开关
- 隐私模式
- 浏览历史保存
- 自动清理

#### 9.6 下载设置
- 下载目录
- 并发下载数
- WiFi下载策略
- 存储空间管理

#### 9.7 性能设置
- 缓存大小
- 内存限制
- 预加载开关
- 硬件加速

### 技术方案

```
设置管理架构:
┌──────────────────────────────────────┐
│      Settings (设置管理核心)          │
└──────────────┬───────────────────────┘
               │
       ┌───────┴────────┬──────────────┬──────────────┐
       │                │              │              │
  SharedPreferences SettingsKeys  SettingsActivity  ThemeManager
       │                │              │              │
       ├─ 数据存储       ├─ 键定义      ├─ UI入口      ├─ 主题切换
       ├─ 读写操作       ├─ 默认值      ├─ Fragment    ├─ 颜色管理
       └─ 监听器         └─ 类型定义    └─ 验证逻辑    └─ 样式应用
               │
       ┌───────┴──────────────────┬──────────────┐
       │                          │              │
  BrowserSettingsAct      PrivacyFragment  DownloadFragment
       │                          │              │
       ├─ 浏览器配置               ├─ 隐私设置    ├─ 下载设置
       ├─ User-Agent              ├─ 数据清理    ├─ 目录选择
       └─ 字体调整                └─ 统计控制    └─ 并发控制

设置存储格式:
SharedPreferences (XML)
├─ key: "username"        value: "user123"
├─ key: "theme"           value: "dark"
├─ key: "enable_analytics" value: true
└─ ...
```

**关键技术**:
- **SharedPreferences**: Android偏好设置存储
- **PreferenceFragmentCompat**: 设置界面组件
- **Settings.java**: 统一的设置访问接口
- **SettingsKeys**: 常量化的设置键

### 实现目的

1. **用户个性化** - 允许用户自定义应用行为
2. **统一管理** - 集中管理所有设置项
3. **持久化存储** - 设置在应用重启后保持
4. **类型安全** - 统一的getter/setter接口

---

## 10. 安全与隐私

### 功能描述

全方位的安全和隐私保护,包括数据加密、隐私模式、生物识别等。

### 核心功能

#### 10.1 隐私保护
- **隐私模式**
  - 不保存浏览历史
  - 不保存搜索历史
  - 不保存下载历史
  - 退出时自动清除临时数据
  - 隐私窗口标识

- **数据清理**
  - 浏览历史清理
  - Cookie和Session清理
  - 缓存数据清理
  - 搜索历史清理
  - 下载日志清理

- **隐私WebView**
  - 禁用Cookie存储
  - 禁用缓存
  - 禁用LocalStorage
  - 禁用IndexedDB
  - 禁用Service Worker

#### 10.2 密码管理
- 密码保存提示
- 自动填充支持
- 密码加密存储
- 密码管理界面
- 按域名组织

#### 10.3 生物识别认证
- **支持类型**:
  - 指纹识别
  - 人脸识别
  - 虹膜识别
- **应用场景**:
  - 应用锁
  - 隐私内容保护

#### 10.4 权限管理
- **运行时权限**:
  - INTERNET - 网络访问
  - WRITE_EXTERNAL_STORAGE - 文件写入
  - READ_EXTERNAL_STORAGE - 文件读取
  - MANAGE_EXTERNAL_STORAGE - 完整文件访问(Android 11+)
  - ACCESS_NETWORK_STATE - 网络状态
  - REQUEST_INSTALL_PACKAGES - APK安装
  - USE_BIOMETRIC - 生物识别

- **权限引导**:
  - 渐进式权限请求
  - 权限说明对话框
  - 跨设备权限管理

#### 10.5 SSL/TLS安全
- **证书管理**:
  - 系统证书验证
  - 自定义证书支持
  - 证书固定(Certificate Pinning)
  - 过期证书处理

- **安全协议**:
  - TLS 1.2+
  - 现代加密套件
  - Perfect Forward Secrecy (PFS)

#### 10.6 数据保护
- 网络通信全部使用HTTPS
- 敏感数据加密存储
- 内存中敏感数据及时清理
- Proguard/R8代码混淆

### 技术方案

```
安全与隐私架构:
┌──────────────────────────────────────────────┐
│   PrivacyProtectionManager (隐私保护管理)     │
└──────────────┬───────────────────────────────┘
               │
       ┌───────┴────────┬──────────────┬──────────────┬──────────────┐
       │                │              │              │              │
  PrivacyModeMgr  PrivacyDataMgr  PrivacyWebViewMgr PasswordMgr  BiometricAuth
       │                │              │              │              │
       ├─ 模式控制       ├─ 数据清理    ├─ 隐私配置    ├─ 密码保存    ├─ 指纹识别
       ├─ 历史控制       ├─ Cookie     ├─ 禁用存储    ├─ 自动填充    ├─ 人脸识别
       └─ 自动清理       └─ 缓存        └─ 隐私标识    └─ 加密存储    └─ 应用锁
               │
       ┌───────┴──────────────────┬──────────────┐
       │                          │              │
  PermissionManager       EhX509TrustManager  EhSSLSocketFactory
       │                          │              │
       ├─ 权限检查                 ├─ 证书验证    ├─ TLS 1.2+
       ├─ 权限申请                 ├─ 证书固定    ├─ 加密套件
       ├─ 渐进式请求               └─ 信任管理    └─ PFS支持
       └─ 权限引导

安全最佳实践:
1. 网络通信: HTTPS Only
2. 本地存储: 加密敏感数据
3. 内存管理: 及时清理
4. 代码保护: Proguard混淆
```

**关键技术**:
- **BiometricPrompt**: Android生物识别API
- **EncryptedSharedPreferences**: 加密的SharedPreferences
- **SSLSocketFactory**: SSL/TLS配置
- **X509TrustManager**: 证书信任管理
- **Proguard/R8**: 代码混淆和优化

### 实现目的

1. **保护隐私** - 用户数据不被泄露和追踪
2. **数据安全** - 敏感数据加密存储和传输
3. **用户控制** - 用户完全控制隐私设置
4. **合规要求** - 满足GDPR等隐私法规

---

## 11. 辅助模块

### 11.1 通知模块

**核心功能**:
- 下载进度通知
- Firebase Cloud Messaging (FCM)推送
- 后台任务通知
- WiFi状态监控通知

**关键类**:
- `NotificationManager` - 通知管理
- `SmartNotificationManager` - 智能通知
- `FirebaseManager` - FCM管理
- `PushMessageService` - 推送服务
- `TaskTriggerService` - 任务触发
- `WifiMonitorService` - WiFi监控

**技术方案**:
```
通知系统:
NotificationManager → NotificationChannel → Notification
├─ 下载通知: CHANNEL_DOWNLOAD
├─ 推送通知: CHANNEL_PUSH
└─ 系统通知: CHANNEL_SYSTEM
```

### 11.2 性能优化模块

**核心功能**:
- WebView内存管理
- 性能监控和优化
- 预加载优化
- 自适应内存管理

**关键类**:
- `WebViewMemoryManager` - WebView内存管理
- `OptimizedWebViewManager` - 性能优化
- `WebViewPreloader` - WebView预加载
- `AdaptiveMemoryManager` - 自适应内存

**优化策略**:
- 内存动态调整(基于设备内存)
- 缓存优化(LRU + 磁盘缓存)
- WebView池管理(预创建和复用)
- 预加载优化(启动时预加载)

### 11.3 推荐系统

**核心功能**:
- 智能推荐引擎
- 个性化推荐
- 基于用户行为的推荐

**关键类**:
- `SmartRecommendationEngine` - 推荐引擎
- `PersonalizedRecommendationEngine` - 个性化推荐

**技术方案**:
- 协同过滤算法
- 内容推荐算法
- 用户画像构建

### 11.4 缓存管理模块

**核心功能**:
- 网络缓存
- 启动缓存
- WebView缓存
- 地址栏缓存

**关键类**:
- `NetworkCacheManager` - 网络缓存
- `StartupCacheManager` - 启动缓存
- `WebViewCacheInterceptor` - WebView缓存拦截
- `YCWebViewCacheInterceptor` - X5缓存拦截
- `AddressBarCache` - 地址栏缓存

---

## 技术栈总览

### 核心依赖

```kotlin
// 网络层
implementation("com.squareup.okhttp3:okhttp:3.14.7")
implementation("org.jsoup:jsoup:1.15.3")

// 浏览器引擎
implementation("com.tencent.tbs:tbssdk:44286")

// 数据层
implementation("org.greenrobot:greendao:3.0.0")

// UI组件
implementation("androidx.appcompat:appcompat:1.7.0")
implementation("com.google.android.material:material:1.12.0")

// 分析统计
implementation("com.google.firebase:firebase-analytics:22.4.0")
```

### 架构模式

- **分层架构**: UI层 → 业务逻辑层 → 数据层
- **单例模式**: Manager类统一采用单例
- **观察者模式**: 状态变化通知
- **策略模式**: 网络策略、缓存策略
- **工厂模式**: WebView创建、Dao创建

### 开发语言

- **Java**: 主要业务逻辑(约70%)
- **Kotlin**: 新功能和服务(约30%)

---

## 模块依赖关系图

```
┌─────────────────────────────────────────────┐
│        EhApplication (应用主类)              │
└──────────────┬──────────────────────────────┘
               │
       ┌───────┴────────────┬────────────┬──────────────┬─────────┐
       │                    │            │              │         │
    Settings         EhDB(数据)     网络模块        更新模块    统计模块
       │         (GreenDAO)          │              │         │
       │              │              │              │         │
       │        ┌──────┴──────┐      │              │         │
       │        │             │      │              │         │
    偏好      下载表      工具栏表   ├─ EhHosts      AppUpdater  Analytics
    设置      历史表      书签表   ├─ BandwidthMgr UpdateDialog ChannelTracker
    主题      过滤表      缓存表   ├─ ConnectionMgr            UserBehavior
                                  └─ ResourceInterceptor
       │
       │        ┌──────────────────────────────────────┐
       │        │                                      │
    浏览器模块────┤   文件管理模块──────┬── 下载模块      │
       │        │                     │        │       │
       │    X5Manager   FileIndexer   │   DownloadMgr  安全隐私
       │    CompatMgr   FileManager   │   DownloadSvc  │
       │    EnhancedWV  ApkInstaller  │   EnhancedDL   │
       │    TabManager  Media Player  │   SpiderQueen  ├─ Privacy
       │    UserScript  ───────────────┴─ DownloadInfo │
       │    Injection                       TorrentMgr  └─ Security
       │                                                   PasswordMgr
       │                                                   SSL/TLS
       │                                                   BiometricAuth
       │
       └───────────────────────────────────────────────────────────
```

---

## 功能特点总结

### 高性能
- WebView预加载和池化管理
- 多层缓存优化(内存+数据库+磁盘)
- 基于设备内存的自适应调整
- 图片加载和解码优化

### 高稳定性
- 网络重试和熔断机制
- 异常捕获和优雅降级
- 前台服务保活
- 崩溃日志收集

### 安全隐私
- 全站HTTPS通信
- 数据加密存储
- 隐私模式和数据清理
- 生物识别认证

### 用户体验
- 现代化Material Design界面
- 10+语言国际化支持
- Light/Dark主题自动切换
- 丰富的个性化设置

### 扩展性
- Tampermonkey脚本支持
- 用户自定义脚本
- 模块化架构设计
- 清晰的代码结构

---

## 优化建议

### 技术栈升级

1. **数据库**: GreenDAO → Room ORM (官方推荐)
2. **网络**: OkHttp 3.14.7 → 最新版本
3. **WebView**: X5 → Android System WebView (长期规划)
4. **设置**: SharedPreferences → DataStore (官方推荐)
5. **异步**: AsyncTask → Coroutines/Flow

### 功能增强

1. **浏览器**: 增强广告拦截、书签同步
2. **下载**: 支持更多协议(FTP、磁力链接)
3. **文件**: 支持云存储(Google Drive、OneDrive)
4. **脚本**: 扩展脚本市场和社区
5. **更新**: 灰度发布和A/B测试

### 安全加固

1. APP证书固定(Certificate Pinning)
2. 设备指纹识别
3. 数据库加密(SQLCipher)
4. 反调试和反篡改
5. 代码混淆优化

### 测试覆盖

1. 增加单元测试覆盖率(目标80%+)
2. UI自动化测试(Espresso)
3. 性能测试和基准测试
4. 安全扫描(静态分析)
5. 兼容性测试(多设备)

---

## 附录: 文件位置索引

### 浏览器引擎
- `/app/src/main/java/com/hippo/gongjulan/browser/`
- `/app/src/main/java/com/hippo/gongjulan/client/X5WebViewManager.java`
- `/app/src/main/java/com/hippo/gongjulan/ui/browser/`

### 文件管理
- `/app/src/main/java/com/hippo/gongjulan/file/`
- `/app/src/main/java/com/hippo/gongjulan/ui/FileManagerActivity.java`
- `/app/src/main/java/com/hippo/gongjulan/ui/ApkInstallerActivity.java`

### 下载管理
- `/app/src/main/java/com/hippo/gongjulan/download/`
- `/app/src/main/java/com/hippo/gongjulan/download/DownloadManager.java`
- `/app/src/main/java/com/hippo/gongjulan/download/DownloadService.kt`

### 用户脚本
- `/app/src/main/java/com/hippo/gongjulan/userscript/`
- `/app/src/main/java/com/hippo/gongjulan/userscript/UserScriptManager.java`

### 网络管理
- `/app/src/main/java/com/hippo/gongjulan/network/`
- `/app/src/main/java/com/hippo/gongjulan/client/EhHosts.java`
- `/app/src/main/java/com/hippo/gongjulan/client/BandwidthManager.java`

### 数据库
- `/app/src/main/java/com/hippo/gongjulan/EhDB.java`
- `/app/src/main/java/com/hippo/gongjulan/dao/`
- `/app/src/main/java/com/hippo/database/MSQLiteOpenHelper.java`

### 更新模块
- `/app/src/main/java/com/hippo/gongjulan/updater/AppUpdater.kt`
- `/app/src/main/java/com/hippo/gongjulan/ui/dialog/UpdateDialog.kt`

### 统计分析
- `/app/src/main/java/com/hippo/gongjulan/Analytics.java`
- `/app/src/main/java/com/hippo/gongjulan/analytics/`

### 设置管理
- `/app/src/main/java/com/hippo/gongjulan/Settings.java`
- `/app/src/main/java/com/hippo/gongjulan/settings/SettingsKeys.java`
- `/app/src/main/java/com/hippo/gong ju lan/ui/fragment/` (设置Fragment)

### 安全隐私
- `/app/src/main/java/com/hippo/gongjulan/security/`
- `/app/src/main/java/com/hippo/gongjulan/privacy/`
- `/app/src/main/java/com/hippo/gongjulan/ui/browser/PasswordManager.java`

---

<div align="center">

**gongjulan 功能清单文档** - 版本2.0.0
基于详细代码分析生成 | 2025-11-11

[⬆ 回到顶部](#gongjulan-功能清单文档)

</div>
