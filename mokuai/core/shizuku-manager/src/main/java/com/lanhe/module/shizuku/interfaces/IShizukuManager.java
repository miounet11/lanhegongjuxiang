package com.lanhe.module.shizuku.interfaces;

/**
 * Shizuku管理器接口
 *
 * <p>定义Shizuku权限管理的所有公共方法。</p>
 *
 * @author LanHe
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface IShizukuManager {

    /**
     * 检查Shizuku是否可用
     *
     * @return true如果Shizuku可用，false否则
     */
    boolean isShizukuAvailable();

    /**
     * 获取Shizuku状态
     *
     * @return Shizuku状态码
     */
    int getStatus();

    /**
     * 请求Shizuku权限
     *
     * @param callback 权限请求结果回调
     */
    void requestPermission(IShizukuCallback callback);

    /**
     * 获取Shizuku状态描述信息
     *
     * @return 状态描述字符串
     */
    String getStatusMessage();

    /**
     * 执行系统级操作
     *
     * @param operation 要执行的操作
     * @param callback 操作结果回调
     * @param <T> 操作结果类型
     */
    <T> void executeSystemOperation(String operation, IShizukuCallback<T> callback);

    /**
     * 获取系统服务
     *
     * @param serviceName 服务名称
     * @return 系统服务对象，如果获取失败返回null
     */
    Object getSystemService(String serviceName);

    /**
     * 清理资源
     */
    void cleanup();

    // 状态常量
    int STATUS_AVAILABLE = 0;      // Shizuku可用
    int STATUS_NOT_INSTALLED = 1;  // Shizuku未安装
    int STATUS_NOT_RUNNING = 2;    // Shizuku未运行
    int STATUS_NO_PERMISSION = 3;  // 没有权限
}
