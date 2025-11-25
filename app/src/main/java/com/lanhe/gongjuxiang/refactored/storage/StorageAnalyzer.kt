package com.lanhe.gongjuxiang.refactored.storage

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import com.lanhe.gongjuxiang.refactored.interfaces.IAnalyzer

/**
 * 存储分析器 - 负责存储分析和统计
 * 包括空间分布分析、速度测试、健康度评估
 */
class StorageAnalyzer(private val context: Context) : IAnalyzer<StorageAnalysisResult> {

    companion object {
        private const val TAG = "StorageAnalyzer"
        private const val SPEED_TEST_FILE_SIZE = 10 * 1024 * 1024 // 10MB
        private const val SPEED_TEST_ITERATIONS = 3
    }

    private val packageManager = context.packageManager

    // 分析状态
    private val _analysisState = MutableStateFlow<StorageAnalysisResult?>(null)
    val analysisState: StateFlow<StorageAnalysisResult?> = _analysisState.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    /**
     * 执行存储分析
     * @return 分析结果
     */
    override suspend fun performAnalysis(): StorageAnalysisResult = withContext(Dispatchers.IO) {
        _isAnalyzing.value = true

        try {
            val storageInfo = getStorageInfo()
            val spaceDistribution = analyzeSpaceDistribution()
            val appSizeInfo = analyzeAppSizes()
            val speedTestResult = performSpeedTest()
            val healthScore = calculateHealthScore(storageInfo, spaceDistribution)

            val result = StorageAnalysisResult(
                totalSpace = storageInfo.totalSpace,
                usedSpace = storageInfo.usedSpace,
                freeSpace = storageInfo.freeSpace,
                usagePercent = storageInfo.usagePercent,
                spaceDistribution = spaceDistribution,
                appSizes = appSizeInfo,
                readSpeed = speedTestResult.readSpeed,
                writeSpeed = speedTestResult.writeSpeed,
                healthScore = healthScore,
                recommendations = generateRecommendations(storageInfo, spaceDistribution, healthScore)
            )

            _analysisState.value = result
            result
        } finally {
            _isAnalyzing.value = false
        }
    }

    /**
     * 生成分析报告
     * @return 报告内容
     */
    override suspend fun generateReport(): String = withContext(Dispatchers.IO) {
        val analysis = _analysisState.value ?: performAnalysis()

        buildString {
            appendLine("=== 存储分析报告 ===")
            appendLine()
            appendLine("存储概况:")
            appendLine("  总空间: ${formatFileSize(analysis.totalSpace)}")
            appendLine("  已用空间: ${formatFileSize(analysis.usedSpace)} (${analysis.usagePercent}%)")
            appendLine("  剩余空间: ${formatFileSize(analysis.freeSpace)}")
            appendLine()

            appendLine("空间分布:")
            analysis.spaceDistribution.forEach { (category, size) ->
                val percent = (size * 100.0 / analysis.usedSpace).toInt()
                appendLine("  $category: ${formatFileSize(size)} ($percent%)")
            }
            appendLine()

            appendLine("存储性能:")
            appendLine("  读取速度: ${formatSpeed(analysis.readSpeed)}")
            appendLine("  写入速度: ${formatSpeed(analysis.writeSpeed)}")
            appendLine()

            appendLine("健康评分: ${analysis.healthScore}/100")
            appendLine()

            appendLine("优化建议:")
            analysis.recommendations.forEachIndexed { index, recommendation ->
                appendLine("  ${index + 1}. $recommendation")
            }
        }
    }

    /**
     * 获取存储信息
     */
    fun getStorageInfo(): StorageInfo {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)

        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalSpace = totalBlocks * blockSize
        val freeSpace = availableBlocks * blockSize
        val usedSpace = totalSpace - freeSpace
        val usagePercent = ((usedSpace * 100.0) / totalSpace).toInt()

