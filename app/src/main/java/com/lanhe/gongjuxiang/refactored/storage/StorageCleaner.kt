package com.lanhe.gongjuxiang.refactored.storage

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import com.lanhe.gongjuxiang.refactored.interfaces.CleanResult
import com.lanhe.gongjuxiang.refactored.interfaces.CleanableItem
import com.lanhe.gongjuxiang.refactored.interfaces.CleanableType
import com.lanhe.gongjuxiang.refactored.interfaces.ICleaner

/**
 * 存储清理器 - 负责执行清理操作
 * 包括删除、压缩、移动等操作
 */
class StorageCleaner(private val context: Context) : ICleaner {

    companion object {
        private const val TAG = "StorageCleaner"
        private const val COMPRESS_THRESHOLD = 10 * 1024 * 1024L // 10MB以上才压缩
        private const val BATCH_SIZE = 100 // 批量处理大小
    }

    // 清理状态
    private val _cleanProgress = MutableStateFlow(0f)
    val cleanProgress: StateFlow<Float> = _cleanProgress.asStateFlow()

    private val _isCleaning = MutableStateFlow(false)
    val isCleaning: StateFlow<Boolean> = _isCleaning.asStateFlow()

    private val _currentOperation = MutableStateFlow("")
    val currentOperation: StateFlow<String> = _currentOperation.asStateFlow()

    // 待清理项目缓存
    private var pendingCleanItems = mutableListOf<CleanableItem>()

