package com.lanhe.gongjuxiang.utils

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * 权限常量定义
 * 定义应用中所有需要的权限及其分组
 */
object PermissionConstants {

    /**
     * 权限优先级
     */
    enum class Priority {
        CRITICAL,   // 关键权限，启动时请求
        OPTIONAL    // 可选权限，使用时请求
    }

    /**
     * 权限组定义
     */
    data class PermissionGroup(
        val name: String,
        val permissions: Array<String>,
        val description: String,
        val priority: Priority = Priority.OPTIONAL
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as PermissionGroup
            if (name != other.name) return false
            if (!permissions.contentEquals(other.permissions)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + permissions.contentHashCode()
            return result
        }
    }

    // ============================
    // 电话相关权限
    // ============================
    val PHONE_PERMISSIONS = PermissionGroup(
        name = "电话权限",
        permissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ANSWER_PHONE_CALLS
        ),
        description = "用于号码识别、骚扰拦截、通话管理等功能",
        priority = Priority.OPTIONAL
    )

    // ============================
    // 位置权限
    // ============================
    val LOCATION_PERMISSIONS = PermissionGroup(
        name = "位置权限",
        permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ),
        description = "用于网络诊断、基站信息获取、WiFi扫描等功能",
        priority = Priority.OPTIONAL
    )

    // ============================
    // 存储权限
    // ============================
    val STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Android 11及以上使用MANAGE_EXTERNAL_STORAGE
        PermissionGroup(
            name = "存储权限",
            permissions = arrayOf(
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            ),
            description = "用于文件管理、垃圾清理、应用备份等功能",
            priority = Priority.CRITICAL
        )
    } else {
        // Android 10及以下使用传统存储权限
        PermissionGroup(
            name = "存储权限",
            permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            description = "用于文件管理、垃圾清理、应用备份等功能",
            priority = Priority.CRITICAL
        )
    }

    // ============================
    // 日历权限
    // ============================
    val CALENDAR_PERMISSIONS = PermissionGroup(
        name = "日历权限",
        permissions = arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        ),
        description = "用于提醒事项、日程管理等功能",
        priority = Priority.OPTIONAL
    )

    // ============================
    // 短信权限
    // ============================
    val SMS_PERMISSIONS = PermissionGroup(
        name = "短信权限",
        permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS
        ),
        description = "用于短信管理、垃圾短信过滤、验证码提取等功能",
        priority = Priority.OPTIONAL
    )

    // ============================
    // 相机权限
    // ============================
    val CAMERA_PERMISSIONS = PermissionGroup(
        name = "相机权限",
        permissions = arrayOf(
            Manifest.permission.CAMERA
        ),
        description = "用于扫码功能、拍照等",
        priority = Priority.OPTIONAL
    )

    // ============================
    // 联系人权限
    // ============================
    val CONTACTS_PERMISSIONS = PermissionGroup(
        name = "联系人权限",
        permissions = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        ),
        description = "用于联系人备份、号码识别等功能",
        priority = Priority.OPTIONAL
    )

    // ============================
    // 通知权限 (Android 13+)
    // ============================
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val NOTIFICATION_PERMISSIONS = PermissionGroup(
        name = "通知权限",
        permissions = arrayOf(
            Manifest.permission.POST_NOTIFICATIONS
        ),
        description = "用于显示优化提醒、状态更新等通知",
        priority = Priority.CRITICAL
    )

    // ============================
    // 应用使用情况权限
    // ============================
    val USAGE_STATS_PERMISSIONS = PermissionGroup(
        name = "使用情况权限",
        permissions = arrayOf(
            Manifest.permission.PACKAGE_USAGE_STATS
        ),
        description = "用于应用使用分析、电量消耗统计等功能",
        priority = Priority.OPTIONAL
    )

    // ============================
    // 悬浮窗权限
    // ============================
    val OVERLAY_PERMISSIONS = PermissionGroup(
        name = "悬浮窗权限",
        permissions = arrayOf(
            Manifest.permission.SYSTEM_ALERT_WINDOW
        ),
        description = "用于悬浮球、小窗口等功能",
        priority = Priority.OPTIONAL
    )

    // ============================
    // 系统设置权限
    // ============================
    val SYSTEM_SETTINGS_PERMISSIONS = PermissionGroup(
        name = "系统设置权限",
        permissions = arrayOf(
            Manifest.permission.WRITE_SETTINGS
        ),
        description = "用于修改系统设置、快速设置等功能",
        priority = Priority.OPTIONAL
    )

    // ============================
    // 无障碍权限
    // ============================
    val ACCESSIBILITY_PERMISSIONS = PermissionGroup(
        name = "无障碍权限",
        permissions = arrayOf(
            Manifest.permission.BIND_ACCESSIBILITY_SERVICE
        ),
        description = "用于自动化操作、辅助功能等",
        priority = Priority.OPTIONAL
    )

    /**
     * 获取所有关键权限组
     */
    fun getCriticalPermissionGroups(): List<PermissionGroup> {
        val groups = mutableListOf(
            STORAGE_PERMISSIONS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            groups.add(NOTIFICATION_PERMISSIONS)
        }

        return groups
    }

    /**
     * 获取所有可选权限组
     */
    fun getOptionalPermissionGroups(): List<PermissionGroup> {
        return listOf(
            PHONE_PERMISSIONS,
            LOCATION_PERMISSIONS,
            CALENDAR_PERMISSIONS,
            SMS_PERMISSIONS,
            CAMERA_PERMISSIONS,
            CONTACTS_PERMISSIONS,
            USAGE_STATS_PERMISSIONS,
            OVERLAY_PERMISSIONS,
            SYSTEM_SETTINGS_PERMISSIONS
        )
    }

    /**
     * 获取所有权限组
     */
    fun getAllPermissionGroups(): List<PermissionGroup> {
        val allGroups = mutableListOf<PermissionGroup>()
        allGroups.addAll(getCriticalPermissionGroups())
        allGroups.addAll(getOptionalPermissionGroups())
        return allGroups
    }

    /**
     * 根据权限获取权限组
     */
    fun getPermissionGroupByPermission(permission: String): PermissionGroup? {
        return getAllPermissionGroups().find { group ->
            group.permissions.contains(permission)
        }
    }

    /**
     * 获取权限的中文描述
     */
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.READ_PHONE_STATE -> "读取手机状态和身份"
            Manifest.permission.CALL_PHONE -> "直接拨打电话"
            Manifest.permission.READ_CALL_LOG -> "读取通话记录"
            Manifest.permission.ANSWER_PHONE_CALLS -> "接听电话"
            Manifest.permission.ACCESS_FINE_LOCATION -> "访问精确位置"
            Manifest.permission.ACCESS_COARSE_LOCATION -> "访问大概位置"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "读取存储空间"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "写入存储空间"
            Manifest.permission.MANAGE_EXTERNAL_STORAGE -> "管理所有文件"
            Manifest.permission.READ_CALENDAR -> "读取日历"
            Manifest.permission.WRITE_CALENDAR -> "写入日历"
            Manifest.permission.READ_SMS -> "读取短信"
            Manifest.permission.RECEIVE_SMS -> "接收短信"
            Manifest.permission.SEND_SMS -> "发送短信"
            Manifest.permission.CAMERA -> "使用相机"
            Manifest.permission.READ_CONTACTS -> "读取联系人"
            Manifest.permission.WRITE_CONTACTS -> "写入联系人"
            Manifest.permission.POST_NOTIFICATIONS -> "发送通知"
            Manifest.permission.PACKAGE_USAGE_STATS -> "访问应用使用情况"
            Manifest.permission.SYSTEM_ALERT_WINDOW -> "显示悬浮窗"
            Manifest.permission.WRITE_SETTINGS -> "修改系统设置"
            else -> "未知权限"
        }
    }
}