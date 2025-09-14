/*
 * Copyright 2024 LanHe Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.module.network.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;

import com.hippo.ehviewer.module.network.exception.NetworkException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

/**
 * 网络工具类
 * 提供网络相关的工具方法
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class NetworkUtils {

    /**
     * 检查网络是否可用
     *
     * @param context Android上下文
     * @return true网络可用，false网络不可用
     */
    public static boolean isNetworkAvailable(@NonNull Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }

        return false;
    }

    /**
     * 获取网络类型
     *
     * @param context Android上下文
     * @return 网络类型字符串
     */
    public static String getNetworkType(@NonNull Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                int type = activeNetworkInfo.getType();
                switch (type) {
                    case ConnectivityManager.TYPE_WIFI:
                        return "WIFI";
                    case ConnectivityManager.TYPE_MOBILE:
                        return "MOBILE";
                    case ConnectivityManager.TYPE_ETHERNET:
                        return "ETHERNET";
                    default:
                        return "UNKNOWN";
                }
            }
        }

        return "NONE";
    }

    /**
     * 根据异常创建NetworkException
     *
     * @param exception 原始异常
     * @return NetworkException实例
     */
    public static NetworkException createNetworkException(@NonNull Exception exception) {
        if (exception instanceof SocketTimeoutException) {
            return new NetworkException(
                    NetworkException.ERROR_TIMEOUT,
                    "网络请求超时",
                    exception
            );
        } else if (exception instanceof UnknownHostException) {
            return new NetworkException(
                    NetworkException.ERROR_NETWORK,
                    "无法连接到服务器",
                    exception
            );
        } else if (exception instanceof SSLException) {
            return new NetworkException(
                    NetworkException.ERROR_SSL,
                    "SSL证书验证失败",
                    exception
            );
        } else if (exception instanceof IOException) {
            return new NetworkException(
                    NetworkException.ERROR_NETWORK,
                    "网络IO错误",
                    exception
            );
        } else {
            return new NetworkException(
                    NetworkException.ERROR_UNKNOWN,
                    "未知网络错误",
                    exception
            );
        }
    }

    /**
     * 检查URL是否有效
     *
     * @param url URL字符串
     * @return trueURL有效，falseURL无效
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        // 简单的URL格式验证
        return url.startsWith("http://") || url.startsWith("https://");
    }

    /**
     * 获取URL的域名
     *
     * @param url 完整的URL
     * @return 域名，如果解析失败返回null
     */
    public static String getDomainFromUrl(String url) {
        if (!isValidUrl(url)) {
            return null;
        }

        try {
            java.net.URL urlObj = new java.net.URL(url);
            return urlObj.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 格式化文件大小
     *
     * @param bytes 字节数
     * @return 格式化的文件大小字符串
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 计算下载进度百分比
     *
     * @param downloaded 已下载字节数
     * @param total 总字节数
     * @return 进度百分比 (0-100)
     */
    public static int calculateProgress(long downloaded, long total) {
        if (total <= 0) {
            return 0;
        }

        return (int) ((downloaded * 100) / total);
    }
}
