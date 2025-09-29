package com.lanhe.mokuai.crash

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 崩溃处理器 - 捕获并记录应用崩溃信息
 */
class CrashHandler private constructor(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    private val logDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    companion object {
        private const val TAG = "CrashHandler"
        private const val CRASH_DIR_NAME = "crash_logs"
        private const val MAX_LOG_FILES = 10

        @Volatile
        private var instance: CrashHandler? = null

        fun getInstance(context: Context): CrashHandler {
            return instance ?: synchronized(this) {
                instance ?: CrashHandler(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            // 保存崩溃日志
            val crashFile = saveCrashLog(thread, exception)

            // 处理崩溃
            handleCrash(crashFile, exception)

            // 上报崩溃（如果需要）
            reportCrash(crashFile, exception)

        } catch (e: Exception) {
            Log.e(TAG, "Error handling crash", e)
        } finally {
            // 交给系统默认处理器
            defaultHandler?.uncaughtException(thread, exception)
        }
    }

    /**
     * 保存崩溃日志到文件
     */
    private fun saveCrashLog(thread: Thread, exception: Throwable): File? {
        return try {
            val crashDir = getCrashLogDirectory()
            if (!crashDir.exists()) {
                crashDir.mkdirs()
            }

            // 清理旧日志
            cleanOldLogs(crashDir)

            val fileName = "crash_${dateFormat.format(Date())}.log"
            val crashFile = File(crashDir, fileName)

            FileWriter(crashFile).use { writer ->
                writer.write("================== CRASH LOG ==================\n")
                writer.write("Time: ${logDateFormat.format(Date())}\n")
                writer.write("Thread: ${thread.name} (id=${thread.id})\n")
                writer.write("\n")

                // 设备信息
                writer.write("========== Device Info ==========\n")
                writer.write(getDeviceInfo())
                writer.write("\n")

                // 应用信息
                writer.write("========== App Info ==========\n")
                writer.write(getAppInfo())
                writer.write("\n")

                // 异常信息
                writer.write("========== Exception ==========\n")
                writer.write("${exception.javaClass.name}: ${exception.message}\n")
                writer.write("\n")

                // 堆栈信息
                writer.write("========== Stack Trace ==========\n")
                val stackTrace = StringWriter()
                exception.printStackTrace(PrintWriter(stackTrace))
                writer.write(stackTrace.toString())
                writer.write("\n")

                // 内存信息
                writer.write("========== Memory Info ==========\n")
                writer.write(getMemoryInfo())
                writer.write("\n")

                writer.write("================== END ==================\n")
            }

            Log.i(TAG, "Crash log saved to: ${crashFile.absolutePath}")
            crashFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash log", e)
            null
        }
    }

    /**
     * 获取崩溃日志目录
     */
    private fun getCrashLogDirectory(): File {
        return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            File(context.getExternalFilesDir(null), CRASH_DIR_NAME)
        } else {
            File(context.filesDir, CRASH_DIR_NAME)
        }
    }

    /**
     * 清理旧的日志文件
     */
    private fun cleanOldLogs(crashDir: File) {
        val logFiles = crashDir.listFiles { file ->
            file.name.startsWith("crash_") && file.name.endsWith(".log")
        } ?: return

        if (logFiles.size > MAX_LOG_FILES) {
            // 按修改时间排序
            logFiles.sortBy { it.lastModified() }

            // 删除最旧的文件
            val filesToDelete = logFiles.size - MAX_LOG_FILES
            for (i in 0 until filesToDelete) {
                logFiles[i].delete()
            }
        }
    }

    /**
     * 获取设备信息
     */
    private fun getDeviceInfo(): String {
        return buildString {
            append("Manufacturer: ${Build.MANUFACTURER}\n")
            append("Model: ${Build.MODEL}\n")
            append("Brand: ${Build.BRAND}\n")
            append("Device: ${Build.DEVICE}\n")
            append("Android Version: ${Build.VERSION.RELEASE}\n")
            append("API Level: ${Build.VERSION.SDK_INT}\n")
            append("Build ID: ${Build.ID}\n")
            append("Build Type: ${Build.TYPE}\n")
        }
    }

    /**
     * 获取应用信息
     */
    private fun getAppInfo(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            buildString {
                append("Package: ${packageInfo.packageName}\n")
                append("Version Name: ${packageInfo.versionName}\n")
                append("Version Code: ${packageInfo.versionCode}\n")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    append("Long Version Code: ${packageInfo.longVersionCode}\n")
                }

                append("First Install Time: ${dateFormat.format(Date(packageInfo.firstInstallTime))}\n")
                append("Last Update Time: ${dateFormat.format(Date(packageInfo.lastUpdateTime))}\n")
            }
        } catch (e: Exception) {
            "Failed to get app info: ${e.message}"
        }
    }

    /**
     * 获取内存信息
     */
    private fun getMemoryInfo(): String {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory

        return buildString {
            append("Max Memory: ${formatBytes(maxMemory)}\n")
            append("Total Memory: ${formatBytes(totalMemory)}\n")
            append("Free Memory: ${formatBytes(freeMemory)}\n")
            append("Used Memory: ${formatBytes(usedMemory)}\n")
            append("Usage: ${String.format("%.2f", (usedMemory.toFloat() / maxMemory * 100))}%\n")
        }
    }

    /**
     * 格式化字节数
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / 1024.0 / 1024.0)
            else -> String.format("%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0)
        }
    }

    /**
     * 处理崩溃
     */
    private fun handleCrash(crashFile: File?, exception: Throwable) {
        // 可以在这里添加自定义处理逻辑
        // 例如：显示崩溃对话框、重启应用等

        // 保存崩溃标记
        context.getSharedPreferences("crash_info", Context.MODE_PRIVATE).edit()
            .putBoolean("has_crash", true)
            .putString("crash_file", crashFile?.absolutePath)
            .putLong("crash_time", System.currentTimeMillis())
            .apply()
    }

    /**
     * 上报崩溃信息
     */
    private fun reportCrash(crashFile: File?, exception: Throwable) {
        // 这里可以添加崩溃上报逻辑
        // 例如：上传到服务器、发送到邮箱等
        Log.d(TAG, "Crash report ready: ${crashFile?.absolutePath}")
    }

    /**
     * 获取所有崩溃日志文件
     */
    fun getAllCrashLogs(): List<File> {
        val crashDir = getCrashLogDirectory()
        return crashDir.listFiles { file ->
            file.name.startsWith("crash_") && file.name.endsWith(".log")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * 删除所有崩溃日志
     */
    fun clearAllCrashLogs() {
        val crashDir = getCrashLogDirectory()
        crashDir.listFiles { file ->
            file.name.startsWith("crash_") && file.name.endsWith(".log")
        }?.forEach { it.delete() }
    }

    /**
     * 检查是否有崩溃
     */
    fun hasCrash(): Boolean {
        return context.getSharedPreferences("crash_info", Context.MODE_PRIVATE)
            .getBoolean("has_crash", false)
    }

    /**
     * 清除崩溃标记
     */
    fun clearCrashFlag() {
        context.getSharedPreferences("crash_info", Context.MODE_PRIVATE).edit()
            .remove("has_crash")
            .remove("crash_file")
            .remove("crash_time")
            .apply()
    }
}