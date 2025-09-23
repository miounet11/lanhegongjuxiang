package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

/**
 * 智能清理工具类
 * 提供系统垃圾文件扫描、识别和清理功能
 */
class SmartCleaner(private val context: Context) {

    /**
     * 清理项目数据类
     */
    data class CleanItem(
        val path: String,           // 文件路径
        val size: Long,            // 文件大小 (bytes)
        val type: CleanType,       // 清理类型
        val description: String,   // 描述
        val isSafe: Boolean        // 是否安全删除
    )

    /**
     * 清理类型枚举
     */
    enum class CleanType {
        CACHE,          // 缓存文件
        TEMP,           // 临时文件
        LOG,            // 日志文件
        THUMBNAIL,      // 缩略图
        APK,            // APK安装包
        EMPTY_FOLDER,   // 空文件夹
        UNUSED_APP      // 未使用应用数据
    }

    /**
     * 清理结果数据类
     */
    data class CleanResult(
        val totalSize: Long,        // 总清理大小
        val itemsCount: Int,        // 清理项目数量
        val cleanedItems: List<CleanItem> // 已清理项目列表
    )

    /**
     * 扫描垃圾文件
     */
    suspend fun scanJunkFiles(): List<CleanItem> = withContext(Dispatchers.IO) {
        val junkFiles = mutableListOf<CleanItem>()

        // 扫描应用缓存
        junkFiles.addAll(scanAppCache())

        // 扫描系统临时文件
        junkFiles.addAll(scanTempFiles())

        // 扫描日志文件
        junkFiles.addAll(scanLogFiles())

        // 扫描缩略图缓存
        junkFiles.addAll(scanThumbnails())

        // 扫描旧版APK文件
        junkFiles.addAll(scanOldApks())

        // 扫描空文件夹
        junkFiles.addAll(scanEmptyFolders())

        junkFiles
    }

    /**
     * 执行清理操作
     */
    suspend fun performClean(items: List<CleanItem>): CleanResult = withContext(Dispatchers.IO) {
        var totalSize = 0L
        val cleanedItems = mutableListOf<CleanItem>()

        for (item in items) {
            try {
                if (deleteFileOrDirectory(item.path)) {
                    totalSize += item.size
                    cleanedItems.add(item)
                }
            } catch (e: Exception) {
                // 记录删除失败的项目，但不中断整个清理过程
                e.printStackTrace()
            }
        }

        CleanResult(totalSize, cleanedItems.size, cleanedItems)
    }

