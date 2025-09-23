package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlin.coroutines.resume

/**
 * 蓝河助手 - 生物识别认证管理器
 *
 * 功能特性：
 * - 指纹识别验证
 * - 面部识别支持
 * - 安全数据存储
 * - 应用锁功能
 * - 敏感操作保护
 * - 加密密钥管理
 */
class BiometricAuthManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "BiometricAuthManager"

        @Volatile
        private var INSTANCE: BiometricAuthManager? = null

        fun getInstance(context: Context): BiometricAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BiometricAuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        // 密钥库配置
        private const val KEYSTORE_ALIAS = "LanheAssistantBiometricKey"
        private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE

        // 偏好设置键
        private const val PREF_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val PREF_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val PREF_LAST_AUTH_TIME = "last_auth_time"
        private const val PREF_AUTH_TIMEOUT = "auth_timeout"

        // 默认认证超时时间（5分钟）
        private const val DEFAULT_AUTH_TIMEOUT = 5 * 60 * 1000L
    }

    private val analyticsManager = AnalyticsManager.getInstance(context)
    private val biometricManager = BiometricManager.from(context)
    private val keyStore = KeyStore.getInstance("AndroidKeyStore")

    // 加密偏好设置
    private val encryptedPreferences by lazy {
        createEncryptedPreferences()
    }

    init {
        keyStore.load(null)
        initializeBiometricKey()
    }

    /**
     * 生物识别结果
     */
    sealed class BiometricResult {
        object Success : BiometricResult()
        data class Error(val errorCode: Int, val errorMessage: String) : BiometricResult()
        object Failed : BiometricResult()
        object Cancelled : BiometricResult()
    }

    /**
     * 认证配置
     */
    data class AuthConfig(
        val title: String = "生物识别验证",
        val subtitle: String = "使用指纹或面部识别来验证身份",
        val description: String = "请进行生物识别验证以继续操作",
        val negativeButtonText: String = "取消",
        val allowedAuthenticators: Int = BiometricManager.Authenticators.BIOMETRIC_STRONG,
        val requireConfirmation: Boolean = true
    )

    /**
     * 检查生物识别可用性
     */
    fun checkBiometricAvailability(): BiometricAvailability {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HW_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SECURITY_UPDATE_REQUIRED
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricAvailability.UNSUPPORTED
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricAvailability.STATUS_UNKNOWN
            else -> BiometricAvailability.STATUS_UNKNOWN
        }
    }

    /**
     * 生物识别可用性枚举
     */
    enum class BiometricAvailability {
        AVAILABLE,
        NO_HARDWARE,
        HW_UNAVAILABLE,
        NONE_ENROLLED,
        SECURITY_UPDATE_REQUIRED,
        UNSUPPORTED,
        STATUS_UNKNOWN
    }

    /**
     * 执行生物识别认证
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        config: AuthConfig = AuthConfig()
    ): BiometricResult = suspendCancellableCoroutine { continuation ->

        // 检查可用性
        val availability = checkBiometricAvailability()
        if (availability != BiometricAvailability.AVAILABLE) {
            continuation.resume(BiometricResult.Error(-1, "生物识别不可用: $availability"))
            return@suspendCancellableCoroutine
        }

        try {
            val executor = ContextCompat.getMainExecutor(context)

            val biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)

                        analyticsManager.trackEvent("biometric_auth_error", android.os.Bundle().apply {
                            putInt("error_code", errorCode)
                            putString("error_message", errString.toString())
                        })

                        if (continuation.isActive) {
                            when (errorCode) {
                                BiometricPrompt.ERROR_USER_CANCELED,
                                BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                                    continuation.resume(BiometricResult.Cancelled)
                                }
                                else -> {
                                    continuation.resume(BiometricResult.Error(errorCode, errString.toString()))
                                }
                            }
                        }
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)

                        // 更新最后认证时间
                        updateLastAuthTime()

                        analyticsManager.trackEvent("biometric_auth_success")

                        if (continuation.isActive) {
                            continuation.resume(BiometricResult.Success)
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()

                        analyticsManager.trackEvent("biometric_auth_failed")

                        if (continuation.isActive) {
                            continuation.resume(BiometricResult.Failed)
                        }
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(config.title)
                .setSubtitle(config.subtitle)
                .setDescription(config.description)
                .setNegativeButtonText(config.negativeButtonText)
                .setAllowedAuthenticators(config.allowedAuthenticators)
                .setConfirmationRequired(config.requireConfirmation)
                .build()

            biometricPrompt.authenticate(promptInfo)

            // 设置取消回调
            continuation.invokeOnCancellation {
                try {
                    biometricPrompt.cancelAuthentication()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to cancel authentication", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Authentication setup failed", e)
            analyticsManager.trackError("biometric_auth_setup_failed", e.message ?: "Unknown error", e)

            if (continuation.isActive) {
                continuation.resume(BiometricResult.Error(-1, "认证设置失败: ${e.message}"))
            }
        }
    }

    /**
     * 启用生物识别
     */
    fun enableBiometric() {
        encryptedPreferences.edit()
            .putBoolean(PREF_BIOMETRIC_ENABLED, true)
            .apply()

        analyticsManager.trackFeatureUsed("biometric_enabled")
    }

    /**
     * 禁用生物识别
     */
    fun disableBiometric() {
        encryptedPreferences.edit()
            .putBoolean(PREF_BIOMETRIC_ENABLED, false)
            .apply()

        analyticsManager.trackFeatureUsed("biometric_disabled")
    }

    /**
     * 检查生物识别是否已启用
     */
    fun isBiometricEnabled(): Boolean {
        return encryptedPreferences.getBoolean(PREF_BIOMETRIC_ENABLED, false)
    }

    /**
     * 启用应用锁
     */
    fun enableAppLock() {
        encryptedPreferences.edit()
            .putBoolean(PREF_APP_LOCK_ENABLED, true)
            .apply()

        analyticsManager.trackFeatureUsed("app_lock_enabled")
    }

    /**
     * 禁用应用锁
     */
    fun disableAppLock() {
        encryptedPreferences.edit()
            .putBoolean(PREF_APP_LOCK_ENABLED, false)
            .apply()

        analyticsManager.trackFeatureUsed("app_lock_disabled")
    }

    /**
     * 检查应用锁是否已启用
     */
    fun isAppLockEnabled(): Boolean {
        return encryptedPreferences.getBoolean(PREF_APP_LOCK_ENABLED, false)
    }

    /**
     * 检查是否需要认证
     */
    fun needsAuthentication(): Boolean {
        if (!isAppLockEnabled() || !isBiometricEnabled()) return false

        val lastAuthTime = encryptedPreferences.getLong(PREF_LAST_AUTH_TIME, 0)
        val authTimeout = encryptedPreferences.getLong(PREF_AUTH_TIMEOUT, DEFAULT_AUTH_TIMEOUT)
        val currentTime = System.currentTimeMillis()

        return (currentTime - lastAuthTime) > authTimeout
    }

    /**
     * 更新最后认证时间
     */
    private fun updateLastAuthTime() {
        encryptedPreferences.edit()
            .putLong(PREF_LAST_AUTH_TIME, System.currentTimeMillis())
            .apply()
    }

    /**
     * 设置认证超时时间
     */
    fun setAuthTimeout(timeoutMillis: Long) {
        encryptedPreferences.edit()
            .putLong(PREF_AUTH_TIMEOUT, timeoutMillis)
            .apply()
    }

    /**
     * 获取认证超时时间
     */
    fun getAuthTimeout(): Long {
        return encryptedPreferences.getLong(PREF_AUTH_TIMEOUT, DEFAULT_AUTH_TIMEOUT)
    }

    /**
     * 加密数据
     */
    fun encryptData(data: String): ByteArray? {
        return try {
            val cipher = getCipher()
            val secretKey = getSecretKey()
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            cipher.doFinal(data.toByteArray())
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            analyticsManager.trackError("data_encryption_failed", e.message ?: "Unknown error", e)
            null
        }
    }

    /**
     * 解密数据
     */
    fun decryptData(encryptedData: ByteArray): String? {
        return try {
            val cipher = getCipher()
            val secretKey = getSecretKey()
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            String(cipher.doFinal(encryptedData))
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            analyticsManager.trackError("data_decryption_failed", e.message ?: "Unknown error", e)
            null
        }
    }

    /**
     * 初始化生物识别密钥
     */
    private fun initializeBiometricKey() {
        try {
            if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
                generateSecretKey()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize biometric key", e)
            analyticsManager.trackError("biometric_key_init_failed", e.message ?: "Unknown error", e)
        }
    }

    /**
     * 生成密钥
     */
    private fun generateSecretKey() {
        val keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM, "AndroidKeyStore")

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(ENCRYPTION_BLOCK_MODE)
            .setEncryptionPaddings(ENCRYPTION_PADDING)
            .setUserAuthenticationRequired(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setUserAuthenticationParameters(
                        0,
                        KeyProperties.AUTH_BIOMETRIC_STRONG
                    )
                } else {
                    @Suppress("DEPRECATION")
                    setUserAuthenticationValidityDurationSeconds(-1)
                }
            }
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    /**
     * 获取密钥
     */
    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
    }

    /**
     * 获取加密器
     */
    private fun getCipher(): Cipher {
        return Cipher.getInstance("$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING")
    }

    /**
     * 创建加密偏好设置
     */
    private fun createEncryptedPreferences() = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "biometric_preferences",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create encrypted preferences", e)
        // 降级到普通SharedPreferences
        context.getSharedPreferences("biometric_preferences_fallback", Context.MODE_PRIVATE)
    }

    /**
     * 清除认证数据
     */
    fun clearAuthData() {
        encryptedPreferences.edit()
            .remove(PREF_LAST_AUTH_TIME)
            .apply()
    }

    /**
     * 重置生物识别设置
     */
    fun resetBiometricSettings() {
        encryptedPreferences.edit()
            .clear()
            .apply()

        try {
            if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
                keyStore.deleteEntry(KEYSTORE_ALIAS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete keystore entry", e)
        }

        analyticsManager.trackFeatureUsed("biometric_settings_reset")
    }

    /**
     * 获取生物识别类型
     */
    fun getBiometricType(): String {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // 这里可以进一步检测具体的生物识别类型
                "biometric_available"
            }
            else -> "biometric_unavailable"
        }
    }
}

/**
 * 扩展函数
 */
fun Context.biometricAuth(): BiometricAuthManager = BiometricAuthManager.getInstance(this)

/**
 * 简化的认证函数
 */
suspend fun FragmentActivity.authenticateWithBiometric(
    title: String = "身份验证",
    subtitle: String = "使用生物识别验证身份"
): BiometricAuthManager.BiometricResult {
    val authManager = BiometricAuthManager.getInstance(this)
    val config = BiometricAuthManager.AuthConfig(
        title = title,
        subtitle = subtitle
    )
    return authManager.authenticate(this, config)
}