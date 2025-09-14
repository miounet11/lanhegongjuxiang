package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.net.http.SslError
import android.webkit.SslErrorHandler
import java.security.cert.X509Certificate

/**
 * 安全管理器
 * 处理SSL证书验证和网络安全
 */
class SecurityManager(private val context: Context) {

    // 可信证书指纹列表
    private val trustedCertificates = setOf(
        // 这里可以添加可信证书的SHA-256指纹
    )

    // 危险域名列表
    private val dangerousDomains = setOf(
        "malicious-site.com",
        "phishing-site.net",
        "suspicious-domain.org"
    )

    // 统计数据
    private var sslErrorsHandled = 0
    private var dangerousSitesBlocked = 0
    private var totalRequests = 0

    /**
     * 检查是否应该允许SSL错误
     */
    fun shouldAllowSslError(error: SslError?): Boolean {
        if (error == null) return false

        sslErrorsHandled++

        // 检查证书是否在可信列表中
        val certificate = getCertificateFromError(error)
        if (certificate != null && isCertificateTrusted(certificate)) {
            return true
        }

        // 检查是否是已知的安全错误类型
        return when (error.primaryError) {
            SslError.SSL_DATE_INVALID -> false  // 证书过期
            SslError.SSL_EXPIRED -> false       // 证书过期
            SslError.SSL_IDMISMATCH -> false    // 域名不匹配
            SslError.SSL_NOTYETVALID -> false   // 证书尚未生效
            SslError.SSL_UNTRUSTED -> false     // 不受信任的证书
            else -> false
        }
    }

    /**
     * 检查域名是否危险
     */
    fun isDomainDangerous(domain: String?): Boolean {
        if (domain.isNullOrEmpty()) return false

        totalRequests++

        return dangerousDomains.contains(domain.lowercase())
    }

    /**
     * 检查URL是否安全
     */
    fun isUrlSafe(url: String?): Boolean {
        if (url.isNullOrEmpty()) return true

        // 检查是否是HTTPS
        if (!url.startsWith("https://")) {
            return false
        }

        // 检查域名是否危险
        val domain = extractDomain(url)
        if (isDomainDangerous(domain)) {
            dangerousSitesBlocked++
            return false
        }

        return true
    }

    /**
     * 获取安全统计信息
     */
    fun getSecurityStats(): SecurityStats {
        return SecurityStats(
            totalRequests = totalRequests,
            sslErrorsHandled = sslErrorsHandled,
            dangerousSitesBlocked = dangerousSitesBlocked,
            securityScore = calculateSecurityScore()
        )
    }

    /**
     * 计算安全评分
     */
    private fun calculateSecurityScore(): Int {
        if (totalRequests == 0) return 100

        val unsafeRequests = sslErrorsHandled + dangerousSitesBlocked
        val safeRate = ((totalRequests - unsafeRequests).toFloat() / totalRequests.toFloat()) * 100f

        return safeRate.toInt().coerceIn(0, 100)
    }

    /**
     * 从SSL错误中获取证书
     */
    private fun getCertificateFromError(error: SslError): X509Certificate? {
        return try {
            // 这里需要访问Android的证书信息
            // 暂时返回null
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查证书是否可信
     */
    private fun isCertificateTrusted(certificate: X509Certificate): Boolean {
        return try {
            // 计算证书指纹并检查是否在可信列表中
            val fingerprint = calculateCertificateFingerprint(certificate)
            trustedCertificates.contains(fingerprint)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 计算证书指纹
     */
    private fun calculateCertificateFingerprint(certificate: X509Certificate): String {
        return try {
            // 这里应该计算证书的SHA-256指纹
            // 暂时返回空字符串
            ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 提取域名
     */
    private fun extractDomain(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.host ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 重置统计数据
     */
    fun resetStats() {
        sslErrorsHandled = 0
        dangerousSitesBlocked = 0
        totalRequests = 0
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        resetStats()
    }

    /**
     * 安全统计数据类
     */
    data class SecurityStats(
        val totalRequests: Int,
        val sslErrorsHandled: Int,
        val dangerousSitesBlocked: Int,
        val securityScore: Int
    )
}
