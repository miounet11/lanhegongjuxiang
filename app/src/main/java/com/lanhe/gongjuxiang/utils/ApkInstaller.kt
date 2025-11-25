package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * APK安装工具类
 * 用于处理应用内APK的安装
 */
object ApkInstaller {

    /**
     * 从assets文件夹安装APK
     * @param context 上下文
     * @param assetFileName assets中的APK文件名
     */
    fun installApkFromAssets(context: Context, assetFileName: String): Boolean {
        return try {
            // 将APK从assets复制到缓存目录
            val apkFile = copyApkFromAssets(context, assetFileName)
            
            if (apkFile != null && apkFile.exists()) {
                // 安装APK
                installApk(context, apkFile)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 从assets复制APK到缓存目录
     */
    private fun copyApkFromAssets(context: Context, assetFileName: String): File? {
        return try {
            val cacheDir = context.externalCacheDir ?: context.cacheDir
            val apkFile = File(cacheDir, assetFileName)

            // 如果文件已存在，删除旧文件
            if (apkFile.exists()) {
                apkFile.delete()
            }

            // 从assets复制文件
            context.assets.open(assetFileName).use { input ->
                FileOutputStream(apkFile).use { output ->
                    input.copyTo(output)
                }
            }

            apkFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 安装APK文件
     */
    fun installApk(context: Context, apkFile: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android 7.0及以上使用FileProvider
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }

        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        context.startActivity(intent)
    }

    /**
     * 检查文件是否是有效的APK
     */
    fun isValidApk(file: File): Boolean {
        return file.exists() && file.extension.equals("apk", ignoreCase = true) && file.length() > 0
    }
}
