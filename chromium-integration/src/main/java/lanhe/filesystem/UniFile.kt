package lanhe.filesystem

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream

/**
 * 统一文件访问接口
 * 支持本地文件、SAF、MediaStore等多种存储方式
 */
interface UniFile {
    val uri: Uri
    val path: String
    val name: String
    val isDirectory: Boolean
    val isFile: Boolean
    val size: Long
    val lastModified: Long
    val mimeType: String?
    val canRead: Boolean
    val canWrite: Boolean

    /**
     * 获取父目录
     */
    val parent: UniFile?

    /**
     * 获取文件扩展名
     */
    val extension: String?

    /**
     * 获取绝对路径（用于显示）
     */
    val absolutePath: String

    /**
     * 列出目录下的文件
     */
    suspend fun listFiles(): List<UniFile>

    /**
     * 列出目录下的文件（支持过滤）
     */
    suspend fun listFiles(filter: (UniFile) -> Boolean): List<UniFile>

    /**
     * 创建子目录
     */
    suspend fun createDirectory(name: String): UniFile

    /**
     * 创建文件
     */
    suspend fun createFile(name: String, mimeType: String? = null): UniFile

    /**
     * 删除文件或目录
     */
    suspend fun delete(): Boolean

    /**
     * 复制到目标位置
     */
    suspend fun copyTo(destination: UniFile): Boolean

    /**
     * 移动到目标位置
     */
    suspend fun moveTo(destination: UniFile): Boolean

    /**
     * 重命名
     */
    suspend fun renameTo(newName: String): UniFile

    /**
     * 获取输入流
     */
    suspend fun getInputStream(): InputStream

    /**
     * 获取输出流
     */
    suspend fun getOutputStream(): OutputStream

    /**
     * 检查文件是否存在
     */
    suspend fun exists(): Boolean

    /**
     * 获取文件详细信息
     */
    suspend fun getFileInfo(): FileInfo

    /**
     * 监听文件变化
     */
    fun watch(): Flow<FileChangeEvent>
}

/**
 * 文件信息
 */
data class FileInfo(
    val uri: Uri,
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?,
    val isDirectory: Boolean,
    val permissions: FilePermissions,
    val attributes: Map<String, Any> = emptyMap()
)

/**
 * 文件权限
 */
data class FilePermissions(
    val canRead: Boolean,
    val canWrite: Boolean,
    val canExecute: Boolean,
    val owner: String? = null,
    val group: String? = null
)

/**
 * 文件变化事件
 */
sealed class FileChangeEvent {
    data class Created(val file: UniFile) : FileChangeEvent()
    data class Modified(val file: UniFile) : FileChangeEvent()
    data class Deleted(val file: UniFile) : FileChangeEvent()
    data class Moved(val oldFile: UniFile, val newFile: UniFile) : FileChangeEvent()
}

/**
 * 文件操作结果
 */
sealed class FileOperationResult {
    object Success : FileOperationResult()
    data class Error(val exception: Throwable) : FileOperationResult()
    data class Progress(val bytesProcessed: Long, val totalBytes: Long) : FileOperationResult()
}

/**
 * 文件搜索选项
 */
data class FileSearchOptions(
    val query: String,
    val searchPath: String,
    val includeHidden: Boolean = false,
    val caseSensitive: Boolean = false,
    val fileTypes: Set<String> = emptySet(),
    val minSize: Long = 0,
    val maxSize: Long = Long.MAX_VALUE,
    val modifiedAfter: Long = 0,
    val modifiedBefore: Long = Long.MAX_VALUE
)