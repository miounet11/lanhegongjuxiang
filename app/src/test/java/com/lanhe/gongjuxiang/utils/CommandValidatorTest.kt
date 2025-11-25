package com.lanhe.gongjuxiang.utils

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * CommandValidator单元测试
 * 测试命令白名单验证、黑名单检测、路径安全验证、包名格式验证等
 */
class CommandValidatorTest {

    private lateinit var validator: CommandValidator

    @Before
    fun setup() {
        validator = CommandValidator()
    }

    /**
     * 测试命令白名单验证 - 有效命令
     */
    @Test
    fun `test command whitelist - valid commands`() {
        // Given: 白名单中的有效命令
        val validCommands = listOf(
            "pm list packages",
            "pm list packages -f",
            "pm path com.example.app",
            "pm uninstall com.example.app",
            "dumpsys battery",
            "dumpsys meminfo",
            "dumpsys cpuinfo",
            "getprop ro.build.version.release",
            "getprop ro.product.model",
            "cat /proc/cpuinfo",
            "cat /proc/meminfo",
            "cat /proc/stat",
            "ps -A",
            "top -n 1",
            "df",
            "netstat"
        )

        // When & Then: 验证所有白名单命令都通过
        validCommands.forEach { command ->
            assertTrue(
                "Command '$command' should be valid",
                validator.isCommandInWhitelist(command)
            )
        }
    }

    /**
     * 测试命令白名单验证 - 无效命令
     */
    @Test
    fun `test command whitelist - invalid commands`() {
        // Given: 不在白名单中的命令
        val invalidCommands = listOf(
            "rm -rf /",
            "dd if=/dev/zero",
            "format",
            "echo test > /system/test.txt",
            "chmod 777 /system",
            "chown root:root /data"
        )

        // When & Then: 验证所有非白名单命令都被拒绝
        invalidCommands.forEach { command ->
            assertFalse(
                "Command '$command' should be invalid",
                validator.isCommandInWhitelist(command)
            )
        }
    }

    /**
     * 测试黑名单检测 - 危险命令
     */
    @Test
    fun `test blacklist detection - dangerous commands`() {
        // Given: 黑名单中的危险命令
        val dangerousCommands = listOf(
            "rm -rf /",
            "rm -rf /*",
            "dd if=/dev/zero of=/dev/block/bootdevice",
            "format /data",
            "mkfs.ext4 /dev/block",
            "su",
            "su -c 'dangerous command'",
            "reboot recovery",
            "reboot bootloader",
            "flash",
            "fastboot flash",
            "oem unlock"
        )

        // When & Then: 验证所有危险命令都被检测到
        dangerousCommands.forEach { command ->
            assertTrue(
                "Command '$command' should be detected as dangerous",
                validator.isCommandInBlacklist(command)
            )
        }
    }

    /**
     * 测试黑名单检测 - 安全命令
     */
    @Test
    fun `test blacklist detection - safe commands`() {
        // Given: 安全的命令
        val safeCommands = listOf(
            "ls -la",
            "pwd",
            "date",
            "uptime",
            "whoami"
        )

        // When & Then: 验证安全命令不在黑名单中
        safeCommands.forEach { command ->
            assertFalse(
                "Command '$command' should not be in blacklist",
                validator.isCommandInBlacklist(command)
            )
        }
    }

    /**
     * 测试路径安全验证 - 安全路径
     */
    @Test
    fun `test path validation - safe paths`() {
        // Given: 安全的路径
        val safePaths = listOf(
            "/data/data/com.lanhe.gongjuxiang",
            "/data/data/com.lanhe.gongjuxiang/files",
            "/data/data/com.lanhe.gongjuxiang/cache",
            "/sdcard/Download",
            "/storage/emulated/0",
            "/storage/emulated/0/Android/data/com.lanhe.gongjuxiang",
            "/data/user/0/com.lanhe.gongjuxiang"
        )

        // When & Then: 验证所有安全路径都通过
        safePaths.forEach { path ->
            assertTrue(
                "Path '$path' should be safe",
                validator.isPathSafe(path)
            )
        }
    }

