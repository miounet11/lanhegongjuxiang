# 🖼️ 图片助手模块 (Image Helper Module)

## 🎯 概述

蓝河工具箱图片助手模块提供强大的图片处理功能，包括图片加载、缓存、压缩、格式转换等，帮助优化图片显示性能和用户体验。基于Chromium图片处理规范实现。

## ✨ 主要特性

- ✅ **高效加载**：支持多种图片格式的高效加载
- ✅ **智能缓存**：LRU缓存策略，自动内存管理
- ✅ **图片压缩**：智能压缩，平衡质量和大小
- ✅ **格式转换**：支持多种图片格式转换
- ✅ **缩放处理**：高质量图片缩放和裁剪
- ✅ **内存优化**：防止内存溢出，自动回收
- ✅ **加载优化**：渐进式加载、预加载支持
- ✅ **错误处理**：完善的错误处理和降级策略

## 🚀 快速开始

### 初始化图片助手

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化图片助手
        ImageHelper.initialize(this);
    }
}
```

### 加载图片

```java
// 加载网络图片
ImageHelper.getInstance()
    .load(url)
    .placeholder(R.drawable.placeholder)
    .error(R.drawable.error)
    .resize(300, 300)
    .centerCrop()
    .into(imageView);

// 加载本地图片
ImageHelper.getInstance()
    .load(file)
    .circleCrop()
    .into(imageView);
```

### 图片处理

```java
// 压缩图片
ImageHelper.getInstance()
    .compress(imageFile, 1024 * 1024) // 压缩到1MB
    .setQuality(80) // 质量80%
    .setFormat(ImageFormat.JPEG)
    .compress(new CompressCallback() {
        @Override
        public void onSuccess(File compressedFile) {
            uploadImage(compressedFile);
        }

        @Override
        public void onError(String error) {
            showError(error);
        }
    });
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `ImageHelper` | 图片助手核心类 |
| `ImageLoader` | 图片加载器 |
| `ImageCompressor` | 图片压缩器 |
| `ImageCache` | 图片缓存管理器 |

### 主要方法

#### ImageHelper

```java
// 初始化图片助手
void initialize(Context context)

// 获取单例实例
ImageHelper getInstance()

// 加载图片
ImageRequest load(String url)
ImageRequest load(File file)
ImageRequest load(int resourceId)

// 压缩图片
CompressRequest compress(File file, long maxSize)

// 清理缓存
void clearCache()

// 获取缓存大小
long getCacheSize()

// 设置缓存大小限制
void setCacheSizeLimit(long size)
```

## 📦 依赖项

```gradle
dependencies {
    // Glide图片加载库
    implementation 'com.github.bumptech.glide:glide:4.16.0'

    // 蓝河工具箱图片助手模块
    implementation 'com.hippo.ehviewer:image-helper:1.0.0'
}
```

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
