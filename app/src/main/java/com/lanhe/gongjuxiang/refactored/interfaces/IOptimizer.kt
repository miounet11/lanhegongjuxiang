package com.lanhe.gongjuxiang.refactored.interfaces

/**
 * 优化器通用接口
 */
interface IOptimizer<T> {
    /**
     * 执行优化
     * @return 优化结果
     */
    suspend fun optimize(): T

    /**
     * 分析当前状态
     * @return 分析结果
     */
    suspend fun analyze(): T

    /**
     * 获取优化建议
     * @return 建议列表
     */
    suspend fun getSuggestions(): List<String>
}

/**
 * 监控器通用接口
 */
interface IMonitor<T> {
    /**
     * 开始监控
     */
    fun startMonitoring()

    /**
     * 停止监控
     */
    fun stopMonitoring()

    /**
     * 获取当前状态
     * @return 监控数据
     */
    fun getCurrentState(): T

    /**
     * 获取历史数据
     * @param limit 数据条数限制
     * @return 历史数据列表
     */
    fun getHistory(limit: Int = 100): List<T>
}

/**
 * 分析器通用接口
 */
interface IAnalyzer<T> {
    /**
     * 执行分析
     * @return 分析结果
     */
    suspend fun performAnalysis(): T

    /**
     * 生成报告
     * @return 报告内容
     */
    suspend fun generateReport(): String
}

/**
 * 清理器通用接口
 */
interface ICleaner {
    /**
     * 执行清理
     * @return 清理结果
     */
    suspend fun clean(): CleanResult

    /**
     * 预览清理项目
     * @return 待清理项目列表
     */
    suspend fun preview(): List<CleanableItem>
}

/**
 * 清理结果
 */
data class CleanResult(
    val success: Boolean,
    val freedSpace: Long,
    val itemsCleaned: Int,
    val message: String
)

/**
 * 可清理项目
 */
data class CleanableItem(
    val name: String,
    val path: String,
    val size: Long,
    val type: CleanableType,
    val canDelete: Boolean = true
)

/**
 * 可清理类型
 */
enum class CleanableType {
    CACHE,
    TEMP_FILE,
    LOG_FILE,
    DUPLICATE_FILE,
    LARGE_FILE,
    APK_FILE,
    EMPTY_FOLDER
}