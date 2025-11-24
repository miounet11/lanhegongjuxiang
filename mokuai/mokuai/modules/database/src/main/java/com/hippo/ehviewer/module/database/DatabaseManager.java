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

// GreenDAO imports commented out - Using Room instead for modern Android development
// import com.hippo.ehviewer.module.database.dao.DaoMaster;
// import com.hippo.ehviewer.module.database.dao.DaoSession;
// import com.hippo.ehviewer.module.database.migration.MigrationHelper;
// import org.greenrobot.greendao.database.Database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * 数据库管理器 - 使用Room而非GreenDAO
 * 负责数据库的初始化、升级、备份恢复等操作
 *
 * @author LanHe Team
 * @version 2.0.0 (Migrated from GreenDAO to Room)
 * @since 2024-01-01
 */
public class DatabaseManager {

    private static final String TAG = DatabaseManager.class.getSimpleName();

    // 单例模式实现
    private static volatile DatabaseManager instance;

    // 数据库相关
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
     * 初始化数据库 (使用Room)
     */
    private void initDatabase() {
        try {
            Log.i(TAG, "数据库初始化开始");
            // Room数据库将通过Room DAOs自动初始化
            // 此处保留用于兼容性和未来扩展
            Log.i(TAG, "数据库初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "数据库初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取DaoSession (已弃用 - 使用Room代替)
     * @return null - Room不使用DaoSession概念
     * @deprecated 使用Room DAOs代替
     */
    @Deprecated
    @Nullable
    public Object getDaoSession() {
        Log.w(TAG, "getDaoSession() deprecated - Use Room DAOs instead");
        return null;
    }

    /**
     * 备份数据库
     *
     * @param backupPath 备份路径
     * @return 是否备份成功
     */
    public boolean backupDatabase(String backupPath) {
        try {
            File backupDir = new File(backupPath).getParentFile();
            if (backupDir != null && !backupDir.exists()) {
                backupDir.mkdirs();
            }

            File sourceDatabase = context.getDatabasePath(config.getDatabaseName());
            File backupDatabase = new File(backupPath);

            if (sourceDatabase.exists()) {
                copyFile(sourceDatabase, backupDatabase);
                Log.i(TAG, "数据库备份成功: " + backupPath);
                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "数据库备份失败: " + e.getMessage(), e);
        }
        return false;
    }

    /**
     * 恢复数据库
     *
     * @param backupPath 备份路径
     * @return 是否恢复成功
     */
    public boolean restoreDatabase(String backupPath) {
        try {
            File backupDatabase = new File(backupPath);
            File targetDatabase = context.getDatabasePath(config.getDatabaseName());

            if (backupDatabase.exists()) {
                copyFile(backupDatabase, targetDatabase);
                Log.i(TAG, "数据库恢复成功");
                return true;
            } else {
                Log.w(TAG, "备份文件不存在: " + backupPath);
            }
        } catch (IOException e) {
            Log.e(TAG, "数据库恢复失败: " + e.getMessage(), e);
        }
        return false;
    }

    /**
     * 复制文件
     *
     * @param source 源文件
     * @param dest 目标文件
     * @throws IOException IO异常
     */
    private void copyFile(File source, File dest) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(dest);
             FileChannel sourceChannel = fis.getChannel();
             FileChannel destChannel = fos.getChannel()) {

            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }

    /**
     * 关闭数据库
     */
    public void closeDatabase() {
        try {
            if (sqliteDatabase != null && sqliteDatabase.isOpen()) {
                sqliteDatabase.close();
                Log.i(TAG, "数据库已关闭");
            }
        } catch (Exception e) {
            Log.e(TAG, "关闭数据库时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 获取数据库配置
     *
     * @return DatabaseConfig对象
     */
    public DatabaseConfig getConfig() {
        return config;
    }

    /**
     * 设置数据库配置
     *
     * @param config DatabaseConfig对象
     */
    public void setConfig(DatabaseConfig config) {
        this.config = config;
    }

    /**
     * 获取应用上下文
     *
     * @return Context对象
     */
    public Context getContext() {
        return context;
    }
}
