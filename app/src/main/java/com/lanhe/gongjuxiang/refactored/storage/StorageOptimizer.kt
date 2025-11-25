package com.lanhe.gongjuxiang.refactored.storage

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.lanhe.gongjuxiang.refactored.interfaces.IOptimizer
import com.lanhe.gongjuxiang.refactored.interfaces.CleanResult
import com.lanhe.gongjuxiang.refactored.interfaces.CleanableItem

/**
 * 存储优化器 - 主控制器
 * 协调StorageScanner、StorageCleaner和StorageAnalyzer的工作
 */
class StorageOptimizer(private val context: Context) : IOptimizer<StorageOptimizationResult> {

    companion object {
        private const val TAG = "StorageOptimizer"
        private const val AUTO_CLEAN_THRESHOLD = 90 // 自动清理阈值（使用率%）
    }

    // 子模块
    private val scanner = StorageScanner(context)
    private val cleaner = StorageCleaner(context)
    private val analyzer = StorageAnalyzer(context)

    // 优化状态
    private val _optimizationState = MutableStateFlow(OptimizationState.IDLE)
    val optimizationState: StateFlow<OptimizationState> = _optimizationState.asStateFlow()

    private val _lastOptimizationResult = MutableStateFlow<StorageOptimizationResult?>(null)
    val lastOptimizationResult: StateFlow<StorageOptimizationResult?> = _lastOptimizationResult.asStateFlow()

    /**
     * 执行完整的存储优化
     * @return 优化结果
     */
    override suspend fun optimize(): StorageOptimizationResult = withContext(Dispatchers.IO) {
        _optimizationState.value = OptimizationState.OPTIMIZING

        try {
            val startTime = System.currentTimeMillis()
            val improvements = mutableListOf<String>()
            var totalFreedSpace = 0L

            // 1. 分析存储状态
            _optimizationState.value = OptimizationState.ANALYZING
            val analysisResult = analyzer.performAnalysis()

            // 2. 根据分析结果决定优化策略
            val strategy = determineOptimizationStrategy(analysisResult)

            // 3. 执行扫描
            _optimizationState.value = OptimizationState.SCANNING
            val scanResults = performTargetedScan(strategy)

            // 4. 执行清理
            _optimizationState.value = OptimizationState.CLEANING
            val cleanResult = performTargetedClean(scanResults, strategy)

            totalFreedSpace += cleanResult.freedSpace
            improvements.addAll(generateImprovementMessages(cleanResult, strategy))

            // 5. 执行优化后分析
            _optimizationState.value = OptimizationState.ANALYZING
            val postAnalysis = analyzer.performAnalysis()

            val duration = System.currentTimeMillis() - startTime

            val result = StorageOptimizationResult(
                success = cleanResult.success,
                freedSpace = totalFreedSpace,
                improvements = improvements,
                preOptimizationState = analysisResult,
                postOptimizationState = postAnalysis,
                optimizationDuration = duration,
                strategy = strategy,
                message = generateOptimizationMessage(totalFreedSpace, improvements.size, duration)
            )

            _lastOptimizationResult.value = result
            result

        } catch (e: Exception) {
            Log.e(TAG, "Optimization failed", e)
            StorageOptimizationResult(
                success = false,
                freedSpace = 0,
                improvements = emptyList(),
                preOptimizationState = null,
                postOptimizationState = null,
                optimizationDuration = 0,
                strategy = OptimizationStrategy.CONSERVATIVE,
                message = "优化失败: ${e.message}"
            )
        } finally {
            _optimizationState.value = OptimizationState.IDLE
        }
    }

    /**
     * 分析当前存储状态
     * @return 分析结果
     */
    override suspend fun analyze(): StorageOptimizationResult = withContext(Dispatchers.IO) {
        _optimizationState.value = OptimizationState.ANALYZING

        try {
            val analysis = analyzer.performAnalysis()

            StorageOptimizationResult(
                success = true,
                freedSpace = 0,
                improvements = emptyList(),
                preOptimizationState = analysis,
                postOptimizationState = null,
                optimizationDuration = 0,
                strategy = determineOptimizationStrategy(analysis),
                message = "分析完成，健康评分: ${analysis.healthScore}/100"
            )
        } finally {
            _optimizationState.value = OptimizationState.IDLE
        }
    }

