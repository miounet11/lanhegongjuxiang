package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.lanhe.gongjuxiang.services.NotificationHelper
import com.lanhe.gongjuxiang.services.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.Path

/**
 * 文件管理器
 * 提供文件分类、图片管理、浏览器集成和批量操作功能
 */
class FileManager(private val context: Context) {

    private val notificationHelper = NotificationHelper(context)

    /**
     * 文件信息数据类
     */
    data class FileInfo(
        val file: File,
        val name: String,
        val path: String,
        val size: Long,
        val lastModified: Long,
        val isDirectory: Boolean,
        val extension: String,
        val mimeType: String,
        val category: String
    )

    /**
     * 文件分类结果
     */
    data class FileCategory(
        val category: String,
        val files: List<FileInfo>,
        val totalSize: Long,
        val fileCount: Int
    )

    /**
     * 图片信息
     */
    data class ImageInfo(
        val file: File,
        val width: Int = 0,
        val height: Int = 0,
        val size: Long,
        val lastModified: Long,
        val orientation: Int = 0
    )

    /**
     * 批量操作结果
     */
    data class BatchOperationResult(
        val successCount: Int,
        val failureCount: Int,
        val totalSize: Long,
        val errors: List<String>
    )

    /**
     * 获取存储目录
     */
    fun getStorageDirectories(): List<File> {
        val directories = mutableListOf<File>()

        // 外部存储
        Environment.getExternalStorageDirectory()?.let { directories.add(it) }

        // 外部存储的公共目录
        directories.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))
        directories.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
        directories.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))
        directories.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES))
        directories.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC))
        directories.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))

        return directories.filter { it.exists() && it.canRead() }
    }

    /**
     * 扫描目录中的文件
     */
    suspend fun scanDirectory(directory: File, recursive: Boolean = true): List<FileInfo> = withContext(Dispatchers.IO) {
        val files = mutableListOf<FileInfo>()

        if (!directory.exists() || !directory.canRead()) {
            return@withContext files
        }

        scanDirectoryRecursive(directory, files, recursive)
        files
    }

    /**
     * 递归扫描目录
     */
    private fun scanDirectoryRecursive(directory: File, files: MutableList<FileInfo>, recursive: Boolean) {
        val fileList = directory.listFiles() ?: return

        for (file in fileList) {
            if (file.isDirectory && recursive) {
                scanDirectoryRecursive(file, files, recursive)
            } else if (file.isFile) {
                val fileInfo = createFileInfo(file)
                files.add(fileInfo)
            }
        }
    }

    /**
     * 按类型分类文件
     */
    suspend fun categorizeFiles(directory: File): List<FileCategory> = withContext(Dispatchers.IO) {
        val allFiles = scanDirectory(directory)
        val categories = mutableMapOf<String, MutableList<FileInfo>>()

        for (fileInfo in allFiles) {
            categories.getOrPut(fileInfo.category) { mutableListOf() }.add(fileInfo)
        }

        categories.map { (category, files) ->
            FileCategory(
                category = category,
                files = files.sortedByDescending { it.lastModified },
                totalSize = files.sumOf { it.size },
                fileCount = files.size
            )
        }.sortedByDescending { it.totalSize }
    }

    /**
     * 按大小分类图片
     */
    suspend fun categorizeImagesBySize(directory: File): List<FileCategory> = withContext(Dispatchers.IO) {
        val imageFiles = scanDirectory(directory).filter { it.category == "图片" }
        val categories = mutableMapOf<String, MutableList<FileInfo>>()

        for (fileInfo in imageFiles) {
            val sizeCategory = when {
                fileInfo.size < 1024 * 1024 -> "小于1MB"
                fileInfo.size < 10 * 1024 * 1024 -> "1-10MB"
                fileInfo.size < 50 * 1024 * 1024 -> "10-50MB"
                else -> "大于50MB"
            }
            categories.getOrPut(sizeCategory) { mutableListOf() }.add(fileInfo)
        }

        categories.map { (category, files) ->
            FileCategory(
                category = category,
                files = files.sortedByDescending { it.size },
                totalSize = files.sumOf { it.size },
                fileCount = files.size
            )
        }.sortedBy { getSizeOrder(it.category) }
    }

    /**
     * 按日期分类文件
     */
    suspend fun categorizeFilesByDate(directory: File): List<FileCategory> = withContext(Dispatchers.IO) {
        val allFiles = scanDirectory(directory)
        val categories = mutableMapOf<String, MutableList<FileInfo>>()

        val calendar = Calendar.getInstance()
        val currentTime = System.currentTimeMillis()

        for (fileInfo in allFiles) {
            calendar.timeInMillis = fileInfo.lastModified
            val dateCategory = when {
                currentTime - fileInfo.lastModified < 24 * 60 * 60 * 1000 -> "今天"
                currentTime - fileInfo.lastModified < 7 * 24 * 60 * 60 * 1000 -> "本周"
                currentTime - fileInfo.lastModified < 30 * 24 * 60 * 60 * 1000 -> "本月"
                else -> "更早"
            }
            categories.getOrPut(dateCategory) { mutableListOf() }.add(fileInfo)
        }

        categories.map { (category, files) ->
            FileCategory(
                category = category,
                files = files.sortedByDescending { it.lastModified },
                totalSize = files.sumOf { it.size },
                fileCount = files.size
            )
        }.sortedBy { getDateOrder(it.category) }
    }

    /**
     * 获取文件大小顺序
     */
    private fun getSizeOrder(category: String): Int {
        return when (category) {
            "小于1MB" -> 0
            "1-10MB" -> 1
            "10-50MB" -> 2
            "大于50MB" -> 3
            else -> 4
        }
    }

    /**
     * 获取日期顺序
     */
    private fun getDateOrder(category: String): Int {
        return when (category) {
            "今天" -> 0
            "本周" -> 1
            "本月" -> 2
            "更早" -> 3
            else -> 4
        }
    }

    /**
     * 使用浏览器打开文件
     */
    fun openFileWithBrowser(file: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(file))
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 批量删除文件
     */
    suspend fun deleteFilesBatch(files: List<File>): BatchOperationResult = withContext(Dispatchers.IO) {
        var successCount = 0
        var failureCount = 0
        val errors = mutableListOf<String>()
        var totalSize = 0L

        for (file in files) {
            try {
                if (file.exists()) {
                    val size = if (file.isFile) file.length() else getDirectorySize(file)
                    if (file.deleteRecursively()) {
                        successCount++
                        totalSize += size
                    } else {
                        failureCount++
                        errors.add("删除失败: ${file.name}")
                    }
                } else {
                    failureCount++
                    errors.add("文件不存在: ${file.name}")
                }
            } catch (e: Exception) {
                failureCount++
                errors.add("删除异常: ${file.name} - ${e.message}")
            }
        }

        // 发送清理完成通知
        if (successCount > 0) {
            notificationHelper.showNotification(
                "文件清理完成",
                "已删除 $successCount 个文件，释放 ${formatFileSize(totalSize)} 空间",
                NotificationType.FILE_CLEANUP_COMPLETED
            )
        }

        BatchOperationResult(successCount, failureCount, totalSize, errors)
    }

    /**
     * 批量移动文件
     */
    suspend fun moveFilesBatch(files: List<File>, destination: File): BatchOperationResult = withContext(Dispatchers.IO) {
        var successCount = 0
        var failureCount = 0
        val errors = mutableListOf<String>()
        var totalSize = 0L

        if (!destination.exists()) {
            destination.mkdirs()
        }

        for (file in files) {
            try {
                val destFile = File(destination, file.name)
                if (file.renameTo(destFile)) {
                    successCount++
                    totalSize += file.length()
                } else {
                    failureCount++
                    errors.add("移动失败: ${file.name}")
                }
            } catch (e: Exception) {
                failureCount++
                errors.add("移动异常: ${file.name} - ${e.message}")
            }
        }

        BatchOperationResult(successCount, failureCount, totalSize, errors)
    }

    /**
     * 批量复制文件
     */
    suspend fun copyFilesBatch(files: List<File>, destination: File): BatchOperationResult = withContext(Dispatchers.IO) {
        var successCount = 0
        var failureCount = 0
        val errors = mutableListOf<String>()
        var totalSize = 0L

        if (!destination.exists()) {
            destination.mkdirs()
        }

        for (file in files) {
            try {
                val destFile = File(destination, file.name)
                if (copyFile(file, destFile)) {
                    successCount++
                    totalSize += file.length()
                } else {
                    failureCount++
                    errors.add("复制失败: ${file.name}")
                }
            } catch (e: Exception) {
                failureCount++
                errors.add("复制异常: ${file.name} - ${e.message}")
            }
        }

        BatchOperationResult(successCount, failureCount, totalSize, errors)
    }

    /**
     * 复制单个文件
     */
    private fun copyFile(source: File, destination: File): Boolean {
        return try {
            FileInputStream(source).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取目录大小
     */
    private fun getDirectorySize(directory: File): Long {
        if (!directory.exists() || !directory.isDirectory) return 0L

        var size = 0L
        val files = directory.listFiles() ?: return 0L

        for (file in files) {
            size += if (file.isDirectory) {
                getDirectorySize(file)
            } else {
                file.length()
            }
        }

        return size
    }

    /**
     * 搜索文件
     */
    suspend fun searchFiles(directory: File, query: String, searchInSubdirs: Boolean = true): List<FileInfo> = withContext(Dispatchers.IO) {
        val allFiles = scanDirectory(directory, searchInSubdirs)
        allFiles.filter { fileInfo ->
            fileInfo.name.contains(query, ignoreCase = true) ||
            fileInfo.path.contains(query, ignoreCase = true)
        }
    }

    /**
     * 获取文件详细信息
     */
    suspend fun getFileDetails(file: File): Map<String, Any>? = withContext(Dispatchers.IO) {
        try {
            val attributes = Files.readAttributes(Path(file.absolutePath), BasicFileAttributes::class.java)

            mapOf(
                "name" to file.name,
                "path" to file.absolutePath,
                "size" to file.length(),
                "formattedSize" to formatFileSize(file.length()),
                "lastModified" to file.lastModified(),
                "formattedDate" to formatFileDate(file.lastModified()),
                "isDirectory" to file.isDirectory,
                "canRead" to file.canRead(),
                "canWrite" to file.canWrite(),
                "isHidden" to file.isHidden,
                "creationTime" to attributes.creationTime().toMillis(),
                "lastAccessTime" to attributes.lastAccessTime().toMillis(),
                "extension" to getFileExtension(file),
                "mimeType" to getMimeType(file),
                "category" to getFileCategory(file)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 创建文件信息对象
     */
    private fun createFileInfo(file: File): FileInfo {
        return FileInfo(
            file = file,
            name = file.name,
            path = file.absolutePath,
            size = if (file.isFile) file.length() else getDirectorySize(file),
            lastModified = file.lastModified(),
            isDirectory = file.isDirectory,
            extension = getFileExtension(file),
            mimeType = getMimeType(file),
            category = getFileCategory(file)
        )
    }

    /**
     * 获取文件扩展名
     */
    private fun getFileExtension(file: File): String {
        val name = file.name
        val lastDotIndex = name.lastIndexOf('.')
        return if (lastDotIndex > 0 && lastDotIndex < name.length - 1) {
            name.substring(lastDotIndex + 1).lowercase()
        } else {
            ""
        }
    }

    /**
     * 获取MIME类型
     */
    private fun getMimeType(file: File): String {
        val extension = getFileExtension(file)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    /**
     * 获取文件分类
     */
    private fun getFileCategory(file: File): String {
        if (file.isDirectory) return "文件夹"

        val mimeType = getMimeType(file)
        return when {
            mimeType.startsWith("image/") -> "图片"
            mimeType.startsWith("video/") -> "视频"
            mimeType.startsWith("audio/") -> "音频"
            mimeType.startsWith("text/") || mimeType.contains("document") -> "文档"
            mimeType.contains("pdf") -> "PDF"
            mimeType.contains("zip") || mimeType.contains("rar") -> "压缩文件"
            mimeType.startsWith("application/") -> "应用程序"
            else -> "其他"
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
     * 格式化文件日期
     */
    private fun formatFileDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
