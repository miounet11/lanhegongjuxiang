package lanhe.filesystem

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 蓝河助手文件管理器
 * 统一管理所有文件操作，支持多种存储方式
 */
class LanheFileManager private constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 文件处理器映射
    private val fileHandlers = ConcurrentHashMap<String, UniFileFactory>()

    // 文件索引和搜索
    private val fileIndexer: FileIndexer = FileIndexer(this)

    // 权限管理器
    private val permissionManager: PermissionManager = PermissionManager(context)

    init {
        registerFileHandlers()
    }

    companion object {
        @Volatile
        private var INSTANCE: LanheFileManager? = null

        fun getInstance(context: Context): LanheFileManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LanheFileManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * 注册文件处理器
     */
    private fun registerFileHandlers() {
        fileHandlers["file"] = LocalFileHandler(context)
        fileHandlers["content"] = SAFFileHandler(context)
        fileHandlers["media"] = MediaStoreFileHandler(context)
        fileHandlers["storage"] = ExternalStorageFileHandler(context)
    }

    /**
     * 获取文件对象
     */
    fun getFile(path: String): UniFile {
        val uri = if (path.startsWith("content://") || path.startsWith("file://")) {
            Uri.parse(path)
        } else {
            Uri.fromFile(File(path))
        }
        return getFile(uri)
    }

    /**
     * 获取文件对象
     */
    fun getFile(uri: Uri): UniFile {
        val scheme = uri.scheme ?: "file"
        val handler = fileHandlers[scheme]
            ?: throw IllegalArgumentException("Unsupported URI scheme: $scheme")
        return handler.createFile(uri)
    }

    /**
     * 列出目录文件
     */
    suspend fun listFiles(path: String): List<UniFile> = withContext(Dispatchers.IO) {
        val file = getFile(path)
        if (!file.isDirectory) {
            emptyList()
        } else {
            file.listFiles().sortedBy { it.name.lowercase() }
        }
    }

    /**
     * 搜索文件
     */
    suspend fun searchFiles(options: FileSearchOptions): List<UniFile> = withContext(Dispatchers.IO) {
        fileIndexer.search(options)
    }

    /**
     * 简单搜索
     */
    suspend fun searchFiles(query: String, path: String = "/"): List<UniFile> {
        val options = FileSearchOptions(
            query = query,
            searchPath = path,
            includeHidden = false,
            caseSensitive = false
        )
        return searchFiles(options)
    }

    /**
     * 创建目录
     */
    suspend fun createDirectory(parentPath: String, name: String): UniFile = withContext(Dispatchers.IO) {
        val parent = getFile(parentPath)
        if (!parent.isDirectory) {
            throw IllegalArgumentException("Parent path is not a directory: $parentPath")
        }
        parent.createDirectory(name)
    }

    /**
     * 复制文件
     */
    suspend fun copyFile(sourcePath: String, destinationPath: String): Boolean = withContext(Dispatchers.IO) {
        val source = getFile(sourcePath)
        val destination = getFile(destinationPath)

        try {
            if (source.isDirectory) {
                copyDirectory(source, destination)
            } else {
                source.copyTo(destination)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 移动文件
     */
    suspend fun moveFile(sourcePath: String, destinationPath: String): Boolean = withContext(Dispatchers.IO) {
        val source = getFile(sourcePath)
        val destination = getFile(destinationPath)

        try {
            source.moveTo(destination)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 删除文件
     */
    suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = getFile(path)

        try {
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 重命名文件
     */
    suspend fun renameFile(oldPath: String, newName: String): UniFile? = withContext(Dispatchers.IO) {
        val file = getFile(oldPath)

        try {
            file.renameTo(newName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取文件信息
     */
    suspend fun getFileInfo(path: String): FileInfo? = withContext(Dispatchers.IO) {
        val file = getFile(path)

        try {
            file.getFileInfo()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取存储空间信息
     */
    suspend fun getStorageInfo(path: String): StorageInfo = withContext(Dispatchers.IO) {
        when {
            path.startsWith("/storage/emulated/") -> getInternalStorageInfo()
            path.startsWith("/storage/") -> getExternalStorageInfo(path)
            else -> StorageInfo("", 0, 0, 0)
        }
    }

    /**
     * 检查权限
     */
    fun hasRequiredPermissions(): Boolean {
        return permissionManager.hasStoragePermissions()
    }

    /**
     * 请求权限
     */
    fun requestPermissions(onResult: (Boolean) -> Unit) {
        permissionManager.requestStoragePermissions(onResult)
    }

    /**
     * 复制目录（递归）
     */
    private suspend fun copyDirectory(source: UniFile, destination: UniFile) = withContext(Dispatchers.IO) {
        if (!destination.exists()) {
            destination.createDirectory(destination.name)
        }

        source.listFiles().forEach { sourceFile ->
            val destFile = getFile(Uri.parse("${destination.uri}/${sourceFile.name}"))

            if (sourceFile.isDirectory) {
                copyDirectory(sourceFile, destFile)
            } else {
                sourceFile.copyTo(destFile)
            }
        }
    }

    /**
     * 获取内部存储信息
     */
    private fun getInternalStorageInfo(): StorageInfo {
        val internalDir = context.filesDir
        val totalSpace = internalDir.totalSpace
        val freeSpace = internalDir.freeSpace
        val usedSpace = totalSpace - freeSpace

        return StorageInfo(
            path = internalDir.absolutePath,
            totalSpace = totalSpace,
            freeSpace = freeSpace,
            usedSpace = usedSpace
        )
    }

    /**
     * 获取外部存储信息
     */
    private fun getExternalStorageInfo(path: String): StorageInfo {
        val file = File(path)
        val totalSpace = file.totalSpace
        val freeSpace = file.freeSpace
        val usedSpace = totalSpace - freeSpace

        return StorageInfo(
            path = path,
            totalSpace = totalSpace,
            freeSpace = freeSpace,
            usedSpace = usedSpace
        )
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        scope.cancel()
    }
}

/**
 * 存储空间信息
 */
data class StorageInfo(
    val path: String,
    val totalSpace: Long,
    val freeSpace: Long,
    val usedSpace: Long
) {
    val totalSpaceGB: Double get() = totalSpace / (1024.0 * 1024.0 * 1024.0)
    val freeSpaceGB: Double get() = freeSpace / (1024.0 * 1024.0 * 1024.0)
    val usedSpaceGB: Double get() = usedSpace / (1024.0 * 1024.0 * 1024.0)

    val usagePercentage: Double get() = if (totalSpace > 0) {
        (usedSpace.toDouble() / totalSpace.toDouble()) * 100
    } else 0.0
}