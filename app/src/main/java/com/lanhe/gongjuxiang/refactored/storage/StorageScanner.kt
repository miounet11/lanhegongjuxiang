package com.lanhe.gongjuxiang.refactored.storage

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import com.lanhe.gongjuxiang.refactored.interfaces.CleanableItem
import com.lanhe.gongjuxiang.refactored.interfaces.CleanableType

/**
 * 存储扫描器 - 负责文件扫描
 * 包括重复文件、大文件、缓存文件扫描
 */
class StorageScanner(private val context: Context) {

    companion object {
        private const val TAG = "StorageScanner"
        private const val LARGE_FILE_THRESHOLD = 100 * 1024 * 1024L // 100MB
        private const val DUPLICATE_FILE_MIN_SIZE = 1024 * 1024L // 1MB
        private const val CACHE_SCAN_DEPTH = 5 // 缓存扫描深度
    }

    // 扫描状态
    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    /**
     * 扫描重复文件
     * @param directories 要扫描的目录列表
     * @return 重复文件映射（文件哈希 -> 文件列表）
     */
    suspend fun scanDuplicateFiles(
        directories: List<File> = getDefaultScanDirectories()
    ): Map<String, List<File>> = withContext(Dispatchers.IO) {
        _isScanning.value = true
        _scanProgress.value = 0f

        val fileHashMap = mutableMapOf<String, MutableList<File>>()
        val allFiles = mutableListOf<File>()

        try {
            // 收集所有文件
            directories.forEach { dir ->
                if (dir.exists() && dir.canRead()) {
                    collectFiles(dir, allFiles, DUPLICATE_FILE_MIN_SIZE)
                }
            }

            // 计算文件哈希
            allFiles.forEachIndexed { index, file ->
                try {
                    if (file.exists() && file.canRead() && file.length() >= DUPLICATE_FILE_MIN_SIZE) {
                        val hash = calculateFileHash(file)
                        fileHashMap.getOrPut(hash) { mutableListOf() }.add(file)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error hashing file: ${file.absolutePath}", e)
                }
                _scanProgress.value = (index + 1).toFloat() / allFiles.size
            }

            // 过滤出重复文件（多于1个的哈希组）
            fileHashMap.filter { it.value.size > 1 }
        } finally {
            _isScanning.value = false
            _scanProgress.value = 1f
        }
    }

    /**
     * 扫描大文件
     * @param directories 要扫描的目录列表
     * @param threshold 大文件阈值（字节）
     * @return 大文件列表
     */
    suspend fun scanLargeFiles(
        directories: List<File> = getDefaultScanDirectories(),
        threshold: Long = LARGE_FILE_THRESHOLD
    ): List<CleanableItem> = withContext(Dispatchers.IO) {
        _isScanning.value = true
        _scanProgress.value = 0f

        val largeFiles = mutableListOf<CleanableItem>()

        try {
            directories.forEachIndexed { dirIndex, dir ->
                if (dir.exists() && dir.canRead()) {
                    scanLargeFilesRecursive(dir, threshold, largeFiles)
                }
                _scanProgress.value = (dirIndex + 1).toFloat() / directories.size
            }

            // 按文件大小降序排序
            largeFiles.sortByDescending { it.size }
        } finally {
            _isScanning.value = false
            _scanProgress.value = 1f
        }

        largeFiles
    }

    /**
     * 扫描缓存文件
     * @return 缓存文件列表
     */
    suspend fun scanCacheFiles(): List<CleanableItem> = withContext(Dispatchers.IO) {
        _isScanning.value = true
        _scanProgress.value = 0f

        val cacheFiles = mutableListOf<CleanableItem>()

        try {
            // 扫描应用内部缓存
            scanAppInternalCache(cacheFiles)
            _scanProgress.value = 0.25f

            // 扫描应用外部缓存
            scanAppExternalCache(cacheFiles)
            _scanProgress.value = 0.5f

            // 扫描系统缓存目录
            scanSystemCacheDirectories(cacheFiles)
            _scanProgress.value = 0.75f

            // 扫描临时文件
            scanTempFiles(cacheFiles)
            _scanProgress.value = 1f

        } finally {
            _isScanning.value = false
        }

        cacheFiles
    }

    /**
     * 扫描APK文件
     * @return APK文件列表
     */
    suspend fun scanApkFiles(): List<CleanableItem> = withContext(Dispatchers.IO) {
        _isScanning.value = true
        _scanProgress.value = 0f

        val apkFiles = mutableListOf<CleanableItem>()

        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir.exists() && downloadDir.canRead()) {
                scanApkFilesRecursive(downloadDir, apkFiles)
            }

            // 扫描其他常见APK存放位置
            val sdCard = Environment.getExternalStorageDirectory()
            val apkDirs = listOf(
                File(sdCard, "Download"),
                File(sdCard, "APKs"),
                File(sdCard, "backups")
            )

            apkDirs.forEach { dir ->
                if (dir.exists() && dir.canRead()) {
                    scanApkFilesRecursive(dir, apkFiles)
                }
            }
        } finally {
            _isScanning.value = false
            _scanProgress.value = 1f
        }