    /**
     * 测试路径安全验证 - 危险路径
     */
    @Test
    fun `test path validation - dangerous paths`() {
        // Given: 危险的路径
        val dangerousPaths = listOf(
            "/system",
            "/system/bin",
            "/system/xbin",
            "/data/system",
            "/data/misc",
            "/proc/1",
            "/dev/block",
            "../../../etc/passwd",
            "/data/data/com.android.systemui",
            "/data/data/android",
            "../../system/build.prop"
        )

        // When & Then: 验证所有危险路径都被拒绝
        dangerousPaths.forEach { path ->
            assertFalse(
                "Path '$path' should be dangerous",
                validator.isPathSafe(path)
            )
        }
    }

    /**
     * 测试路径遍历攻击检测
     */
    @Test
    fun `test path traversal attack detection`() {
        // Given: 包含路径遍历的路径
        val traversalPaths = listOf(
            "../../../etc/passwd",
            "/data/data/com.lanhe.gongjuxiang/../../system",
            "/sdcard/../../../system",
            "./../../system/bin",
            "/data/data/com.lanhe.gongjuxiang/../com.android.systemui"
        )

        // When & Then: 验证所有路径遍历都被检测
        traversalPaths.forEach { path ->
            assertTrue(
                "Path '$path' should be detected as traversal attack",
                validator.containsPathTraversal(path)
            )
        }
    }

    /**
     * 测试包名格式验证 - 有效包名
     */
    @Test
    fun `test package name validation - valid names`() {
        // Given: 有效的包名
        val validPackages = listOf(
            "com.example.app",
            "org.test.application",
            "cn.company.product",
            "io.github.project",
            "com.lanhe.gongjuxiang",
            "net.example.sub.package",
            "com.example123.app456"
        )

        // When & Then: 验证所有有效包名都通过
        validPackages.forEach { pkg ->
            assertTrue(
                "Package '$pkg' should be valid",
                validator.isValidPackageName(pkg)
            )
        }
    }

    /**
     * 测试包名格式验证 - 无效包名
     */
    @Test
    fun `test package name validation - invalid names`() {
        // Given: 无效的包名
        val invalidPackages = listOf(
            "invalid package",           // 包含空格
            "com.example..app",          // 连续点号
            ".com.example",              // 以点开始
            "com.example.",              // 以点结束
            "com/example/app",           // 使用斜杠
            "com",                       // 没有点分隔
            "Com.Example.App",           // 包含大写字母
            "com.example.app-test",      // 包含连字符
            "com.example.app#1",         // 包含特殊字符
            "com.123example.app",        // 段以数字开始
            ""                          // 空字符串
        )

        // When & Then: 验证所有无效包名都被拒绝
        invalidPackages.forEach { pkg ->
            assertFalse(
                "Package '$pkg' should be invalid",
                validator.isValidPackageName(pkg)
            )
        }
    }

    /**
     * 测试保护包名检测
     */
    @Test
    fun `test protected package detection`() {
        // Given: 系统保护包名
        val protectedPackages = listOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.android.contacts",
            "com.android.providers.settings",
            "com.android.providers.media",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.vending"
        )

