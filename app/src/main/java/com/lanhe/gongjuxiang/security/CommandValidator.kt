package com.lanhe.gongjuxiang.security

import android.util.Log
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * 命令执行安全验证器
 * 用于验证和过滤系统命令，防止恶意操作
 */
class CommandValidator {

    companion object {
        private const val TAG = "CommandValidator"

        // 命令执行超时时间（毫秒）
        const val COMMAND_TIMEOUT_MS = 30000L // 30秒

        // ========================================
        // 白名单：允许执行的命令
        // ========================================
        private val ALLOWED_COMMANDS = setOf(
            // 包管理命令
            "pm list packages",
            "pm list packages -3",  // 第三方应用
            "pm list packages -s",  // 系统应用
            "pm list packages -d",  // 禁用的应用
            "pm list packages -e",  // 启用的应用
            "pm path",              // 获取包路径
            "pm dump",              // 获取包信息
            "pm grant",             // 授权权限
            "pm revoke",            // 撤销权限
            "pm enable",            // 启用组件
            "pm disable",           // 禁用组件
            "pm disable-user",      // 用户级禁用
            "pm clear",             // 清理应用数据
            "pm get-install-location",
            "pm set-install-location",

            // Activity管理命令
            "am force-stop",        // 强制停止
            "am kill",              // 杀死进程
            "am start",             // 启动Activity
            "am startservice",      // 启动服务
            "am broadcast",         // 发送广播
            "am get-config",        // 获取配置
            "am display-size",      // 显示大小
            "am display-density",   // 显示密度

            // 系统设置命令
            "settings get global",
            "settings get system",
            "settings get secure",
            "settings put global",
            "settings put system",
            "settings put secure",
            "settings list",

            // 系统属性命令
            "getprop",              // 获取系统属性
            "setprop",              // 设置系统属性（需要谨慎）

            // 服务管理命令
            "service list",         // 列出服务
            "service check",        // 检查服务
            "service call",         // 调用服务（需要谨慎）

            // 内容管理命令
            "content query",        // 查询内容
            "content insert",       // 插入内容
            "content update",       // 更新内容
            "content delete",       // 删除内容

            // 输入管理命令
            "input text",           // 输入文本
            "input keyevent",       // 发送按键事件
            "input tap",            // 点击屏幕
            "input swipe",          // 滑动屏幕

            // Dumpsys命令（系统信息）
            "dumpsys battery",      // 电池信息
            "dumpsys meminfo",      // 内存信息
            "dumpsys cpuinfo",      // CPU信息
            "dumpsys activity",     // Activity信息
            "dumpsys package",      // 包信息
            "dumpsys window",       // 窗口信息
            "dumpsys power",        // 电源信息
            "dumpsys alarm",        // 闹钟信息
            "dumpsys connectivity", // 连接信息
            "dumpsys wifi",         // WiFi信息
            "dumpsys telephony.registry", // 电话信息

            // 文件操作命令（受限）
            "ls",                   // 列出文件
            "cat",                  // 读取文件（需要路径验证）
            "grep",                 // 搜索文本
            "find",                 // 查找文件（需要路径验证）
            "stat",                 // 文件状态
            "file",                 // 文件类型
            "du",                   // 磁盘使用
            "df",                   // 磁盘空闲

            // 进程管理命令
            "ps",                   // 进程列表
            "top",                  // 进程监控
            "pidof",                // 获取PID

            // 网络命令
            "netstat",              // 网络状态
            "ping",                 // 网络测试
            "ip addr",              // IP地址
            "ip route",             // 路由信息

            // 其他安全命令
            "whoami",               // 当前用户
            "id",                   // 用户ID
            "uptime",               // 运行时间
            "date",                 // 系统时间
            "uname",                // 系统信息
            "logcat"                // 日志查看
        )

        // ========================================
        // 黑名单：危险命令模式
        // ========================================
        private val DANGEROUS_PATTERNS = listOf(
            // 文件系统破坏命令
            Pattern.compile(".*\\brm\\s+-[rf].*", Pattern.CASE_INSENSITIVE),  // rm -rf
            Pattern.compile(".*\\bdd\\s+.*", Pattern.CASE_INSENSITIVE),       // dd命令
            Pattern.compile(".*\\bformat\\s+.*", Pattern.CASE_INSENSITIVE),   // 格式化
            Pattern.compile(".*\\bmkfs\\s+.*", Pattern.CASE_INSENSITIVE),     // 创建文件系统
            Pattern.compile(".*\\bfdisk\\s+.*", Pattern.CASE_INSENSITIVE),    // 磁盘分区

            // 系统控制命令
            Pattern.compile(".*\\breboot\\s*.*", Pattern.CASE_INSENSITIVE),   // 重启
            Pattern.compile(".*\\bshutdown\\s+.*", Pattern.CASE_INSENSITIVE), // 关机
            Pattern.compile(".*\\bpoweroff\\s*.*", Pattern.CASE_INSENSITIVE), // 断电
            Pattern.compile(".*\\bhalt\\s*.*", Pattern.CASE_INSENSITIVE),     // 停机
            Pattern.compile(".*\\binit\\s+[0-6].*", Pattern.CASE_INSENSITIVE),// init级别

            // 权限提升命令
            Pattern.compile(".*\\bsu\\s*.*", Pattern.CASE_INSENSITIVE),       // su提权
            Pattern.compile(".*\\bsudo\\s+.*", Pattern.CASE_INSENSITIVE),     // sudo提权
            Pattern.compile(".*\\bsetuid\\s+.*", Pattern.CASE_INSENSITIVE),   // 设置UID
            Pattern.compile(".*\\bsetgid\\s+.*", Pattern.CASE_INSENSITIVE),   // 设置GID

            // 危险的权限修改
            Pattern.compile(".*\\bchmod\\s+777.*", Pattern.CASE_INSENSITIVE), // 完全开放权限
            Pattern.compile(".*\\bchmod\\s+\\+s.*", Pattern.CASE_INSENSITIVE),// SUID/SGID
            Pattern.compile(".*\\bchown\\s+root.*", Pattern.CASE_INSENSITIVE),// 改为root所有

            // 文件系统操作
            Pattern.compile(".*\\bmount\\s+.*", Pattern.CASE_INSENSITIVE),    // 挂载
            Pattern.compile(".*\\bumount\\s+.*", Pattern.CASE_INSENSITIVE),   // 卸载
            Pattern.compile(".*\\blosetup\\s+.*", Pattern.CASE_INSENSITIVE),  // 循环设备

            // 内核模块操作
            Pattern.compile(".*\\binsmod\\s+.*", Pattern.CASE_INSENSITIVE),   // 插入模块
            Pattern.compile(".*\\brmmod\\s+.*", Pattern.CASE_INSENSITIVE),    // 删除模块
            Pattern.compile(".*\\bmodprobe\\s+.*", Pattern.CASE_INSENSITIVE), // 模块管理

            // 网络危险命令
            Pattern.compile(".*\\biptables\\s+.*", Pattern.CASE_INSENSITIVE), // 防火墙规则
            Pattern.compile(".*\\btcpdump\\s+.*", Pattern.CASE_INSENSITIVE),  // 网络抓包
            Pattern.compile(".*\\bnc\\s+-l.*", Pattern.CASE_INSENSITIVE),     // netcat监听

            // SELinux操作
            Pattern.compile(".*\\bsetenforce\\s+0.*", Pattern.CASE_INSENSITIVE), // 禁用SELinux
            Pattern.compile(".*\\bgetenforce\\s+.*", Pattern.CASE_INSENSITIVE),  // SELinux状态

            // 危险的Shell操作
            Pattern.compile(".*\\beval\\s+.*", Pattern.CASE_INSENSITIVE),     // eval执行
            Pattern.compile(".*\\bexec\\s+.*", Pattern.CASE_INSENSITIVE),     // exec替换
            Pattern.compile(".*\\|\\s*sh\\s*", Pattern.CASE_INSENSITIVE),     // 管道到shell
            Pattern.compile(".*\\|\\s*bash\\s*", Pattern.CASE_INSENSITIVE),   // 管道到bash
            Pattern.compile(".*`.*`.*", Pattern.CASE_INSENSITIVE),            // 命令替换
            Pattern.compile(".*\\$\\(.*\\).*", Pattern.CASE_INSENSITIVE),     // 命令替换

            // 危险的文件操作
            Pattern.compile(".*>\\s*/dev/.*", Pattern.CASE_INSENSITIVE),      // 写入设备文件
            Pattern.compile(".*>\\s*/sys/.*", Pattern.CASE_INSENSITIVE),      // 写入sys文件
            Pattern.compile(".*>\\s*/proc/.*", Pattern.CASE_INSENSITIVE),     // 写入proc文件

            // 危险路径操作
            Pattern.compile(".*\\.\\./\\.\\./.*", Pattern.CASE_INSENSITIVE),  // 路径遍历
            Pattern.compile(".*\\brm\\s+/.*", Pattern.CASE_INSENSITIVE),      // 删除根目录文件
            Pattern.compile(".*\\bcp\\s+.*/system/.*", Pattern.CASE_INSENSITIVE), // 修改system
            Pattern.compile(".*\\bmv\\s+.*/system/.*", Pattern.CASE_INSENSITIVE)  // 移动到system
        )

        // ========================================
        // 受保护的系统包名
        // ========================================
        private val PROTECTED_PACKAGES = setOf(
            // Android核心系统
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.shell",
            "com.android.providers.settings",
            "com.android.providers.media",
            "com.android.providers.downloads",
            "com.android.providers.contacts",
            "com.android.providers.calendar",
            "com.android.providers.telephony",
            "com.android.packageinstaller",
            "com.android.permissioncontroller",
            "com.android.keychain",
            "com.android.certinstaller",
            "com.android.carrierconfig",

            // Google Play服务
            "com.android.vending",              // Google Play Store
            "com.google.android.gms",           // Google Play Services
            "com.google.android.gsf",           // Google Services Framework
            "com.google.android.play.games",    // Play Games
            "com.google.android.googlequicksearchbox", // Google搜索

            // 系统关键服务
            "com.android.inputmethod.latin",    // 系统输入法
            "com.android.webview",               // WebView
            "com.android.chrome",                // Chrome浏览器
            "com.android.bluetooth",             // 蓝牙服务
            "com.android.nfc",                   // NFC服务
            "com.android.location.fused",       // 位置服务

            // OEM关键应用（示例）
            "com.xiaomi.finddevice",             // 小米查找设备
            "com.miui.securitycenter",           // MIUI安全中心
            "com.huawei.systemmanager",          // 华为系统管理
            "com.oppo.safe",                     // OPPO安全中心
            "com.vivo.safecenter",               // VIVO安全中心
            "com.samsung.android.lool",          // 三星设备关怀
            "com.oneplus.security"               // 一加安全中心
        )

        // ========================================
        // 危险的文件路径
        // ========================================
        private val DANGEROUS_PATHS = setOf(
            "/system",
            "/sys",
            "/proc",
            "/dev",
            "/data/system",
            "/data/data/com.android.systemui",
            "/data/data/com.android.settings",
            "/data/data/com.android.phone",
            "/data/dalvik-cache",
            "/data/local/tmp/su",
            "/data/local/tmp/magisk",
            "/vendor",
            "/boot",
            "/recovery",
            "/cache",
            "/sdcard/Android/data/com.android.systemui",
            "/storage/emulated/0/Android/data/com.android.systemui"
        )
    }

