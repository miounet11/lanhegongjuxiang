pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "蓝河工具箱"
include(":app")

// 蓝河助手模块库 - 完整模块列表
include(":mokuai:mokuai:modules:network")
include(":mokuai:mokuai:modules:performance-monitor")
include(":mokuai:mokuai:modules:memory-manager")
include(":mokuai:mokuai:modules:filesystem")
include(":mokuai:mokuai:modules:database")
include(":mokuai:mokuai:modules:analytics")
include(":mokuai:mokuai:modules:crash")
include(":mokuai:mokuai:modules:bookmark-manager")
include(":mokuai:mokuai:modules:download-manager")
include(":mokuai:mokuai:modules:image-helper")
include(":mokuai:mokuai:modules:notification")
include(":mokuai:mokuai:modules:password-manager")
include(":mokuai:mokuai:modules:proxy-selector")
include(":mokuai:mokuai:modules:security-manager")
include(":mokuai:mokuai:modules:text-extractor")
include(":mokuai:mokuai:modules:ui")
include(":mokuai:mokuai:modules:url-opener")
include(":mokuai:mokuai:modules:ad-blocker")

// 核心基础模块
include(":mokuai:mokuai:core:common")
include(":mokuai:mokuai:core:shizuku-api")
include(":mokuai:mokuai:core:data")