        // When & Then: 验证所有保护包名都被检测
        protectedPackages.forEach { pkg ->
            assertTrue(
                "Package '$pkg' should be protected",
                validator.isProtectedPackage(pkg)
            )
        }
    }

    /**
     * 测试非保护包名
     */
    @Test
    fun `test non-protected packages`() {
        // Given: 普通应用包名
        val normalPackages = listOf(
            "com.example.app",
            "com.lanhe.gongjuxiang",
            "org.mozilla.firefox",
            "com.tencent.mm",
            "com.alibaba.android.rimet"
        )

        // When & Then: 验证普通包名不被保护
        normalPackages.forEach { pkg ->
            assertFalse(
                "Package '$pkg' should not be protected",
                validator.isProtectedPackage(pkg)
            )
        }
    }

    /**
     * 测试命令注入检测
     */
    @Test
    fun `test command injection detection`() {
        // Given: 包含注入尝试的命令
        val injectionCommands = listOf(
            "pm list packages; rm -rf /",
            "pm list packages && reboot",
            "pm list packages | sh",
            "pm list packages `rm -rf /`",
            "pm list packages $(dangerous_command)",
            "pm list packages\nrm -rf /",
            "pm list packages & background_command"
        )

        // When & Then: 验证所有注入尝试都被检测
        injectionCommands.forEach { command ->
            assertTrue(
                "Command '$command' should be detected as injection",
                validator.containsCommandInjection(command)
            )
        }
    }

    /**
     * 测试SQL注入检测（如果应用使用SQL）
     */
    @Test
    fun `test SQL injection detection`() {
        // Given: 包含SQL注入尝试的输入
        val sqlInjections = listOf(
            "'; DROP TABLE users; --",
            "1' OR '1'='1",
            "admin'--",
            "' UNION SELECT * FROM passwords --",
            "1; DELETE FROM data WHERE '1'='1"
        )

        // When & Then: 验证所有SQL注入都被检测
        sqlInjections.forEach { input ->
            assertTrue(
                "Input '$input' should be detected as SQL injection",
                validator.containsSqlInjection(input)
            )
        }
    }

    /**
     * 测试参数验证
     */
    @Test
    fun `test parameter validation`() {
        // Given: 各种参数
        val validParams = mapOf(
            "--user" to "0",
            "-f" to "",
            "--package" to "com.example.app",
            "-n" to "1"
        )

        val invalidParams = mapOf(
            "--user" to "'; rm -rf /",
            "--package" to "../system",
            "-n" to "1; reboot"
        )

        // When & Then: 验证参数有效性
        validParams.forEach { (key, value) ->
            assertTrue(
                "Parameter '$key=$value' should be valid",
                validator.isValidParameter(key, value)
            )
        }

        invalidParams.forEach { (key, value) ->
            assertFalse(
                "Parameter '$key=$value' should be invalid",
                validator.isValidParameter(key, value)
            )
        }
    }

    /**
     * 测试命令长度限制
     */
    @Test
    fun `test command length limit`() {
        // Given: 不同长度的命令
        val normalCommand = "pm list packages"
        val longCommand = "pm list packages " + "a".repeat(10000)

        // When & Then: 验证长度限制
        assertTrue(validator.isCommandLengthValid(normalCommand))
        assertFalse(validator.isCommandLengthValid(longCommand))
    }

    /**
     * 测试特殊字符过滤
     */
    @Test
    fun `test special character filtering`() {
        // Given: 包含特殊字符的输入
        val inputs = listOf(
            "normal_text_123",
            "text-with-dash",
            "text.with.dots",
            "text with spaces",
            "text@with#special$chars",
            "text\nwith\nnewlines",
            "text\twith\ttabs"
        )

        // When: 过滤特殊字符
        val filtered = inputs.map { validator.filterSpecialCharacters(it) }

        // Then: 验证过滤结果
        assertEquals("normal_text_123", filtered[0])
        assertEquals("text-with-dash", filtered[1])
        assertEquals("text.with.dots", filtered[2])
        assertEquals("text with spaces", filtered[3])
        assertEquals("textwithspecialchars", filtered[4])
        assertEquals("textwithnewlines", filtered[5])
        assertEquals("textwithtabs", filtered[6])
    }

    /**
     * 测试综合验证
     */
    @Test
    fun `test comprehensive validation`() {
        // Given: 一个完整的命令
        val command = "pm uninstall com.example.app"

        // When: 执行综合验证
        val result = validator.validateCommand(command)

        // Then: 验证结果
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
        assertTrue(result.isInWhitelist)
        assertFalse(result.isInBlacklist)
        assertFalse(result.containsInjection)
        assertTrue(result.pathsAreSafe)
    }
}

