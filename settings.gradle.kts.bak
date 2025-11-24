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

// 包含蓝河工具箱模块库
include(":mokuai:mokuai:modules:network")
include(":mokuai:mokuai:modules:performance-monitor")
include(":mokuai:mokuai:modules:memory-manager")
include(":mokuai:mokuai:modules:filesystem")
