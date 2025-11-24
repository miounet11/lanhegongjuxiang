package lanhe.filesystem

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 本地文件处理器
 * 处理标准的file://协议文件
 */
class LocalFileHandler(
    private val context: Context
) : UniFileFactory {

    override fun createFile(uri: Uri): UniFile {
        return LocalUniFile(context, uri)
    }
}

/**
 * 本地文件实现
 */
private class LocalUniFile(
    private val context: Context,
    override val uri: Uri
) : UniFile {

    private val file: File by lazy {
        when (uri.scheme) {
            "file" -> File(uri.path ?: throw IllegalArgumentException("Invalid file URI"))
            else -> File(uri.toString().substringAfter("://"))
        }
    }

    override val path: String get() = file.absolutePath
    override val name: String get() = file.name
    override val isDirectory: Boolean get() = file.isDirectory
    override val isFile: Boolean get() = file.isFile
    override val size: Long get() = file.length()
    override val lastModified: Long get() = file.lastModified()
    override val mimeType: String? get() = getMimeType(file)
    override val canRead: Boolean get() = file.canRead()
    override val canWrite: Boolean get() = file.canWrite()

    override val parent: UniFile? by lazy {
        file.parentFile?.let { LocalUniFile(context, Uri.fromFile(it)) }
    }

    override val extension: String? get() = file.extension.ifEmpty { null }

    override val absolutePath: String get() = file.absolutePath

    override suspend fun listFiles(): List<UniFile> = withContext {
        file.listFiles()?.map { LocalUniFile(context, Uri.fromFile(it)) }?.sortedBy { it.name.lowercase() }
        ?: emptyList()
    }

    override suspend fun listFiles(filter: (UniFile) -> Boolean): List<UniFile> {
        return listFiles().filter(filter)
    }

    override suspend fun createDirectory(name: String): UniFile = withContext {
        val newDir = File(file, name)
        if (!newDir.exists()) {
            newDir.mkdirs()
        }
        LocalUniFile(context, Uri.fromFile(newDir))
    }

    override suspend fun createFile(name: String, mimeType: String?): UniFile = withContext {
        val newFile = File(file, name)
        if (!newFile.exists()) {
            newFile.createNewFile()
        }
        LocalUniFile(context, Uri.fromFile(newFile))
    }

    override suspend fun delete(): Boolean = withContext {
        if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }

    override suspend fun copyTo(destination: UniFile): Boolean = withContext {
        try {
            file.copyTo(File(destination.path))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun moveTo(destination: UniFile): Boolean = withContext {
        try {
            file.renameTo(File(destination.path))
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun renameTo(newName: String): UniFile = withContext {
        val newFile = File(file.parent, newName)
        if (file.renameTo(newFile)) {
            LocalUniFile(context, Uri.fromFile(newFile))
        } else {
            throw RuntimeException("Failed to rename file")
        }
    }

    override suspend fun getInputStream(): InputStream = withContext {
        FileInputStream(file)
    }

    override suspend fun getOutputStream(): OutputStream = withContext {
        FileOutputStream(file)
    }

    override suspend fun exists(): Boolean = withContext {
        file.exists()
    }

    override suspend fun getFileInfo(): FileInfo = withContext {
        FileInfo(
            uri = uri,
            name = name,
            path = path,
            size = size,
            lastModified = lastModified,
            mimeType = mimeType,
            isDirectory = isDirectory,
            permissions = FilePermissions(
                canRead = file.canRead(),
                canWrite = file.canWrite(),
                canExecute = file.canExecute()
            )
        )
    }

    override fun watch(): Flow<FileChangeEvent> = channelFlow {
        // 这里可以实现文件系统监控
        // 由于Android文件系统限制，简化实现
        trySend(FileChangeEvent.Modified(this@LocalUniFile))
        close()
    }

    /**
     * 获取文件MIME类型
     */
    private fun getMimeType(file: File): String? {
        val name = file.name.lowercase()
        return when {
            name.endsWith(".jpg") || name.endsWith(".jpeg") -> "image/jpeg"
            name.endsWith(".png") -> "image/png"
            name.endsWith(".gif") -> "image/gif"
            name.endsWith(".webp") -> "image/webp"
            name.endsWith(".mp4") -> "video/mp4"
            name.endsWith(".mkv") -> "video/x-matroska"
            name.endsWith(".avi") -> "video/x-msvideo"
            name.endsWith(".mp3") -> "audio/mpeg"
            name.endsWith(".aac") -> "audio/aac"
            name.endsWith(".flac") -> "audio/flac"
            name.endsWith(".txt") -> "text/plain"
            name.endsWith(".json") -> "application/json"
            name.endsWith(".xml") -> "text/xml"
            name.endsWith(".apk") -> "application/vnd.android.package-archive"
            name.endsWith(".pdf") -> "application/pdf"
            name.endsWith(".zip") -> "application/zip"
            name.endsWith(".rar") -> "application/x-rar-compressed"
            name.endsWith(".7z") -> "application/x-7z-compressed"
            else -> null
        }
    }

    /**
     * 递归删除目录
     */
    private fun File.deleteRecursively(): Boolean {
        if (!exists()) return true

        return if (isDirectory) {
            listFiles()?.forEach { child ->
                if (child.isDirectory) {
                    child.deleteRecursively()
                } else {
                    child.delete()
                }
            }
            delete()
        } else {
            delete()
        }
    }

    /**
     * 复制文件
     */
    private fun File.copyTo(target: File) {
        inputStream().use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private suspend fun <T> withContext(block: suspend CoroutineScope.() -> T): T {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { block() }
    }
}