    /**
     * 验证命令是否安全
     * @param command 要执行的命令
     * @return 是否允许执行
     */
    fun validateCommand(command: String): Boolean {
        val trimmedCommand = command.trim()

        // 空命令拒绝
        if (trimmedCommand.isEmpty()) {
            Log.w(TAG, "拒绝执行：空命令")
            return false
        }

        // 检查是否在白名单中
        val isAllowed = ALLOWED_COMMANDS.any { allowedCmd ->
            trimmedCommand.startsWith(allowedCmd)
        }

        if (!isAllowed) {
            Log.w(TAG, "拒绝执行：命令不在白名单中 - $trimmedCommand")
            return false
        }

        // 检查是否匹配黑名单模式
        for (pattern in DANGEROUS_PATTERNS) {
            if (pattern.matcher(trimmedCommand).matches()) {
                Log.e(TAG, "拒绝执行：命令匹配危险模式 - $trimmedCommand")
                return false
            }
        }

        // 检查路径遍历攻击
        if (containsPathTraversal(trimmedCommand)) {
            Log.e(TAG, "拒绝执行：检测到路径遍历攻击 - $trimmedCommand")
            return false
        }

        // 检查危险路径
        if (containsDangerousPath(trimmedCommand)) {
            Log.e(TAG, "拒绝执行：命令包含危险路径 - $trimmedCommand")
            return false
        }

        Log.d(TAG, "命令验证通过：$trimmedCommand")
        return true
    }

