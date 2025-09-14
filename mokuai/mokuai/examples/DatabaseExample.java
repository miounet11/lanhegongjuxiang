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
import com.hippo.ehviewer.module.database.DatabaseConfig;

import java.util.List;

/**
 * 数据库模块使用示例
 * 演示如何在项目中使用数据库模块进行数据操作
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class DatabaseExample {

    private static final String TAG = DatabaseExample.class.getSimpleName();

    private final Context context;
    private final DatabaseManager databaseManager;

    public DatabaseExample(Context context) {
        this.context = context;
        this.databaseManager = DatabaseManager.getInstance(context);

        // 配置数据库
        configureDatabase();
    }

    /**
     * 配置数据库
     */
    private void configureDatabase() {
        DatabaseConfig config = new DatabaseConfig.Builder()
            .setDatabaseName("example.db")
            .setDatabaseVersion(1)
            .enableWAL(true)
            .enableForeignKeys(true)
            .setCacheSize(10 * 1024 * 1024) // 10MB
            .build();

        databaseManager.setConfig(config);
    }

    /**
     * 插入数据示例
     */
    public void insertDataExample() {
        Log.d(TAG, "Executing insert data example");

        try {
            databaseManager.runInTransaction(() -> {
                // 创建示例数据
                DownloadInfo info1 = createSampleDownloadInfo(1, "Sample Gallery 1");
                DownloadInfo info2 = createSampleDownloadInfo(2, "Sample Gallery 2");

                // 插入数据
                DownloadInfoDao dao = databaseManager.getDao(DownloadInfoDao.class);
                dao.insert(info1);
                dao.insert(info2);

                Log.i(TAG, "Data inserted successfully");
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to insert data", e);
        }
    }

    /**
     * 查询数据示例
     */
    public void queryDataExample() {
        Log.d(TAG, "Executing query data example");

        try {
            DownloadInfoDao dao = databaseManager.getDao(DownloadInfoDao.class);

            // 查询所有数据
            List<DownloadInfo> allData = dao.loadAll();
            Log.i(TAG, "Total records: " + allData.size());

            // 条件查询
            List<DownloadInfo> finishedDownloads = dao.queryBuilder()
                .where(DownloadInfoDao.Properties.State.eq(DownloadInfo.STATE_FINISH))
                .list();
            Log.i(TAG, "Finished downloads: " + finishedDownloads.size());

            // 模糊查询
            List<DownloadInfo> searchResults = dao.queryBuilder()
                .where(DownloadInfoDao.Properties.Title.like("%Sample%"))
                .list();
            Log.i(TAG, "Search results: " + searchResults.size());

            // 分页查询
            List<DownloadInfo> pageResults = dao.queryBuilder()
                .orderDesc(DownloadInfoDao.Properties.Time)
                .offset(0)
                .limit(10)
                .list();
            Log.i(TAG, "Page results: " + pageResults.size());

        } catch (Exception e) {
            Log.e(TAG, "Failed to query data", e);
        }
    }

    /**
     * 更新数据示例
     */
    public void updateDataExample() {
        Log.d(TAG, "Executing update data example");

        try {
            databaseManager.runInTransaction(() -> {
                DownloadInfoDao dao = databaseManager.getDao(DownloadInfoDao.class);

                // 查找要更新的数据
                DownloadInfo info = dao.load(1L);
                if (info != null) {
                    // 更新数据
                    info.setState(DownloadInfo.STATE_FINISH);
                    info.setTitle(info.getTitle() + " (Updated)");
                    dao.update(info);

                    Log.i(TAG, "Data updated successfully");
                } else {
                    Log.w(TAG, "Data not found for update");
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to update data", e);
        }
    }

    /**
     * 删除数据示例
     */
    public void deleteDataExample() {
        Log.d(TAG, "Executing delete data example");

        try {
            databaseManager.runInTransaction(() -> {
                DownloadInfoDao dao = databaseManager.getDao(DownloadInfoDao.class);

                // 删除指定记录
                dao.deleteByKey(2L);

                // 删除符合条件的所有记录
                DeleteQuery<DownloadInfo> deleteQuery = dao.queryBuilder()
                    .where(DownloadInfoDao.Properties.State.eq(DownloadInfo.STATE_CANCELLED))
                    .buildDelete();
                deleteQuery.executeDeleteWithoutDetaching();

                Log.i(TAG, "Data deleted successfully");
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to delete data", e);
        }
    }

    /**
     * 复杂查询示例
     */
    public void complexQueryExample() {
        Log.d(TAG, "Executing complex query example");

        try {
            DownloadInfoDao dao = databaseManager.getDao(DownloadInfoDao.class);

            // 复杂条件查询
            List<DownloadInfo> results = dao.queryBuilder()
                .where(
                    dao.queryBuilder().and(
                        DownloadInfoDao.Properties.State.eq(DownloadInfo.STATE_DOWNLOADING),
                        DownloadInfoDao.Properties.Speed.gt(0),
                        DownloadInfoDao.Properties.Time.lt(System.currentTimeMillis())
                    )
                )
                .orderDesc(DownloadInfoDao.Properties.Speed)
                .list();

            Log.i(TAG, "Complex query results: " + results.size());

            // 统计查询
            long totalCount = dao.count();
            long finishedCount = dao.queryBuilder()
                .where(DownloadInfoDao.Properties.State.eq(DownloadInfo.STATE_FINISH))
                .count();

            Log.i(TAG, "Total: " + totalCount + ", Finished: " + finishedCount);

        } catch (Exception e) {
            Log.e(TAG, "Failed to execute complex query", e);
        }
    }

    /**
     * 批量操作示例
     */
    public void batchOperationExample() {
        Log.d(TAG, "Executing batch operation example");

        try {
            databaseManager.runInTransaction(() -> {
                DownloadInfoDao dao = databaseManager.getDao(DownloadInfoDao.class);

                // 批量插入
                List<DownloadInfo> batchData = createBatchSampleData(100);
                dao.insertInTx(batchData);

                Log.i(TAG, "Batch insert completed");

                // 批量更新
                List<DownloadInfo> pendingUpdates = dao.queryBuilder()
                    .where(DownloadInfoDao.Properties.State.eq(DownloadInfo.STATE_WAIT))
                    .list();

                for (DownloadInfo info : pendingUpdates) {
                    info.setState(DownloadInfo.STATE_DOWNLOADING);
                }

                dao.updateInTx(pendingUpdates);
                Log.i(TAG, "Batch update completed");
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to execute batch operation", e);
        }
    }

    /**
     * 数据库备份示例
     */
    public void backupExample() {
        Log.d(TAG, "Executing backup example");

        try {
            String backupPath = context.getExternalFilesDir(null) + "/backup/example_backup.db";

            boolean success = databaseManager.backup(backupPath);
            if (success) {
                Log.i(TAG, "Database backup successful: " + backupPath);
            } else {
                Log.e(TAG, "Database backup failed");
            }

        } catch (Exception e) {
            Log.e(TAG, "Backup operation failed", e);
        }
    }

    /**
     * 数据库恢复示例
     */
    public void restoreExample() {
        Log.d(TAG, "Executing restore example");

        try {
            String backupPath = context.getExternalFilesDir(null) + "/backup/example_backup.db";

            boolean success = databaseManager.restore(backupPath);
            if (success) {
                Log.i(TAG, "Database restore successful");
            } else {
                Log.e(TAG, "Database restore failed");
            }

        } catch (Exception e) {
            Log.e(TAG, "Restore operation failed", e);
        }
    }

    /**
     * 数据监听示例
     */
    public void dataObservationExample() {
        Log.d(TAG, "Executing data observation example");

        try {
            // 注意：这是一个简化的示例，实际项目中可能需要使用LiveData或RxJava

            DownloadInfoDao dao = databaseManager.getDao(DownloadInfoDao.class);

            // 模拟数据监听
            new Thread(() -> {
                while (true) {
                    try {
                        // 定期检查数据变化
                        long count = dao.count();
                        Log.d(TAG, "Current data count: " + count);

                        Thread.sleep(5000); // 5秒检查一次

                    } catch (InterruptedException e) {
                        Log.w(TAG, "Data observation interrupted", e);
                        break;
                    }
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Failed to setup data observation", e);
        }
    }

    /**
     * 创建示例下载信息
     */
    private DownloadInfo createSampleDownloadInfo(long gid, String title) {
        DownloadInfo info = new DownloadInfo();
        info.setGid(gid);
        info.setTitle(title);
        info.setState(DownloadInfo.STATE_WAIT);
        info.setTime(System.currentTimeMillis());
        info.setSpeed(0);
        info.setRead(0);
        info.setTotal(0);
        return info;
    }

    /**
     * 创建批量示例数据
     */
    private List<DownloadInfo> createBatchSampleData(int count) {
        List<DownloadInfo> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            data.add(createSampleDownloadInfo(1000L + i, "Batch Gallery " + i));
        }
        return data;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        databaseManager.cleanup();
    }

    /**
     * 关闭数据库
     */
    public void close() {
        databaseManager.close();
    }
}
