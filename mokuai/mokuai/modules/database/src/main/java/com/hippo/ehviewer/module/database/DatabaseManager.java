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

package com.hippo.ehviewer.module.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.module.database.dao.DaoMaster;
import com.hippo.ehviewer.module.database.dao.DaoSession;
import com.hippo.ehviewer.module.database.migration.MigrationHelper;

import org.greenrobot.greendao.database.Database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * 数据库管理器
 * 负责数据库的初始化、升级、备份恢复等操作
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class DatabaseManager {

    private static final String TAG = DatabaseManager.class.getSimpleName();

    // 单例模式实现
    private static volatile DatabaseManager instance;

    // 数据库相关
    private DaoMaster daoMaster;
    private DaoSession daoSession;
    private SQLiteDatabase sqliteDatabase;

    // 配置
    private DatabaseConfig config;

    // 上下文
    private Context context;

    /**
     * 获取DatabaseManager实例
     *
     * @param context Android上下文
     * @return DatabaseManager实例
     */
    public static DatabaseManager getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager(context.getApplicationContext());
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
    private DatabaseManager(@NonNull Context context) {
        this.context = context;
        this.config = new DatabaseConfig(); // 默认配置
        initDatabase();
    }

    /**
     * 初始化数据库
     */
    private void initDatabase() {
        try {
            // 创建数据库帮助类
            DatabaseHelper helper = new DatabaseHelper(
                context,
                config.getDatabaseName(),
                config.getDatabaseVersion()
            );

            // 获取数据库实例
            sqliteDatabase = helper.getWritableDatabase();

            // 配置数据库
            configureDatabase(sqliteDatabase);

            // 创建GreenDAO Master和Session
            daoMaster = new DaoMaster(sqliteDatabase);
            daoSession = daoMaster.newSession();

            Log.i(TAG, "Database initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * 配置数据库参数
     */
    private void configureDatabase(SQLiteDatabase database) {
        // 启用WAL模式提高并发性能
        if (config.isEnableWAL()) {
            database.enableWriteAheadLogging();
        }

        // 启用外键约束
        if (config.isEnableForeignKeys()) {
            database.execSQL("PRAGMA foreign_keys = ON;");
        }

        // 设置缓存大小
        if (config.getCacheSize() > 0) {
            database.execSQL("PRAGMA cache_size = " + config.getCacheSize() + ";");
        }

        // 设置同步模式
        database.execSQL("PRAGMA synchronous = NORMAL;");

        // 设置临时存储
        database.execSQL("PRAGMA temp_store = MEMORY;");
    }

    /**
     * 获取DAO会话
     *
     * @return DaoSession实例
     */
    public DaoSession getDaoSession() {
        return daoSession;
    }

    /**
     * 获取指定类型的DAO
     *
     * @param daoClass DAO类
     * @param <T> DAO类型
     * @return DAO实例
     */
    @SuppressWarnings("unchecked")
    public <T> T getDao(@NonNull Class<T> daoClass) {
        if (daoSession == null) {
            throw new IllegalStateException("Database not initialized");
        }

        // 这里需要根据实际的DAO类来返回对应的DAO实例
        // 这是一个简化的实现，实际项目中需要更完善的DAO管理

        if (daoClass.getSimpleName().equals("DownloadInfoDao")) {
            return (T) daoSession.getDownloadInfoDao();
        } else if (daoClass.getSimpleName().equals("HistoryDao")) {
            return (T) daoSession.getHistoryDao();
        } else if (daoClass.getSimpleName().equals("LocalFavoritesDao")) {
            return (T) daoSession.getLocalFavoritesDao();
        }

        throw new IllegalArgumentException("Unknown DAO class: " + daoClass.getName());
    }

    /**
     * 执行数据库事务
     *
     * @param runnable 事务执行代码
     */
    public void runInTransaction(@NonNull Runnable runnable) {
        if (daoSession == null) {
            throw new IllegalStateException("Database not initialized");
        }

        daoSession.runInTx(runnable);
    }

    /**
     * 备份数据库
     *
     * @param backupPath 备份文件路径
     * @return true备份成功，false备份失败
     */
    public boolean backup(@NonNull String backupPath) {
        if (sqliteDatabase == null) {
            Log.e(TAG, "Database not initialized");
            return false;
        }

        try {
            // 关闭所有连接
            daoSession.clear();

            // 执行备份
            File backupFile = new File(backupPath);
            backupFile.getParentFile().mkdirs();

            try (FileInputStream inputStream = new FileInputStream(sqliteDatabase.getPath());
                 FileOutputStream outputStream = new FileOutputStream(backupFile)) {

                FileChannel inputChannel = inputStream.getChannel();
                FileChannel outputChannel = outputStream.getChannel();

                outputChannel.transferFrom(inputChannel, 0, inputChannel.size());

                Log.i(TAG, "Database backup completed: " + backupPath);
                return true;
            }

        } catch (IOException e) {
            Log.e(TAG, "Database backup failed", e);
            return false;
        }
    }

    /**
     * 恢复数据库
     *
     * @param backupPath 备份文件路径
     * @return true恢复成功，false恢复失败
     */
    public boolean restore(@NonNull String backupPath) {
        File backupFile = new File(backupPath);
        if (!backupFile.exists()) {
            Log.e(TAG, "Backup file not found: " + backupPath);
            return false;
        }

        try {
            // 关闭数据库连接
            close();

            // 复制备份文件到数据库位置
            String dbPath = sqliteDatabase.getPath();
            try (FileInputStream inputStream = new FileInputStream(backupFile);
                 FileOutputStream outputStream = new FileOutputStream(dbPath)) {

                FileChannel inputChannel = inputStream.getChannel();
                FileChannel outputChannel = outputStream.getChannel();

                outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
            }

            // 重新初始化数据库
            initDatabase();

            Log.i(TAG, "Database restore completed: " + backupPath);
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Database restore failed", e);
            return false;
        }
    }

    /**
     * 设置数据库配置
     *
     * @param config 数据库配置
     */
    public void setConfig(@NonNull DatabaseConfig config) {
        this.config = config;
        // 注意：配置更改需要重启应用才能生效
        Log.w(TAG, "Database config changed, restart required for changes to take effect");
    }

    /**
     * 获取当前配置
     *
     * @return 数据库配置
     */
    public DatabaseConfig getConfig() {
        return config;
    }

    /**
     * 清理数据库缓存
     */
    public void cleanup() {
        if (daoSession != null) {
            daoSession.clear();
        }

        if (sqliteDatabase != null) {
            // 清理临时文件和缓存
            sqliteDatabase.execSQL("VACUUM;");
        }
    }

    /**
     * 关闭数据库
     */
    public void close() {
        try {
            if (daoSession != null) {
                daoSession.clear();
                daoSession = null;
            }

            if (sqliteDatabase != null && sqliteDatabase.isOpen()) {
                sqliteDatabase.close();
                sqliteDatabase = null;
            }

            if (daoMaster != null) {
                daoMaster = null;
            }

            Log.i(TAG, "Database closed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error closing database", e);
        }
    }
}
