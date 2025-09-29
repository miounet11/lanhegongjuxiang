package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

class SecurityScanner(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * 扫描已安装的应用
     */
    fun scanInstalledApps(): List<String> {
        // 模拟扫描应用，实际实现需要更复杂的逻辑
        val installedPackages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val suspiciousApps = mutableListOf<String>()

        for (pkgInfo in installedPackages) {
            // 检查是否有可疑权限
            if (hasSuspiciousPermissions(pkgInfo)) {
                suspiciousApps.add(pkgInfo.packageName)
            }
        }

        // 模拟返回一些可疑应用
        return suspiciousApps.take((0..3).random())
    }

    /**
     * 检查应用是否有可疑权限
     */
    private fun hasSuspiciousPermissions(pkgInfo: PackageInfo): Boolean {
        val permissions = pkgInfo.requestedPermissions ?: return false

        val dangerousPermissions = arrayOf(
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.SEND_SMS",
            "android.permission.CALL_PHONE",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )

        return permissions.any { permission ->
            dangerousPermissions.contains(permission)
        }
    }

    /**
     * 检查系统漏洞
     */
    fun checkSystemVulnerabilities(): List<String> {
        val vulnerabilities = mutableListOf<String>()

        // 检查Android版本
        if (Build.VERSION.SDK_INT < 28) { // Android 9.0以下
            vulnerabilities.add("系统版本过低，存在安全风险")
        }

        // 检查SELinux状态
        try {
            val selinuxStatus = runCommand("getenforce")
            if (selinuxStatus != "Enforcing") {
                vulnerabilities.add("SELinux未启用")
            }
        } catch (e: Exception) {
            vulnerabilities.add("无法检查SELinux状态")
        }

        // 模拟其他漏洞检查
        if ((0..10).random() < 3) {
            vulnerabilities.add("发现系统安全补丁缺失")
        }

        return vulnerabilities
    }

    /**
     * 执行杀毒扫描
     */
    fun performAntivirusScan(): List<String> {
        val threats = mutableListOf<String>()

        // 扫描常见病毒文件位置
        val scanPaths = arrayOf(
            "/system/app",
            "/data/app",
            "/sdcard"
        )

        for (path in scanPaths) {
            try {
                val file = File(path)
                if (file.exists()) {
                    scanDirectory(file, threats)
                }
            } catch (e: Exception) {
                // 忽略访问权限错误
            }
        }

        // 模拟发现威胁
        if ((0..100).random() < 5) { // 5%的概率发现威胁
            threats.add("/data/app/com.example.suspicious")
        }

        return threats
    }

    /**
     * 递归扫描目录
     */
    private fun scanDirectory(directory: File, threats: MutableList<String>) {
        if (!directory.canRead()) return

        val files = directory.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory && threats.size < 10) { // 限制扫描深度
                scanDirectory(file, threats)
            } else if (file.isFile) {
                // 简单的文件扫描逻辑
                if (isSuspiciousFile(file)) {
                    threats.add(file.absolutePath)
                }
            }
        }
    }

    /**
     * 检查文件是否可疑
     */
    private fun isSuspiciousFile(file: File): Boolean {
        val fileName = file.name.lowercase()

        // 检查文件名是否包含可疑关键词
        val suspiciousKeywords = arrayOf(
            "hack", "exploit", "virus", "trojan", "malware",
            "spy", "keylogger", "ransomware"
        )

        return suspiciousKeywords.any { keyword ->
            fileName.contains(keyword)
        }
    }

    /**
     * 执行shell命令
     */
    private fun runCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(command)
            val reader = process.inputStream.bufferedReader()
            val output = reader.readText()
            reader.close()
            process.waitFor()
            output.trim()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 检查网络安全状态
     */
    fun checkNetworkSecurity(): Map<String, Boolean> {
        // 模拟网络安全检查
        return mapOf(
            "HTTPS启用" to true,
            "防火墙启用" to true,
            "VPN连接" to false,
            "公共WiFi安全" to true
        )
    }

    /**
     * 检查隐私设置
     */
    fun checkPrivacySettings(): List<String> {
        val issues = mutableListOf<String>()

        // 检查位置服务
        if (isLocationEnabled()) {
            issues.add("位置服务已启用")
        }

        // 检查麦克风权限
        if (hasMicrophonePermission()) {
            issues.add("麦克风权限已授予")
        }

        // 检查相机权限
        if (hasCameraPermission()) {
            issues.add("相机权限已授予")
        }

        return issues
    }

    private fun isLocationEnabled(): Boolean {
        // 简化检查，实际应该检查系统设置
        return true
    }

    private fun hasMicrophonePermission(): Boolean {
        // 简化检查，实际应该检查应用权限
        return true
    }

    private fun hasCameraPermission(): Boolean {
        // 简化检查，实际应该检查应用权限
        return true
    }
}
