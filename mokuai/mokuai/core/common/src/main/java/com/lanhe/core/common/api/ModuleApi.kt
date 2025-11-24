package com.lanhe.core.common.api

import kotlinx.coroutines.flow.Flow

/**
 * 模块基础API接口
 */
interface ModuleApi {
    /**
     * 获取模块名称
     */
    fun getModuleName(): String
    
    /**
     * 获取模块版本
     */
    fun getModuleVersion(): String
    
    /**
     * 初始化模块
     */
    suspend fun initialize(): Result<Unit>
    
    /**
     * 清理资源
     */
    suspend fun cleanup(): Result<Unit>
    
    /**
     * 获取模块状态
     */
    fun getModuleStatus(): Flow<ModuleStatus>
}

/**
 * 模块状态枚举
 */
enum class ModuleStatus {
    UNINITIALIZED,
    INITIALIZING,
    INITIALIZED,
    ERROR,
    DISABLED
}

/**
 * 网络模块API
 */
interface NetworkModuleApi : ModuleApi {
    suspend fun <T> request(config: NetworkRequestConfig<T>): Result<T>
    suspend fun download(url: String, destination: String): Result<String>
}

/**
 * 性能监控模块API
 */
interface PerformanceMonitorApi : ModuleApi {
    fun getCpuUsage(): Flow<Float>
    fun getMemoryUsage(): Flow<MemoryInfo>
    fun getBatteryInfo(): Flow<BatteryInfo>
    suspend fun startMonitoring(): Result<Unit>
    suspend fun stopMonitoring(): Result<Unit>
}

/**
 * 内存管理模块API
 */
interface MemoryManagerApi : ModuleApi {
    suspend fun cleanMemory(): Result<MemoryCleanupResult>
    suspend fun getMemoryStats(): Result<MemoryStats>
    suspend fun optimizeMemory(): Result<MemoryOptimizationResult>
}

/**
 * 文件系统模块API
 */
interface FilesystemModuleApi : ModuleApi {
    suspend fun listFiles(path: String): Result<List<FileItem>>
    suspend fun deleteFile(path: String): Result<Unit>
    suspend fun createDirectory(path: String): Result<Unit>
    suspend fun getFileStats(path: String): Result<FileStats>
}

/**
 * 数据传输对象
 */
data class NetworkRequestConfig<T>(
    val url: String,
    val method: HttpMethod = HttpMethod.GET,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val responseType: Class<T>
)

enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH
}

data class MemoryInfo(
    val totalMemory: Long,
    val availableMemory: Long,
    val usedMemory: Long,
    val usagePercentage: Float
)

data class BatteryInfo(
    val level: Int,
    val temperature: Float,
    val voltage: Float,
    val health: Int,
    val isCharging: Boolean
)

data class MemoryCleanupResult(
    val freedMemory: Long,
    val cleanedProcesses: Int,
    val success: Boolean
)

data class MemoryStats(
    val totalRAM: Long,
    val availableRAM: Long,
    val usedRAM: Long,
    val cachedMemory: Long,
    val buffersMemory: Long
)

data class MemoryOptimizationResult(
    val optimizedMemory: Long,
    val closedApps: List<String>,
    val recommendations: List<String>
)

data class FileItem(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Long,
    val permissions: String
)

data class FileStats(
    val size: Long,
    val created: Long,
    val modified: Long,
    val accessed: Long,
    val isReadable: Boolean,
    val isWritable: Boolean,
    val isExecutable: Boolean
)
