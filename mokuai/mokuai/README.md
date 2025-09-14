# 📦 蓝河工具箱模块库 (LanHe Toolbox Module Library)

## 🎯 项目概述

蓝河工具箱模块库是一个完整的Android模块化解决方案，基于[Pro模块生成与引入标准规范](./pro_module_standard.md)构建。该项目基于Chromium浏览器引擎，将蓝河工具箱的核心功能模块化，提供标准化、可复用的模块组件。

## 🏗️ 项目结构

```
mokuai/
├── pro_module_standard.md     # 模块生成与引入标准规范
├── README.md                  # 项目总文档
├── docs/                      # 项目文档目录
├── examples/                  # 使用示例
│   ├── NetworkExample.java           # 网络模块使用示例
│   ├── DatabaseExample.java          # 数据库模块使用示例
│   ├── ModuleIntegrationExample.java # 大模块集成示例
│   └── SmallModuleIntegrationExample.java # 小模块集成示例
├── main-base/                 # 快速开发底座项目
│   ├── README.md              # 底座项目文档
│   ├── app/                   # 主应用模块
│   ├── libraries/             # 模块库目录
│   ├── build.gradle.kts       # 根构建配置
│   ├── settings.gradle.kts    # 项目设置
│   └── gradle.properties      # 全局配置
└── modules/                   # 核心功能模块
    ├── network/               # 网络模块
    ├── database/              # 数据库模块
    ├── ui/                    # UI模块
    ├── utils/                 # 工具模块
    ├── settings/              # 设置管理模块
    ├── notification/          # 通知模块
    ├── image/                 # 图片处理模块
    ├── filesystem/            # 文件系统模块
    ├── analytics/             # 数据分析模块
    ├── crash/                 # 崩溃处理模块
    ├── password-manager/      # 密码管理模块
    ├── bookmark-manager/      # 书签管理模块
    ├── ad-blocker/            # 广告拦截模块
    ├── proxy-selector/        # 代理选择器模块
    ├── performance-monitor/   # 性能监控模块
    ├── memory-manager/        # 内存管理模块
    ├── security-manager/      # 安全管理模块
    ├── image-helper/          # 图片助手模块
    ├── text-extractor/        # 文本提取模块
    └── url-opener/            # URL打开器模块
```

## 📦 核心模块

### 1. 网络模块 (Network Module)
- **位置**: `modules/network/`
- **功能**: HTTP请求、Cookie管理、SSL证书验证
- **文档**: [网络模块文档](./modules/network/README.md)

### 2. 数据库模块 (Database Module)
- **位置**: `modules/database/`
- **功能**: SQLite数据库操作、数据迁移、查询优化
- **文档**: [数据库模块文档](./modules/database/README.md)

### 3. UI模块 (UI Module)
- **位置**: `modules/ui/`
- **功能**: Activity管理、Fragment组件、自定义控件
- **文档**: [UI模块文档](./modules/ui/README.md)

### 4. 工具模块 (Utils Module)
- **位置**: `modules/utils/`
- **功能**: 通用工具类、辅助方法
- **文档**: [工具模块文档](./modules/utils/README.md)

### 5. 设置管理模块 (Settings Module)
- **位置**: `modules/settings/`
- **功能**: 应用配置管理、偏好设置
- **文档**: [设置模块文档](./modules/settings/README.md)

### 6. 通知模块 (Notification Module)
- **位置**: `modules/notification/`
- **功能**: 系统通知、推送消息
- **文档**: [通知模块文档](./modules/notification/README.md)

### 7. 图片处理模块 (Image Module)
- **位置**: `modules/image/`
- **功能**: 图片加载、缓存、处理
- **文档**: [图片模块文档](./modules/image/README.md)

### 8. 文件系统模块 (Filesystem Module)
- **位置**: `modules/filesystem/`
- **功能**: 文件操作、存储管理
- **文档**: [文件系统模块文档](./modules/filesystem/README.md)

## 🔧 小功能模块

除了上述8个大模块外，我们还提供了14个专注特定功能的小模块，可以根据项目需求单独引入：

### 数据分析系列
| 模块名称 | 功能说明 | 适用场景 |
|---------|---------|---------|
| **Analytics** | Firebase数据分析集成、用户行为跟踪 | 产品数据分析、用户研究 |
| **Crash Handler** | 崩溃检测、错误日志、设备信息收集 | 应用稳定性监控、错误诊断 |
| **Performance Monitor** | CPU/内存/网络性能监控 | 性能优化、问题诊断 |

### 安全管理系列
| 模块名称 | 功能说明 | 适用场景 |
|---------|---------|---------|
| **Password Manager** | AES加密存储、生物识别认证、自动填充 | 用户认证管理、密码安全 |
| **Security Manager** | 数据加密、网络安全、权限管理、反调试 | 应用安全加固、数据保护 |
| **Memory Manager** | 内存监控、泄漏检测、缓存优化 | 内存优化、稳定性提升 |

