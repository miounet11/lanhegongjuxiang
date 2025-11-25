package com.lanhe.gongjuxiang.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

/**
 * PermissionHelper单元测试
 * 测试权限检查、请求、特殊权限处理等功能
 */
@RunWith(MockitoJUnitRunner::class)
class PermissionHelperTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockActivity: Activity

    @Mock
    private lateinit var mockPackageManager: PackageManager

    private lateinit var permissionHelper: PermissionHelper

    @Before
    fun setup() {
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        `when`(mockActivity.packageManager).thenReturn(mockPackageManager)

        permissionHelper = PermissionHelper.getInstance(mockContext)
    }

    /**
     * 测试单个权限检查 - 已授权
     */
    @Test
    fun `test check single permission - granted`() {
        // Given: 权限已授予
        val permission = Manifest.permission.CAMERA
        `when`(ContextCompat.checkSelfPermission(mockContext, permission))
            .thenReturn(PackageManager.PERMISSION_GRANTED)

        // When: 检查权限
        val hasPermission = permissionHelper.hasPermission(permission)

        // Then: 验证权限已授予
        assertTrue(hasPermission)
    }

    /**
     * 测试单个权限检查 - 未授权
     */
    @Test
    fun `test check single permission - denied`() {
        // Given: 权限被拒绝
        val permission = Manifest.permission.CAMERA
        `when`(ContextCompat.checkSelfPermission(mockContext, permission))
            .thenReturn(PackageManager.PERMISSION_DENIED)

        // When: 检查权限
        val hasPermission = permissionHelper.hasPermission(permission)

        // Then: 验证权限被拒绝
        assertFalse(hasPermission)
    }

    /**
     * 测试多个权限检查
     */
    @Test
    fun `test check multiple permissions`() {
        // Given: 设置多个权限状态
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        `when`(ContextCompat.checkSelfPermission(mockContext, permissions[0]))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        `when`(ContextCompat.checkSelfPermission(mockContext, permissions[1]))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        `when`(ContextCompat.checkSelfPermission(mockContext, permissions[2]))
            .thenReturn(PackageManager.PERMISSION_GRANTED)

        // When: 检查所有权限
        val hasAllPermissions = permissionHelper.hasAllPermissions(*permissions)
        val missingPermissions = permissionHelper.getMissingPermissions(*permissions)

        // Then: 验证结果
        assertFalse(hasAllPermissions) // 不是所有权限都已授予
        assertEquals(1, missingPermissions.size)
        assertTrue(missingPermissions.contains(permissions[1]))
    }

    /**
     * 测试权限组处理
     */
    @Test
    fun `test permission group handling`() {
        // Given: 存储权限组
        val storagePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        // When: 检查权限组
        storagePermissions.forEach { permission ->
            `when`(ContextCompat.checkSelfPermission(mockContext, permission))
                .thenReturn(PackageManager.PERMISSION_GRANTED)
        }

        val hasStoragePermissions = permissionHelper.hasStoragePermissions()

        // Then: 验证权限组状态
        assertTrue(hasStoragePermissions)
    }

    /**
     * 测试危险权限识别
     */
    @Test
    fun `test dangerous permission identification`() {
        // Given: 各种权限
        val dangerousPermissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val normalPermissions = listOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WAKE_LOCK
        )

        // When & Then: 验证权限分类
        dangerousPermissions.forEach { permission ->
            assertTrue(
                "Should be dangerous: $permission",
                permissionHelper.isDangerousPermission(permission)
            )
        }

        normalPermissions.forEach { permission ->
            assertFalse(
                "Should be normal: $permission",
                permissionHelper.isDangerousPermission(permission)
            )
        }
    }

    /**
     * 测试特殊权限处理 - 系统设置写入权限
     */
    @Test
    fun `test special permission - write settings`() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Given: 系统设置写入权限
            val permission = Manifest.permission.WRITE_SETTINGS

            // When: 检查特殊权限
            val needsSpecialHandling = permissionHelper.needsSpecialPermissionHandling(permission)

            // Then: 验证需要特殊处理
            assertTrue(needsSpecialHandling)
        }
    }

    /**
     * 测试特殊权限处理 - 悬浮窗权限
     */
    @Test
    fun `test special permission - overlay`() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Given: 悬浮窗权限
            val permission = Manifest.permission.SYSTEM_ALERT_WINDOW

            // When: 检查特殊权限
            val needsSpecialHandling = permissionHelper.needsSpecialPermissionHandling(permission)

            // Then: 验证需要特殊处理
            assertTrue(needsSpecialHandling)
        }
    }

    /**
     * 测试权限请求前的解释检查
     */
    @Test
    fun `test should show permission rationale`() {
        // Given: 设置权限解释状态
        val permission = Manifest.permission.CAMERA
        `when`(ActivityCompat.shouldShowRequestPermissionRationale(mockActivity, permission))
            .thenReturn(true)

        // When: 检查是否需要解释
        val shouldShowRationale = permissionHelper.shouldShowRationale(mockActivity, permission)

        // Then: 验证结果
        assertTrue(shouldShowRationale)
    }

    /**
     * 测试权限永久拒绝检测
     */
    @Test
    fun `test permanently denied permission detection`() {
        // Given: 权限被拒绝且不显示理由
        val permission = Manifest.permission.CAMERA
        `when`(ContextCompat.checkSelfPermission(mockContext, permission))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        `when`(ActivityCompat.shouldShowRequestPermissionRationale(mockActivity, permission))
            .thenReturn(false)

        // When: 检查是否永久拒绝
        val isPermanentlyDenied = permissionHelper.isPermanentlyDenied(mockActivity, permission)

        // Then: 验证永久拒绝状态
        assertTrue(isPermanentlyDenied)
    }

    /**
     * 测试权限请求代码生成
     */
    @Test
    fun `test permission request code generation`() {
        // Given: 多个权限
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        // When: 生成请求代码
        val requestCode1 = permissionHelper.generateRequestCode(permissions)
        val requestCode2 = permissionHelper.generateRequestCode(permissions)

        // Then: 验证请求代码唯一性
        assertEquals(requestCode1, requestCode2) // 相同权限集应生成相同代码
    }

    /**
     * 测试权限结果处理
     */
    @Test
    fun `test permission result handling`() {
        // Given: 权限请求结果
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val grantResults = intArrayOf(
            PackageManager.PERMISSION_GRANTED,
            PackageManager.PERMISSION_DENIED,
            PackageManager.PERMISSION_GRANTED
        )

        // When: 处理权限结果
        val result = permissionHelper.handlePermissionResult(permissions, grantResults)

        // Then: 验证结果分析
        assertEquals(2, result.grantedPermissions.size)
        assertEquals(1, result.deniedPermissions.size)
        assertTrue(result.grantedPermissions.contains(permissions[0]))
        assertTrue(result.grantedPermissions.contains(permissions[2]))
        assertTrue(result.deniedPermissions.contains(permissions[1]))
    }

    /**
     * 测试Android版本兼容性
     */
    @Test
    fun `test android version compatibility`() {
        // Given: 不同Android版本的权限
        val storagePermissions = permissionHelper.getStoragePermissionsForVersion()

        // Then: 验证版本适配
        assertNotNull(storagePermissions)
        assertTrue(storagePermissions.isNotEmpty())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            assertTrue(storagePermissions.contains(Manifest.permission.READ_MEDIA_IMAGES))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-12
            assertFalse(storagePermissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        } else {
            // Android 9及以下
            assertTrue(storagePermissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    /**
     * 测试权限分组功能
     */
    @Test
    fun `test permission grouping`() {
        // Given: 混合权限列表
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS
        )

        // When: 按组分类权限
        val groupedPermissions = permissionHelper.groupPermissions(permissions)

        // Then: 验证分组结果
        assertTrue(groupedPermissions.containsKey("LOCATION"))
        assertEquals(2, groupedPermissions["LOCATION"]?.size)
        assertTrue(groupedPermissions.containsKey("CAMERA"))
        assertEquals(1, groupedPermissions["CAMERA"]?.size)
    }

    /**
     * 测试权限状态缓存
     */
    @Test
    fun `test permission status caching`() {
        // Given: 权限状态
        val permission = Manifest.permission.CAMERA
        `when`(ContextCompat.checkSelfPermission(mockContext, permission))
            .thenReturn(PackageManager.PERMISSION_GRANTED)

        // When: 多次检查同一权限
        val result1 = permissionHelper.hasPermission(permission)
        val result2 = permissionHelper.hasPermission(permission)
        val result3 = permissionHelper.hasPermission(permission)

        // Then: 验证缓存生效（只调用一次实际检查）
        verify(mockContext, times(1)).checkSelfPermission(permission)
        assertTrue(result1)
        assertTrue(result2)
        assertTrue(result3)
    }
}

/**
 * PermissionHelper扩展函数（测试用）
 */
private fun PermissionHelper.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        getInstance().context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

private fun PermissionHelper.hasAllPermissions(vararg permissions: String): Boolean {
    return permissions.all { hasPermission(it) }
}

private fun PermissionHelper.getMissingPermissions(vararg permissions: String): List<String> {
    return permissions.filter { !hasPermission(it) }
}

private fun PermissionHelper.hasStoragePermissions(): Boolean {
    val permissions = getStoragePermissionsForVersion()
    return hasAllPermissions(*permissions.toTypedArray())
}

private fun PermissionHelper.isDangerousPermission(permission: String): Boolean {
    val dangerousPermissions = setOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    return dangerousPermissions.contains(permission)
}

private fun PermissionHelper.needsSpecialPermissionHandling(permission: String): Boolean {
    return permission == Manifest.permission.WRITE_SETTINGS ||
            permission == Manifest.permission.SYSTEM_ALERT_WINDOW
}

private fun PermissionHelper.shouldShowRationale(activity: Activity, permission: String): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

private fun PermissionHelper.isPermanentlyDenied(activity: Activity, permission: String): Boolean {
    return !hasPermission(permission) && !shouldShowRationale(activity, permission)
}

private fun PermissionHelper.generateRequestCode(permissions: Array<String>): Int {
    return permissions.contentHashCode()
}

private fun PermissionHelper.handlePermissionResult(
    permissions: Array<String>,
    grantResults: IntArray
): PermissionResult {
    val granted = mutableListOf<String>()
    val denied = mutableListOf<String>()

    permissions.forEachIndexed { index, permission ->
        if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
            granted.add(permission)
        } else {
            denied.add(permission)
        }
    }

    return PermissionResult(granted, denied)
}

private fun PermissionHelper.getStoragePermissionsForVersion(): List<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    } else {
        listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}

private fun PermissionHelper.groupPermissions(permissions: Array<String>): Map<String, List<String>> {
    val groups = mutableMapOf<String, MutableList<String>>()

    permissions.forEach { permission ->
        val group = when {
            permission.contains("LOCATION") -> "LOCATION"
            permission.contains("CAMERA") -> "CAMERA"
            permission.contains("AUDIO") || permission.contains("RECORD") -> "MICROPHONE"
            permission.contains("CONTACTS") -> "CONTACTS"
            permission.contains("STORAGE") || permission.contains("MEDIA") -> "STORAGE"
            else -> "OTHER"
        }

        groups.getOrPut(group) { mutableListOf() }.add(permission)
    }

    return groups
}

data class PermissionResult(
    val grantedPermissions: List<String>,
    val deniedPermissions: List<String>
)

private val PermissionHelper.context: Context
    get() = mockContext // For testing purposes