        return StorageInfo(
            totalSpace = totalSpace,
            usedSpace = usedSpace,
            freeSpace = freeSpace,
            usagePercent = usagePercent
        )
    }

    /**
     * 分析空间分布
     */
    private suspend fun analyzeSpaceDistribution(): Map<String, Long> = withContext(Dispatchers.IO) {
        val distribution = mutableMapOf<String, Long>()

        // 分析各类文件占用
        val sdCard = Environment.getExternalStorageDirectory()

        // 图片
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        distribution["图片"] = calculateDirectorySize(picturesDir)

        // 视频
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        distribution["视频"] = calculateDirectorySize(moviesDir)

        // 音频
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        distribution["音频"] = calculateDirectorySize(musicDir)

        // 下载
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        distribution["下载"] = calculateDirectorySize(downloadDir)

        // 文档
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        distribution["文档"] = calculateDirectorySize(documentsDir)

        // 应用数据
        val androidDataDir = File(sdCard, "Android/data")
        distribution["应用数据"] = calculateDirectorySize(androidDataDir)

        // 系统缓存
        distribution["缓存"] = calculateCacheSize()

        // 其他
        val totalUsed = distribution.values.sum()
        val storageInfo = getStorageInfo()
        distribution["其他"] = storageInfo.usedSpace - totalUsed

        distribution
    }

    /**
     * 分析应用大小
     */
    private suspend fun analyzeAppSizes(): List<AppSizeInfo> = withContext(Dispatchers.IO) {
        val appSizes = mutableListOf<AppSizeInfo>()

        try {
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            installedApps.forEach { appInfo ->
                try {
                    val appSize = getAppSize(appInfo)
                    appSizes.add(
                        AppSizeInfo(
                            packageName = appInfo.packageName,
                            appName = appInfo.loadLabel(packageManager).toString(),
                            codeSize = appSize.codeSize,
                            dataSize = appSize.dataSize,
                            cacheSize = appSize.cacheSize,
                            totalSize = appSize.totalSize
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get size for: ${appInfo.packageName}", e)
                }
            }

            // 按总大小排序
            appSizes.sortByDescending { it.totalSize }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze app sizes", e)
        }

        appSizes
    }

    /**
     * 执行速度测试
     */
    private suspend fun performSpeedTest(): SpeedTestResult = withContext(Dispatchers.IO) {
        val testFile = File(context.cacheDir, "speed_test.tmp")
        val testData = ByteArray(SPEED_TEST_FILE_SIZE)

        var totalWriteTime = 0L
        var totalReadTime = 0L

        try {
            repeat(SPEED_TEST_ITERATIONS) {
                // 写入测试
                val writeStart = System.currentTimeMillis()
                testFile.writeBytes(testData)
                val writeEnd = System.currentTimeMillis()
                totalWriteTime += (writeEnd - writeStart)

                // 读取测试
                val readStart = System.currentTimeMillis()
                testFile.readBytes()
                val readEnd = System.currentTimeMillis()
                totalReadTime += (readEnd - readStart)

                testFile.delete()
            }

            val avgWriteTime = totalWriteTime / SPEED_TEST_ITERATIONS.toDouble()
            val avgReadTime = totalReadTime / SPEED_TEST_ITERATIONS.toDouble()

            // 计算速度 (MB/s)
            val writeSpeed = (SPEED_TEST_FILE_SIZE / 1024.0 / 1024.0) / (avgWriteTime / 1000.0)
            val readSpeed = (SPEED_TEST_FILE_SIZE / 1024.0 / 1024.0) / (avgReadTime / 1000.0)

            SpeedTestResult(
                readSpeed = readSpeed,
                writeSpeed = writeSpeed
            )
        } catch (e: Exception) {
            Log.e(TAG, "Speed test failed", e)
            SpeedTestResult(0.0, 0.0)
        } finally {
            testFile.delete()
        }
    }

    /**
     * 计算健康评分
     */
    private fun calculateHealthScore(
        storageInfo: StorageInfo,
        spaceDistribution: Map<String, Long>
    ): Int {
        var score = 100

        // 根据剩余空间扣分
        when {
            storageInfo.usagePercent > 95 -> score -= 40
            storageInfo.usagePercent > 90 -> score -= 30
            storageInfo.usagePercent > 85 -> score -= 20
            storageInfo.usagePercent > 80 -> score -= 10
        }

        // 根据缓存占比扣分
        val cacheSize = spaceDistribution["缓存"] ?: 0L
        val cachePercent = (cacheSize * 100.0 / storageInfo.usedSpace).toInt()
        when {
            cachePercent > 20 -> score -= 20
            cachePercent > 15 -> score -= 15
            cachePercent > 10 -> score -= 10
            cachePercent > 5 -> score -= 5
        }

        // 根据垃圾文件扣分
        val otherSize = spaceDistribution["其他"] ?: 0L
        val otherPercent = (otherSize * 100.0 / storageInfo.usedSpace).toInt()
        when {
            otherPercent > 30 -> score -= 15
            otherPercent > 20 -> score -= 10
            otherPercent > 10 -> score -= 5
        }

        return score.coerceIn(0, 100)
    }

    /**
     * 生成优化建议
     */
    private fun generateRecommendations(
        storageInfo: StorageInfo,
        spaceDistribution: Map<String, Long>,
        healthScore: Int
    ): List<String> {
        val recommendations = mutableListOf<String>()

        // 根据剩余空间建议
        if (storageInfo.usagePercent > 90) {
            recommendations.add("存储空间严重不足，建议立即清理")
        } else if (storageInfo.usagePercent > 80) {
            recommendations.add("存储空间较少，建议定期清理")
        }

        // 根据缓存大小建议
        val cacheSize = spaceDistribution["缓存"] ?: 0L
        if (cacheSize > 500 * 1024 * 1024) { // 500MB
            recommendations.add("缓存文件过多(${formatFileSize(cacheSize)})，建议清理缓存")
        }

        // 根据大文件建议
        val videoSize = spaceDistribution["视频"] ?: 0L
        if (videoSize > 2L * 1024 * 1024 * 1024) { // 2GB
            recommendations.add("视频文件占用较大空间，考虑移动到云存储")
        }

        // 根据健康评分建议
        when {
            healthScore < 40 -> recommendations.add("存储健康状况差，需要立即优化")
            healthScore < 60 -> recommendations.add("存储健康状况一般，建议优化")
            healthScore < 80 -> recommendations.add("存储健康状况良好，定期维护即可")
        }

        // 通用建议
        if (recommendations.isEmpty()) {
            recommendations.add("存储状况良好，继续保持")
        }

        return recommendations
    }

    // ========== 辅助方法 ==========

    private fun calculateDirectorySize(dir: File?): Long {
        if (dir == null || !dir.exists() || !dir.isDirectory) return 0L

        var size = 0L
        try {
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    file.length()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error calculating directory size: ${dir.absolutePath}", e)
        }
        return size
    }

    private fun calculateCacheSize(): Long {
        var totalCacheSize = 0L

        // 内部缓存
        context.cacheDir?.let {
            totalCacheSize += calculateDirectorySize(it)
        }

        // 外部缓存
        context.externalCacheDir?.let {
            totalCacheSize += calculateDirectorySize(it)
        }

        // 系统缓存目录
        val sdCard = Environment.getExternalStorageDirectory()
        val systemCacheDirs = listOf(
            File(sdCard, ".cache"),
            File(sdCard, "Android/data")
        )

        systemCacheDirs.forEach { dir ->
            if (dir.exists() && dir.name.contains("cache", ignoreCase = true)) {
                totalCacheSize += calculateDirectorySize(dir)
            }
        }

        return totalCacheSize
    }

    private fun getAppSize(appInfo: ApplicationInfo): AppSize {
        // 注意：这个方法在新版本Android中可能需要特殊权限
        // 这里提供一个简化版本
        val appDir = File(appInfo.publicSourceDir)
        val codeSize = if (appDir.exists()) appDir.length() else 0L

        // 数据和缓存大小需要特殊权限才能准确获取
        // 这里使用估算值
        val dataSize = 0L
        val cacheSize = 0L

        return AppSize(
            codeSize = codeSize,
            dataSize = dataSize,
            cacheSize = cacheSize,
            totalSize = codeSize + dataSize + cacheSize
        )
    }

    private fun formatFileSize(size: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var bytes = size.toDouble()
        var unitIndex = 0

        while (bytes >= 1024 && unitIndex < units.size - 1) {
            bytes /= 1024
            unitIndex++
        }

        return String.format("%.2f %s", bytes, units[unitIndex])
    }

    private fun formatSpeed(speed: Double): String {
        return String.format("%.2f MB/s", speed)
    }
}

// ========== 数据类定义 ==========

data class StorageAnalysisResult(
    val totalSpace: Long,
    val usedSpace: Long,
    val freeSpace: Long,
    val usagePercent: Int,
    val spaceDistribution: Map<String, Long>,
    val appSizes: List<AppSizeInfo>,
    val readSpeed: Double,
    val writeSpeed: Double,
    val healthScore: Int,
    val recommendations: List<String>
)

data class StorageInfo(
    val totalSpace: Long,
    val usedSpace: Long,
    val freeSpace: Long,
    val usagePercent: Int
)

data class AppSizeInfo(
    val packageName: String,
    val appName: String,
    val codeSize: Long,
    val dataSize: Long,
    val cacheSize: Long,
    val totalSize: Long
)

data class AppSize(
    val codeSize: Long,
    val dataSize: Long,
    val cacheSize: Long,
    val totalSize: Long
)

data class SpeedTestResult(
    val readSpeed: Double,
    val writeSpeed: Double
)