    /**
     * 执行清理
     * @return 清理结果
     */
    override suspend fun clean(): CleanResult = withContext(Dispatchers.IO) {
        _isCleaning.value = true
        _cleanProgress.value = 0f

        var totalFreedSpace = 0L
        var itemsCleaned = 0
        val errors = mutableListOf<String>()

        try {
            val itemsToClean = pendingCleanItems.toList()
            val totalItems = itemsToClean.size

            // 按类型分组处理
            val groupedItems = itemsToClean.groupBy { it.type }

            groupedItems.forEach { (type, items) ->
                _currentOperation.value = "正在清理${getTypeName(type)}..."

                val result = when (type) {
                    CleanableType.CACHE -> cleanCacheFiles(items)
                    CleanableType.TEMP_FILE -> cleanTempFiles(items)
                    CleanableType.LOG_FILE -> cleanLogFiles(items)
                    CleanableType.DUPLICATE_FILE -> cleanDuplicateFiles(items)
                    CleanableType.LARGE_FILE -> handleLargeFiles(items)
                    CleanableType.APK_FILE -> cleanApkFiles(items)
                    CleanableType.EMPTY_FOLDER -> cleanEmptyFolders(items)
                }

                totalFreedSpace += result.freedSpace
                itemsCleaned += result.itemsCleaned

                if (!result.success) {
                    errors.add(result.message)
                }

                _cleanProgress.value = itemsCleaned.toFloat() / totalItems
            }

            CleanResult(
                success = errors.isEmpty(),
                freedSpace = totalFreedSpace,
                itemsCleaned = itemsCleaned,
                message = if (errors.isEmpty()) {
                    "清理完成，释放了${formatFileSize(totalFreedSpace)}空间"
                } else {
                    "清理部分完成，有${errors.size}个错误"
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Clean operation failed", e)
            CleanResult(
                success = false,
                freedSpace = totalFreedSpace,
                itemsCleaned = itemsCleaned,
                message = "清理失败: ${e.message}"
            )
        } finally {
            _isCleaning.value = false
            _cleanProgress.value = 1f
            _currentOperation.value = ""
            pendingCleanItems.clear()
        }
    }

    /**
     * 预览清理项目
     * @return 待清理项目列表
     */
    override suspend fun preview(): List<CleanableItem> {
        return pendingCleanItems.toList()
    }

    /**
     * 添加待清理项目
     * @param items 要添加的项目
     */
    fun addCleanableItems(items: List<CleanableItem>) {
        pendingCleanItems.addAll(items.filter { it.canDelete })
    }

    /**
     * 移除待清理项目
     * @param item 要移除的项目
     */
    fun removeCleanableItem(item: CleanableItem) {
        pendingCleanItems.remove(item)
    }

    /**
     * 清空待清理项目
     */
    fun clearPendingItems() {
        pendingCleanItems.clear()
    }

    /**
     * 删除文件列表
     * @param files 要删除的文件列表
     * @return 删除结果
     */
    suspend fun deleteFiles(files: List<File>): CleanResult = withContext(Dispatchers.IO) {
        var freedSpace = 0L
        var deletedCount = 0

        files.forEach { file ->
            try {
                val fileSize = if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    file.length()
                }

                if (deleteRecursively(file)) {
                    freedSpace += fileSize
                    deletedCount++
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete: ${file.absolutePath}", e)
            }
        }

        CleanResult(
            success = deletedCount > 0,
            freedSpace = freedSpace,
            itemsCleaned = deletedCount,
            message = "删除了${deletedCount}个文件"
        )
    }

    /**
     * 压缩文件
     * @param files 要压缩的文件列表
     * @param outputPath 输出路径
     * @return 压缩结果
     */
    suspend fun compressFiles(
        files: List<File>,
        outputPath: String
    ): CleanResult = withContext(Dispatchers.IO) {
        var savedSpace = 0L
        var compressedCount = 0

        try {
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()

            ZipOutputStream(outputFile.outputStream()).use { zos ->
                files.forEach { file ->
                    if (file.exists() && file.canRead()) {
                        val originalSize = if (file.isDirectory) {
                            compressDirectoryToZip(file, file.name, zos)
                        } else {
                            compressFileToZip(file, file.name, zos)
                        }

                        // 压缩后删除原文件
                        if (deleteRecursively(file)) {
                            savedSpace += originalSize
                            compressedCount++
                        }
                    }
                }
            }

            // 计算实际节省的空间
            savedSpace -= outputFile.length()

            CleanResult(
                success = compressedCount > 0,
                freedSpace = savedSpace,
                itemsCleaned = compressedCount,
                message = "压缩了${compressedCount}个文件，节省${formatFileSize(savedSpace)}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Compression failed", e)
            CleanResult(
                success = false,
                freedSpace = 0,
                itemsCleaned = 0,
                message = "压缩失败: ${e.message}"
            )
        }
    }

    // ========== 私有辅助方法 ==========

    private suspend fun cleanCacheFiles(items: List<CleanableItem>): CleanResult {
        return cleanItemsByType(items, "缓存文件")
    }

    private suspend fun cleanTempFiles(items: List<CleanableItem>): CleanResult {
        return cleanItemsByType(items, "临时文件")
    }

    private suspend fun cleanLogFiles(items: List<CleanableItem>): CleanResult {
        return cleanItemsByType(items, "日志文件")
    }

    private suspend fun cleanApkFiles(items: List<CleanableItem>): CleanResult {
        return cleanItemsByType(items, "APK文件")
    }

    private suspend fun cleanEmptyFolders(items: List<CleanableItem>): CleanResult {
        return cleanItemsByType(items, "空文件夹")
    }

    private suspend fun cleanDuplicateFiles(items: List<CleanableItem>): CleanResult {
        // 对于重复文件，保留第一个，删除其余
        val groupedBySize = items.groupBy { it.size }
        var freedSpace = 0L
        var deletedCount = 0

        groupedBySize.forEach { (_, duplicates) ->
            if (duplicates.size > 1) {
                // 跳过第一个文件，删除其余重复文件
                duplicates.drop(1).forEach { item ->
                    val file = File(item.path)
                    if (file.exists() && deleteRecursively(file)) {
                        freedSpace += item.size
                        deletedCount++
                    }
                }
            }
        }

        return CleanResult(
            success = deletedCount > 0,
            freedSpace = freedSpace,
            itemsCleaned = deletedCount,
            message = "清理了${deletedCount}个重复文件"
        )
    }

    private suspend fun handleLargeFiles(items: List<CleanableItem>): CleanResult {
        // 对于大文件，可以选择压缩或移动
        var freedSpace = 0L
        var processedCount = 0

        items.forEach { item ->
            val file = File(item.path)
            if (file.exists()) {
                // 如果文件超过压缩阈值，尝试压缩
                if (item.size > COMPRESS_THRESHOLD && shouldCompress(file)) {
                    val zipPath = "${file.absolutePath}.zip"
                    val result = compressFiles(listOf(file), zipPath)
                    if (result.success) {
                        freedSpace += result.freedSpace
                        processedCount++
                    }
                } else {
                    // 否则直接删除
                    if (deleteRecursively(file)) {
                        freedSpace += item.size
                        processedCount++
                    }
                }
            }
        }

        return CleanResult(
            success = processedCount > 0,
            freedSpace = freedSpace,
            itemsCleaned = processedCount,
            message = "处理了${processedCount}个大文件"
        )
    }

    private suspend fun cleanItemsByType(
        items: List<CleanableItem>,
        typeName: String
    ): CleanResult {
        var freedSpace = 0L
        var deletedCount = 0

        // 批量处理以提高效率
        items.chunked(BATCH_SIZE).forEach { batch ->
            batch.forEach { item ->
                val file = File(item.path)
                if (file.exists() && deleteRecursively(file)) {
                    freedSpace += item.size
                    deletedCount++
                }
            }
        }

        return CleanResult(
            success = deletedCount > 0,
            freedSpace = freedSpace,
            itemsCleaned = deletedCount,
            message = "清理了${deletedCount}个${typeName}"
        )
    }

    private fun deleteRecursively(file: File): Boolean {
        return try {
            if (file.isDirectory) {
                file.listFiles()?.forEach { child ->
                    deleteRecursively(child)
                }
            }
            file.delete()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete: ${file.absolutePath}", e)
            false
        }
    }

    private fun calculateDirectorySize(dir: File): Long {
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

    private fun compressFileToZip(file: File, entryName: String, zos: ZipOutputStream): Long {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        file.inputStream().use { input ->
            input.copyTo(zos)
        }
        zos.closeEntry()
        return file.length()
    }

    private fun compressDirectoryToZip(dir: File, baseName: String, zos: ZipOutputStream): Long {
        var totalSize = 0L
        dir.listFiles()?.forEach { file ->
            val entryName = "$baseName/${file.name}"
            totalSize += if (file.isDirectory) {
                compressDirectoryToZip(file, entryName, zos)
            } else {
                compressFileToZip(file, entryName, zos)
            }
        }
        return totalSize
    }

    private fun shouldCompress(file: File): Boolean {
        // 判断文件是否适合压缩（避免压缩已压缩的文件）
        val compressedExtensions = listOf(".zip", ".rar", ".7z", ".gz", ".bz2", ".xz", ".jpg", ".jpeg", ".png", ".mp3", ".mp4", ".avi", ".mkv")
        return !compressedExtensions.any { file.name.endsWith(it, ignoreCase = true) }
    }

    private fun getTypeName(type: CleanableType): String {
        return when (type) {
            CleanableType.CACHE -> "缓存文件"
            CleanableType.TEMP_FILE -> "临时文件"
            CleanableType.LOG_FILE -> "日志文件"
            CleanableType.DUPLICATE_FILE -> "重复文件"
            CleanableType.LARGE_FILE -> "大文件"
            CleanableType.APK_FILE -> "APK文件"
            CleanableType.EMPTY_FOLDER -> "空文件夹"
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