        apkFiles
    }

    /**
     * 扫描空文件夹
     * @return 空文件夹列表
     */
    suspend fun scanEmptyFolders(): List<CleanableItem> = withContext(Dispatchers.IO) {
        _isScanning.value = true
        _scanProgress.value = 0f

        val emptyFolders = mutableListOf<CleanableItem>()

        try {
            val sdCard = Environment.getExternalStorageDirectory()
            if (sdCard.exists() && sdCard.canRead()) {
                scanEmptyFoldersRecursive(sdCard, emptyFolders)
            }
        } finally {
            _isScanning.value = false
            _scanProgress.value = 1f
        }

        emptyFolders
    }

    // ========== 私有辅助方法 ==========

    private fun getDefaultScanDirectories(): List<File> {
        val dirs = mutableListOf<File>()

        // 添加外部存储目录
        val sdCard = Environment.getExternalStorageDirectory()
        if (sdCard.exists() && sdCard.canRead()) {
            dirs.add(sdCard)
        }

        // 添加公共目录
        listOf(
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_PICTURES,
            Environment.DIRECTORY_MUSIC,
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_DOCUMENTS
        ).forEach { dirType ->
            Environment.getExternalStoragePublicDirectory(dirType)?.let {
                if (it.exists() && it.canRead()) {
                    dirs.add(it)
                }
            }
        }

        return dirs
    }

    private fun collectFiles(dir: File, allFiles: MutableList<File>, minSize: Long) {
        try {
            dir.listFiles()?.forEach { file ->
                when {
                    file.isFile && file.length() >= minSize -> allFiles.add(file)
                    file.isDirectory && !file.name.startsWith(".") -> collectFiles(file, allFiles, minSize)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error collecting files from: ${dir.absolutePath}", e)
        }
    }

    private fun calculateFileHash(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun scanLargeFilesRecursive(dir: File, threshold: Long, largeFiles: MutableList<CleanableItem>) {
        try {
            dir.listFiles()?.forEach { file ->
                when {
                    file.isFile && file.length() >= threshold -> {
                        largeFiles.add(
                            CleanableItem(
                                name = file.name,
                                path = file.absolutePath,
                                size = file.length(),
                                type = CleanableType.LARGE_FILE,
                                canDelete = true
                            )
                        )
                    }
                    file.isDirectory && !file.name.startsWith(".") -> {
                        scanLargeFilesRecursive(file, threshold, largeFiles)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning directory: ${dir.absolutePath}", e)
        }
    }

    private fun scanAppInternalCache(cacheFiles: MutableList<CleanableItem>) {
        context.cacheDir?.let { cacheDir ->
            if (cacheDir.exists()) {
                scanCacheDirectory(cacheDir, cacheFiles)
            }
        }
    }

    private fun scanAppExternalCache(cacheFiles: MutableList<CleanableItem>) {
        context.externalCacheDir?.let { externalCacheDir ->
            if (externalCacheDir.exists()) {
                scanCacheDirectory(externalCacheDir, cacheFiles)
            }
        }
    }

    private fun scanSystemCacheDirectories(cacheFiles: MutableList<CleanableItem>) {
        val sdCard = Environment.getExternalStorageDirectory()
        val cacheDirs = listOf(
            File(sdCard, ".cache"),
            File(sdCard, "Android/data"),
            File(sdCard, "DCIM/.thumbnails")
        )

        cacheDirs.forEach { dir ->
            if (dir.exists() && dir.canRead()) {
                scanCacheDirectory(dir, cacheFiles, depth = 2)
            }
        }
    }

    private fun scanTempFiles(cacheFiles: MutableList<CleanableItem>) {
        val tempPatterns = listOf(".tmp", ".temp", "~", ".bak", ".old")
        val sdCard = Environment.getExternalStorageDirectory()

        scanForPatterns(sdCard, tempPatterns) { file ->
            cacheFiles.add(
                CleanableItem(
                    name = file.name,
                    path = file.absolutePath,
                    size = file.length(),
                    type = CleanableType.TEMP_FILE,
                    canDelete = true
                )
            )
        }
    }

    private fun scanCacheDirectory(dir: File, cacheFiles: MutableList<CleanableItem>, depth: Int = CACHE_SCAN_DEPTH) {
        if (depth <= 0) return

        try {
            dir.listFiles()?.forEach { file ->
                when {
                    file.isFile -> {
                        cacheFiles.add(
                            CleanableItem(
                                name = file.name,
                                path = file.absolutePath,
                                size = file.length(),
                                type = CleanableType.CACHE,
                                canDelete = true
                            )
                        )
                    }
                    file.isDirectory && file.name.toLowerCase().contains("cache") -> {
                        scanCacheDirectory(file, cacheFiles, depth - 1)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning cache directory: ${dir.absolutePath}", e)
        }
    }

    private fun scanApkFilesRecursive(dir: File, apkFiles: MutableList<CleanableItem>) {
        try {
            dir.listFiles()?.forEach { file ->
                when {
                    file.isFile && file.name.endsWith(".apk", ignoreCase = true) -> {
                        apkFiles.add(
                            CleanableItem(
                                name = file.name,
                                path = file.absolutePath,
                                size = file.length(),
                                type = CleanableType.APK_FILE,
                                canDelete = isApkSafeToDelete(file)
                            )
                        )
                    }
                    file.isDirectory && !file.name.startsWith(".") -> {
                        scanApkFilesRecursive(file, apkFiles)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning APK files: ${dir.absolutePath}", e)
        }
    }

    private fun isApkSafeToDelete(apkFile: File): Boolean {
        // 检查APK是否已安装，如果已安装则可以安全删除
        try {
            val packageInfo = context.packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                0
            )
            if (packageInfo != null) {
                val packageName = packageInfo.packageName
                return try {
                    context.packageManager.getPackageInfo(packageName, 0)
                    true // 已安装，可以删除
                } catch (e: Exception) {
                    false // 未安装，保留
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking APK: ${apkFile.absolutePath}", e)
        }
        return false
    }

    private fun scanEmptyFoldersRecursive(dir: File, emptyFolders: MutableList<CleanableItem>) {
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory && !file.name.startsWith(".")) {
                    val children = file.listFiles()
                    if (children == null || children.isEmpty()) {
                        emptyFolders.add(
                            CleanableItem(
                                name = file.name,
                                path = file.absolutePath,
                                size = 0L,
                                type = CleanableType.EMPTY_FOLDER,
                                canDelete = true
                            )
                        )
                    } else {
                        scanEmptyFoldersRecursive(file, emptyFolders)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning empty folders: ${dir.absolutePath}", e)
        }
    }

    private fun scanForPatterns(dir: File, patterns: List<String>, onFileFound: (File) -> Unit) {
        try {
            dir.listFiles()?.forEach { file ->
                when {
                    file.isFile && patterns.any { pattern -> file.name.endsWith(pattern) } -> {
                        onFileFound(file)
                    }
                    file.isDirectory && !file.name.startsWith(".") -> {
                        scanForPatterns(file, patterns, onFileFound)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning for patterns: ${dir.absolutePath}", e)
        }
    }
}