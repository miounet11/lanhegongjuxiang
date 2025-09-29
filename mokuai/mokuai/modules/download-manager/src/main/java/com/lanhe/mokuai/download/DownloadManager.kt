package com.lanhe.mokuai.download

import android.app.DownloadManager as SystemDownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * 下载管理器 - 多线程下载、断点续传、下载管理
 */
class DownloadManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "download_manager_prefs"
        private const val KEY_DOWNLOADS = "download_list"
        private const val KEY_SETTINGS = "download_settings"
        private const val KEY_HISTORY = "download_history"

        private const val DEFAULT_THREAD_COUNT = 4
        private const val BUFFER_SIZE = 8192
        private const val PROGRESS_UPDATE_INTERVAL = 500L // 500ms更新一次进度
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY = 2000L // 2秒重试间隔
    }

    private val sharedPrefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val systemDownloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as SystemDownloadManager
    }

    // 下载任务缓存
    private val downloadTasks = ConcurrentHashMap<String, DownloadTask>()
    private val downloadJobs = ConcurrentHashMap<String, Job>()

    // 下载状态流
    private val _downloadStates = ConcurrentHashMap<String, MutableStateFlow<DownloadState>>()

    data class DownloadTask(
        val id: String = UUID.randomUUID().toString(),
        val url: String,
        val fileName: String,
        val savePath: String,
        val fileSize: Long = 0,
        val downloadedSize: Long = 0,
        val status: DownloadStatus = DownloadStatus.PENDING,
        val progress: Float = 0f,
        val speed: Long = 0,
        val remainingTime: Long = 0,
        val threadCount: Int = DEFAULT_THREAD_COUNT,
        val resumable: Boolean = false,
        val headers: Map<String, String> = emptyMap(),
        val retryCount: Int = 0,
        val createTime: Long = System.currentTimeMillis(),
        val completeTime: Long = 0,
        val errorMessage: String? = null,
        val metadata: Map<String, String> = emptyMap()
    )

    enum class DownloadStatus {
        PENDING,        // 等待中
        CONNECTING,     // 连接中
        DOWNLOADING,    // 下载中
        PAUSED,        // 已暂停
        COMPLETED,     // 已完成
        FAILED,        // 失败
        CANCELLED      // 已取消
    }

    data class DownloadState(
        val task: DownloadTask,
        val progress: Float,
        val speed: Long,
        val remainingTime: Long
    )

    data class DownloadSettings(
        val maxConcurrentDownloads: Int = 3,
        val defaultThreadCount: Int = DEFAULT_THREAD_COUNT,
        val defaultSavePath: String = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        ).absolutePath,
        val wifiOnly: Boolean = false,
        val notificationEnabled: Boolean = true,
        val autoRetry: Boolean = true,
        val maxRetryCount: Int = MAX_RETRY_COUNT,
        val speedLimit: Long = 0, // 0表示不限速，单位：bytes/s
        val deleteFileOnCancel: Boolean = true
    )

    data class DownloadHistory(
        val id: String,
        val url: String,
        val fileName: String,
        val fileSize: Long,
        val savePath: String,
        val completeTime: Long,
        val duration: Long
    )

    data class ThreadInfo(
        val id: Int,
        val startPos: Long,
        val endPos: Long,
        val currentPos: Long,
        val completed: Boolean = false
    )

    /**
     * 创建下载任务
     */
    fun createDownload(
        url: String,
        fileName: String? = null,
        savePath: String? = null,
        headers: Map<String, String> = emptyMap(),
        threadCount: Int = DEFAULT_THREAD_COUNT,
        metadata: Map<String, String> = emptyMap()
    ): String {
        val settings = getSettings()
        val actualFileName = fileName ?: extractFileName(url)
        val actualSavePath = savePath ?: settings.defaultSavePath

        val task = DownloadTask(
            url = url,
            fileName = actualFileName,
            savePath = actualSavePath,
            threadCount = threadCount,
            headers = headers,
            metadata = metadata
        )

        downloadTasks[task.id] = task
        saveDownloadTasks()

        // 创建状态流
        _downloadStates[task.id] = MutableStateFlow(
            DownloadState(task, 0f, 0, 0)
        )

        return task.id
    }

    /**
     * 开始下载
     */
    fun startDownload(downloadId: String) {
        val task = downloadTasks[downloadId] ?: return

        if (task.status == DownloadStatus.DOWNLOADING) {
            return
        }

        val job = GlobalScope.launch(Dispatchers.IO) {
            try {
                updateTaskStatus(downloadId, DownloadStatus.CONNECTING)

                // 获取文件信息
                val fileInfo = getFileInfo(task.url, task.headers)
                val updatedTask = task.copy(
                    fileSize = fileInfo.first,
                    resumable = fileInfo.second
                )
                downloadTasks[downloadId] = updatedTask

                updateTaskStatus(downloadId, DownloadStatus.DOWNLOADING)

                if (updatedTask.resumable && updatedTask.threadCount > 1) {
                    // 多线程下载
                    downloadWithMultipleThreads(updatedTask)
                } else {
                    // 单线程下载
                    downloadWithSingleThread(updatedTask)
                }

            } catch (e: Exception) {
                handleDownloadError(downloadId, e)
            }
        }

        downloadJobs[downloadId] = job
    }

    /**
     * 暂停下载
     */
    fun pauseDownload(downloadId: String) {
        downloadJobs[downloadId]?.cancel()
        downloadJobs.remove(downloadId)
        updateTaskStatus(downloadId, DownloadStatus.PAUSED)
    }

    /**
     * 恢复下载
     */
    fun resumeDownload(downloadId: String) {
        val task = downloadTasks[downloadId] ?: return

        if (task.resumable) {
            startDownload(downloadId)
        } else {
            // 不支持断点续传，重新开始
            resetDownload(downloadId)
            startDownload(downloadId)
        }
    }

    /**
     * 取消下载
     */
    fun cancelDownload(downloadId: String) {
        downloadJobs[downloadId]?.cancel()
        downloadJobs.remove(downloadId)

        val task = downloadTasks[downloadId]
        if (task != null) {
            updateTaskStatus(downloadId, DownloadStatus.CANCELLED)

            // 删除文件
            val settings = getSettings()
            if (settings.deleteFileOnCancel) {
                deleteDownloadFile(task)
            }
        }

        downloadTasks.remove(downloadId)
        _downloadStates.remove(downloadId)
        saveDownloadTasks()
    }

    /**
     * 删除下载记录
     */
    fun deleteDownload(downloadId: String, deleteFile: Boolean = false) {
        cancelDownload(downloadId)

        if (deleteFile) {
            downloadTasks[downloadId]?.let { deleteDownloadFile(it) }
        }

        downloadTasks.remove(downloadId)
        saveDownloadTasks()
    }

    /**
     * 重新下载
     */
    fun retryDownload(downloadId: String) {
        resetDownload(downloadId)
        startDownload(downloadId)
    }

    /**
     * 重置下载
     */
    private fun resetDownload(downloadId: String) {
        val task = downloadTasks[downloadId] ?: return
        val resetTask = task.copy(
            downloadedSize = 0,
            progress = 0f,
            status = DownloadStatus.PENDING,
            errorMessage = null,
            retryCount = 0
        )
        downloadTasks[downloadId] = resetTask
        deleteDownloadFile(task)
        saveDownloadTasks()
    }

    /**
     * 获取下载状态流
     */
    fun getDownloadState(downloadId: String): StateFlow<DownloadState>? {
        return _downloadStates[downloadId]
    }

    /**
     * 获取所有下载任务
     */
    fun getAllDownloads(): List<DownloadTask> {
        return downloadTasks.values.toList()
    }

    /**
     * 获取下载任务
     */
    fun getDownload(downloadId: String): DownloadTask? {
        return downloadTasks[downloadId]
    }

    // ========== 多线程下载实现 ==========

    private suspend fun downloadWithMultipleThreads(task: DownloadTask) = coroutineScope {
        val file = File(task.savePath, task.fileName)
        val tempFile = File(task.savePath, "${task.fileName}.tmp")

        // 创建临时文件
        if (!tempFile.exists()) {
            tempFile.createNewFile()
            val raf = RandomAccessFile(tempFile, "rw")
            raf.setLength(task.fileSize)
            raf.close()
        }

        // 计算每个线程的下载范围
        val blockSize = task.fileSize / task.threadCount
        val threads = mutableListOf<ThreadInfo>()

        for (i in 0 until task.threadCount) {
            val startPos = i * blockSize
            val endPos = if (i == task.threadCount - 1) {
                task.fileSize - 1
            } else {
                (i + 1) * blockSize - 1
            }

            threads.add(ThreadInfo(
                id = i,
                startPos = startPos,
                endPos = endPos,
                currentPos = startPos
            ))
        }

        // 启动下载线程
        val jobs = threads.map { threadInfo ->
            async {
                downloadThread(task, tempFile, threadInfo)
            }
        }

        // 等待所有线程完成
        jobs.awaitAll()

        // 重命名临时文件
        if (tempFile.renameTo(file)) {
            completeDownload(task.id, file)
        } else {
            throw IOException("Failed to rename temp file")
        }
    }

    private suspend fun downloadThread(
        task: DownloadTask,
        file: File,
        threadInfo: ThreadInfo
    ) = withContext(Dispatchers.IO) {
        val url = URL(task.url)
        val connection = url.openConnection() as HttpURLConnection

        // 设置请求头
        task.headers.forEach { (key, value) ->
            connection.setRequestProperty(key, value)
        }

        // 设置下载范围
        connection.setRequestProperty(
            "Range",
            "bytes=${threadInfo.currentPos}-${threadInfo.endPos}"
        )

        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val raf = RandomAccessFile(file, "rw")
        raf.seek(threadInfo.currentPos)

        connection.inputStream.use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            var currentPos = threadInfo.currentPos

            while (isActive && currentPos <= threadInfo.endPos) {
                bytesRead = input.read(buffer)
                if (bytesRead == -1) break

                val writeSize = min(bytesRead, (threadInfo.endPos - currentPos + 1).toInt())
                raf.write(buffer, 0, writeSize)
                currentPos += writeSize

                // 更新进度
                updateProgress(task.id, writeSize.toLong())
            }
        }

        raf.close()
        connection.disconnect()
    }

    // ========== 单线程下载实现 ==========

    private suspend fun downloadWithSingleThread(task: DownloadTask) = withContext(Dispatchers.IO) {
        val file = File(task.savePath, task.fileName)
        val tempFile = File(task.savePath, "${task.fileName}.tmp")

        val url = URL(task.url)
        val connection = url.openConnection() as HttpURLConnection

        // 设置请求头
        task.headers.forEach { (key, value) ->
            connection.setRequestProperty(key, value)
        }

        // 断点续传
        var downloadedSize = 0L
        if (tempFile.exists() && task.resumable) {
            downloadedSize = tempFile.length()
            connection.setRequestProperty("Range", "bytes=$downloadedSize-")
        }

        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val fileOutputStream = FileOutputStream(tempFile, downloadedSize > 0)

        connection.inputStream.use { input ->
            fileOutputStream.use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int

                while (isActive) {
                    bytesRead = input.read(buffer)
                    if (bytesRead == -1) break

                    output.write(buffer, 0, bytesRead)
                    downloadedSize += bytesRead

                    // 更新进度
                    updateProgress(task.id, bytesRead.toLong())
                }
            }
        }

        connection.disconnect()

        // 重命名临时文件
        if (tempFile.renameTo(file)) {
            completeDownload(task.id, file)
        } else {
            throw IOException("Failed to rename temp file")
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 获取文件信息
     */
    private fun getFileInfo(url: String, headers: Map<String, String>): Pair<Long, Boolean> {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"

        headers.forEach { (key, value) ->
            connection.setRequestProperty(key, value)
        }

        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val fileSize = connection.contentLengthLong
        val acceptRanges = connection.getHeaderField("Accept-Ranges")
        val resumable = acceptRanges == "bytes"

        connection.disconnect()

        return Pair(fileSize, resumable)
    }

    /**
     * 提取文件名
     */
    private fun extractFileName(url: String): String {
        val uri = Uri.parse(url)
        var fileName = uri.lastPathSegment ?: "download"

        // 移除查询参数
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf("?"))
        }

        // 如果没有扩展名，添加默认扩展名
        if (!fileName.contains(".")) {
            fileName = "$fileName.bin"
        }

        return fileName
    }

    /**
     * 更新任务状态
     */
    private fun updateTaskStatus(downloadId: String, status: DownloadStatus) {
        val task = downloadTasks[downloadId] ?: return
        val updatedTask = task.copy(status = status)
        downloadTasks[downloadId] = updatedTask
        saveDownloadTasks()

        // 更新状态流
        _downloadStates[downloadId]?.value = DownloadState(
            updatedTask,
            task.progress,
            task.speed,
            task.remainingTime
        )
    }

    /**
     * 更新下载进度
     */
    private fun updateProgress(downloadId: String, bytesDownloaded: Long) {
        val task = downloadTasks[downloadId] ?: return
        val updatedSize = task.downloadedSize + bytesDownloaded
        val progress = if (task.fileSize > 0) {
            (updatedSize.toFloat() / task.fileSize * 100)
        } else {
            0f
        }

        val updatedTask = task.copy(
            downloadedSize = updatedSize,
            progress = progress
        )
        downloadTasks[downloadId] = updatedTask

        // 计算速度和剩余时间
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - task.createTime
        val speed = if (elapsedTime > 0) {
            (updatedSize * 1000 / elapsedTime)
        } else {
            0L
        }

        val remainingBytes = task.fileSize - updatedSize
        val remainingTime = if (speed > 0) {
            remainingBytes / speed * 1000
        } else {
            0L
        }

        // 更新状态流
        _downloadStates[downloadId]?.value = DownloadState(
            updatedTask,
            progress,
            speed,
            remainingTime
        )
    }

    /**
     * 完成下载
     */
    private fun completeDownload(downloadId: String, file: File) {
        val task = downloadTasks[downloadId] ?: return
        val completedTask = task.copy(
            status = DownloadStatus.COMPLETED,
            progress = 100f,
            completeTime = System.currentTimeMillis(),
            downloadedSize = file.length()
        )
        downloadTasks[downloadId] = completedTask
        saveDownloadTasks()

        // 保存到历史记录
        saveToHistory(completedTask)

        // 更新状态流
        _downloadStates[downloadId]?.value = DownloadState(
            completedTask,
            100f,
            0,
            0
        )
    }

    /**
     * 处理下载错误
     */
    private fun handleDownloadError(downloadId: String, error: Exception) {
        val task = downloadTasks[downloadId] ?: return
        val settings = getSettings()

        if (settings.autoRetry && task.retryCount < settings.maxRetryCount) {
            // 自动重试
            val updatedTask = task.copy(
                retryCount = task.retryCount + 1,
                status = DownloadStatus.PENDING
            )
            downloadTasks[downloadId] = updatedTask

            GlobalScope.launch {
                delay(RETRY_DELAY)
                startDownload(downloadId)
            }
        } else {
            // 标记为失败
            val failedTask = task.copy(
                status = DownloadStatus.FAILED,
                errorMessage = error.message
            )
            downloadTasks[downloadId] = failedTask
            saveDownloadTasks()

            _downloadStates[downloadId]?.value = DownloadState(
                failedTask,
                task.progress,
                0,
                0
            )
        }
    }

    /**
     * 删除下载文件
     */
    private fun deleteDownloadFile(task: DownloadTask) {
        val file = File(task.savePath, task.fileName)
        if (file.exists()) {
            file.delete()
        }

        val tempFile = File(task.savePath, "${task.fileName}.tmp")
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    // ========== 设置管理 ==========

    /**
     * 获取设置
     */
    fun getSettings(): DownloadSettings {
        val json = sharedPrefs.getString(KEY_SETTINGS, null)
        return if (json != null) {
            try {
                val obj = JSONObject(json)
                DownloadSettings(
                    maxConcurrentDownloads = obj.getInt("maxConcurrentDownloads"),
                    defaultThreadCount = obj.getInt("defaultThreadCount"),
                    defaultSavePath = obj.getString("defaultSavePath"),
                    wifiOnly = obj.getBoolean("wifiOnly"),
                    notificationEnabled = obj.getBoolean("notificationEnabled"),
                    autoRetry = obj.getBoolean("autoRetry"),
                    maxRetryCount = obj.getInt("maxRetryCount"),
                    speedLimit = obj.getLong("speedLimit"),
                    deleteFileOnCancel = obj.getBoolean("deleteFileOnCancel")
                )
            } catch (e: Exception) {
                DownloadSettings()
            }
        } else {
            DownloadSettings()
        }
    }

    /**
     * 保存设置
     */
    fun saveSettings(settings: DownloadSettings) {
        val json = JSONObject().apply {
            put("maxConcurrentDownloads", settings.maxConcurrentDownloads)
            put("defaultThreadCount", settings.defaultThreadCount)
            put("defaultSavePath", settings.defaultSavePath)
            put("wifiOnly", settings.wifiOnly)
            put("notificationEnabled", settings.notificationEnabled)
            put("autoRetry", settings.autoRetry)
            put("maxRetryCount", settings.maxRetryCount)
            put("speedLimit", settings.speedLimit)
            put("deleteFileOnCancel", settings.deleteFileOnCancel)
        }
        sharedPrefs.edit().putString(KEY_SETTINGS, json.toString()).apply()
    }

    // ========== 历史记录管理 ==========

    /**
     * 保存到历史记录
     */
    private fun saveToHistory(task: DownloadTask) {
        val history = DownloadHistory(
            id = task.id,
            url = task.url,
            fileName = task.fileName,
            fileSize = task.fileSize,
            savePath = task.savePath,
            completeTime = task.completeTime,
            duration = task.completeTime - task.createTime
        )

        val histories = getHistory().toMutableList()
        histories.add(0, history)

        // 限制历史记录数量
        if (histories.size > 100) {
            histories.subList(100, histories.size).clear()
        }

        saveHistory(histories)
    }

    /**
     * 获取历史记录
     */
    fun getHistory(): List<DownloadHistory> {
        val json = sharedPrefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val list = mutableListOf<DownloadHistory>()

        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                list.add(DownloadHistory(
                    id = item.getString("id"),
                    url = item.getString("url"),
                    fileName = item.getString("fileName"),
                    fileSize = item.getLong("fileSize"),
                    savePath = item.getString("savePath"),
                    completeTime = item.getLong("completeTime"),
                    duration = item.getLong("duration")
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }

    /**
     * 清除历史记录
     */
    fun clearHistory() {
        sharedPrefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun saveHistory(histories: List<DownloadHistory>) {
        val jsonArray = JSONArray()
        histories.forEach { history ->
            jsonArray.put(JSONObject().apply {
                put("id", history.id)
                put("url", history.url)
                put("fileName", history.fileName)
                put("fileSize", history.fileSize)
                put("savePath", history.savePath)
                put("completeTime", history.completeTime)
                put("duration", history.duration)
            })
        }
        sharedPrefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
    }

    // ========== 持久化管理 ==========

    private fun saveDownloadTasks() {
        val jsonArray = JSONArray()
        downloadTasks.values.forEach { task ->
            jsonArray.put(taskToJson(task))
        }
        sharedPrefs.edit().putString(KEY_DOWNLOADS, jsonArray.toString()).apply()
    }

    private fun loadDownloadTasks() {
        val json = sharedPrefs.getString(KEY_DOWNLOADS, "[]") ?: "[]"
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val taskJson = jsonArray.getJSONObject(i)
                val task = jsonToTask(taskJson)
                downloadTasks[task.id] = task

                // 创建状态流
                _downloadStates[task.id] = MutableStateFlow(
                    DownloadState(task, task.progress, 0, 0)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun taskToJson(task: DownloadTask): JSONObject {
        return JSONObject().apply {
            put("id", task.id)
            put("url", task.url)
            put("fileName", task.fileName)
            put("savePath", task.savePath)
            put("fileSize", task.fileSize)
            put("downloadedSize", task.downloadedSize)
            put("status", task.status.name)
            put("progress", task.progress)
            put("speed", task.speed)
            put("remainingTime", task.remainingTime)
            put("threadCount", task.threadCount)
            put("resumable", task.resumable)
            put("retryCount", task.retryCount)
            put("createTime", task.createTime)
            put("completeTime", task.completeTime)
            task.errorMessage?.let { put("errorMessage", it) }

            if (task.headers.isNotEmpty()) {
                val headers = JSONObject()
                task.headers.forEach { (key, value) ->
                    headers.put(key, value)
                }
                put("headers", headers)
            }

            if (task.metadata.isNotEmpty()) {
                val metadata = JSONObject()
                task.metadata.forEach { (key, value) ->
                    metadata.put(key, value)
                }
                put("metadata", metadata)
            }
        }
    }

    private fun jsonToTask(json: JSONObject): DownloadTask {
        val headers = mutableMapOf<String, String>()
        if (json.has("headers")) {
            val headersJson = json.getJSONObject("headers")
            headersJson.keys().forEach { key ->
                headers[key] = headersJson.getString(key)
            }
        }

        val metadata = mutableMapOf<String, String>()
        if (json.has("metadata")) {
            val metadataJson = json.getJSONObject("metadata")
            metadataJson.keys().forEach { key ->
                metadata[key] = metadataJson.getString(key)
            }
        }

        return DownloadTask(
            id = json.getString("id"),
            url = json.getString("url"),
            fileName = json.getString("fileName"),
            savePath = json.getString("savePath"),
            fileSize = json.getLong("fileSize"),
            downloadedSize = json.getLong("downloadedSize"),
            status = DownloadStatus.valueOf(json.getString("status")),
            progress = json.getDouble("progress").toFloat(),
            speed = json.getLong("speed"),
            remainingTime = json.getLong("remainingTime"),
            threadCount = json.getInt("threadCount"),
            resumable = json.getBoolean("resumable"),
            headers = headers,
            retryCount = json.getInt("retryCount"),
            createTime = json.getLong("createTime"),
            completeTime = json.getLong("completeTime"),
            errorMessage = json.optString("errorMessage", null),
            metadata = metadata
        )
    }

    init {
        loadDownloadTasks()
    }
}