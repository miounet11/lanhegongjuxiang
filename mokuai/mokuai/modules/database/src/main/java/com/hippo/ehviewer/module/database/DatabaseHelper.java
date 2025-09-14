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
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.hippo.ehviewer.module.database.migration.MigrationHelper;

/**
 * 数据库帮助类
 * 负责数据库的创建、升级和版本管理
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = DatabaseHelper.class.getSimpleName();

    private static final int DEFAULT_DATABASE_VERSION = 1;

    private int currentVersion;

    /**
     * 构造函数
     *
     * @param context 上下文
     * @param name 数据库名称
     * @param version 数据库版本
     */
    public DatabaseHelper(@NonNull Context context, @NonNull String name, int version) {
        super(context, name, null, version);
        this.currentVersion = version;
    }

    /**
     * 构造函数（使用默认版本）
     *
     * @param context 上下文
     * @param name 数据库名称
     */
    public DatabaseHelper(@NonNull Context context, @NonNull String name) {
        this(context, name, DEFAULT_DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Creating database tables...");

        try {
            // 创建所有表结构
            createTables(db);

            // 创建索引
            createIndexes(db);

            Log.i(TAG, "Database tables created successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to create database tables", e);
            throw e;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        try {
            // 执行数据库迁移
            MigrationHelper.migrate(db, oldVersion, newVersion);

            Log.i(TAG, "Database upgrade completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to upgrade database", e);
            throw e;
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Downgrading database from version " + oldVersion + " to " + newVersion);

        // 处理版本降级的情况
        // 在生产环境中，通常不建议降级，这里只是记录警告
        // 可以选择删除所有表并重新创建，或者保留数据但标记为不兼容

        Log.w(TAG, "Database downgrade completed (no action taken)");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);

        // 数据库打开时的初始化操作
        Log.d(TAG, "Database opened");

        // 可以在这里执行一些初始化SQL语句
        // 例如设置PRAGMA参数等
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);

        // 数据库配置
        // 启用外键约束
        db.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * 创建所有数据库表
     */
    private void createTables(SQLiteDatabase db) {
        // 下载信息表
        db.execSQL("CREATE TABLE IF NOT EXISTS DOWNLOAD_INFO (" +
                "GID INTEGER PRIMARY KEY NOT NULL," +
                "TOKEN TEXT," +
                "TITLE TEXT," +
                "TITLE_JPN TEXT," +
                "THUMB TEXT," +
                "CATEGORY INTEGER NOT NULL," +
                "POSTED TEXT," +
                "UPLOADER TEXT," +
                "RATING REAL NOT NULL," +
                "SIMPLE_LANGUAGE TEXT," +
                "STATE INTEGER NOT NULL," +
                "LEGACY INTEGER NOT NULL," +
                "TIME INTEGER NOT NULL," +
                "LABEL TEXT," +
                "POSITION INTEGER NOT NULL," +
                "SPEED INTEGER NOT NULL," +
                "READ INTEGER NOT NULL," +
                "TOTAL INTEGER NOT NULL," +
                "FINISHED INTEGER NOT NULL," +
                "STARTED INTEGER NOT NULL," +
                "INVALID INTEGER NOT NULL," +
                "FAVORITE_NAME TEXT," +
                "FAVORITE_SLOT INTEGER NOT NULL);");

        // 历史记录表
        db.execSQL("CREATE TABLE IF NOT EXISTS HISTORY (" +
                "GID INTEGER PRIMARY KEY NOT NULL," +
                "TOKEN TEXT," +
                "TITLE TEXT," +
                "TITLE_JPN TEXT," +
                "THUMB TEXT," +
                "CATEGORY INTEGER NOT NULL," +
                "POSTED TEXT," +
                "UPLOADER TEXT," +
                "RATING REAL NOT NULL," +
                "SIMPLE_LANGUAGE TEXT," +
                "TIME INTEGER NOT NULL," +
                "MODE INTEGER NOT NULL);");

        // 本地收藏表
        db.execSQL("CREATE TABLE IF NOT EXISTS LOCAL_FAVORITES (" +
                "GID INTEGER PRIMARY KEY NOT NULL," +
                "TOKEN TEXT," +
                "TITLE TEXT," +
                "TITLE_JPN TEXT," +
                "THUMB TEXT," +
                "CATEGORY INTEGER NOT NULL," +
                "POSTED TEXT," +
                "UPLOADER TEXT," +
                "RATING REAL NOT NULL," +
                "SIMPLE_LANGUAGE TEXT," +
                "TIME INTEGER NOT NULL," +
                "FAVORITE_NAME TEXT);");

        // 快速搜索表
        db.execSQL("CREATE TABLE IF NOT EXISTS QUICK_SEARCH (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "NAME TEXT NOT NULL," +
                "MODE INTEGER NOT NULL," +
                "CATEGORY INTEGER NOT NULL," +
                "KEYWORD TEXT," +
                "ADVANCE_SEARCH TEXT," +
                "MIN_RATING INTEGER NOT NULL," +
                "MAX_RATING INTEGER NOT NULL," +
                "PAGE_FROM INTEGER NOT NULL," +
                "PAGE_TO INTEGER NOT NULL);");

        // 过滤器表
        db.execSQL("CREATE TABLE IF NOT EXISTS FILTER (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "MODE INTEGER NOT NULL," +
                "TEXT TEXT NOT NULL," +
                "ENABLE INTEGER NOT NULL);");

        // 黑名单表
        db.execSQL("CREATE TABLE IF NOT EXISTS BLACK_LIST (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "BAD_GUY TEXT NOT NULL);");

        Log.d(TAG, "All database tables created");
    }

    /**
     * 创建索引
     */
    private void createIndexes(SQLiteDatabase db) {
        // 下载信息表索引
        db.execSQL("CREATE INDEX IF NOT EXISTS INDEX_DOWNLOAD_INFO_TIME ON DOWNLOAD_INFO(TIME);");
        db.execSQL("CREATE INDEX IF NOT EXISTS INDEX_DOWNLOAD_INFO_STATE ON DOWNLOAD_INFO(STATE);");
        db.execSQL("CREATE INDEX IF NOT EXISTS INDEX_DOWNLOAD_INFO_LABEL ON DOWNLOAD_INFO(LABEL);");

        // 历史记录表索引
        db.execSQL("CREATE INDEX IF NOT EXISTS INDEX_HISTORY_TIME ON HISTORY(TIME);");
        db.execSQL("CREATE INDEX IF NOT EXISTS INDEX_HISTORY_MODE ON HISTORY(MODE);");

        // 本地收藏表索引
        db.execSQL("CREATE INDEX IF NOT EXISTS INDEX_LOCAL_FAVORITES_TIME ON LOCAL_FAVORITES(TIME);");

        Log.d(TAG, "Database indexes created");
    }
}