### 内容处理系列
| 模块名称 | 功能说明 | 适用场景 |
|---------|---------|---------|
| **Bookmark Manager** | 书签管理、分类标签、搜索功能 | 内容收藏、快速访问 |
| **Ad Blocker** | 广告过滤、规则管理、统计报告 | 改善用户体验、隐私保护 |
| **Image Helper** | 图片加载、缓存、压缩、格式转换 | 图片显示优化、内存管理 |
| **Text Extractor** | 文本提取、内容分析、格式转换 | 内容处理、数据提取 |

### 网络增强系列
| 模块名称 | 功能说明 | 适用场景 |
|---------|---------|---------|
| **Proxy Selector** | 多协议代理、智能选择、负载均衡 | 网络优化、安全访问 |
| **URL Opener** | 智能URL处理、应用内打开、外部浏览器 | URL管理、应用跳转 |

### 小模块特点
- ✅ **功能单一**: 每个模块专注于一个特定功能
- ✅ **独立性强**: 可以单独引入，不依赖其他模块
- ✅ **集成简单**: 标准化的API接口，易于集成
- ✅ **资源占用少**: 轻量级实现，最小化对应用的影响
- ✅ **配置灵活**: 丰富的配置选项，支持自定义

### 小模块选择指南

**根据项目需求选择小模块：**
- 需要用户行为分析 → Analytics + Performance Monitor
- 注重应用安全性 → Password Manager + Security Manager
- 优化用户体验 → Ad Blocker + Image Helper
- 提升应用稳定性 → Crash Handler + Memory Manager

**查看完整小模块列表**: [小模块详细列表](./modules/SMALL_MODULES_LIST.md)

## 🚀 快速开始

### 使用Main底座项目

1. **克隆项目**
```bash
git clone <repository-url>
cd mokuai/main-base
```

2. **配置项目**
```bash
# 复制gradle配置
cp gradle/wrapper/gradle-wrapper.properties.backup gradle/wrapper/gradle-wrapper.properties

# 配置本地属性
echo "sdk.dir=/path/to/your/android/sdk" > local.properties
```

3. **同步项目**
```bash
./gradlew sync
```

4. **运行项目**
```bash
./gradlew installDebug
```

### 单独引入模块

#### 引入大模块
```gradle
dependencies {
    // 引入网络模块
    implementation 'com.hippo.ehviewer:network:1.0.0'

    // 引入数据库模块
    implementation 'com.hippo.ehviewer:database:1.0.0'

    // 引入UI模块
    implementation 'com.hippo.ehviewer:ui:1.0.0'
}
```

#### 引入小模块
```gradle
dependencies {
    // 引入数据分析模块
    implementation 'com.hippo.ehviewer:analytics:1.0.0'

    // 引入密码管理模块
    implementation 'com.hippo.ehviewer:password-manager:1.0.0'

    // 引入崩溃处理模块
    implementation 'com.hippo.ehviewer:crash-handler:1.0.0'

    // 引入性能监控模块
    implementation 'com.hippo.ehviewer:performance-monitor:1.0.0'
}
```

## 📋 使用示例

### 网络请求示例
```java
// 初始化网络管理器
NetworkManager manager = NetworkManager.getInstance(context);

// 发送GET请求
manager.get("https://api.example.com/data")
    .enqueue(new INetworkCallback<String>() {
        @Override
        public void onSuccess(String result) {
            Log.d(TAG, "Response: " + result);
        }

        @Override
        public void onFailure(Exception error) {
            Log.e(TAG, "Error: " + error.getMessage());
        }
    });
```

### 数据库操作示例
```java
// 初始化数据库管理器
DatabaseManager dbManager = DatabaseManager.getInstance(context);

// 获取DAO
DownloadInfoDao dao = dbManager.getDao(DownloadInfoDao.class);

// 插入数据
DownloadInfo info = new DownloadInfo();
info.setTitle("Sample Gallery");
dao.insert(info);

// 查询数据
List<DownloadInfo> results = dao.loadAll();
```

### UI组件使用示例
```java
// 继承BaseActivity
public class MainActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 显示消息
        showMessage("Welcome!");
    }
}
```

## 🛠️ 开发指南

### 模块开发规范

1. **遵循标准结构**: 参考[Pro模块规范](./pro_module_standard.md)
2. **标准化接口**: 提供统一的接口和回调
3. **完整文档**: 为每个模块编写详细的README
4. **单元测试**: 保证代码质量和稳定性
5. **示例代码**: 提供完整的使用示例

### 代码质量要求

- **测试覆盖率**: 核心代码 >= 80%
- **代码风格**: 遵循Kotlin/Java官方规范
- **文档完整性**: 公开API必须有JavaDoc/KDoc
- **向后兼容**: 保证API的向后兼容性

### 构建和发布

```bash
# 构建所有模块
./gradlew :modules:network:build
./gradlew :modules:database:build

# 运行测试
./gradlew testAll

# 生成文档
./gradlew dokkaAll

# 发布到仓库
./gradlew publishAll
```

## 📊 版本管理