    /**
     * 扫描应用缓存文件
     */
    private fun scanAppCache(): List<CleanItem> {
        val cacheItems = mutableListOf<CleanItem>()

        try {
            // 应用内部缓存
            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                cacheItems.addAll(scanDirectory(cacheDir, CleanType.CACHE, "应用缓存文件"))
            }

            // 应用外部缓存
            val externalCacheDir = context.externalCacheDir
            if (externalCacheDir?.exists() == true) {
                cacheItems.addAll(scanDirectory(externalCacheDir, CleanType.CACHE, "外部缓存文件"))
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return cacheItems
    }

    /**
     * 扫描临时文件
     */
    private fun scanTempFiles(): List<CleanItem> {
        val tempItems = mutableListOf<CleanItem>()

        try {
            // 系统临时目录
            val tempDir = File(System.getProperty("java.io.tmpdir"))
            if (tempDir.exists()) {
                tempItems.addAll(scanDirectory(tempDir, CleanType.TEMP, "系统临时文件"))
            }

            // Download目录下的临时文件
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir.exists()) {
                val tempFiles = downloadDir.listFiles { file ->
                    file.name.endsWith(".tmp") || file.name.endsWith(".temp") ||
                    file.name.contains("temp") || file.name.contains("tmp")
                }
                tempFiles?.forEach { file ->
                    tempItems.add(CleanItem(
                        path = file.absolutePath,
                        size = file.length(),
                        type = CleanType.TEMP,
                        description = "下载临时文件: ${file.name}",
                        isSafe = true
                    ))
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return tempItems
    }

    /**
     * 扫描日志文件
     */
    private fun scanLogFiles(): List<CleanItem> {
        val logItems = mutableListOf<CleanItem>()

        try {
            val logExtensions = arrayOf(".log", ".txt", ".logcat", ".bugreport")

            // 扫描外部存储目录
            val externalDir = Environment.getExternalStorageDirectory()
            if (externalDir.exists()) {
                val logFiles = findFilesByExtensions(externalDir, logExtensions, maxDepth = 3)
                logFiles.forEach { file ->
                    // 只清理超过7天的日志文件
                    if (isFileOlderThan(file, 7)) {
                        logItems.add(CleanItem(
                            path = file.absolutePath,
                            size = file.length(),
                            type = CleanType.LOG,
                            description = "旧日志文件: ${file.name}",
                            isSafe = true
                        ))
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return logItems
    }

    /**
     * 扫描缩略图缓存
     */
    private fun scanThumbnails(): List<CleanItem> {
        val thumbnailItems = mutableListOf<CleanItem>()

        try {
            // Android缩略图缓存目录
            val thumbnailDir = File(Environment.getExternalStorageDirectory(), ".thumbnails")
            if (thumbnailDir.exists()) {
                thumbnailItems.addAll(scanDirectory(thumbnailDir, CleanType.THUMBNAIL, "系统缩略图缓存"))
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return thumbnailItems
    }

    /**
     * 扫描旧版APK文件
     */
    private fun scanOldApks(): List<CleanItem> {
        val apkItems = mutableListOf<CleanItem>()

        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir.exists()) {
                val apkFiles = downloadDir.listFiles { file ->
                    file.name.endsWith(".apk")
                }

                apkFiles?.forEach { file ->
                    // 只清理超过30天的APK文件
                    if (isFileOlderThan(file, 30)) {
                        apkItems.add(CleanItem(
                            path = file.absolutePath,
                            size = file.length(),
                            type = CleanType.APK,
                            description = "旧版APK文件: ${file.name}",
                            isSafe = true
                        ))
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return apkItems
    }

    /**
     * 扫描空文件夹
     */
    private fun scanEmptyFolders(): List<CleanItem> {
        val emptyFolders = mutableListOf<CleanItem>()

        try {
            val externalDir = Environment.getExternalStorageDirectory()
            if (externalDir.exists()) {
                val emptyDirs = findEmptyDirectories(externalDir, maxDepth = 3)
                emptyDirs.forEach { dir ->
                    emptyFolders.add(CleanItem(
                        path = dir.absolutePath,
                        size = 0L,
                        type = CleanType.EMPTY_FOLDER,
                        description = "空文件夹: ${dir.name}",
                        isSafe = true
                    ))
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return emptyFolders
    }

    /**
     * 递归扫描目录
     */
    private fun scanDirectory(dir: File, type: CleanType, description: String): List<CleanItem> {
        val items = mutableListOf<CleanItem>()

        try {
            dir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    items.add(CleanItem(
                        path = file.absolutePath,
                        size = file.length(),
                        type = type,
                        description = "$description: ${file.name}",
                        isSafe = isSafeToDelete(file, type)
                    ))
                } else if (file.isDirectory && shouldScanSubDir(file, type)) {
                    items.addAll(scanDirectory(file, type, description))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return items
    }

    /**
     * 判断文件是否安全删除
     */
    private fun isSafeToDelete(file: File, type: CleanType): Boolean {
        return when (type) {
            CleanType.CACHE -> true
            CleanType.TEMP -> true
            CleanType.LOG -> isFileOlderThan(file, 7)
            CleanType.THUMBNAIL -> true
            CleanType.APK -> isFileOlderThan(file, 30)
            CleanType.EMPTY_FOLDER -> true
            CleanType.UNUSED_APP -> false // 需要额外验证
        }
    }

    /**
     * 判断是否应该扫描子目录
     */
    private fun shouldScanSubDir(dir: File, type: CleanType): Boolean {
        // 避免扫描系统关键目录
        val systemDirs = setOf("android", "data", "obb", "system")
        return !systemDirs.contains(dir.name.lowercase())
    }

    /**
     * 查找指定扩展名的文件
     */
    private fun findFilesByExtensions(dir: File, extensions: Array<String>, maxDepth: Int): List<File> {
        val files = mutableListOf<File>()

        fun scan(currentDir: File, currentDepth: Int) {
            if (currentDepth > maxDepth) return

            currentDir.listFiles()?.forEach { file ->
                if (file.isFile && extensions.any { file.name.endsWith(it) }) {
                    files.add(file)
                } else if (file.isDirectory) {
                    scan(file, currentDepth + 1)
                }
            }
        }

        scan(dir, 0)
        return files
    }

    /**
     * 查找空目录
     */
    private fun findEmptyDirectories(dir: File, maxDepth: Int): List<File> {
        val emptyDirs = mutableListOf<File>()

        fun scan(currentDir: File, currentDepth: Int) {
            if (currentDepth > maxDepth) return

            currentDir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    val subFiles = file.listFiles()
                    if (subFiles.isNullOrEmpty()) {
                        emptyDirs.add(file)
                    } else {
                        scan(file, currentDepth + 1)
                    }
                }
            }
        }

        scan(dir, 0)
        return emptyDirs
    }

    /**
     * 判断文件是否超过指定天数
     */
    private fun isFileOlderThan(file: File, days: Int): Boolean {
        val fileTime = file.lastModified()
        val currentTime = System.currentTimeMillis()
        val daysInMillis = days * 24 * 60 * 60 * 1000L
        return (currentTime - fileTime) > daysInMillis
    }

    /**
     * 删除文件或目录
     */
    private fun deleteFileOrDirectory(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.isDirectory) {
                deleteDirectory(file)
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 递归删除目录
     */
    private fun deleteDirectory(dir: File): Boolean {
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    file.delete()
                }
            }
            return dir.delete()
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 格式化文件大小
     */
    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()

        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    /**
     * 清理内存 (模拟)
     */
    suspend fun cleanMemory(): Long = withContext(Dispatchers.IO) {
        // 这里可以实现实际的内存清理逻辑
        // 目前返回一个模拟的清理大小
        (50..200).random() * 1024L * 1024L // 50-200MB
    }

    /**
     * 清理存储空间
     */
    suspend fun cleanStorage(): Long = withContext(Dispatchers.IO) {
        try {
            var totalCleaned = 0L

            // 清理缓存目录
            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                totalCleaned += cleanDirectory(cacheDir, CleanType.CACHE)
            }

            // 清理外部缓存
            val externalCacheDir = context.externalCacheDir
            if (externalCacheDir?.exists() == true) {
                totalCleaned += cleanDirectory(externalCacheDir, CleanType.CACHE)
            }

            // 清理临时文件
            val tempFiles = scanTempFiles()
            tempFiles.forEach { item ->
                if (item.isSafe && File(item.path).delete()) {
                    totalCleaned += item.size
                }
            }

            totalCleaned
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 清理指定目录
     */
    private fun cleanDirectory(dir: File, type: CleanType): Long {
        var cleanedSize = 0L
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.canWrite()) {
                    val size = file.length()
                    if (file.delete()) {
                        cleanedSize += size
                    }
                }
            }
        } catch (e: Exception) {
            // 忽略清理过程中的异常
        }
        return cleanedSize
    }

    /**
     * 获取上次清理大小
     */
    fun getLastCleanupSize(): Long {
        // 这里可以实现从SharedPreferences或其他存储中读取上次清理大小
        // 目前返回一个默认值
        return 150 * 1024L * 1024L // 150MB
    }
}
