# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

蓝河助手 (Lanhe Assistant) is an Android system optimization tool with 20+ utility modules including performance monitoring, battery management, network optimization, and system management. Built with Kotlin using MVVM architecture pattern.

**Key Technologies:**
- Platform: Android (Kotlin 2.0.21)
- Min SDK: 24 (Android 7.0) / Target SDK: 36 (Android 15)
- Architecture: MVVM + Repository pattern
- UI: Material Design 3.0 with ViewBinding
- Database: Room with Coroutines
- Build: Gradle 8.12.1 with Kotlin DSL
- Special: Shizuku framework for system-level operations

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

1. **No Dependency Injection Framework**: Uses manual dependency management rather than Hilt/Dagger
2. **Shizuku Integration**: System-level operations require Shizuku framework (optional but recommended)
3. **Hidden API Access**: Uses `org.lsposed.hiddenapibypass` to access hidden Android APIs
4. **ViewBinding**: All layouts use ViewBinding (enabled in build.gradle.kts)
5. **Coroutines**: All async operations use Kotlin Coroutines

### Room Database Schema

Main entities tracked in Room database:
- `PerformanceDataEntity`: CPU/memory/battery metrics over time
- `OptimizationHistoryEntity`: History of optimization operations
- `BatteryStatsEntity`: Battery usage statistics

Database version: 1 (see `AppDatabase.kt` in utils/)

## Critical Implementation Notes

### Shizuku Permission System

The app uses Shizuku for system-level operations. Key points:

```kotlin
// Check availability
ShizukuManager.isShizukuAvailable(): Boolean

// Request permission
ShizukuManager.requestPermission(context: Context)

// System operations (requires Shizuku)
ShizukuManager.getRunningProcesses(): List<ProcessInfo>
ShizukuManager.getCpuUsage(): Float
```

User must manually install and activate Shizuku app for advanced features.

### Module Dependencies

The project includes 4 custom modules under `mokuai/`:
- `:mokuai:mokuai:modules:network`
- `:mokuai:mokuai:modules:performance-monitor`
- `:mokuai:mokuai:modules:memory-manager`
- `:mokuai:mokuai:modules:filesystem`

These are declared in `settings.gradle.kts` and provide reusable functionality.

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

### Unit Tests

Location: `app/src/test/`
- JUnit 4 for test framework
- Mockito 5.8.0 for mocking
- Focus on utils package core logic

### Instrumentation Tests

Location: `app/src/androidTest/`
- Espresso 3.6.1 for UI testing
- AndroidX Test for testing infrastructure
- Focus on Activity and Fragment interactions

## Common Development Tasks

### Adding a New Feature Module

1. Create utility class in `utils/` package
2. Add corresponding Activity in `activities/` package
3. Update navigation in MainActivity
4. Add Room entities if data persistence needed
5. Update layouts in `res/layout/`

### Working with Shizuku

When adding system-level features:
1. Check Shizuku availability before operations
2. Handle permission denial gracefully
3. Provide fallback for non-Shizuku users
4. Document required Shizuku version

### Database Migrations

Current version: 1. When modifying entities:
1. Update entity classes with @Entity
2. Increment database version in AppDatabase
3. Provide migration strategy or set fallbackToDestructiveMigration

## Project Structure Notes

165 Kotlin source files total in `app/src/main/`

Main entry point: `MainActivity.kt` + `LanheApplication.kt`

The app uses a hybrid architecture:
- Bottom Navigation for primary tabs
- Navigation Component for fragment navigation
- DrawerLayout for side menu
- Toolbar with CoordinatorLayout for collapsing effects