### 版本号规范
采用[语义化版本](https://semver.org/)格式：
- **MAJOR.MINOR.PATCH** (如: 1.2.3)
- **MAJOR**: 破坏性变更
- **MINOR**: 新功能
- **PATCH**: 修复

### 发布流程
1. 更新版本号
2. 运行完整测试
3. 生成发布说明
4. 创建Git标签
5. 发布到Maven仓库

## 🤝 贡献指南

### 开发流程
1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

### 代码规范
- 遵循现有的代码风格
- 提交前运行所有测试
- 更新相关文档
- 添加必要的注释

### 模块贡献
- 新模块必须遵循[模块规范](./pro_module_standard.md)
- 提供完整的文档和示例
- 包含单元测试和集成测试

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情

## 📞 支持与联系

- 📧 邮箱: support@ehviewer.com
- 📖 文档: [完整文档](https://docs.ehviewer.com/mokuai/)
- 🐛 问题跟踪: [GitHub Issues](https://github.com/ehviewer/ehviewer/issues)
- 💬 讨论: [GitHub Discussions](https://github.com/ehviewer/ehviewer/discussions)
- 📱 Telegram: [@ehviewer](https://t.me/ehviewer)

## 🙏 致谢

感谢所有为蓝河工具箱项目做出贡献的开发者！

特别感谢：
- Hippo Seven (原项目作者)
- 所有社区贡献者
- 开源社区的支持

---

## 🎯 项目目标

- **模块化**: 将复杂项目分解为可管理的模块
- **标准化**: 建立统一的开发和使用规范
- **复用性**: 提高代码的可复用性和维护性
- **易用性**: 降低模块的使用门槛和学习成本
- **扩展性**: 支持灵活的功能扩展和定制

## 📊 模块化解决方案总览

蓝河工具箱模块化项目提供了**8个大模块 + 14个小模块**的完整解决方案：

### 大模块 (Core Modules)
专注于完整功能子系统的模块化组件：
- **Network**: 完整的网络通信解决方案
- **Database**: 全功能的数据存储管理
- **UI**: 完整的用户界面框架
- **Utils**: 基础工具类集合
- **Settings**: 配置管理子系统
- **Notification**: 通知管理系统
- **Image**: 图片处理子系统
- **Filesystem**: 文件系统管理

### 小模块 (Small Modules)
专注于特定功能的微模块组件：
- **数据分析系列**: Analytics, Crash Handler, Performance Monitor
- **安全管理系列**: Password Manager, Security Manager, Memory Manager
- **内容处理系列**: Bookmark Manager, Ad Blocker, Image Helper, Text Extractor
- **网络增强系列**: Proxy Selector, URL Opener

### 模块化优势

| 特性 | 大模块 | 小模块 | 说明 |
|------|--------|--------|------|
| **功能范围** | 广 | 窄 | 大模块功能全面，小模块功能单一 |
| **集成复杂度** | 高 | 低 | 小模块更容易集成和维护 |
| **资源占用** | 多 | 少 | 小模块更加轻量级 |
| **定制性** | 中 | 高 | 小模块更易于定制和扩展 |
| **依赖关系** | 复杂 | 简单 | 小模块依赖关系更清晰 |
| **适用场景** | 企业级应用 | 快速原型 | 根据项目规模选择合适的模块 |

### 选择指南

#### 新项目开发
1. **快速原型**: 从Main底座项目开始，预集成了所有模块
2. **功能定制**: 根据需求选择合适的小模块组合
3. **渐进式开发**: 从核心功能开始，逐步添加其他模块

#### 现有项目集成
1. **评估需求**: 分析现有项目缺少哪些功能
2. **选择模块**: 从小模块开始，逐步引入大模块
3. **平滑迁移**: 确保新模块与现有代码的兼容性

#### 模块组合推荐
- **基础应用**: Network + Database + UI + Analytics
- **社交应用**: 上述 + Password Manager + Notification + Image Helper
- **内容应用**: 上述 + Bookmark Manager + Ad Blocker + Text Extractor
- **工具应用**: 上述 + Proxy Selector + Security Manager + Performance Monitor

### 技术特点

- ✅ **标准化**: 统一的模块开发规范和API设计
- ✅ **高质量**: 完整的文档、测试和代码质量保证
- ✅ **易维护**: 模块化架构便于维护和升级
- ✅ **高性能**: 优化的实现，最小化性能影响
- ✅ **安全可靠**: 内置安全机制和错误处理
- ✅ **向后兼容**: 保证API的稳定性

### 未来规划

**短期目标 (v1.0)**:
- [x] 完成所有核心模块开发
- [x] 建立完整的文档体系
- [x] 提供丰富的集成示例
- [ ] 完善自动化测试覆盖

**中期目标 (v1.5)**:
- [ ] 增加更多专用模块
- [ ] 优化模块间的集成方式
- [ ] 提供可视化配置工具
- [ ] 增强国际化支持

**长期目标 (v2.0)**:
- [ ] 实现模块热更新机制
- [ ] 支持自定义模块开发
- [ ] 提供云端配置服务
- [ ] 构建完整的模块生态系统

通过蓝河工具箱模块库，我们希望为Android社区提供一套高质量、可复用的模块化解决方案！ 🚀
