# 蓝河助手测试体系指南

## 概述

蓝河助手建立了完整的测试体系，包括单元测试、集成测试、UI测试和性能测试，确保代码质量和系统稳定性。

## 测试架构

### 1. 单元测试 (Unit Tests)
**位置**: `app/src/test/`
- **目的**: 测试独立的类和方法
- **框架**: JUnit 4 + Mockito + Robolectric
- **覆盖率目标**: 80%+

#### 核心测试组件
- `TestBase.kt` - 测试基类，提供通用测试工具
- `TestDataFactory.kt` - 测试数据工厂
- `ShizukuManagerTest.kt` - Shizuku权限管理器测试
- `PerformanceMonitorManagerTest.kt` - 性能监控管理器测试
- `AppDatabaseTest.kt` - Room数据库测试

#### 运行单元测试
```bash
# 运行所有单元测试
./gradlew testDebugUnitTest

# 运行特定测试类
./gradlew testDebugUnitTest --tests "com.lanhe.gongjuxiang.utils.ShizukuManagerTest"

# 运行特定测试方法
./gradlew testDebugUnitTest --tests "*.ShizukuManagerTest.test Shizuku availability check*"
```

### 2. 集成测试 (Integration Tests)
**位置**: `app/src/androidTest/`
- **目的**: 测试组件间交互和Android系统集成
- **框架**: AndroidX Test + Espresso + Robolectric
- **设备要求**: Android模拟器或真实设备

#### 核心集成测试
- `MainActivityIntegrationTest.kt` - 主界面交互测试
- `ShizukuIntegrationTest.kt` - Shizuku框架集成测试
- `DatabaseIntegrationTest.kt` - 数据库集成测试

#### 运行集成测试
```bash
# 运行所有集成测试
./gradlew connectedDebugAndroidTest

# 运行特定集成测试
./gradlew connectedDebugAndroidTest --tests "*.MainActivityIntegrationTest"
```

### 3. 性能测试 (Performance Tests)
**位置**: `app/src/androidTest/java/com/lanhe/gongjuxiang/performance/`
- **目的**: 测试应用性能和稳定性
- **测试内容**: 内存使用、CPU性能、数据库性能、并发访问

#### 性能测试指标
- 内存泄漏检测
- CPU使用率监控
- 数据库查询性能
- 并发访问稳定性

#### 运行性能测试
```bash
./gradlew connectedDebugAndroidTest --tests "*.PerformanceTest"
```

## 测试配置

### 测试覆盖率
使用JaCoCo生成测试覆盖率报告：
```bash
# 生成单元测试覆盖率
./gradlew jacocoTestReport

# 生成集成测试覆盖率
./gradlew jacocoAndroidTestReport

# 生成合并覆盖率报告
./gradlew jacocoMergedReport
```

覆盖率报告位置：
- HTML报告: `app/build/reports/jacoco/jacocoTestReport/html/index.html`
- XML报告: `app/build/reports/jacoco/jacocoTestReport.xml`

### 测试环境配置

#### Robolectric配置 (`app/src/test/resources/robolectric.properties`)
```
sdk=28,29,30
manifest=src/main/AndroidManifest.xml
package=com.lanhe.gongjuxiang
```

#### Gradle测试配置 (`gradle/test-coverage.gradle`)
- Jacoco插件配置
- 测试覆盖率设置
- 报告生成规则

## CI/CD集成

### GitHub Actions流水线
**文件**: `.github/workflows/test.yml`

#### 流水线阶段
1. **测试阶段**
   - 运行单元测试
   - 运行Lint检查
   - 生成测试覆盖率
   - 上传测试报告

2. **构建检查**
   - 构建Debug APK
   - 验证APK完整性
   - 上传构建产物

3. **安全扫描**
   - 检查敏感信息泄露
   - 验证安全配置

#### 触发条件
- 推送到`main`或`develop`分支
- 创建针对`main`分支的Pull Request

## 测试最佳实践