    /**
     * 获取优化建议
     * @return 建议列表
     */
    override suspend fun getSuggestions(): List<String> = withContext(Dispatchers.IO) {
        val analysis = analyzer.performAnalysis()
        val suggestions = mutableListOf<String>()

        // 基于分析结果的建议
        suggestions.addAll(analysis.recommendations)

        // 基于扫描结果的建议
        val duplicates = scanner.scanDuplicateFiles()
        if (duplicates.isNotEmpty()) {
            val duplicateSize = duplicates.values.flatten().sumOf { it.length() }
            suggestions.add("发现${duplicates.size}组重复文件，可释放${formatFileSize(duplicateSize)}空间")
        }

        val largeFiles = scanner.scanLargeFiles()
        if (largeFiles.size > 10) {
            suggestions.add("发现${largeFiles.size}个大文件，建议检查并清理不需要的文件")
        }

        suggestions
    }

    /**
     * 快速清理（只清理安全的项目）
     */
    suspend fun quickClean(): CleanResult = withContext(Dispatchers.IO) {
        _optimizationState.value = OptimizationState.CLEANING

        try {
            // 只扫描和清理缓存文件
            val cacheFiles = scanner.scanCacheFiles()
            cleaner.addCleanableItems(cacheFiles)
            cleaner.clean()
        } finally {
            _optimizationState.value = OptimizationState.IDLE
        }
    }

    /**
     * 深度清理（包括所有类型的文件）
     */
    suspend fun deepClean(): CleanResult = withContext(Dispatchers.IO) {
        _optimizationState.value = OptimizationState.CLEANING

        try {
            // 扫描所有类型的文件
            val allCleanableItems = mutableListOf<CleanableItem>()

            // 缓存文件
            allCleanableItems.addAll(scanner.scanCacheFiles())

            // APK文件
            allCleanableItems.addAll(scanner.scanApkFiles())

            // 空文件夹
            allCleanableItems.addAll(scanner.scanEmptyFolders())

            // 重复文件（只保留一份）
            val duplicates = scanner.scanDuplicateFiles()
            duplicates.forEach { (_, files) ->
                if (files.size > 1) {
                    // 跳过第一个文件，将其余标记为可清理
                    files.drop(1).forEach { file ->
                        allCleanableItems.add(
                            CleanableItem(
                                name = file.name,
                                path = file.absolutePath,
                                size = file.length(),
                                type = com.lanhe.gongjuxiang.refactored.interfaces.CleanableType.DUPLICATE_FILE,
                                canDelete = true
                            )
                        )
                    }
                }
            }

            cleaner.addCleanableItems(allCleanableItems)
            cleaner.clean()
        } finally {
            _optimizationState.value = OptimizationState.IDLE
        }
    }

    /**
     * 自定义清理（用户选择要清理的项目）
     * @param items 要清理的项目列表
     */
    suspend fun customClean(items: List<CleanableItem>): CleanResult = withContext(Dispatchers.IO) {
        _optimizationState.value = OptimizationState.CLEANING

        try {
            cleaner.clearPendingItems()
            cleaner.addCleanableItems(items)
            cleaner.clean()
        } finally {
            _optimizationState.value = OptimizationState.IDLE
        }
    }

    // ========== 私有辅助方法 ==========

    private fun determineOptimizationStrategy(analysis: StorageAnalysisResult): OptimizationStrategy {
        return when {
            analysis.usagePercent > 95 -> OptimizationStrategy.AGGRESSIVE
            analysis.usagePercent > 85 -> OptimizationStrategy.BALANCED
            analysis.healthScore < 40 -> OptimizationStrategy.AGGRESSIVE
            analysis.healthScore < 60 -> OptimizationStrategy.BALANCED
            else -> OptimizationStrategy.CONSERVATIVE
        }
    }

    private suspend fun performTargetedScan(strategy: OptimizationStrategy): ScanResults {
        val results = ScanResults()

        when (strategy) {
            OptimizationStrategy.AGGRESSIVE -> {
                // 扫描所有类型
                results.cacheFiles = scanner.scanCacheFiles()
                results.duplicateFiles = scanner.scanDuplicateFiles()
                results.largeFiles = scanner.scanLargeFiles()
                results.apkFiles = scanner.scanApkFiles()
                results.emptyFolders = scanner.scanEmptyFolders()
            }
            OptimizationStrategy.BALANCED -> {
                // 扫描主要类型
                results.cacheFiles = scanner.scanCacheFiles()
                results.duplicateFiles = scanner.scanDuplicateFiles()
                results.apkFiles = scanner.scanApkFiles()
            }
            OptimizationStrategy.CONSERVATIVE -> {
                // 只扫描安全类型
                results.cacheFiles = scanner.scanCacheFiles()
            }
        }

        return results
    }

