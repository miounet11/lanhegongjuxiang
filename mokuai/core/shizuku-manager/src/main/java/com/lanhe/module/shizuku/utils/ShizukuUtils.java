package com.lanhe.module.shizuku.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import com.lanhe.module.shizuku.constants.ShizukuConstants;

/**
 * Shizuku工具类
 *
 * <p>提供Shizuku相关的工具方法和辅助功能。</p>
 *
 * @author LanHe
 * @version 1.0.0
 * @since 2024-01-01
 */
public final class ShizukuUtils {

    // 私有构造函数，防止实例化
    private ShizukuUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 检查Shizuku应用是否已安装
     *
     * @param context 应用上下文
     * @return true如果已安装，false否则
     */
    public static boolean isShizukuInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo("rikka.shizuku", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(ShizukuConstants.TAG, "Shizuku app not installed", e);
            return false;
        }
    }

    /**
     * 获取Shizuku版本信息
     *
     * @param context 应用上下文
     * @return 版本名称，如果获取失败返回null
     */
    public static String getShizukuVersion(Context context) {
        try {
            return context.getPackageManager()
                    .getPackageInfo("rikka.shizuku", 0)
                    .versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(ShizukuConstants.TAG, "Failed to get Shizuku version", e);
            return null;
        }
    }

    /**
     * 检查权限是否已授予
     *
     * @param context 应用上下文
     * @return true如果权限已授予，false否则
     */
    public static boolean isPermissionGranted(Context context) {
        // 这里应该实现实际的权限检查逻辑
        // 由于Shizuku的特殊性，这里返回模拟结果
        return false;
    }

    /**
     * 格式化状态消息
     *
     * @param status 状态码
     * @return 格式化的状态消息
     */
    public static String formatStatusMessage(int status) {
        switch (status) {
            case ShizukuConstants.STATUS_AVAILABLE:
                return ShizukuConstants.STATUS_MESSAGE_AVAILABLE;
            case ShizukuConstants.STATUS_NOT_INSTALLED:
                return ShizukuConstants.STATUS_MESSAGE_NOT_INSTALLED;
            case ShizukuConstants.STATUS_NOT_RUNNING:
                return ShizukuConstants.STATUS_MESSAGE_NOT_RUNNING;
            case ShizukuConstants.STATUS_NO_PERMISSION:
                return ShizukuConstants.STATUS_MESSAGE_NO_PERMISSION;
            default:
                return "未知状态";
        }
    }

    /**
     * 验证输入参数
     *
     * @param value 要验证的值
     * @param fieldName 字段名称
     * @throws IllegalArgumentException 如果参数无效
     */
    public static void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }

    /**
     * 验证字符串参数
     *
     * @param value 要验证的字符串
     * @param fieldName 字段名称
     * @throws IllegalArgumentException 如果参数无效
     */
    public static void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }

    /**
     * 记录调试信息
     *
     * @param message 调试消息
     */
    public static void logDebug(String message) {
        Log.d(ShizukuConstants.TAG, message);
    }

    /**
     * 记录警告信息
     *
     * @param message 警告消息
     * @param throwable 可选的异常信息
     */
    public static void logWarning(String message, Throwable throwable) {
        if (throwable != null) {
            Log.w(ShizukuConstants.TAG, message, throwable);
        } else {
            Log.w(ShizukuConstants.TAG, message);
        }
    }

    /**
     * 记录错误信息
     *
     * @param message 错误消息
     * @param throwable 可选的异常信息
     */
    public static void logError(String message, Throwable throwable) {
        if (throwable != null) {
            Log.e(ShizukuConstants.TAG, message, throwable);
        } else {
            Log.e(ShizukuConstants.TAG, message);
        }
    }

    /**
     * 获取当前时间戳
     *
     * @return 当前时间戳（毫秒）
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 检查是否为主线程
     *
     * @return true如果是主线程，false否则
     */
    public static boolean isMainThread() {
        return android.os.Looper.getMainLooper() == android.os.Looper.myLooper();
    }

    /**
     * 在主线程中运行
     *
     * @param runnable 要运行的任务
     */
    public static void runOnMainThread(Runnable runnable) {
        if (isMainThread()) {
            runnable.run();
        } else {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(runnable);
        }
    }
}
