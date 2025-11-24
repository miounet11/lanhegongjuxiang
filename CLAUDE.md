# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

蓝河助手 (Lanhe Assistant) is a production-ready Android system optimization tool with 20+ utility modules including performance monitoring, battery management, network optimization, and system management. **Recently upgraded from prototype to production-quality** with comprehensive architectural improvements, real-time monitoring, and enterprise-grade testing.

**Key Technologies:**
- Platform: Android (Kotlin 2.0.21)
- Min SDK: 24 (Android 7.0) / Target SDK: 36 (Android 15)
- Architecture: MVVM + Repository pattern + Modular architecture
- UI: Material Design 3.0 with ViewBinding + Custom components
- Database: Room with Coroutines + Real-time data persistence
- Build: Gradle 8.12.1 with Kotlin DSL + 18-module system
- Special: Shizuku framework for system-level operations + Hilt DI
- Testing: Comprehensive 4-layer testing (Unit/Integration/UI/Performance)

## Build & Development Commands

### Essential Commands

```bash
# Clean and build project
./gradlew clean build

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires keystore config in local.properties)
./gradlew assembleRelease

# Install debug build to connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run unit tests for a specific module
./gradlew :app:testDebugUnitTest

# Run lint checks
./gradlew lint

# List all available tasks
./gradlew tasks
```

### Signing Configuration

Release builds require keystore configuration in `local.properties`:

```properties
RELEASE_STORE_FILE=keystore.jks
RELEASE_STORE_PASSWORD=<password>
RELEASE_KEY_ALIAS=<alias>
RELEASE_KEY_PASSWORD=<password>
```

The keystore file `keystore.jks` is already present in the project root.

## Architecture Patterns

### MVVM Structure

```
app/src/main/java/com/lanhe/gongjuxiang/
├── activities/          # 26 Activity classes (UI controllers)
├── fragments/           # Fragment components for navigation
├── viewmodels/          # ViewModel classes for UI state
├── utils/               # 59 utility classes (core business logic)
│   ├── ShizukuManager.kt      # Shizuku permission management
│   ├── SystemOptimizer.kt     # System optimization engine
│   ├── PerformanceMonitor.kt  # Performance monitoring
│   └── AppDatabase.kt         # Room database definition
├── models/              # Data models and entities
├── services/            # Background and foreground services
├── adapters/            # RecyclerView adapters
└── browser/             # Built-in WebView browser
```

### Key Architectural Decisions

1. **Hilt Dependency Injection**: **UPGRADED** - Now uses Hilt for comprehensive dependency management across all modules
2. **Shizuku Integration**: System-level operations require Shizuku framework with proper error handling and fallback mechanisms
3. **Hidden API Access**: Uses `org.lsposed.hiddenapibypass` to access hidden Android APIs with safety checks
4. **ViewBinding**: All layouts use ViewBinding (enabled in build.gradle.kts)
5. **Coroutines**: All async operations use Kotlin Coroutines with proper scoping and error handling
6. **Modular Architecture**: 18-module system with standardized APIs and independent compilation
7. **Real-time Monitoring**: **NEW** - True hardware monitoring replacing placeholder data
8. **Production Testing**: **NEW** - 4-layer testing architecture with CI/CD automation

### Room Database Schema

**Enhanced with real-time data persistence and historical analysis:**

Main entities tracked in Room database:
- `PerformanceDataEntity`: **REAL** CPU/memory/battery metrics from actual hardware sensors
- `OptimizationHistoryEntity`: Complete history of optimization operations with results
- `BatteryStatsEntity`: Detailed battery usage statistics with health analysis
- `NetworkUsageEntity`: **NEW** - Per-app network usage tracking
- `SystemEventsEntity`: **NEW** - System events and performance anomalies

**Database version: 2** (see `AppDatabase.kt` in utils/) with migration support for historical data.

## Critical Implementation Notes

### Shizuku Permission System

**FULLY IMPLEMENTED** - The app uses production-ready Shizuku integration with comprehensive error handling:

```kotlin
// Enhanced permission management
ShizukuManager.isShizukuAvailable(): Boolean
ShizukuManager.requestPermission(context: Context)
ShizukuManager.shizukuState: StateFlow<ShizukuState>

// **REAL** System operations (no more placeholders)
ShizukuManager.getRunningProcesses(): List<ProcessInfo>
ShizukuManager.getCpuUsage(): Float
ShizukuManager.installPackage(packagePath: String): Boolean
ShizukuManager.uninstallPackage(packageName: String): Boolean
ShizukuManager.getNetworkStats(): NetworkStats
```

**Critical Fix**: All `Toast.makeText(null, ...)` calls have been replaced with proper context handling via `LanheApplication.getContext()`.

User must manually install and activate Shizuku app for advanced features. The app gracefully handles permission denial with fallback functionality.

### Module Dependencies

The project includes **18 custom modules** under `mokuai/` with complete Hilt integration:

**Core Modules:**
- `:mokuai:mokuai:core:common` - Common utilities and interfaces
- `:mokuai:mokuai:core:shizuku-api` - Shizuku API abstraction layer
- `:mokuai:mokuai:core:data` - Data sharing and management

**Feature Modules:**
- `:mokuai:mokuai:modules:network` - Network communication and diagnostics
- `:mokuai:mokuai:modules:performance-monitor` - Real-time performance monitoring
- `:mokuai:mokuai:modules:memory-manager` - Memory optimization and cleanup
- `:mokuai:mokuai:modules:filesystem` - File operations and management
- `:mokuai:mokuai:modules:database` - Database utilities and migrations
- `:mokuai:mokuai:modules:analytics` - Usage analytics and reporting
- `:mokuai:mokuai:modules:crash` - Crash handling and reporting
- `:mokuai:mokuai:modules:ui` - Reusable UI components
- Plus 10 additional specialized modules for bookmark management, download management, image processing, etc.

All modules use standardized API interfaces and support independent testing.

## Coding Conventions

### Kotlin Style

- Use ViewBinding instead of findViewById
- Prefer Kotlin Coroutines for async operations
- Use sealed classes for state management
- Follow official Kotlin coding conventions

### Naming Patterns

- Activities: `XxxActivity.kt`
- Fragments: `XxxFragment.kt`
- Adapters: `XxxAdapter.kt`
- ViewModels: `XxxViewModel.kt`
- Utilities: `XxxManager.kt` or `XxxHelper.kt`

### Architecture Guidelines

- Use Repository pattern for data management
- ViewModels must not hold Context references
- Use LiveData/StateFlow for reactive data
- Centralize error handling in base classes

## Dependency Management

Dependencies are managed via `gradle/libs.versions.toml` using Gradle version catalogs.

**Major dependencies:**
- AndroidX Core/AppCompat/Material
- Lifecycle (ViewModel, LiveData): 2.8.7
- Room: 2.7.0
- Navigation: 2.8.3
- Retrofit: 2.9.0
- Shizuku API: 13.1.0
- Glide: 4.16.0
- Lottie: 6.4.0
- TensorFlow Lite: 2.14.0 (for AI suggestions)

## Testing Strategy

**PRODUCTION-GRADE 4-LAYER TESTING ARCHITECTURE:**

### Unit Tests (80%+ coverage target)

Location: `app/src/test/`
- JUnit 4 for test framework
- Mockito 5.8.0 for mocking
- **Comprehensive coverage** of utils package core logic
- **NEW**: `ShizukuManagerTest.kt`, `RealPerformanceMonitorManagerTest.kt`, `AppDatabaseTest.kt`
- Custom test utilities: `TestBase.kt`, `CoroutineTestRule.kt`, `TestDataFactory.kt`

### Integration Tests

Location: `app/src/androidTest/`
- **NEW**: `MainActivityIntegrationTest.kt` - Complete UI flow testing
- **NEW**: `ShizukuIntegrationTest.kt` - System-level integration testing
- **NEW**: `PerformanceTest.kt` - Memory leak and performance testing
- AndroidX Test for testing infrastructure