    private suspend fun performTargetedClean(
        scanResults: ScanResults,
        strategy: OptimizationStrategy
    ): CleanResult {
        val itemsToClean = mutableListOf<CleanableItem>()

        // 根据策略添加清理项目
        when (strategy) {
            OptimizationStrategy.AGGRESSIVE -> {
                // 清理所有发现的项目
                itemsToClean.addAll(scanResults.cacheFiles)
                itemsToClean.addAll(scanResults.apkFiles)
                itemsToClean.addAll(scanResults.emptyFolders)

                // 处理重复文件
                scanResults.duplicateFiles.forEach { (_, files) ->
                    if (files.size > 1) {
                        files.drop(1).forEach { file ->
                            itemsToClean.add(
                                CleanableItem(
                                    name = file.name,
                                    path = file.absolutePath,
                                    size = file.length(),
                                    type = com.lanhe.gongjuxiang.refactored.interfaces.CleanableType.DUPLICATE_FILE,
                                    canDelete = true
                                )
                            )
                        }
                    }
                }

                // 处理大文件（需要用户确认）
                itemsToClean.addAll(scanResults.largeFiles.filter { it.canDelete })
            }
            OptimizationStrategy.BALANCED -> {
                // 清理安全的项目
                itemsToClean.addAll(scanResults.cacheFiles)
                itemsToClean.addAll(scanResults.apkFiles.filter { it.canDelete })

                // 只清理明显的重复文件
                scanResults.duplicateFiles.forEach { (_, files) ->
                    if (files.size > 2) { // 多个重复时才清理
                        files.drop(1).forEach { file ->
                            itemsToClean.add(
                                CleanableItem(
                                    name = file.name,
                                    path = file.absolutePath,
                                    size = file.length(),
                                    type = com.lanhe.gongjuxiang.refactored.interfaces.CleanableType.DUPLICATE_FILE,
                                    canDelete = true
                                )
                            )
                        }
                    }
                }
            }
            OptimizationStrategy.CONSERVATIVE -> {
                // 只清理缓存
                itemsToClean.addAll(scanResults.cacheFiles)
            }
        }

        cleaner.clearPendingItems()
        cleaner.addCleanableItems(itemsToClean)
        return cleaner.clean()
    }

    private fun generateImprovementMessages(
        cleanResult: CleanResult,
        strategy: OptimizationStrategy
    ): List<String> {
        val messages = mutableListOf<String>()

        if (cleanResult.freedSpace > 0) {
            messages.add("释放了${formatFileSize(cleanResult.freedSpace)}存储空间")
        }

        if (cleanResult.itemsCleaned > 0) {
            messages.add("清理了${cleanResult.itemsCleaned}个文件")
        }

        when (strategy) {
            OptimizationStrategy.AGGRESSIVE -> messages.add("执行了深度优化")
            OptimizationStrategy.BALANCED -> messages.add("执行了平衡优化")
            OptimizationStrategy.CONSERVATIVE -> messages.add("执行了保守优化")
        }

        return messages
    }

    private fun generateOptimizationMessage(
        freedSpace: Long,
        improvementCount: Int,
        duration: Long
    ): String {
        val durationSeconds = duration / 1000.0
        return buildString {
            append("优化完成：")
            if (freedSpace > 0) {
                append("释放${formatFileSize(freedSpace)}空间，")
            }
            append("执行了${improvementCount}项优化，")
            append("耗时${String.format("%.1f", durationSeconds)}秒")
        }
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
}

// ========== 数据类和枚举定义 ==========

/**
 * 优化状态
 */
enum class OptimizationState {
    IDLE,       // 空闲
    ANALYZING,  // 分析中
    SCANNING,   // 扫描中
    CLEANING,   // 清理中
    OPTIMIZING  // 优化中
}

/**
 * 优化策略
 */
enum class OptimizationStrategy {
    CONSERVATIVE,  // 保守策略（只清理缓存）
    BALANCED,      // 平衡策略（清理缓存和明显垃圾）
    AGGRESSIVE     // 激进策略（清理所有可清理项目）
}

/**
 * 扫描结果
 */
data class ScanResults(
    var cacheFiles: List<CleanableItem> = emptyList(),
    var duplicateFiles: Map<String, List<java.io.File>> = emptyMap(),
    var largeFiles: List<CleanableItem> = emptyList(),
    var apkFiles: List<CleanableItem> = emptyList(),
    var emptyFolders: List<CleanableItem> = emptyList()
)

/**
 * 存储优化结果
 */
data class StorageOptimizationResult(
    val success: Boolean,
    val freedSpace: Long,
    val improvements: List<String>,
    val preOptimizationState: StorageAnalysisResult?,
    val postOptimizationState: StorageAnalysisResult?,
    val optimizationDuration: Long,
    val strategy: OptimizationStrategy,
    val message: String
)