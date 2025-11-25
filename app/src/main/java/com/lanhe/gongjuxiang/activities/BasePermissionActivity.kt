package com.lanhe.gongjuxiang.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lanhe.gongjuxiang.utils.PermissionHelper
import com.lanhe.gongjuxiang.utils.PermissionConstants

/**
 * 基础Activity，提供权限管理的通用功能
 */
abstract class BasePermissionActivity : AppCompatActivity() {

    protected lateinit var permissionHelper: PermissionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHelper = PermissionHelper.getInstance(this)
    }

    /**
     * 请求特定权限组
     */
    protected fun requestPermissionGroup(
        permissionGroup: PermissionConstants.PermissionGroup,
        onGranted: () -> Unit,
        onDenied: (() -> Unit)? = null
    ) {
        permissionHelper.requestPermissionGroup(
            this,
            permissionGroup,
            object : PermissionHelper.PermissionCallback {
                override fun onPermissionsGranted() {
                    onGranted()
                }

                override fun onPermissionsDenied(deniedPermissions: List<String>) {
                    onDenied?.invoke() ?: showPermissionDeniedToast(permissionGroup.name)
                }

                override fun onPermissionsPermanentlyDenied(permanentlyDeniedPermissions: List<String>) {
                    onDenied?.invoke() ?: showPermissionPermanentlyDeniedToast(permissionGroup.name)
                }
            }
        )
    }

    /**
     * 检查权限组
     */
    protected fun checkPermissionGroup(permissionGroup: PermissionConstants.PermissionGroup): Boolean {
        return permissionHelper.hasAllPermissions(permissionGroup.permissions)
    }

    /**
     * 带权限检查的执行
     */
    protected fun executeWithPermission(
        permissionGroup: PermissionConstants.PermissionGroup,
        action: () -> Unit
    ) {
        if (checkPermissionGroup(permissionGroup)) {
            action()
        } else {
            requestPermissionGroup(
                permissionGroup,
                onGranted = action,
                onDenied = {
                    showPermissionRequiredToast(permissionGroup.name)
                }
            )
        }
    }

    /**
     * 带权限检查的安全执行（权限缺失时降级处理）
     */
    protected fun executeWithPermissionSafe(
        permissionGroup: PermissionConstants.PermissionGroup,
        action: () -> Unit,
        fallbackAction: (() -> Unit)? = null
    ) {
        if (checkPermissionGroup(permissionGroup)) {
            action()
        } else {
            fallbackAction?.invoke() ?: showPermissionDegradedToast(permissionGroup.name)
        }
    }

    /**
     * 显示权限被拒绝的提示
     */
    private fun showPermissionDeniedToast(permissionName: String) {
        Toast.makeText(
            this,
            "$permissionName 被拒绝，相关功能将不可用",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * 显示权限被永久拒绝的提示
     */
    private fun showPermissionPermanentlyDeniedToast(permissionName: String) {
        Toast.makeText(
            this,
            "$permissionName 被永久拒绝，请在设置中手动开启",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * 显示需要权限的提示
     */
    private fun showPermissionRequiredToast(permissionName: String) {
        Toast.makeText(
            this,
            "此功能需要 $permissionName",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * 显示降级模式提示
     */
    private fun showPermissionDegradedToast(permissionName: String) {
        Toast.makeText(
            this,
            "缺少 $permissionName，功能受限",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * 打开应用设置
     */
    protected fun openAppSettings() {
        permissionHelper.openAppSettings(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PermissionHelper.REQUEST_CODE_SETTINGS) {
            // 从设置返回后，可以重新检查权限
            onReturnFromSettings()
        }
    }

    /**
     * 从设置页面返回后的回调
     * 子类可以重写此方法以处理返回后的逻辑
     */
    protected open fun onReturnFromSettings() {
        // 默认空实现，子类可重写
    }

    override fun onDestroy() {
        super.onDestroy()
        permissionHelper.clear()
    }
}