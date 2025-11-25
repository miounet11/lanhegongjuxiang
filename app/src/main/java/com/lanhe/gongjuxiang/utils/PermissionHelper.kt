package com.lanhe.gongjuxiang.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.lanhe.gongjuxiang.LanheApplication

/**
 * 权限管理助手类
 * 提供统一的权限检查和请求接口
 */
class PermissionHelper private constructor(private val context: Context) {

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 1001
        const val REQUEST_CODE_SETTINGS = 1002

        @Volatile
        private var INSTANCE: PermissionHelper? = null

        /**
         * 获取单例实例
         */
        fun getInstance(context: Context = LanheApplication.getContext()): PermissionHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PermissionHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // 权限回调接口
    interface PermissionCallback {
        fun onPermissionsGranted()
        fun onPermissionsDenied(deniedPermissions: List<String>)
        fun onPermissionsPermanentlyDenied(permanentlyDeniedPermissions: List<String>)
    }

    // 存储权限请求回调
    private var currentCallback: PermissionCallback? = null
    private var currentActivity: Activity? = null

    /**
     * 检查单个权限
     */
    fun checkPermission(permission: String): Boolean {
        // 特殊权限处理
        return when (permission) {
            android.Manifest.permission.SYSTEM_ALERT_WINDOW -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else {
                    true
                }
            }
            android.Manifest.permission.WRITE_SETTINGS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.System.canWrite(context)
                } else {
                    true
                }
            }
            android.Manifest.permission.PACKAGE_USAGE_STATS -> {
                checkUsageStatsPermission()
            }
            android.Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.os.Environment.isExternalStorageManager()
                } else {
                    checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            else -> {
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    /**
     * 检查多个权限
     */
    fun hasAllPermissions(permissions: Array<String>): Boolean {
        return permissions.all { checkPermission(it) }
    }

    /**
     * 检查多个权限，返回未授权的权限列表
     */
    fun getDeniedPermissions(permissions: Array<String>): List<String> {
        return permissions.filter { !checkPermission(it) }
    }

    /**
     * 请求权限
     */
    fun requestPermissions(
        activity: Activity,
        permissions: Array<String>,
        callback: PermissionCallback? = null
    ) {
        currentActivity = activity
        currentCallback = callback

        // 分离特殊权限和普通权限
        val specialPermissions = mutableListOf<String>()
        val normalPermissions = mutableListOf<String>()

        permissions.forEach { permission ->
            if (isSpecialPermission(permission)) {
                specialPermissions.add(permission)
            } else {
                normalPermissions.add(permission)
            }
        }

        // 处理特殊权限
        specialPermissions.forEach { permission ->
            if (!checkPermission(permission)) {
                requestSpecialPermission(activity, permission)
            }
        }

        // 处理普通权限
        if (normalPermissions.isNotEmpty()) {
            val deniedPermissions = getDeniedPermissions(normalPermissions.toTypedArray())
            if (deniedPermissions.isNotEmpty()) {
                // 检查是否需要显示权限解释
                val shouldShowRationale = deniedPermissions.any {
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
                }

                if (shouldShowRationale) {
                    showPermissionRationaleDialog(activity, deniedPermissions.toTypedArray())
                } else {
                    ActivityCompat.requestPermissions(
                        activity,
                        deniedPermissions.toTypedArray(),
                        REQUEST_CODE_PERMISSIONS
                    )
                }
            } else {
                // 所有权限都已授予
                callback?.onPermissionsGranted()
            }
        }
    }

    /**
     * 请求权限组
     */
    fun requestPermissionGroup(
        activity: Activity,
        permissionGroup: PermissionConstants.PermissionGroup,
        callback: PermissionCallback? = null
    ) {
        requestPermissions(activity, permissionGroup.permissions, callback)
    }

    /**
     * 检查权限组
     */
    fun checkPermissionGroup(permissionGroup: PermissionConstants.PermissionGroup): Boolean {
        return hasAllPermissions(permissionGroup.permissions)
    }

    /**
     * 请求关键权限
     */
    fun requestCriticalPermissions(
        activity: Activity,
        callback: PermissionCallback? = null
    ) {
        val criticalGroups = PermissionConstants.getCriticalPermissionGroups()
        val allPermissions = mutableListOf<String>()

        criticalGroups.forEach { group ->
            allPermissions.addAll(group.permissions)
        }

        requestPermissions(activity, allPermissions.toTypedArray(), callback)
    }

    /**
     * 打开应用设置页面
     */
    fun openAppSettings(activity: Activity? = currentActivity) {
        activity?.let {
            val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", it.packageName, null)
            }
            it.startActivityForResult(intent, REQUEST_CODE_SETTINGS)
        }
    }

    /**
     * 处理权限请求结果
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != REQUEST_CODE_PERMISSIONS) return

        val deniedPermissions = mutableListOf<String>()
        val permanentlyDeniedPermissions = mutableListOf<String>()

        permissions.forEachIndexed { index, permission ->
            if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permission)

                currentActivity?.let { activity ->
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                        permanentlyDeniedPermissions.add(permission)
                    }
                }
            }
        }

        when {
            deniedPermissions.isEmpty() -> {
                currentCallback?.onPermissionsGranted()
            }
            permanentlyDeniedPermissions.isNotEmpty() -> {
                currentCallback?.onPermissionsPermanentlyDenied(permanentlyDeniedPermissions)
                showPermissionDeniedDialog(permanentlyDeniedPermissions)
            }
            else -> {
                currentCallback?.onPermissionsDenied(deniedPermissions)
                showRetryPermissionDialog(deniedPermissions)
            }
        }
    }

    /**
     * 显示权限解释对话框
     */
    private fun showPermissionRationaleDialog(activity: Activity, permissions: Array<String>) {
        val permissionGroup = PermissionConstants.getPermissionGroupByPermission(permissions[0])
        val message = buildPermissionRationaleMessage(permissions)

        AlertDialog.Builder(activity)
            .setTitle("需要${permissionGroup?.name ?: "相关"}权限")
            .setMessage(message)
            .setPositiveButton("授权") { _, _ ->
                ActivityCompat.requestPermissions(
                    activity,
                    permissions,
                    REQUEST_CODE_PERMISSIONS
                )
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                currentCallback?.onPermissionsDenied(permissions.toList())
            }
            .setCancelable(false)
            .show()
    }

    /**
     * 显示权限被拒绝的重试对话框
     */
    private fun showRetryPermissionDialog(deniedPermissions: List<String>) {
        currentActivity?.let { activity ->
            val message = buildPermissionDeniedMessage(deniedPermissions)

            AlertDialog.Builder(activity)
                .setTitle("权限未授予")
                .setMessage(message)
                .setPositiveButton("重试") { _, _ ->
                    ActivityCompat.requestPermissions(
                        activity,
                        deniedPermissions.toTypedArray(),
                        REQUEST_CODE_PERMISSIONS
                    )
                }
                .setNegativeButton("暂不授权") { dialog, _ ->
                    dialog.dismiss()
                    showToastForDegradedMode(deniedPermissions)
                }
                .setCancelable(false)
                .show()
        }
    }

    /**
     * 显示权限被永久拒绝的对话框
     */
    private fun showPermissionDeniedDialog(permanentlyDeniedPermissions: List<String>) {
        currentActivity?.let { activity ->
            val message = buildPermanentlyDeniedMessage(permanentlyDeniedPermissions)

            AlertDialog.Builder(activity)
                .setTitle("需要手动授权")
                .setMessage(message)
                .setPositiveButton("去设置") { _, _ ->
                    openAppSettings(activity)
                }
                .setNegativeButton("暂不设置") { dialog, _ ->
                    dialog.dismiss()
                    showToastForDegradedMode(permanentlyDeniedPermissions)
                }
                .setCancelable(false)
                .show()
        }
    }

    /**
     * 构建权限解释消息
     */
    private fun buildPermissionRationaleMessage(permissions: Array<String>): String {
        val permissionGroup = PermissionConstants.getPermissionGroupByPermission(permissions[0])
        val builder = StringBuilder()

        builder.append("为了提供完整的功能体验，")
        builder.append(permissionGroup?.description ?: "需要以下权限")
        builder.append("\n\n需要的权限：\n")

        permissions.forEach { permission ->
            builder.append("• ${PermissionConstants.getPermissionDescription(permission)}\n")
        }

        return builder.toString()
    }

    /**
     * 构建权限被拒绝的消息
     */
    private fun buildPermissionDeniedMessage(deniedPermissions: List<String>): String {
        val builder = StringBuilder()
        builder.append("以下权限未被授予，相关功能将无法使用：\n\n")

        deniedPermissions.forEach { permission ->
            builder.append("• ${PermissionConstants.getPermissionDescription(permission)}\n")
        }

        builder.append("\n是否重新请求权限？")
        return builder.toString()
    }

    /**
     * 构建权限被永久拒绝的消息
     */
    private fun buildPermanentlyDeniedMessage(permanentlyDeniedPermissions: List<String>): String {
        val builder = StringBuilder()
        builder.append("您已拒绝以下权限，需要在设置中手动开启：\n\n")

        permanentlyDeniedPermissions.forEach { permission ->
            builder.append("• ${PermissionConstants.getPermissionDescription(permission)}\n")
        }

        builder.append("\n请点击\"去设置\"按钮，然后在权限管理中开启相应权限。")
        return builder.toString()
    }

    /**
     * 显示降级模式提示
     */
    private fun showToastForDegradedMode(deniedPermissions: List<String>) {
        val message = when {
            deniedPermissions.any { it.contains("STORAGE") } -> {
                "存储权限未授予，文件管理功能将不可用"
            }
            deniedPermissions.any { it.contains("PHONE") } -> {
                "电话权限未授予，号码识别功能将不可用"
            }
            deniedPermissions.any { it.contains("LOCATION") } -> {
                "位置权限未授予，网络诊断功能将受限"
            }
            deniedPermissions.any { it.contains("SMS") } -> {
                "短信权限未授予，短信管理功能将不可用"
            }
            else -> {
                "部分权限未授予，相关功能将受限"
            }
        }

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * 检查是否是特殊权限
     */
    private fun isSpecialPermission(permission: String): Boolean {
        return permission in listOf(
            android.Manifest.permission.SYSTEM_ALERT_WINDOW,
            android.Manifest.permission.WRITE_SETTINGS,
            android.Manifest.permission.PACKAGE_USAGE_STATS,
            android.Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE
        )
    }

    /**
     * 请求特殊权限
     */
    private fun requestSpecialPermission(activity: Activity, permission: String) {
        when (permission) {
            android.Manifest.permission.SYSTEM_ALERT_WINDOW -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivityForResult(intent, REQUEST_CODE_SETTINGS)
                }
            }
            android.Manifest.permission.WRITE_SETTINGS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivityForResult(intent, REQUEST_CODE_SETTINGS)
                }
            }
            android.Manifest.permission.PACKAGE_USAGE_STATS -> {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                activity.startActivityForResult(intent, REQUEST_CODE_SETTINGS)
            }
            android.Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivityForResult(intent, REQUEST_CODE_SETTINGS)
                }
            }
        }
    }

    /**
     * 检查使用情况统计权限
     */
    private fun checkUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    /**
     * 清理资源
     */
    fun clear() {
        currentActivity = null
        currentCallback = null
    }
}