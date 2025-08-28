package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * NEURAL 更新检查器
 * 从GitHub仓库检查版本更新
 */
class UpdateChecker(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()

    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos/miounet11/lanhegongjuxiang/contents/version.json"
        private const val RAW_CONTENT_URL = "https://raw.githubusercontent.com/miounet11/lanhegongjuxiang/main/version.json"
        private const val GITHUB_REPO_URL = "https://github.com/miounet11/lanhegongjuxiang"
    }

    /**
     * 版本信息数据类
     */
    data class VersionInfo(
        @SerializedName("version")
        val version: String,

        @SerializedName("versionCode")
        val versionCode: Int,

        @SerializedName("releaseDate")
        val releaseDate: String,

        @SerializedName("releaseNotes")
        val releaseNotes: List<String>,

        @SerializedName("downloadUrl")
        val downloadUrl: String,

        @SerializedName("minAndroidVersion")
        val minAndroidVersion: String,

        @SerializedName("targetAndroidVersion")
        val targetAndroidVersion: String,

        @SerializedName("features")
        val features: List<String>,

        @SerializedName("requirements")
        val requirements: Requirements,

        @SerializedName("changelog")
        val changelog: Map<String, VersionChangelog>
    )

    data class Requirements(
        @SerializedName("minSdk")
        val minSdk: Int,

        @SerializedName("targetSdk")
        val targetSdk: Int,

        @SerializedName("recommendedRam")
        val recommendedRam: String,

        @SerializedName("recommendedStorage")
        val recommendedStorage: String
    )

    data class VersionChangelog(
        @SerializedName("date")
        val date: String,

        @SerializedName("changes")
        val changes: List<String>
    )

    /**
     * 检查更新
     */
    fun checkForUpdates(onResult: (UpdateResult) -> Unit) {
        scope.launch {
            try {
                val latestVersion = fetchLatestVersion()
                val currentVersion = getCurrentVersion()

                val result = when {
                    latestVersion == null -> {
                        UpdateResult.Error("无法获取最新版本信息")
                    }
                    isNewerVersion(latestVersion.version, currentVersion) -> {
                        UpdateResult.UpdateAvailable(latestVersion)
                    }
                    else -> {
                        UpdateResult.UpToDate
                    }
                }

                withContext(Dispatchers.Main) {
                    onResult(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(UpdateResult.Error("检查更新失败: ${e.localizedMessage}"))
                }
            }
        }
    }

    /**
     * 从GitHub获取最新版本信息
     */
    private fun fetchLatestVersion(): VersionInfo? {
        return try {
            val connection = URL(RAW_CONTENT_URL).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "NEURAL-App")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val json = reader.use { it.readText() }
                gson.fromJson(json, VersionInfo::class.java)
            } else {
                null
            }
        } catch (e: IOException) {
            null
        }
    }

    /**
     * 获取当前版本
     */
    private fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /**
     * 比较版本号
     */
    private fun isNewerVersion(remoteVersion: String, localVersion: String): Boolean {
        return try {
            val remoteParts = remoteVersion.split(".").map { it.toInt() }
            val localParts = localVersion.split(".").map { it.toInt() }

            for (i in 0 until maxOf(remoteParts.size, localParts.size)) {
                val remote = remoteParts.getOrElse(i) { 0 }
                val local = localParts.getOrElse(i) { 0 }

                if (remote > local) return true
                if (remote < local) return false
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 打开GitHub仓库
     */
    fun openGitHubRepo() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPO_URL))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 下载最新版本
     */
    fun downloadUpdate(versionInfo: VersionInfo) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(versionInfo.downloadUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开下载链接", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        scope.cancel()
    }

    /**
     * 更新检查结果
     */
    sealed class UpdateResult {
        data class UpdateAvailable(val versionInfo: VersionInfo) : UpdateResult()
        object UpToDate : UpdateResult()
        data class Error(val message: String) : UpdateResult()
    }
}
