package com.lanhe.module.shizuku.constants;

/**
 * Shizuku模块常量定义
 *
 * <p>集中管理Shizuku模块的所有常量，避免魔法数字和硬编码字符串。</p>
 *
 * @author LanHe
 * @version 1.0.0
 * @since 2024-01-01
 */
public final class ShizukuConstants {

    // 私有构造函数，防止实例化
    private ShizukuConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ===============================
    // 状态常量
    // ===============================
    public static final int STATUS_AVAILABLE = 0;           // Shizuku可用
    public static final int STATUS_NOT_INSTALLED = 1;       // Shizuku未安装
    public static final int STATUS_NOT_RUNNING = 2;         // Shizuku未运行
    public static final int STATUS_NO_PERMISSION = 3;       // 没有权限

    // ===============================
    // 错误码常量
    // ===============================
    public static final int ERROR_UNKNOWN = 0;
    public static final int ERROR_NOT_INSTALLED = 1;
    public static final int ERROR_NOT_RUNNING = 2;
    public static final int ERROR_PERMISSION_DENIED = 3;
    public static final int ERROR_TIMEOUT = 4;
    public static final int ERROR_SYSTEM_SERVICE = 5;

    // ===============================
    // 权限请求常量
    // ===============================
    public static final int SHIZUKU_PERMISSION_REQUEST_CODE = 1001;
    public static final String SHIZUKU_PERMISSION = "rikka.shizuku.permission.API_V23";

    // ===============================
    // 系统服务常量
    // ===============================
    public static final String SERVICE_ACTIVITY = "activity";
    public static final String SERVICE_PACKAGE = "package";
    public static final String SERVICE_WINDOW = "window";
    public static final String SERVICE_NOTIFICATION = "notification";
    public static final String SERVICE_POWER = "power";

    // ===============================
    // 配置常量
    // ===============================
    public static final long DEFAULT_TIMEOUT = 30000L;      // 默认超时时间(30秒)
    public static final int DEFAULT_RETRY_COUNT = 3;         // 默认重试次数
    public static final long CONNECTION_TIMEOUT = 10000L;    // 连接超时时间(10秒)

    // ===============================
    // 日志标签
    // ===============================
    public static final String TAG = "ShizukuManager";

    // ===============================
    // 版本信息
    // ===============================
    public static final String VERSION_NAME = "1.0.0";
    public static final int VERSION_CODE = 1;
    public static final String AUTHOR = "LanHe";
    public static final String DESCRIPTION = "Shizuku权限管理系统";

    // ===============================
    // 字符串常量
    // ===============================
    public static final String STATUS_MESSAGE_AVAILABLE = "Shizuku权限已授予，可以使用全部高级功能";
    public static final String STATUS_MESSAGE_NOT_INSTALLED = "Shizuku服务不可用，请安装并启动Shizuku";
    public static final String STATUS_MESSAGE_NOT_RUNNING = "Shizuku服务未运行，请启动Shizuku服务";
    public static final String STATUS_MESSAGE_NO_PERMISSION = "Shizuku权限被拒绝，请授予权限";

    // ===============================
    // SharedPreferences键
    // ===============================
    public static final String PREF_SHIZUKU_ENABLED = "shizuku_enabled";
    public static final String PREF_LAST_PERMISSION_CHECK = "last_permission_check";
    public static final String PREF_PERMISSION_GRANTED = "permission_granted";
}
