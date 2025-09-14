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

import com.hippo.ehviewer.module.database.DatabaseManager;
import com.hippo.ehviewer.module.network.NetworkManager;
import com.hippo.ehviewer.module.network.interfaces.INetworkCallback;
import com.hippo.ehviewer.module.settings.SettingsManager;
import com.hippo.ehviewer.module.notification.NotificationManager;

import org.json.JSONObject;

import java.util.List;

/**
 * 模块集成使用示例
 * 演示如何在实际应用中集成和使用所有蓝河工具箱模块
 * 这个示例展示了一个完整的用户数据同步流程
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class ModuleIntegrationExample {

    private static final String TAG = ModuleIntegrationExample.class.getSimpleName();

    // 模块管理器
    private final NetworkManager networkManager;
    private final DatabaseManager databaseManager;
    private final SettingsManager settingsManager;
    private final NotificationManager notificationManager;

    // 上下文
    private final Context context;

    public ModuleIntegrationExample(Context context) {
        this.context = context;

        // 初始化所有模块
        this.networkManager = NetworkManager.getInstance(context);
        this.databaseManager = DatabaseManager.getInstance(context);
        this.settingsManager = SettingsManager.getInstance(context);
        this.notificationManager = NotificationManager.getInstance(context);

        Log.i(TAG, "All modules initialized successfully");
    }

    /**
     * 完整的数据同步流程示例
     * 这个方法演示了如何将网络、数据库、设置、通知模块组合使用
     */
    public void performDataSync() {
        Log.i(TAG, "Starting data synchronization process");

        // 1. 检查网络状态
        if (!networkManager.isNetworkAvailable(context)) {
            Log.w(TAG, "Network not available, sync cancelled");
            showNotification("同步失败", "网络不可用，请检查网络连接");
            return;
        }

        // 2. 检查同步设置
        boolean autoSync = settingsManager.getBoolean("auto_sync_enabled", true);
        if (!autoSync) {
            Log.i(TAG, "Auto sync disabled, skipping sync");
            return;
        }

        // 3. 显示同步开始通知
        showNotification("开始同步", "正在同步用户数据...");

        // 4. 从服务器获取数据
        fetchUserDataFromServer();

        Log.d(TAG, "Data synchronization process initiated");
    }

    /**
     * 从服务器获取用户数据
     */
    private void fetchUserDataFromServer() {
        String apiUrl = settingsManager.getString("api_base_url", "https://api.example.com");
        String userId = settingsManager.getString("user_id", "");

        if (userId.isEmpty()) {
            Log.w(TAG, "User ID not found, cannot sync data");
            showNotification("同步失败", "用户未登录");
            return;
        }

        String url = apiUrl + "/user/" + userId + "/data";

        networkManager.get(url)
            .enqueue(new INetworkCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    Log.i(TAG, "Successfully fetched user data from server");
                    processServerData(result);
                }

                @Override
                public void onFailure(Exception error) {
                    Log.e(TAG, "Failed to fetch user data from server", error);
                    handleSyncError("网络请求失败: " + error.getMessage());
                }

                @Override
                public void onCancel() {
                    Log.w(TAG, "User data fetch cancelled");
                    handleSyncError("同步已取消");
                }

                @Override
                public void onProgress(int progress, String message) {
                    Log.d(TAG, "Fetch progress: " + progress + "% - " + message);
                    updateSyncProgress(progress, message);
                }
            });
    }

    /**
     * 处理服务器返回的数据
     */
    private void processServerData(String jsonData) {
        try {
            // 解析JSON数据
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray downloadsArray = jsonObject.getJSONArray("downloads");

            // 在事务中处理数据
            databaseManager.runInTransaction(() -> {
                // 清除旧数据
                clearOldData();

                // 插入新数据
                insertNewData(downloadsArray);

                // 更新同步时间戳
                updateLastSyncTime();

                Log.i(TAG, "Server data processed successfully");
            });

            // 同步完成
            onSyncCompleted();

        } catch (Exception e) {
            Log.e(TAG, "Failed to process server data", e);
            handleSyncError("数据处理失败: " + e.getMessage());
        }
    }

    /**
     * 清除旧数据
     */
    private void clearOldData() {
        Log.d(TAG, "Clearing old data");

        // 这里应该实现清除旧数据的逻辑
        // 例如：删除过期的下载记录、清理缓存等
    }

    /**
     * 插入新数据
     */
    private void insertNewData(JSONArray downloadsArray) {
        Log.d(TAG, "Inserting new data");

        // 这里应该实现插入新数据的逻辑
        // 例如：解析downloadsArray并插入到数据库中
    }

    /**
     * 更新最后同步时间
     */
    private void updateLastSyncTime() {
        long currentTime = System.currentTimeMillis();
        settingsManager.putLong("last_sync_time", currentTime);
        Log.d(TAG, "Last sync time updated: " + currentTime);
    }

    /**
     * 同步完成处理
     */
    private void onSyncCompleted() {
        Log.i(TAG, "Data synchronization completed successfully");

        // 更新设置
        settingsManager.putBoolean("sync_in_progress", false);
        settingsManager.putBoolean("last_sync_successful", true);

        // 显示成功通知
        showNotification("同步完成", "用户数据同步成功");

        // 可以在这里触发其他业务逻辑
        // 例如：刷新UI、更新缓存等
    }

    /**
     * 处理同步错误
     */
    private void handleSyncError(String errorMessage) {
        Log.e(TAG, "Sync error: " + errorMessage);

        // 更新设置
        settingsManager.putBoolean("sync_in_progress", false);
        settingsManager.putBoolean("last_sync_successful", false);

        // 显示错误通知
        showNotification("同步失败", errorMessage);

        // 可以在这里实现重试逻辑
        scheduleRetry();
    }

    /**
     * 更新同步进度
     */
    private void updateSyncProgress(int progress, String message) {
        // 更新进度通知
        showProgressNotification("同步中", message, progress);
    }

    /**
     * 显示通知
     */
    private void showNotification(String title, String message) {
        notificationManager.showNotification(title, message);
    }

    /**
     * 显示进度通知
     */
    private void showProgressNotification(String title, String message, int progress) {
        notificationManager.showProgressNotification(title, message, progress);
    }

    /**
     * 安排重试
     */
    private void scheduleRetry() {
        // 实现重试逻辑
        // 例如：延迟一定时间后重新尝试同步
        Log.d(TAG, "Scheduling retry...");
    }

    /**
     * 获取同步状态
     */
    public SyncStatus getSyncStatus() {
        boolean inProgress = settingsManager.getBoolean("sync_in_progress", false);
        boolean lastSuccessful = settingsManager.getBoolean("last_sync_successful", false);
        long lastSyncTime = settingsManager.getLong("last_sync_time", 0);

        return new SyncStatus(inProgress, lastSuccessful, lastSyncTime);
    }

    /**
     * 手动触发同步
     */
    public void manualSync() {
        Log.i(TAG, "Manual sync triggered");

        // 检查是否已有同步在进行
        if (getSyncStatus().isInProgress()) {
            Log.w(TAG, "Sync already in progress, ignoring manual trigger");
            showNotification("同步中", "已有同步任务在进行中");
            return;
        }

        // 标记同步开始
        settingsManager.putBoolean("sync_in_progress", true);

        // 开始同步
        performDataSync();
    }

    /**
     * 取消同步
     */
    public void cancelSync() {
        Log.i(TAG, "Sync cancellation requested");

        // 这里应该实现取消同步的逻辑
        // 例如：取消网络请求、标记同步状态等

        settingsManager.putBoolean("sync_in_progress", false);
        showNotification("同步取消", "用户取消了同步操作");
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up resources");

        networkManager.cleanup();
        databaseManager.cleanup();
        notificationManager.cleanup();
    }

    /**
     * 同步状态类
     */
    public static class SyncStatus {
        private final boolean inProgress;
        private final boolean lastSuccessful;
        private final long lastSyncTime;

        public SyncStatus(boolean inProgress, boolean lastSuccessful, long lastSyncTime) {
            this.inProgress = inProgress;
            this.lastSuccessful = lastSuccessful;
            this.lastSyncTime = lastSyncTime;
        }

        public boolean isInProgress() {
            return inProgress;
        }

        public boolean isLastSuccessful() {
            return lastSuccessful;
        }

        public long getLastSyncTime() {
            return lastSyncTime;
        }

        @Override
        public String toString() {
            return "SyncStatus{" +
                    "inProgress=" + inProgress +
                    ", lastSuccessful=" + lastSuccessful +
                    ", lastSyncTime=" + lastSyncTime +
                    '}';
        }
    }
}