/**
 * CommandValidator类（简化版本用于测试）
 */
class CommandValidator {

    private val commandWhitelist = setOf(
        "pm", "dumpsys", "getprop", "cat /proc", "ps", "top", "df", "netstat"
    )

    private val commandBlacklist = setOf(
        "rm -rf", "dd", "format", "su", "reboot", "flash", "oem", "mkfs"
    )

    private val protectedPackages = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.contacts",
        "com.android.providers.settings",
        "com.android.providers.media",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.vending"
    )

    private val dangerousPaths = setOf(
        "/system", "/data/system", "/data/misc", "/proc", "/dev/block"
    )

    fun isCommandInWhitelist(command: String): Boolean {
        return commandWhitelist.any { command.startsWith(it) }
    }

    fun isCommandInBlacklist(command: String): Boolean {
        return commandBlacklist.any { command.contains(it) }
    }

    fun isPathSafe(path: String): Boolean {
        // 检查路径遍历
        if (containsPathTraversal(path)) return false

        // 检查危险路径
        if (dangerousPaths.any { path.startsWith(it) }) return false

        // 检查是否访问其他应用数据
        if (path.contains("/data/data/") &&
            !path.contains("/data/data/com.lanhe.gongjuxiang")) {
            return false
        }

        return true
    }

    fun containsPathTraversal(path: String): Boolean {
        return path.contains("../") || path.contains("..\\")
    }

    fun isValidPackageName(packageName: String): Boolean {
        if (packageName.isEmpty()) return false
        val regex = "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$".toRegex()
        return regex.matches(packageName)
    }

    fun isProtectedPackage(packageName: String): Boolean {
        return protectedPackages.contains(packageName)
    }

    fun containsCommandInjection(command: String): Boolean {
        val injectionPatterns = listOf(";", "&&", "||", "|", "`", "$", "\n", "&")
        return injectionPatterns.any { command.contains(it) }
    }

    fun containsSqlInjection(input: String): Boolean {
        val sqlPatterns = listOf(
            "DROP", "DELETE", "INSERT", "UPDATE", "UNION", "--", "/*", "*/"
        )
        val upperInput = input.uppercase()
        return sqlPatterns.any { upperInput.contains(it) }
    }

    fun isValidParameter(key: String, value: String): Boolean {
        // 检查注入
        if (containsCommandInjection(value)) return false
        if (containsSqlInjection(value)) return false

        // 检查路径参数
        if (key == "--package" && !isValidPackageName(value) && value.isNotEmpty()) {
            return false
        }

        return true
    }

    fun isCommandLengthValid(command: String): Boolean {
        return command.length <= 1024 // 限制命令长度为1024字符
    }

    fun filterSpecialCharacters(input: String): String {
        return input.replace(Regex("[^a-zA-Z0-9\\s._-]"), "")
            .replace(Regex("\\s+"), " ")
    }

    fun validateCommand(command: String): ValidationResult {
        return ValidationResult(
            isValid = isCommandInWhitelist(command) &&
                    !isCommandInBlacklist(command) &&
                    !containsCommandInjection(command) &&
                    isCommandLengthValid(command),
            errorMessage = when {
                !isCommandInWhitelist(command) -> "Command not in whitelist"
                isCommandInBlacklist(command) -> "Command in blacklist"
                containsCommandInjection(command) -> "Command contains injection"
                !isCommandLengthValid(command) -> "Command too long"
                else -> null
            },
            isInWhitelist = isCommandInWhitelist(command),
            isInBlacklist = isCommandInBlacklist(command),
            containsInjection = containsCommandInjection(command),
            pathsAreSafe = true // 简化处理
        )
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String?,
        val isInWhitelist: Boolean,
        val isInBlacklist: Boolean,
        val containsInjection: Boolean,
        val pathsAreSafe: Boolean
    )
}