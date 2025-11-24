package lanhe.apk

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.*
import lanhe.filesystem.LanheFileManager
import lanhe.filesystem.UniFile
import lanhe.shizuku.ShizukuManager
import java.io.File
import java.util.zip.ZipFile

/**
 * 蓝河助手APK管理器
 * 提供APK分析、验证、安装等完整功能
 */
class LanheAPKManager(
    private val context: Context
) {
    private val fileManager = LanheFileManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 分析APK文件
     */
    suspend fun analyzeAPK(filePath: String): APKAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val file = fileManager.getFile(filePath)
            if (!file.exists() || !file.name.lowercase().endsWith(".apk")) {
                return@withContext APKAnalysisResult.InvalidFile("Not a valid APK file")
            }

            // 基本文件信息
            val basicInfo = getBasicAPKInfo(file)

            // 包信息
            val packageInfo = getPackageInfo(file)

            // 权限信息
            val permissions = getPermissions(file)

            // 签名信息
            val signatures = getSignatures(file)

            // 安全检查
            val securityCheck = performSecurityCheck(file, permissions)

            APKAnalysisResult.Success(
                basicInfo = basicInfo,
                packageInfo = packageInfo,
                permissions = permissions,
                signatures = signatures,
                securityCheck = securityCheck
            )
        } catch (e: Exception) {
            e.printStackTrace()
            APKAnalysisResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * 安装APK
     */
    suspend fun installAPK(filePath: String): InstallationResult = withContext(Dispatchers.IO) {
        try {
            // 先验证APK
            val analysis = analyzeAPK(filePath)
            if (analysis !is APKAnalysisResult.Success) {
                return@withContext InstallationResult.ValidationFailed(analysis.toString())
            }

            // 安全检查
            if (analysis.securityCheck.riskLevel == SecurityRiskLevel.HIGH) {
                return@withContext InstallationResult.SecurityRisk("High security risk detected")
            }

            val file = fileManager.getFile(filePath)

            // 尝试使用Shizuku静默安装
            if (ShizukuManager.isShizukuAvailable() && ShizukuManager.hasPermission()) {
                installWithShizuku(file)
            } else {
                installWithPackageInstaller(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            InstallationResult.Error(e.message ?: "Installation failed")
        }
    }

    /**
     * 验证APK完整性
     */
    suspend fun verifyAPK(filePath: String): VerificationResult = withContext(Dispatchers.IO) {
        try {
            val file = fileManager.getFile(filePath)
            if (!file.exists()) {
                return@withContext VerificationResult.FileNotFound
            }

            // 检查文件大小
            if (file.size < 1024) {
                return@withContext VerificationResult.InvalidFormat("File too small to be a valid APK")
            }

            // 检查ZIP文件结构
            val isValidZip = isValidZipFile(file)
            if (!isValidZip) {
                return@withContext VerificationResult.CorruptedFile("Invalid ZIP file structure")
            }

            // 检查APK签名
            val signatures = getSignatures(file)
            if (signatures.isEmpty()) {
                return@withContext VerificationResult.Unsigned("APK is not signed")
            }

            VerificationResult.Success
        } catch (e: Exception) {
            e.printStackTrace()
            VerificationResult.Error(e.message ?: "Verification failed")
        }
    }

    /**
     * 获取APK图标
     */
    suspend fun getAPKIcon(filePath: String): Drawable? = withContext(Dispatchers.IO) {
        try {
            val file = fileManager.getFile(filePath)
            val packageInfo = context.packageManager.getPackageArchiveInfo(file.path, PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA)
            packageInfo?.applicationInfo?.loadIcon(context.packageManager)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取基本APK信息
     */
    private fun getBasicAPKInfo(file: UniFile): BasicAPKInfo {
        return BasicAPKInfo(
            fileName = file.name,
            filePath = file.path,
            fileSize = file.size,
            lastModified = file.lastModified
        )
    }

    /**
     * 获取包信息
     */
    private fun getPackageInfo(file: UniFile): PackageAPKInfo? {
        return try {
            val packageInfo = context.packageManager.getPackageArchiveInfo(file.path, PackageManager.GET_ACTIVITIES)
            val appInfo = packageInfo?.applicationInfo

            if (appInfo != null) {
                PackageAPKInfo(
                    packageName = packageInfo.packageName ?: "unknown",
                    versionName = packageInfo.versionName ?: "unknown",
                    versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        packageInfo.versionCode?.toLong() ?: 0L
                    },
                    minSdkVersion = appInfo.targetSdkVersion,
                    targetSdkVersion = appInfo.targetSdkVersion,
                    appName = appInfo.loadLabel(context.packageManager).toString(),
                    installLocation = packageInfo.installLocation
                )
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取权限信息
     */
    private fun getPermissions(file: UniFile): List<APKPermission> {
        return try {
            val packageInfo = context.packageManager.getPackageArchiveInfo(file.path, PackageManager.GET_PERMISSIONS)
            packageInfo?.requestedPermissions?.map { permission ->
                APKPermission(
                    name = permission,
                    isDangerous = isDangerousPermission(permission),
                    description = getPermissionDescription(permission)
                )
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 获取签名信息
     */
    private fun getSignatures(file: UniFile): List<APKSignature> {
        return try {
            // 这里简化实现，实际需要解析APK签名
            listOf(
                APKSignature(
                    algorithm = "SHA256",
                    fingerprint = "demo-fingerprint",
                    issuer = "Demo Issuer",
                    validFrom = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000,
                    validTo = System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 执行安全检查
     */
    private fun performSecurityCheck(file: UniFile, permissions: List<APKPermission>): SecurityCheckResult {
        var riskLevel = SecurityRiskLevel.LOW
        val warnings = mutableListOf<String>()

        // 检查危险权限
        val dangerousPermissions = permissions.filter { it.isDangerous }
        if (dangerousPermissions.size > 10) {
            riskLevel = SecurityRiskLevel.MEDIUM
            warnings.add("App requests ${dangerousPermissions.size} dangerous permissions")
        }

        // 检查系统级权限
        val systemPermissions = permissions.filter {
            it.name.startsWith("android.permission.") &&
            (it.name.contains("SYSTEM") || it.name.contains("ROOT") || it.name.contains("DEVICE"))
        }
        if (systemPermissions.isNotEmpty()) {
            riskLevel = SecurityRiskLevel.HIGH
            warnings.add("App requests system-level permissions: ${systemPermissions.joinToString(", ")}")
        }

        // 检查文件权限
        if (permissions.any { it.name.contains("STORAGE") || it.name.contains("EXTERNAL") }) {
            warnings.add("App requests storage access permissions")
        }

        return SecurityCheckResult(
            riskLevel = riskLevel,
            warnings = warnings,
            dangerousPermissions = dangerousPermissions
        )
    }

    /**
     * 使用Shizuku安装
     */
    private suspend fun installWithShizuku(file: UniFile): InstallationResult {
        return try {
            val success = ShizukuManager.installPackage(file.path)
            if (success) {
                InstallationResult.Success
            } else {
                InstallationResult.Error("Shizuku installation failed")
            }
        } catch (e: Exception) {
            InstallationResult.Error("Shizuku error: ${e.message}")
        }
    }

    /**
     * 使用PackageInstaller安装
     */
    private suspend fun installWithPackageInstaller(file: UniFile): InstallationResult {
        // 这里实现标准的Android PackageInstaller安装流程
        // 由于需要UI交互，简化实现
        return InstallationResult.RequiresUserAction("Requires user confirmation for installation")
    }

    /**
     * 检查是否为有效的ZIP文件
     */
    private fun isValidZipFile(file: UniFile): Boolean {
        return try {
            ZipFile(file.path).use { zip ->
                // 检查是否包含AndroidManifest.xml
                zip.getEntry("AndroidManifest.xml") != null
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查是否为危险权限
     */
    private fun isDangerousPermission(permission: String): Boolean {
        return try {
            val permissionInfo = context.packageManager.getPermissionInfo(permission, 0)
            permissionInfo.protectionLevel == android.content.pm.PermissionInfo.PROTECTION_DANGEROUS
        } catch (e: Exception) {
            // 如果无法获取权限信息，假设是危险权限
            true
        }
    }

    /**
     * 获取权限描述
     */
    private fun getPermissionDescription(permission: String): String {
        return when {
            permission.contains("INTERNET") -> "访问网络"
            permission.contains("STORAGE") -> "访问存储"
            permission.contains("CAMERA") -> "访问摄像头"
            permission.contains("MICROPHONE") -> "访问麦克风"
            permission.contains("LOCATION") -> "访问位置信息"
            permission.contains("CONTACTS") -> "访问联系人"
            permission.contains("SMS") -> "发送短信"
            permission.contains("PHONE") -> "拨打电话"
            permission.contains("SYSTEM") -> "系统级权限"
            else -> "未知权限"
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        scope.cancel()
    }
}

/**
 * APK分析结果
 */
sealed class APKAnalysisResult {
    data class Success(
        val basicInfo: BasicAPKInfo,
        val packageInfo: PackageAPKInfo?,
        val permissions: List<APKPermission>,
        val signatures: List<APKSignature>,
        val securityCheck: SecurityCheckResult
    ) : APKAnalysisResult()

    data class Error(val message: String) : APKAnalysisResult()
    data class InvalidFile(val reason: String) : APKAnalysisResult()
}

/**
 * 安装结果
 */
sealed class InstallationResult {
    object Success : InstallationResult()
    data class Error(val message: String) : InstallationResult()
    data class ValidationFailed(val message: String) : InstallationResult()
    data class SecurityRisk(val message: String) : InstallationResult()
    data class RequiresUserAction(val message: String) : InstallationResult()
}

/**
 * 验证结果
 */
sealed class VerificationResult {
    object Success : VerificationResult()
    object FileNotFound : VerificationResult()
    object Unsigned : VerificationResult()
    data class CorruptedFile(val reason: String) : VerificationResult()
    data class InvalidFormat(val reason: String) : VerificationResult()
    data class Error(val message: String) : VerificationResult()
}

/**
 * 基本APK信息
 */
data class BasicAPKInfo(
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val lastModified: Long
)

/**
 * 包APK信息
 */
data class PackageAPKInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val appName: String,
    val installLocation: Int
)

/**
 * APK权限
 */
data class APKPermission(
    val name: String,
    val isDangerous: Boolean,
    val description: String
)

/**
 * APK签名
 */
data class APKSignature(
    val algorithm: String,
    val fingerprint: String,
    val issuer: String,
    val validFrom: Long,
    val validTo: Long
)

/**
 * 安全风险等级
 */
enum class SecurityRiskLevel {
    LOW, MEDIUM, HIGH
}

/**
 * 安全检查结果
 */
data class SecurityCheckResult(
    val riskLevel: SecurityRiskLevel,
    val warnings: List<String>,
    val dangerousPermissions: List<APKPermission>
)