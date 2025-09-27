package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * 增强存储优化器
 * 提供重复文件查找、智能缓存清理、APK分析、大文件查找和存储速度测试
 */
class EnhancedStorageOptimizer(private val context: Context) {

    companion object {
        private const val TAG = "EnhancedStorageOptimizer"
        private const val LARGE_FILE_THRESHOLD = 100 * 1024 * 1024L // 100MB
        private const val DUPLICATE_FILE_MIN_SIZE = 1024 * 1024L // 1MB
        private const val CACHE_CLEANUP_DAYS = 7 // 7天前的缓存
        private const val LOG_CLEANUP_DAYS = 30 // 30天前的日志
    }

    private val packageManager = context.packageManager
    private val dataManager = DataManager(context)

    // 存储状态流
    private val _storageState = MutableStateFlow<StorageState>(StorageState())
    val storageState: StateFlow<StorageState> = _storageState.asStateFlow()

    // 文件扫描结果
    private val _scanResults = MutableStateFlow<StorageScanResults>(StorageScanResults())
    val scanResults: StateFlow<StorageScanResults> = _scanResults.asStateFlow()

    init {
        updateStorageState()
    }

    /**
     * 执行全面存储优化
     */
    suspend fun performFullStorageOptimization(): StorageOptimizationResult {
        return withContext(Dispatchers.IO) {
            try {
                val results = mutableListOf<String>()
                var totalFreed = 0L

                // 1. 重复文件清理
                val duplicateCleanup = removeDuplicateFiles()
                results.addAll(duplicateCleanup.improvements)
                totalFreed += duplicateCleanup.freedSpace

                // 2. 智能缓存清理
                val cacheCleanup = performSmartCacheCleanup()
                results.addAll(cacheCleanup.improvements)
                totalFreed += cacheCleanup.freedSpace

                // 3. APK文件清理
                val apkCleanup = cleanupApkFiles()
                results.addAll(apkCleanup.improvements)
                totalFreed += apkCleanup.freedSpace

                // 4. 大文件分析
                val largeFileAnalysis = analyzeLargeFiles()
                results.addAll(largeFileAnalysis.improvements)

                // 5. 临时文件清理
                val tempCleanup = cleanupTempFiles()
                results.addAll(tempCleanup.improvements)
                totalFreed += tempCleanup.freedSpace

                // 6. 日志文件清理
                val logCleanup = cleanupLogFiles()
                results.addAll(logCleanup.improvements)
                totalFreed += logCleanup.freedSpace

                // 7. 空文件夹清理
                val emptyFolderCleanup = removeEmptyDirectories()
                results.addAll(emptyFolderCleanup.improvements)

                StorageOptimizationResult(
                    success = results.isNotEmpty(),
                    improvements = results,
                    freedSpace = totalFreed,
                    message = "存储优化完成，释放了${formatFileSize(totalFreed)}空间"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Storage optimization failed", e)
                StorageOptimizationResult(
                    success = false,
                    message = "存储优化失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 重复文件检测和清理
     */
    suspend fun findAndRemoveDuplicateFiles(): DuplicateFileResult {
        return withContext(Dispatchers.IO) {
            try {
                val duplicates = findDuplicateFiles()
                val totalSize = duplicates.sumOf { it.totalSize }
                val removedSize = removeDuplicates(duplicates)

                DuplicateFileResult(
                    duplicateGroups = duplicates,
                    totalDuplicateSize = totalSize,
                    removedSize = removedSize,
                    message = "发现${duplicates.size}组重复文件，总大小${formatFileSize(totalSize)}"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Duplicate file detection failed", e)
                DuplicateFileResult(message = "重复文件检测失败: ${e.message}")
            }
        }
    }

    /**
     * 智能缓存清理
     */
    suspend fun performSmartCacheCleanup(): StorageCleanupResult {
        return withContext(Dispatchers.IO) {
            try {
                val improvements = mutableListOf<String>()
                var freedSpace = 0L

                // 1. 应用缓存清理
                val appCacheFreed = cleanupAppCaches()
                if (appCacheFreed > 0) {
                    improvements.add("清理应用缓存${formatFileSize(appCacheFreed)}")
                    freedSpace += appCacheFreed
                }

                // 2. 系统缓存清理
                val systemCacheFreed = cleanupSystemCache()
                if (systemCacheFreed > 0) {
                    improvements.add("清理系统缓存${formatFileSize(systemCacheFreed)}")
                    freedSpace += systemCacheFreed
                }

                // 3. 下载缓存清理
                val downloadCacheFreed = cleanupDownloadCache()
                if (downloadCacheFreed > 0) {
                    improvements.add("清理下载缓存${formatFileSize(downloadCacheFreed)}")
                    freedSpace += downloadCacheFreed
                }

                // 4. 浏览器缓存清理
                val browserCacheFreed = cleanupBrowserCache()
                if (browserCacheFreed > 0) {
                    improvements.add("清理浏览器缓存${formatFileSize(browserCacheFreed)}")
                    freedSpace += browserCacheFreed
                }

                StorageCleanupResult(
                    improvements = improvements,
                    freedSpace = freedSpace
                )

            } catch (e: Exception) {
                Log.e(TAG, "Smart cache cleanup failed", e)
                StorageCleanupResult(improvements = listOf("智能缓存清理失败: ${e.message}"))
            }
        }
    }

    /**
     * APK文件分析和清理
     */
    suspend fun analyzeAndCleanupApkFiles(): ApkAnalysisResult {
        return withContext(Dispatchers.IO) {
            try {
                val apkFiles = findApkFiles()
                val analysis = analyzeApkFiles(apkFiles)
                val cleanupResult = cleanupUnneededApks(analysis)

                ApkAnalysisResult(
                    totalApkFiles = apkFiles.size,
                    totalApkSize = apkFiles.sumOf { it.length() },
                    installedApks = analysis.installedApks,
                    uninstalledApks = analysis.uninstalledApks,
                    outdatedApks = analysis.outdatedApks,
                    cleanupResult = cleanupResult
                )

            } catch (e: Exception) {
                Log.e(TAG, "APK analysis failed", e)
                ApkAnalysisResult(cleanupResult = StorageCleanupResult(
                    improvements = listOf("APK分析失败: ${e.message}")
                ))
            }
        }
    }

    /**
     * 大文件查找和可视化
     */
    suspend fun findLargeFiles(minSize: Long = LARGE_FILE_THRESHOLD): LargeFileResult {
        return withContext(Dispatchers.IO) {
            try {
                val largeFiles = mutableListOf<LargeFileInfo>()
                val directories = listOf(
                    Environment.getExternalStorageDirectory(),
                    context.getExternalFilesDir(null),
                    context.cacheDir,
                    context.filesDir
                )

                directories.filterNotNull().forEach { dir ->
                    findLargeFilesInDirectory(dir, minSize, largeFiles)
                }

                // 按大小排序
                largeFiles.sortByDescending { it.size }

                LargeFileResult(
                    largeFiles = largeFiles,
                    totalSize = largeFiles.sumOf { it.size },
                    message = "发现${largeFiles.size}个大文件，总大小${formatFileSize(largeFiles.sumOf { it.size })}"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Large file search failed", e)
                LargeFileResult(message = "大文件搜索失败: ${e.message}")
            }
        }
    }

    /**
     * 存储速度测试
     */
    suspend fun performStorageSpeedTest(): StorageSpeedTest {
        return withContext(Dispatchers.IO) {
            try {
                val testFile = File(context.cacheDir, "storage_speed_test.tmp")
                val testSize = 10 * 1024 * 1024 // 10MB

                // 写入测试
                val writeStartTime = System.currentTimeMillis()
                val testData = ByteArray(testSize) { (it % 256).toByte() }
                testFile.writeBytes(testData)
                val writeEndTime = System.currentTimeMillis()
                val writeSpeed = testSize.toDouble() / ((writeEndTime - writeStartTime) / 1000.0) / (1024 * 1024) // MB/s

                // 读取测试
                val readStartTime = System.currentTimeMillis()
                testFile.readBytes()
                val readEndTime = System.currentTimeMillis()
                val readSpeed = testSize.toDouble() / ((readEndTime - readStartTime) / 1000.0) / (1024 * 1024) // MB/s

                // 随机访问测试
                val randomStartTime = System.currentTimeMillis()
                testFile.inputStream().use { input ->
                    val buffer = ByteArray(4096)
                    for (i in 0 until 100) {
                        input.skip((Math.random() * (testSize - 4096)).toLong())
                        input.read(buffer)
                    }
                }
                val randomEndTime = System.currentTimeMillis()
                val randomAccessTime = randomEndTime - randomStartTime

                // 清理测试文件
                testFile.delete()

                StorageSpeedTest(
                    writeSpeed = writeSpeed,
                    readSpeed = readSpeed,
                    randomAccessTime = randomAccessTime,
                    timestamp = System.currentTimeMillis()
                )

            } catch (e: Exception) {
                Log.e(TAG, "Storage speed test failed", e)
                StorageSpeedTest(
                    writeSpeed = 0.0,
                    readSpeed = 0.0,
                    randomAccessTime = -1L,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    /**
     * 获取详细存储信息
     */
    fun getDetailedStorageInfo(): DetailedStorageInfo {
        val internal = getStorageInfo(Environment.getDataDirectory())
        val external = getStorageInfo(Environment.getExternalStorageDirectory())

        return DetailedStorageInfo(
            internalStorage = internal,
            externalStorage = external,
            appDataSize = getAppDataSize(),
            cacheSize = getCacheSize(),
            downloadSize = getDownloadFolderSize(),
            picturesSize = getPicturesFolderSize(),
            videosSize = getVideosFolderSize(),
            musicSize = getMusicFolderSize(),
            documentsSize = getDocumentsFolderSize()
        )
    }

    // 私有实现方法

    private fun updateStorageState() {
        val internal = getStorageInfo(Environment.getDataDirectory())
        val external = getStorageInfo(Environment.getExternalStorageDirectory())

        _storageState.value = StorageState(
            internalTotal = internal.totalSpace,
            internalFree = internal.freeSpace,
            internalUsed = internal.usedSpace,
            externalTotal = external.totalSpace,
            externalFree = external.freeSpace,
            externalUsed = external.usedSpace,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun getStorageInfo(path: File): StorageInfo {
        return try {
            val stat = StatFs(path.absolutePath)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val freeBlocks = stat.availableBlocksLong

            val totalSpace = totalBlocks * blockSize
            val freeSpace = freeBlocks * blockSize
            val usedSpace = totalSpace - freeSpace

            StorageInfo(
                totalSpace = totalSpace,
                freeSpace = freeSpace,
                usedSpace = usedSpace,
                usagePercent = (usedSpace.toFloat() / totalSpace.toFloat() * 100).roundToInt()
            )
        } catch (e: Exception) {
            StorageInfo()
        }
    }

    // 重复文件检测
    private fun findDuplicateFiles(): List<DuplicateFileGroup> {
        val fileHashes = mutableMapOf<String, MutableList<File>>()
        val duplicateGroups = mutableListOf<DuplicateFileGroup>()

        val directories = listOf(
            Environment.getExternalStorageDirectory(),
            context.getExternalFilesDir(null)
        )

        directories.filterNotNull().forEach { dir ->
            scanDirectoryForDuplicates(dir, fileHashes)
        }

        fileHashes.filter { it.value.size > 1 }.forEach { (hash, files) ->
            val totalSize = files.sumOf { it.length() }
            if (totalSize >= DUPLICATE_FILE_MIN_SIZE) {
                duplicateGroups.add(DuplicateFileGroup(
                    hash = hash,
                    files = files,
                    totalSize = totalSize
                ))
            }
        }

        return duplicateGroups
    }

    private fun scanDirectoryForDuplicates(directory: File, fileHashes: MutableMap<String, MutableList<File>>) {
        try {
            directory.listFiles()?.forEach { file ->
                when {
                    file.isFile && file.length() >= DUPLICATE_FILE_MIN_SIZE -> {
                        val hash = calculateFileHash(file)
                        if (hash != null) {
                            fileHashes.getOrPut(hash) { mutableListOf() }.add(file)
                        }
                    }
                    file.isDirectory -> {
                        scanDirectoryForDuplicates(file, fileHashes)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to scan directory: ${directory.absolutePath}", e)
        }
    }

    private fun calculateFileHash(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun removeDuplicates(duplicateGroups: List<DuplicateFileGroup>): Long {
        var removedSize = 0L

        duplicateGroups.forEach { group ->
            // 保留第一个文件，删除其他重复文件
            group.files.drop(1).forEach { file ->
                try {
                    if (file.delete()) {
                        removedSize += file.length()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete duplicate file: ${file.absolutePath}", e)
                }
            }
        }

        return removedSize
    }

    // 缓存清理实现
    private fun cleanupAppCaches(): Long {
        var freedSpace = 0L
        val cacheDir = context.cacheDir

        try {
            cacheDir.listFiles()?.forEach { file ->
                if (isOldFile(file, CACHE_CLEANUP_DAYS)) {
                    val size = if (file.isDirectory) {
                        FileUtils.sizeOfDirectory(file)
                    } else {
                        file.length()
                    }

                    if (file.deleteRecursively()) {
                        freedSpace += size
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup app caches", e)
        }

        return freedSpace
    }

    private fun cleanupSystemCache(): Long {
        // 系统缓存清理需要root权限或特殊权限
        return 0L
    }

    private fun cleanupDownloadCache(): Long {
        var freedSpace = 0L
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        try {
            downloadDir?.listFiles()?.forEach { file ->
                if (file.name.contains("cache", ignoreCase = true) ||
                    file.name.contains("temp", ignoreCase = true)) {
                    val size = if (file.isDirectory) {
                        FileUtils.sizeOfDirectory(file)
                    } else {
                        file.length()
                    }

                    if (file.deleteRecursively()) {
                        freedSpace += size
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup download cache", e)
        }

        return freedSpace
    }

    private fun cleanupBrowserCache(): Long {
        // 浏览器缓存清理
        return 0L
    }

    // APK分析实现
    private fun findApkFiles(): List<File> {
        val apkFiles = mutableListOf<File>()
        val searchDirectories = listOf(
            Environment.getExternalStorageDirectory(),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        )

        searchDirectories.filterNotNull().forEach { dir ->
            findApkFilesInDirectory(dir, apkFiles)
        }

        return apkFiles
    }

    private fun findApkFilesInDirectory(directory: File, apkFiles: MutableList<File>) {
        try {
            directory.listFiles()?.forEach { file ->
                when {
                    file.isFile && file.name.endsWith(".apk", ignoreCase = true) -> {
                        apkFiles.add(file)
                    }
                    file.isDirectory -> {
                        findApkFilesInDirectory(file, apkFiles)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to scan directory for APKs: ${directory.absolutePath}", e)
        }
    }

    private fun analyzeApkFiles(apkFiles: List<File>): ApkAnalysis {
        val installedApks = mutableListOf<ApkFileInfo>()
        val uninstalledApks = mutableListOf<ApkFileInfo>()
        val outdatedApks = mutableListOf<ApkFileInfo>()

        apkFiles.forEach { apkFile ->
            val apkInfo = getApkInfo(apkFile)
            if (apkInfo != null) {
                try {
                    val installedPackage = packageManager.getPackageInfo(apkInfo.packageName, 0)
                    if (installedPackage.versionCode < apkInfo.versionCode) {
                        outdatedApks.add(apkInfo)
                    } else {
                        installedApks.add(apkInfo)
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    uninstalledApks.add(apkInfo)
                }
            }
        }

        return ApkAnalysis(
            installedApks = installedApks,
            uninstalledApks = uninstalledApks,
            outdatedApks = outdatedApks
        )
    }

    private fun getApkInfo(apkFile: File): ApkFileInfo? {
        return try {
            val packageInfo = packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            packageInfo?.let {
                ApkFileInfo(
                    file = apkFile,
                    packageName = it.packageName,
                    versionName = it.versionName ?: "",
                    versionCode = it.versionCode,
                    size = apkFile.length()
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanupUnneededApks(analysis: ApkAnalysis): StorageCleanupResult {
        val improvements = mutableListOf<String>()
        var freedSpace = 0L

        // 删除已安装但过时的APK
        analysis.installedApks.forEach { apkInfo ->
            try {
                if (apkInfo.file.delete()) {
                    freedSpace += apkInfo.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete APK: ${apkInfo.file.absolutePath}", e)
            }
        }

        if (analysis.installedApks.isNotEmpty()) {
            improvements.add("删除${analysis.installedApks.size}个已安装的APK文件")
        }

        return StorageCleanupResult(
            improvements = improvements,
            freedSpace = freedSpace
        )
    }

    // 大文件查找实现
    private fun findLargeFilesInDirectory(directory: File, minSize: Long, largeFiles: MutableList<LargeFileInfo>) {
        try {
            directory.listFiles()?.forEach { file ->
                when {
                    file.isFile && file.length() >= minSize -> {
                        largeFiles.add(LargeFileInfo(
                            file = file,
                            size = file.length(),
                            type = getFileType(file),
                            lastModified = file.lastModified()
                        ))
                    }
                    file.isDirectory -> {
                        findLargeFilesInDirectory(file, minSize, largeFiles)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to scan directory for large files: ${directory.absolutePath}", e)
        }
    }

    private fun getFileType(file: File): String {
        val extension = file.extension.lowercase()
        return when (extension) {
            "mp4", "avi", "mkv", "mov", "wmv" -> "视频"
            "mp3", "wav", "flac", "aac" -> "音频"
            "jpg", "jpeg", "png", "gif", "bmp" -> "图片"
            "pdf", "doc", "docx", "txt" -> "文档"
            "zip", "rar", "7z", "tar" -> "压缩包"
            "apk" -> "应用"
            else -> "其他"
        }
    }

    // 其他清理实现
    private fun cleanupTempFiles(): StorageCleanupResult {
        val improvements = mutableListOf<String>()
        var freedSpace = 0L

        val tempDirectories = listOf(
            File("/tmp"),
            File(System.getProperty("java.io.tmpdir") ?: "/tmp"),
            context.cacheDir
        )

        tempDirectories.forEach { tempDir ->
            if (tempDir.exists()) {
                try {
                    tempDir.listFiles()?.forEach { file ->
                        if (file.name.startsWith("tmp") || file.name.endsWith(".tmp")) {
                            val size = if (file.isDirectory) {
                                FileUtils.sizeOfDirectory(file)
                            } else {
                                file.length()
                            }

                            if (file.deleteRecursively()) {
                                freedSpace += size
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to cleanup temp directory: ${tempDir.absolutePath}", e)
                }
            }
        }

        if (freedSpace > 0) {
            improvements.add("清理临时文件${formatFileSize(freedSpace)}")
        }

        return StorageCleanupResult(
            improvements = improvements,
            freedSpace = freedSpace
        )
    }

    private fun cleanupLogFiles(): StorageCleanupResult {
        val improvements = mutableListOf<String>()
        var freedSpace = 0L

        val logDirectories = listOf(
            File("/sdcard/Android/data"),
            File(context.filesDir, "logs"),
            File(context.getExternalFilesDir(null), "logs")
        )

        logDirectories.filterNotNull().forEach { logDir ->
            if (logDir.exists()) {
                try {
                    logDir.walkTopDown().forEach { file ->
                        if (file.isFile && (file.name.endsWith(".log") || file.name.endsWith(".txt"))
                            && isOldFile(file, LOG_CLEANUP_DAYS)) {
                            val size = file.length()
                            if (file.delete()) {
                                freedSpace += size
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to cleanup log directory: ${logDir.absolutePath}", e)
                }
            }
        }

        if (freedSpace > 0) {
            improvements.add("清理日志文件${formatFileSize(freedSpace)}")
        }

        return StorageCleanupResult(
            improvements = improvements,
            freedSpace = freedSpace
        )
    }

    private fun removeEmptyDirectories(): StorageCleanupResult {
        val improvements = mutableListOf<String>()
        var removedDirs = 0

        val searchDirs = listOf(
            Environment.getExternalStorageDirectory(),
            context.getExternalFilesDir(null)
        )

        searchDirs.filterNotNull().forEach { dir ->
            removedDirs += removeEmptyDirectoriesRecursive(dir)
        }

        if (removedDirs > 0) {
            improvements.add("删除${removedDirs}个空文件夹")
        }

        return StorageCleanupResult(improvements = improvements)
    }

    private fun removeEmptyDirectoriesRecursive(directory: File): Int {
        var removedCount = 0

        try {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    removedCount += removeEmptyDirectoriesRecursive(file)
                    if (file.listFiles()?.isEmpty() == true) {
                        if (file.delete()) {
                            removedCount++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to remove empty directories in: ${directory.absolutePath}", e)
        }

        return removedCount
    }

    // 辅助方法
    private fun isOldFile(file: File, days: Int): Boolean {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return file.lastModified() < cutoffTime
    }

    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return String.format("%.1f%s", size, units[unitIndex])
    }

    // 文件夹大小计算
    private fun getAppDataSize(): Long = try { FileUtils.sizeOfDirectory(context.filesDir) } catch (e: Exception) { 0L }
    private fun getCacheSize(): Long = try { FileUtils.sizeOfDirectory(context.cacheDir) } catch (e: Exception) { 0L }
    private fun getDownloadFolderSize(): Long = try { FileUtils.sizeOfDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)) } catch (e: Exception) { 0L }
    private fun getPicturesFolderSize(): Long = try { FileUtils.sizeOfDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)) } catch (e: Exception) { 0L }
    private fun getVideosFolderSize(): Long = try { FileUtils.sizeOfDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)) } catch (e: Exception) { 0L }
    private fun getMusicFolderSize(): Long = try { FileUtils.sizeOfDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)) } catch (e: Exception) { 0L }
    private fun getDocumentsFolderSize(): Long = try { FileUtils.sizeOfDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)) } catch (e: Exception) { 0L }

    // 快速清理方法实现
    private fun removeDuplicateFiles(): StorageCleanupResult {
        val duplicates = findDuplicateFiles()
        val removedSize = removeDuplicates(duplicates)

        return StorageCleanupResult(
            improvements = if (removedSize > 0) listOf("删除重复文件${formatFileSize(removedSize)}") else emptyList(),
            freedSpace = removedSize
        )
    }

    private fun cleanupApkFiles(): StorageCleanupResult {
        val apkFiles = findApkFiles()
        val analysis = analyzeApkFiles(apkFiles)
        return cleanupUnneededApks(analysis)
    }

    private fun analyzeLargeFiles(): OptimizationItem {
        val largeFiles = mutableListOf<LargeFileInfo>()
        val directories = listOf(
            Environment.getExternalStorageDirectory(),
            context.getExternalFilesDir(null)
        )

        directories.filterNotNull().forEach { dir ->
            findLargeFilesInDirectory(dir, LARGE_FILE_THRESHOLD, largeFiles)
        }

        val improvements = if (largeFiles.isNotEmpty()) {
            listOf("发现${largeFiles.size}个大文件，总大小${formatFileSize(largeFiles.sumOf { it.size })}")
        } else {
            listOf("未发现超大文件")
        }

        return OptimizationItem(
            name = "大文件分析",
            success = true,
            improvements = improvements
        )
    }
}

// 数据类定义

data class StorageState(
    val internalTotal: Long = 0L,
    val internalFree: Long = 0L,
    val internalUsed: Long = 0L,
    val externalTotal: Long = 0L,
    val externalFree: Long = 0L,
    val externalUsed: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

data class StorageOptimizationResult(
    val success: Boolean = false,
    val improvements: List<String> = emptyList(),
    val freedSpace: Long = 0L,
    val message: String = ""
)

data class StorageCleanupResult(
    val improvements: List<String> = emptyList(),
    val freedSpace: Long = 0L
)

data class StorageScanResults(
    val duplicateFiles: List<DuplicateFileGroup> = emptyList(),
    val largeFiles: List<LargeFileInfo> = emptyList(),
    val apkFiles: List<ApkFileInfo> = emptyList(),
    val totalScannedFiles: Int = 0,
    val scanDuration: Long = 0L
)

data class StorageInfo(
    val totalSpace: Long = 0L,
    val freeSpace: Long = 0L,
    val usedSpace: Long = 0L,
    val usagePercent: Int = 0
)

data class DuplicateFileGroup(
    val hash: String,
    val files: List<File>,
    val totalSize: Long
)

data class DuplicateFileResult(
    val duplicateGroups: List<DuplicateFileGroup> = emptyList(),
    val totalDuplicateSize: Long = 0L,
    val removedSize: Long = 0L,
    val message: String = ""
)

data class LargeFileInfo(
    val file: File,
    val size: Long,
    val type: String,
    val lastModified: Long
)

data class LargeFileResult(
    val largeFiles: List<LargeFileInfo> = emptyList(),
    val totalSize: Long = 0L,
    val message: String = ""
)

data class ApkFileInfo(
    val file: File,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val size: Long
)

data class ApkAnalysis(
    val installedApks: List<ApkFileInfo>,
    val uninstalledApks: List<ApkFileInfo>,
    val outdatedApks: List<ApkFileInfo>
)

data class ApkAnalysisResult(
    val totalApkFiles: Int = 0,
    val totalApkSize: Long = 0L,
    val installedApks: List<ApkFileInfo> = emptyList(),
    val uninstalledApks: List<ApkFileInfo> = emptyList(),
    val outdatedApks: List<ApkFileInfo> = emptyList(),
    val cleanupResult: StorageCleanupResult = StorageCleanupResult()
)

data class StorageSpeedTest(
    val writeSpeed: Double = 0.0,  // MB/s
    val readSpeed: Double = 0.0,   // MB/s
    val randomAccessTime: Long = 0L, // ms
    val timestamp: Long = System.currentTimeMillis()
)

data class DetailedStorageInfo(
    val internalStorage: StorageInfo,
    val externalStorage: StorageInfo,
    val appDataSize: Long,
    val cacheSize: Long,
    val downloadSize: Long,
    val picturesSize: Long,
    val videosSize: Long,
    val musicSize: Long,
    val documentsSize: Long
)