### 1. 测试命名规范
- 使用描述性的测试方法名
- 格式: `test [场景] when [条件] should [期望结果]`
- 示例: `test Shizuku availability check when Shizuku is not installed should return false`

### 2. 测试结构 (AAA模式)
```kotlin
@Test
fun testMethodName() {
    // Arrange - 准备测试数据和环境
    val testData = TestDataFactory.createTestData()
    
    // Act - 执行被测试的操作
    val result = systemUnderTest.doSomething(testData)
    
    // Assert - 验证结果
    assertEquals(expectedResult, result)
}
```

### 3. Mock使用
- 使用Mockito模拟外部依赖
- 避免测试中的真实网络调用和数据库操作
- 验证交互而不是实现细节

### 4. 异步测试
- 使用Coroutines测试框架
- 使用`runTest`和`TestScope`
- 正确处理协程调度器

### 5. 测试数据管理
- 使用`TestDataFactory`创建一致的测试数据
- 避免硬编码测试数据
- 使用有意义的边界值

## 测试报告

### 本地测试报告
运行测试后，可以在以下位置查看报告：
- 单元测试: `app/build/reports/tests/testDebugUnitTest/index.html`
- 集成测试: `app/build/reports/androidTests/connected/index.html`
- 覆盖率: `app/build/reports/jacoco/`

### CI测试报告
GitHub Actions会自动生成和上传测试报告：
- 测试结果
- Lint报告
- 覆盖率报告
- APK构建产物

## 故障排除

### 常见问题

#### 1. 测试运行缓慢
**问题**: 单元测试运行时间过长
**解决方案**:
- 使用Robolectric替代模拟器
- 减少不必要的数据库操作
- 优化Mock对象使用

#### 2. 集成测试不稳定
**问题**: 测试结果不一致
**解决方案**:
- 添加适当的等待时间
- 使用`IdlingResource`处理异步操作
- 确保测试间的隔离

#### 3. 覆盖率不准确
**问题**: 覆盖率报告与实际情况不符
**解决方案**:
- 检查JaCoCo配置
- 排除不相关的文件（R.class、BuildConfig等）
- 确保所有测试都运行

#### 4. Shizuku测试失败
**问题**: Shizuku相关测试在没有Shizuku环境的CI中失败
**解决方案**:
- 使用Mock模拟Shizuku API
- 添加环境检测和fallback逻辑
- 在CI中跳过需要真实Shizuku的测试

## 扩展测试

### 添加新的测试用例

1. **单元测试**
```kotlin
@Test
fun `test new feature should behave correctly`() = runTest {
    // Arrange
    val testData = TestDataFactory.createTestData()
    
    // Act
    val result = featureUnderTest.process(testData)
    
    // Assert
    assertNotNull(result)
    assertEquals(expectedValue, result.property)
}
```

2. **集成测试**
```kotlin
@Test
fun testNewFeatureIntegration() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        onView(withId(R.id.new_feature_button))
            .perform(click())
            
        onView(withId(R.id.result_view))
            .check(matches(isDisplayed()))
    }
}
```

### 测试工具类
- `TestBase` - 提供通用测试工具
- `TestDataFactory` - 生成测试数据
- `CoroutineTestRule` - 协程测试规则
- `TestConstants` - 测试常量定义

## 维护指南

### 定期维护任务
1. **更新测试依赖**: 保持测试框架版本最新
2. **审查覆盖率**: 确保新代码有足够测试覆盖
3. **清理过时测试**: 移除不再需要的测试用例
4. **优化测试性能**: 定期检查和优化慢速测试

### 测试质量标准
- 单元测试覆盖率 >= 80%
- 集成测试覆盖主要用户流程
- 性能测试确保系统稳定性
- 所有测试在CI中稳定运行

## 参考资料
- [JUnit 4 文档](https://junit.org/junit4/)
- [Mockito 文档](https://site.mockito.org/)
- [Espresso 测试指南](https://developer.android.com/training/testing/espresso)
- [Robolectric 文档](http://robolectric.org/)
- [JaCoco 文档](https://www.jacoco.org/)