    /**
     * 验证包名是否安全（不在保护列表中）
     * @param packageName 包名
     * @return 是否允许操作
     */
    fun validatePackageName(packageName: String): Boolean {
        val trimmedPackageName = packageName.trim()

        // 空包名拒绝
        if (trimmedPackageName.isEmpty()) {
            Log.w(TAG, "拒绝操作：空包名")
            return false
        }

        // 检查包名格式
        if (!isValidPackageName(trimmedPackageName)) {
            Log.w(TAG, "拒绝操作：无效的包名格式 - $trimmedPackageName")
            return false
        }

        // 检查是否是受保护的系统包
        if (PROTECTED_PACKAGES.contains(trimmedPackageName)) {
            Log.e(TAG, "拒绝操作：系统关键包受保护 - $trimmedPackageName")
            return false
        }

        // 检查是否是系统包前缀
        if (trimmedPackageName.startsWith("android.") ||
            trimmedPackageName.startsWith("com.android.")) {
            // 允许某些非关键的android包
            if (!isNonCriticalAndroidPackage(trimmedPackageName)) {
                Log.w(TAG, "警告：操作系统包需要谨慎 - $trimmedPackageName")
                // 这里可以选择返回false或者添加额外确认
            }
        }

        Log.d(TAG, "包名验证通过：$trimmedPackageName")
        return true
    }