### UI Tests

- Espresso 3.6.1 for UI testing
- Focus on critical user flows and navigation
- Fragment interaction testing
- Accessibility testing integration

### Performance Tests

- **NEW**: Memory usage monitoring (< 30MB overhead)
- **NEW**: CPU impact testing (< 5% usage)
- **NEW**: Battery drain analysis (< 1%/day)
- **NEW**: Startup time optimization

### CI/CD Automation

- **NEW**: GitHub Actions workflows for automated testing
- Jacoco coverage reporting with 80% minimum threshold
- Multi-environment testing (debug/release)
- Automated APK generation and artifact management

## Critical Implementation Status

### ✅ COMPLETED UPGRADES (Prototype → Production)

1. **Application Initialization**: `LanheApplication.initializeComponents()` fully implemented with proper dependency injection
2. **Shizuku Integration**: All `Toast.makeText(null, ...)` crashes fixed, real system operations implemented
3. **Performance Monitoring**: `RealPerformanceMonitorManager.kt` replaces all placeholder data with actual hardware readings
4. **Module System**: 18-module architecture with Hilt integration and standardized APIs
5. **Testing Infrastructure**: Complete 4-layer testing with 80%+ coverage target
6. **CI/CD Pipeline**: GitHub Actions automation with security scanning and coverage reporting
7. **Database Enhancement**: Real-time data persistence with migration support (v2)
8. **Dependency Injection**: Full Hilt integration replacing manual DI

### Development Commands

```bash
# Build and test with coverage
./gradlew clean build test jacocoTestReport

# Module-specific testing
./gradlew :mokuai:mokuai:modules:network:test
./gradlew :mokuai:mokuai:modules:performance-monitor:test

# Performance validation
./gradlew :app:connectedAndroidTest
./gradlew lint
```

### Working with Shizuku (Production Ready)

System-level features now include:
- **Real permission state management** with `StateFlow<ShizukuState>`
- **Comprehensive error handling** with proper fallback mechanisms
- **Context-safe operations** via `LanheApplication.getContext()`
- **Input validation** and security whitelisting for system commands

### Database Migrations (Current: v2)

When modifying entities:
1. Update entity classes with @Entity
2. Increment database version in AppDatabase
3. **Provide incremental migration** (don't use fallbackToDestructiveMigration)
4. Test migration paths with `MigrationTestHelper`

### Module Development

When creating new modules:
1. Use `ModuleApi` interface standardization
2. Follow Hilt dependency injection patterns
3. Include comprehensive unit tests
4. Register in `ModuleRegistry`
5. Update `settings.gradle.kts`

## Project Structure Notes

**UPGRADED**: 198+ Kotlin source files in `app/src/main/` + 18 additional modules

**Main entry points:**
- `MainActivity.kt` - Enhanced with Material Design 3.0 and simplified 4-tab navigation
- `LanheApplication.kt` - **FULLY IMPLEMENTED** initialization chain with dependency injection

**Current Architecture (Post-Upgrade):**
- **Simplified Navigation**: Removed DrawerLayout, streamlined to 4-tab Bottom Navigation
- **Material Design 3.0**: Custom components (`CircularProgressView`, `BreadcrumbView`)
- **Modular Integration**: All 18 modules properly integrated with Hilt DI
- **Real-time Monitoring**: Live performance data from actual hardware sensors
- **Production Testing**: Comprehensive test coverage with automated CI/CD

**Key Implementation Files:**
- `RealPerformanceMonitorManager.kt` - **NEW** - True hardware monitoring
- `ShizukuManagerImpl.kt` - **NEW** - Production Shizuku implementation
- `DependencyContainer.kt` - **NEW** - Manual dependency injection container
- `ModuleRegistry.kt` - **NEW** - Module lifecycle management

**Documentation:**
- `MODULE_USAGE_GUIDE.md` - Complete module integration guide
- `PERFORMANCE_OPTIMIZATION.md` - Performance optimization strategies
- `implementation_report.md` - Detailed upgrade documentation
