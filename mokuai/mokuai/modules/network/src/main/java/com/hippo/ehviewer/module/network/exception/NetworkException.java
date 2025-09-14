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

package com.hippo.ehviewer.module.network.exception;

/**
 * 网络异常类
 * 统一处理网络相关的异常情况
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class NetworkException extends Exception {

    // 错误代码常量
    public static final int ERROR_UNKNOWN = 0;
    public static final int ERROR_NETWORK = 1;
    public static final int ERROR_TIMEOUT = 2;
    public static final int ERROR_SERVER = 3;
    public static final int ERROR_AUTH = 4;
    public static final int ERROR_SSL = 5;
    public static final int ERROR_PROXY = 6;

    private final int errorCode;

    /**
     * 构造函数
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     */
    public NetworkException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param cause 原始异常
     */
    public NetworkException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误代码
     *
     * @return 错误代码
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * 根据错误代码获取错误消息
     *
     * @param errorCode 错误代码
     * @return 错误消息
     */
    public static String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case ERROR_NETWORK:
                return "网络连接错误";
            case ERROR_TIMEOUT:
                return "网络请求超时";
            case ERROR_SERVER:
                return "服务器错误";
            case ERROR_AUTH:
                return "身份验证失败";
            case ERROR_SSL:
                return "SSL证书错误";
            case ERROR_PROXY:
                return "代理服务器错误";
            case ERROR_UNKNOWN:
            default:
                return "未知网络错误";
        }
    }

    /**
     * 获取本地化错误消息
     *
     * @return 本地化错误消息
     */
    public String getLocalizedMessage() {
        return getErrorMessage(errorCode) + (getMessage() != null ? ": " + getMessage() : "");
    }
}
