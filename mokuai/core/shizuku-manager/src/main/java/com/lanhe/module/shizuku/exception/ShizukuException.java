package com.lanhe.module.shizuku.exception;

/**
 * Shizuku异常基类
 *
 * <p>所有Shizuku相关异常的基类，提供统一的错误处理机制。</p>
 *
 * @author LanHe
 * @version 1.0.0
 * @since 2024-01-01
 */
public class ShizukuException extends Exception {

    public static final int ERROR_UNKNOWN = 0;
    public static final int ERROR_NOT_INSTALLED = 1;
    public static final int ERROR_NOT_RUNNING = 2;
    public static final int ERROR_PERMISSION_DENIED = 3;
    public static final int ERROR_TIMEOUT = 4;
    public static final int ERROR_SYSTEM_SERVICE = 5;

    private final int errorCode;

    /**
     * 构造异常
     *
     * @param errorCode 错误码
     * @param message 错误信息
     */
    public ShizukuException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造异常
     *
     * @param errorCode 错误码
     * @param message 错误信息
     * @param cause 原始异常
     */
    public ShizukuException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * 获取错误信息
     *
     * @param errorCode 错误码
     * @return 对应的错误信息
     */
    public static String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case ERROR_NOT_INSTALLED:
                return "Shizuku未安装，请先安装Shizuku应用";
            case ERROR_NOT_RUNNING:
                return "Shizuku服务未运行，请启动Shizuku服务";
            case ERROR_PERMISSION_DENIED:
                return "Shizuku权限被拒绝，请在Shizuku中授予权限";
            case ERROR_TIMEOUT:
                return "操作超时，请重试";
            case ERROR_SYSTEM_SERVICE:
                return "系统服务访问失败";
            default:
                return "未知错误";
        }
    }
}
