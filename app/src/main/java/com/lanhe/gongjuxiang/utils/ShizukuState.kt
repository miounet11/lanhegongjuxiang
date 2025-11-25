package com.lanhe.gongjuxiang.utils

/**
 * Shizuku权限状态枚举
 */
enum class ShizukuState {
    /** Shizuku服务不可用（未安装或未运行） */
    Unavailable,

    /** Shizuku权限被拒绝 */
    Denied,

    /** Shizuku权限已授予 */
    Granted,

    /** 正在检查Shizuku状态 */
    Checking
}