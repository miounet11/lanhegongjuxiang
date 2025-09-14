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

package com.ehviewer.example;

import android.content.Context;
import android.util.Log;

import com.hippo.ehviewer.module.analytics.AnalyticsManager;
import com.hippo.ehviewer.module.crash.CrashHandler;
import com.hippo.ehviewer.module.password.PasswordManager;
import com.hippo.ehviewer.module.bookmark.BookmarkManager;
import com.hippo.ehviewer.module.adblock.AdBlocker;
import com.hippo.ehviewer.module.image.ImageHelper;
import com.hippo.ehviewer.module.proxy.ProxySelector;
import com.hippo.ehviewer.module.performance.PerformanceMonitor;
import com.hippo.ehviewer.module.memory.MemoryManager;
import com.hippo.ehviewer.module.security.SecurityManager;

/**
 * 小模块集成使用示例
 * 演示如何在实际项目中集成和使用各个小功能模块
 * 这个示例展示了如何组合多个小模块构建一个功能完整的应用
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class SmallModuleIntegrationExample {

    private static final String TAG = SmallModuleIntegrationExample.class.getSimpleName();

    private final Context context;

    // 各个小模块的实例
    private AnalyticsManager analyticsManager;
    private CrashHandler crashHandler;
    private PasswordManager passwordManager;
    private BookmarkManager bookmarkManager;
    private AdBlocker adBlocker;
    private ImageHelper imageHelper;
    private ProxySelector proxySelector;
    private PerformanceMonitor performanceMonitor;
    private MemoryManager memoryManager;
    private SecurityManager securityManager;

    public SmallModuleIntegrationExample(Context context) {
        this.context = context;
        initializeModules();
    }

    /**
     * 初始化所有小模块
     * 按照依赖关系顺序初始化各个模块
     */
    private void initializeModules() {
        Log.i(TAG, "Initializing small modules...");

        try {
            // 1. 初始化基础模块（无依赖）
            analyticsManager = AnalyticsManager.getInstance(context);
            crashHandler = CrashHandler.getInstance(context);
            performanceMonitor = PerformanceMonitor.getInstance(context);
            memoryManager = MemoryManager.getInstance(context);
            securityManager = SecurityManager.getInstance(context);

            // 2. 初始化功能模块
            passwordManager = PasswordManager.getInstance(context);
            bookmarkManager = BookmarkManager.getInstance(context);
            adBlocker = AdBlocker.getInstance(context);
            imageHelper = ImageHelper.getInstance(context);
            proxySelector = ProxySelector.getInstance(context);

            Log.i(TAG, "All small modules initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize modules", e);
            throw new RuntimeException("Module initialization failed", e);
        }
    }

    /**
     * 示例：用户登录流程
     * 展示如何组合使用密码管理器、安全管理器和数据分析模块
     */
    public void userLoginExample(String username, String password) {
        Log.d(TAG, "Starting user login example");

        // 1. 性能监控：开始登录操作监控
        PerformanceMonitor.PerformanceSession session =
            performanceMonitor.startSession("user_login");

        try {
            // 2. 安全检查：验证输入安全性
            if (!securityManager.validateInput(username) ||
                !securityManager.validateInput(password)) {
                analyticsManager.trackEvent("login_failed", "invalid_input");
                throw new IllegalArgumentException("Invalid input");
            }

            // 3. 密码管理：尝试自动填充或验证密码
            boolean autoFilled = passwordManager.autoFill(username, password);
            if (autoFilled) {
                analyticsManager.trackEvent("login_auto_fill", "success");
            }

            // 4. 执行登录逻辑（这里是模拟）
            boolean loginSuccess = performLogin(username, password);

            if (loginSuccess) {
                // 5. 数据分析：跟踪登录成功
                analyticsManager.trackEvent("login_success", "manual");
                analyticsManager.setUserProperty("last_login", String.valueOf(System.currentTimeMillis()));

                // 6. 密码管理：更新密码使用统计
                passwordManager.updatePasswordStats(username);

                Log.i(TAG, "User login successful");
                return;
            }

            // 登录失败
            analyticsManager.trackEvent("login_failed", "invalid_credentials");

        } catch (Exception e) {
            // 崩溃处理：记录异常
            crashHandler.logException(e, "User login failed");

            // 数据分析：跟踪登录异常
            analyticsManager.trackEvent("login_error", e.getClass().getSimpleName());

        } finally {
            // 性能监控：结束会话
            session.end();
        }
    }

    /**
     * 示例：内容浏览流程
     * 展示如何组合使用书签管理器、广告拦截器和图片助手模块
     */
    public void contentBrowsingExample(String contentUrl, String contentTitle) {
        Log.d(TAG, "Starting content browsing example");

        try {
            // 1. 检查是否已收藏
            boolean isBookmarked = bookmarkManager.isBookmarked(contentUrl);
            if (isBookmarked) {
                analyticsManager.trackEvent("content_view", "bookmarked");
            } else {
                analyticsManager.trackEvent("content_view", "new");
            }

            // 2. 广告拦截：为WebView启用广告拦截
            adBlocker.enableAdBlocking();

            // 3. 图片优化：预加载内容中的图片
            imageHelper.preloadImages(contentUrl);

            // 4. 书签管理：更新访问统计
            bookmarkManager.recordVisit(contentUrl);

            // 5. 性能监控：监控内容加载性能
            performanceMonitor.monitorOperation("content_loading", () -> {
                loadContent(contentUrl);
                return null;
            });

            Log.i(TAG, "Content browsing completed");

        } catch (Exception e) {
            crashHandler.logException(e, "Content browsing failed");
            analyticsManager.trackEvent("content_error", e.getClass().getSimpleName());
        }
    }

    /**
     * 示例：网络请求流程
     * 展示如何组合使用代理选择器、安全管理器和性能监控模块
     */
    public void networkRequestExample(String apiUrl) {
        Log.d(TAG, "Starting network request example");

        // 1. 代理选择：选择最优代理
        ProxyConfig bestProxy = proxySelector.selectBestProxy();
        if (bestProxy != null) {
            analyticsManager.trackEvent("proxy_selected", bestProxy.getType());
        }

        // 2. 安全管理：创建安全的HTTP客户端
        OkHttpClient secureClient = securityManager.createSecureHttpClient(bestProxy);

        // 3. 性能监控：监控网络请求
        performanceMonitor.monitorOperation("network_request", () -> {
            // 执行网络请求
            return executeNetworkRequest(secureClient, apiUrl);
        }, result -> {
            // 记录网络请求性能
            analyticsManager.trackEvent("network_performance",
                "duration=" + result.getDuration() + "ms");
        });
    }

    /**
     * 示例：内存优化流程
     * 展示如何组合使用内存管理器和性能监控模块
     */
    public void memoryOptimizationExample() {
        Log.d(TAG, "Starting memory optimization example");

        // 1. 获取当前内存状态
        MemoryInfo memoryInfo = memoryManager.getMemoryInfo();
        Log.d(TAG, "Current memory usage: " + memoryInfo.getUsedPercentage() + "%");

        // 2. 设置内存监听器
        memoryManager.setMemoryListener(new MemoryListener() {
            @Override
            public void onMemoryLow(MemoryInfo info) {
                // 内存不足时执行优化
                analyticsManager.trackEvent("memory_low", "used=" + info.getUsedPercentage() + "%");

                // 清理图片缓存
                imageHelper.clearCache();

                // 压缩内存使用
                memoryManager.trimMemory(ComponentCallbacks2.TRIM_MEMORY_MODERATE);
            }

            @Override
            public void onMemoryCritical(MemoryInfo info) {
                // 内存严重不足
                analyticsManager.trackEvent("memory_critical", "used=" + info.getUsedPercentage() + "%");

                // 执行深度清理
                memoryManager.forceGC();
                adBlocker.clearCache();
                proxySelector.clearCache();
            }
        });

        // 3. 定期检查内存泄漏
        memoryManager.scheduleLeakDetection();
    }

    /**
     * 示例：安全检查流程
     * 展示如何使用安全管理器进行全面的安全检查
     */
    public void securityCheckExample() {
        Log.d(TAG, "Starting security check example");

        // 1. 执行设备安全检查
        SecurityCheckResult checkResult = securityManager.performSecurityCheck();

        // 2. 记录安全检查结果
        analyticsManager.trackEvent("security_check",
            "rooted=" + checkResult.isDeviceRooted() +
            ",debuggable=" + checkResult.isDebuggable() +
            ",tampered=" + checkResult.isAppTampered());

        // 3. 根据检查结果执行相应措施
        if (checkResult.isDeviceRooted()) {
            // 设备已Root，启用额外安全措施
            securityManager.enableExtraSecurity();
            analyticsManager.trackEvent("security_extra_enabled", "root_detected");
        }

        if (checkResult.isDebuggable()) {
            // 应用可调试，记录警告
            crashHandler.logWarning("App is debuggable");
        }
    }

    /**
     * 示例：数据同步流程
     * 展示如何组合使用所有相关模块进行数据同步
     */
    public void dataSyncExample() {
        Log.d(TAG, "Starting data sync example");

        // 1. 性能监控：开始同步会话
        PerformanceMonitor.PerformanceSession syncSession =
            performanceMonitor.startSession("data_sync");

        try {
            // 2. 网络检查
            if (!networkManager.isNetworkAvailable(context)) {
                analyticsManager.trackEvent("sync_failed", "no_network");
                return;
            }

            // 3. 安全验证
            if (!securityManager.validateSession()) {
                analyticsManager.trackEvent("sync_failed", "invalid_session");
                return;
            }

            // 4. 数据同步
            boolean syncSuccess = performDataSync();

            if (syncSuccess) {
                analyticsManager.trackEvent("sync_success", "full_sync");

                // 5. 更新本地数据
                updateLocalData();

                // 6. 清理缓存
                memoryManager.trimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);

            } else {
                analyticsManager.trackEvent("sync_failed", "sync_error");
            }

        } catch (Exception e) {
            crashHandler.logException(e, "Data sync failed");
            analyticsManager.trackEvent("sync_error", e.getClass().getSimpleName());

        } finally {
            syncSession.end();
        }
    }

    /**
     * 模拟登录操作
     */
    private boolean performLogin(String username, String password) {
        // 这里是模拟的登录逻辑
        // 实际项目中应该调用真实的登录API
        try {
            Thread.sleep(1000); // 模拟网络延迟
            return username.equals("demo") && password.equals("password");
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * 模拟内容加载
     */
    private void loadContent(String url) {
        // 这里是模拟的内容加载逻辑
        try {
            Thread.sleep(500); // 模拟加载延迟
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    /**
     * 模拟网络请求
     */
    private Object executeNetworkRequest(OkHttpClient client, String url) {
        // 这里是模拟的网络请求逻辑
        try {
            Thread.sleep(300); // 模拟网络延迟
            return "response_data";
        } catch (InterruptedException e) {
            return null;
        }
    }

    /**
     * 模拟数据同步
     */
    private boolean performDataSync() {
        // 这里是模拟的数据同步逻辑
        try {
            Thread.sleep(2000); // 模拟同步延迟
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * 更新本地数据
     */
    private void updateLocalData() {
        // 这里是模拟的本地数据更新逻辑
        // 实际项目中应该更新数据库和缓存
        Log.d(TAG, "Local data updated");
    }

    /**
     * 清理所有模块资源
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up all modules");

        try {
            analyticsManager.cleanup();
            crashHandler.cleanup();
            passwordManager.cleanup();
            bookmarkManager.cleanup();
            adBlocker.cleanup();
            imageHelper.cleanup();
            proxySelector.cleanup();
            performanceMonitor.cleanup();
            memoryManager.cleanup();
            securityManager.cleanup();

        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
}

// 注意：上述代码中的一些类和方法可能需要根据实际的模块API进行调整
// 这只是一个演示性的集成示例