    /**
     * 验证文件路径是否安全
     * @param filePath 文件路径
     * @return 是否允许访问
     */
    fun validateFilePath(filePath: String): Boolean {
        val trimmedPath = filePath.trim()

        // 空路径拒绝
        if (trimmedPath.isEmpty()) {
            Log.w(TAG, "拒绝访问：空路径")
            return false
        }

        // 检查路径遍历
        if (containsPathTraversal(trimmedPath)) {
            Log.e(TAG, "拒绝访问：检测到路径遍历攻击 - $trimmedPath")
            return false
        }

        // 检查危险路径
        for (dangerousPath in DANGEROUS_PATHS) {
            if (trimmedPath.startsWith(dangerousPath)) {
                Log.e(TAG, "拒绝访问：危险路径 - $trimmedPath")
                return false
            }
        }

        // 检查文件是否存在且可访问
        try {
            val file = File(trimmedPath)
            val canonicalPath = file.canonicalPath

            // 确保规范路径不包含危险路径
            for (dangerousPath in DANGEROUS_PATHS) {
                if (canonicalPath.startsWith(dangerousPath)) {
                    Log.e(TAG, "拒绝访问：规范路径包含危险路径 - $canonicalPath")
                    return false
                }
            }

            // 检查是否是APK文件的有效路径
            if (trimmedPath.endsWith(".apk")) {
                if (!canonicalPath.startsWith("/storage/") &&
                    !canonicalPath.startsWith("/sdcard/") &&
                    !canonicalPath.startsWith("/data/app/") &&
                    !canonicalPath.startsWith("/data/local/tmp/")) {
                    Log.e(TAG, "拒绝访问：APK文件路径不在允许的目录 - $canonicalPath")
                    return false
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "路径验证异常：$trimmedPath - ${e.message}")
            // 文件不存在或无法访问，但这不一定是安全问题
        }

        Log.d(TAG, "路径验证通过：$trimmedPath")
        return true
    }

    /**
     * 验证命令超时时间
     * @param timeout 超时时间（毫秒）
     * @return 有效的超时时间
     */
    fun validateTimeout(timeout: Long): Long {
        return when {
            timeout <= 0 -> COMMAND_TIMEOUT_MS
            timeout > COMMAND_TIMEOUT_MS -> {
                Log.w(TAG, "超时时间超过最大值，使用默认值：${COMMAND_TIMEOUT_MS}ms")
                COMMAND_TIMEOUT_MS
            }
            else -> timeout
        }
    }

    /**
     * 记录命令执行审计日志
     * @param command 执行的命令
     * @param result 执行结果
     * @param executionTime 执行时间
     */
    fun auditCommandExecution(command: String, result: Boolean, executionTime: Long) {
        val timestamp = System.currentTimeMillis()
        val user = android.os.Process.myUserHandle()

        Log.i(TAG, "命令审计 - " +
            "时间: $timestamp, " +
            "用户: $user, " +
            "命令: $command, " +
            "结果: ${if (result) "成功" else "失败"}, " +
            "耗时: ${executionTime}ms")

        // TODO: 可以将审计日志保存到数据库或文件中
    }

    /**
     * 生成命令执行警告信息
     * @param packageName 包名
     * @return 警告信息
     */
    fun generateWarningMessage(packageName: String): String {
        return when {
            PROTECTED_PACKAGES.contains(packageName) -> {
                "⚠️ 警告：您正在尝试操作系统核心应用 [$packageName]。\n" +
                "这可能导致系统不稳定或无法正常使用。\n" +
                "是否确定要继续？"
            }
            packageName.startsWith("com.android.") -> {
                "⚠️ 注意：您正在操作系统应用 [$packageName]。\n" +
                "请确保您了解操作的后果。\n" +
                "是否继续？"
            }
            packageName.startsWith("com.google.") -> {
                "⚠️ 注意：您正在操作Google服务 [$packageName]。\n" +
                "这可能影响应用商店和其他Google服务。\n" +
                "是否继续？"
            }
            else -> {
                "您即将操作应用 [$packageName]。\n" +
                "是否继续？"
            }
        }
    }

    // ========================================
    // 私有辅助方法
    // ========================================

    /**
     * 检查命令是否包含路径遍历攻击
     */
    private fun containsPathTraversal(text: String): Boolean {
        return text.contains("../") ||
               text.contains("..\\") ||
               text.contains("..${File.separator}")
    }

    /**
     * 检查命令是否包含危险路径
     */
    private fun containsDangerousPath(command: String): Boolean {
        for (path in DANGEROUS_PATHS) {
            if (command.contains(path)) {
                // 允许读取某些系统信息（如dumpsys）
                if (command.startsWith("dumpsys") ||
                    command.startsWith("cat /proc/") ||
                    command.startsWith("ls ")) {
                    continue
                }
                return true
            }
        }
        return false
    }

    /**
     * 验证包名格式
     */
    private fun isValidPackageName(packageName: String): Boolean {
        // 包名应该符合Java包名规范
        val packagePattern = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$",
                                            Pattern.CASE_INSENSITIVE)
        return packagePattern.matcher(packageName).matches()
    }

    /**
     * 检查是否是非关键的Android包
     */
    private fun isNonCriticalAndroidPackage(packageName: String): Boolean {
        // 这些是相对安全的Android包
        val safeAndroidPackages = setOf(
            "com.android.calculator2",
            "com.android.calendar",
            "com.android.camera",
            "com.android.contacts",
            "com.android.deskclock",
            "com.android.dialer",
            "com.android.documentsui",
            "com.android.email",
            "com.android.gallery3d",
            "com.android.launcher3",
            "com.android.mms",
            "com.android.music",
            "com.android.quicksearchbox",
            "com.android.soundrecorder"
        )
        return safeAndroidPackages.contains(packageName)
    }
}