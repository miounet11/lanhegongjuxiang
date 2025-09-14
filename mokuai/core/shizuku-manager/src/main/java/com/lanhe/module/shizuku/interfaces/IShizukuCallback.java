package com.lanhe.module.shizuku.interfaces;

/**
 * Shizuku操作回调接口
 *
 * <p>统一的回调接口模板，用于处理异步操作的结果。</p>
 *
 * @param <T> 回调结果的类型
 * @author LanHe
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface IShizukuCallback<T> {

    /**
     * 操作成功回调
     *
     * @param result 操作结果
     */
    void onSuccess(T result);

    /**
     * 操作失败回调
     *
     * @param error 错误信息
     */
    void onFailure(ShizukuException error);

    /**
     * 操作取消回调
     */
    void onCancel();

    /**
     * 进度更新回调
     *
     * @param progress 进度值 (0-100)
     * @param message 进度消息
     */
    default void onProgress(int progress, String message) {
        // 默认空实现
    }
}
