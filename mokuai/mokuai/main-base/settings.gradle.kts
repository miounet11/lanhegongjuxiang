pluginManagement {
    repositories {
        google()
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
    }
}

// 包含主应用和所有模块
include(":app")

// 包含所有蓝河工具箱模块库
include(":libraries:network")
include(":libraries:database")
include(":libraries:ui")
include(":libraries:utils")
include(":libraries:settings")
include(":libraries:notification")
include(":libraries:image")
include(":libraries:filesystem")

// 项目名称
rootProject.name = "MainBase"

// 设置子项目目录
project(":libraries:network").projectDir = file("libraries/network")
project(":libraries:database").projectDir = file("libraries/database")
project(":libraries:ui").projectDir = file("libraries/ui")
project(":libraries:utils").projectDir = file("libraries/utils")
project(":libraries:settings").projectDir = file("libraries/settings")
project(":libraries:notification").projectDir = file("libraries/notification")
project(":libraries:image").projectDir = file("libraries/image")
project(":libraries:filesystem").projectDir = file("libraries/filesystem")
