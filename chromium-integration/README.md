# 蓝河助手 - Chromium文件管理集成

## 项目概述

基于Chromium开源项目的蓝河助手文件管理功能实现，将强大的文件管理能力与现代浏览器内核完美融合。

## 核心特色

### 🌟 必胜功能：完整文件管理
- **文件浏览**: 完整的目录浏览、搜索、排序功能
- **文件操作**: 复制、移动、删除、重命名、压缩包处理
- **多媒体预览**: 图片、视频、音频、文本文件预览
- **APK管理**: 智能APK检测、验证、安装引导
- **权限适配**: Android 6.0+ 到 Android 15+ 全版本适配

### 🚀 Chromium集成优势
- **现代渲染**: 最顶级的网页渲染能力
- **扩展生态**: 支持Chrome扩展和自定义插件
- **性能卓越**: 硬件加速、多线程处理
- **标准兼容**: 完整的Web标准支持

## 技术架构

```
lanhe-assistant/
├── app/                          # 主应用模块
│   ├── src/main/
│   │   ├── java/lanhe/browser/    # Chromium集成
│   │   ├── java/lanhe/filesystem/ # 文件系统核心
│   │   ├── java/lanhe/media/      # 多媒体处理
│   │   ├── java/lanhe/apk/        # APK管理
│   │   └── assets/web/            # Web界面资源
├── chromium/                      # Chromium源码和构建
├── native/                        # C++原生代码
└── modules/                       # 功能模块
```

## 开发环境

- **Android Studio**: Arctic Fox+
- **Kotlin**: 2.0.21
- **NDK**: r25c+
- **Chromium**: Stable分支
- **Gradle**: 8.12.1

## 快速开始

```bash
# 克隆项目
git clone <repository-url>
cd lanhe-assistant

# 构建Chromium组件
./build-chromium.sh

# 编译Android应用
./gradlew assembleDebug

# 运行测试
./gradlew test
```

## 核心功能

### 文件管理API
```kotlin
// 文件浏览
val files = fileManager.listFiles("/sdcard/")
val searchResults = fileManager.searchFiles("*.jpg", "/sdcard/")

// 文件操作
fileManager.copyFile("/src/path", "/dest/path")
fileManager.moveFile("/src/path", "/dest/path")
fileManager.deleteFile("/path/to/file")

// 多媒体预览
mediaManager.previewImage("/path/to/image.jpg", webView)
mediaManager.playVideo("/path/to/video.mp4", webView)

// APK安装
apkInstaller.installAPK("/path/to/app.apk")
```

### Chromium扩展
```javascript
// 文件管理JavaScript API
window.lanheFileManager = {
    browseDirectory: (path) => nativeAPI.browseDirectory(path),
    previewFile: (path) => nativeAPI.previewFile(path),
    installAPK: (path) => nativeAPI.installAPK(path),
    searchFiles: (query, path) => nativeAPI.searchFiles(query, path)
};
```

## 开发指南

详细的开发文档请参考：
- [Chromium集成指南](./docs/chromium-integration.md)
- [文件管理API](./docs/file-api.md)
- [多媒体处理](./docs/media-handling.md)
- [APK管理功能](./docs/apk-management.md)

## 贡献

欢迎贡献代码和想法！请查看 [CONTRIBUTING.md](./CONTRIBUTING.md) 了解详细信息。

## 许可证

本项目基于 MIT 许可证开源。