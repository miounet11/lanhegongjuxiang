package com.lanhe.module.shizuku;

import android.content.Context;
import com.lanhe.module.shizuku.interfaces.IShizukuManager;
import com.lanhe.module.shizuku.interfaces.IShizukuCallback;
import com.lanhe.module.shizuku.exception.ShizukuException;
import com.lanhe.module.shizuku.impl.DefaultShizukuManager;
import com.lanhe.module.shizuku.constants.ShizukuConstants;
import com.lanhe.module.shizuku.utils.ShizukuUtils;

/**
 * Shizuku管理器
 *
 * <p>Shizuku权限管理系统的核心管理类，采用单例模式设计。</p>
 *
 * <h2>基本用法</h2>
 * <pre>{@code
 * // 获取实例
 * ShizukuManager manager = ShizukuManager.getInstance(context);
 *
 * // 检查权限
 * if (manager.isShizukuAvailable()) {
 *     // 执行系统操作
 *     manager.executeSystemOperation("operation_name", new IShizukuCallback<String>() {
 *         @Override
 *         public void onSuccess(String result) {
 *             Log.d(TAG, "Operation successful: " + result);
 *         }
 *
 *         @Override
 *         public void onFailure(ShizukuException error) {
 *             Log.e(TAG, "Operation failed", error);
 *         }
 *     });
 * } else {
 *     // 请求权限
 *     manager.requestPermission(new IShizukuCallback<Boolean>() {
 *         @Override
 *         public void onSuccess(Boolean granted) {
 *             Log.d(TAG, "Permission granted: " + granted);
 *         }
 *
 *         @Override
 *         public void onFailure(ShizukuException error) {
 *             Log.e(TAG, "Permission request failed", error);
 *         }
 *     });
 * }
 * }</pre>
 *
 * <h2>线程安全</h2>
 * <p>该类是线程安全的，所有公共方法都可以安全地从多个线程调用。</p>
 *
 * @author LanHe
 * @version 1.0.0
 * @since 2024-01-01
 */
public class ShizukuManager implements IShizukuManager {

    // 单例实例
    private static volatile ShizukuManager instance;

    // 实际的实现类
    private final IShizukuManager impl;

    /**
     * 获取ShizukuManager单例实例
     *
     * @param context 应用上下文，不能为null
     * @return ShizukuManager实例
     * @throws IllegalArgumentException 如果context为null
     */
    public static ShizukuManager getInstance(Context context) {
        ShizukuUtils.validateNotNull(context, "context");

        if (instance == null) {
            synchronized (ShizukuManager.class) {
                if (instance == null) {
                    instance = new ShizukuManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * 私有构造函数
     *
     * @param context 应用上下文
     */
    private ShizukuManager(Context context) {
        this.impl = new DefaultShizukuManager(context);
        ShizukuUtils.logDebug("ShizukuManager instance created");
    }

    @Override
    public boolean isShizukuAvailable() {
        return impl.isShizukuAvailable();
    }

    @Override
    public int getStatus() {
        return impl.getStatus();
    }

    @Override
    public void requestPermission(IShizukuCallback callback) {
        impl.requestPermission(callback);
    }

    @Override
    public String getStatusMessage() {
        return impl.getStatusMessage();
    }

    @Override
    public <T> void executeSystemOperation(String operation, IShizukuCallback<T> callback) {
        impl.executeSystemOperation(operation, callback);
    }

    @Override
    public Object getSystemService(String serviceName) {
        return impl.getSystemService(serviceName);
    }

    @Override
    public void cleanup() {
        impl.cleanup();
    }

    /**
     * 初始化ShizukuManager
     *
     * <p>在Application的onCreate方法中调用此方法来初始化ShizukuManager。</p>
     *
     * @param context 应用上下文
     */
    public static void init(Context context) {
        getInstance(context);
    }

    /**
     * 销毁单例实例
     *
     * <p>通常在测试环境中使用，用于清理单例状态。</p>
     */
    public static void destroy() {
        if (instance != null) {
            synchronized (ShizukuManager.class) {
                if (instance != null) {
                    instance.cleanup();
                    instance = null;
                }
            }
        }
    }
}
