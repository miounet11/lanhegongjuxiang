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

package com.hippo.ehviewer.module.database.migration;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * 数据库迁移帮助类
 * 处理数据库版本升级时的表结构变更
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class MigrationHelper {

    private static final String TAG = MigrationHelper.class.getSimpleName();

    /**
     * 执行数据库迁移
     *
     * @param db 数据库实例
     * @param oldVersion 旧版本号
     * @param newVersion 新版本号
     */
    public static void migrate(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Starting database migration from " + oldVersion + " to " + newVersion);

        // 执行逐步迁移
        for (int version = oldVersion + 1; version <= newVersion; version++) {
            migrateToVersion(db, version);
        }

        Log.i(TAG, "Database migration completed");
    }

    /**
     * 迁移到指定版本
     *
     * @param db 数据库实例
     * @param targetVersion 目标版本
     */
    private static void migrateToVersion(SQLiteDatabase db, int targetVersion) {
        Log.d(TAG, "Migrating to version " + targetVersion);

        switch (targetVersion) {
            case 2:
                migrateToVersion2(db);
                break;
            case 3:
                migrateToVersion3(db);
                break;
            case 4:
                migrateToVersion4(db);
                break;
            // 添加更多版本迁移...
            default:
                Log.w(TAG, "No migration defined for version " + targetVersion);
                break;
        }
    }

    /**
     * 迁移到版本2
     * 示例：添加新字段到DOWNLOAD_INFO表
     */
    private static void migrateToVersion2(SQLiteDatabase db) {
        Log.d(TAG, "Migrating to version 2");

        try {
            // 添加新字段
            db.execSQL("ALTER TABLE DOWNLOAD_INFO ADD COLUMN NEW_FIELD TEXT;");

            // 创建新索引
            db.execSQL("CREATE INDEX IF NOT EXISTS INDEX_DOWNLOAD_INFO_NEW_FIELD ON DOWNLOAD_INFO(NEW_FIELD);");

            Log.d(TAG, "Migration to version 2 completed");

        } catch (Exception e) {
            Log.e(TAG, "Migration to version 2 failed", e);
            throw e;
        }
    }

    /**
     * 迁移到版本3
     * 示例：重命名表并添加新表
     */
    private static void migrateToVersion3(SQLiteDatabase db) {
        Log.d(TAG, "Migrating to version 3");

        try {
            // 重命名表
            db.execSQL("ALTER TABLE DOWNLOAD_INFO RENAME TO DOWNLOAD_INFO_OLD;");

            // 创建新表结构
            db.execSQL("CREATE TABLE DOWNLOAD_INFO (" +
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
                    "FAVORITE_SLOT INTEGER NOT NULL," +
                    "DOWNLOAD_PATH TEXT);"); // 新增字段

            // 迁移数据
            db.execSQL("INSERT INTO DOWNLOAD_INFO (" +
                    "GID, TOKEN, TITLE, TITLE_JPN, THUMB, CATEGORY, POSTED, " +
                    "UPLOADER, RATING, SIMPLE_LANGUAGE, STATE, LEGACY, TIME, " +
                    "LABEL, POSITION, SPEED, READ, TOTAL, FINISHED, STARTED, " +
                    "INVALID, FAVORITE_NAME, FAVORITE_SLOT) " +
                    "SELECT GID, TOKEN, TITLE, TITLE_JPN, THUMB, CATEGORY, POSTED, " +
                    "UPLOADER, RATING, SIMPLE_LANGUAGE, STATE, LEGACY, TIME, " +
                    "LABEL, POSITION, SPEED, READ, TOTAL, FINISHED, STARTED, " +
                    "INVALID, FAVORITE_NAME, FAVORITE_SLOT " +
                    "FROM DOWNLOAD_INFO_OLD;");

            // 删除旧表
            db.execSQL("DROP TABLE DOWNLOAD_INFO_OLD;");

            // 重新创建索引
            db.execSQL("CREATE INDEX IF NOT EXISTS INDEX_DOWNLOAD_INFO_TIME ON DOWNLOAD_INFO(TIME);");
            db.execSQL("CREATE INDEX IF NOT EXISTS INDEX_DOWNLOAD_INFO_STATE ON DOWNLOAD_INFO(STATE);");
            db.execSQL("CREATE INDEX IF NOT EXISTS INDEX_DOWNLOAD_INFO_LABEL ON DOWNLOAD_INFO(LABEL);");

            Log.d(TAG, "Migration to version 3 completed");

        } catch (Exception e) {
            Log.e(TAG, "Migration to version 3 failed", e);
            throw e;
        }
    }

    /**
     * 迁移到版本4
     * 示例：添加新表
     */
    private static void migrateToVersion4(SQLiteDatabase db) {
        Log.d(TAG, "Migrating to version 4");

        try {
            // 创建新表
            db.execSQL("CREATE TABLE IF NOT EXISTS DOWNLOAD_STATISTICS (" +
                    "GID INTEGER PRIMARY KEY NOT NULL," +
                    "DOWNLOAD_COUNT INTEGER NOT NULL DEFAULT 0," +
                    "LAST_DOWNLOAD_TIME INTEGER," +
                    "AVERAGE_SPEED INTEGER DEFAULT 0," +
                    "TOTAL_SIZE INTEGER DEFAULT 0);");

            // 创建索引
            db.execSQL("CREATE INDEX IF NOT EXISTS INDEX_DOWNLOAD_STATISTICS_COUNT ON DOWNLOAD_STATISTICS(DOWNLOAD_COUNT);");

            Log.d(TAG, "Migration to version 4 completed");

        } catch (Exception e) {
            Log.e(TAG, "Migration to version 4 failed", e);
            throw e;
        }
    }

    /**
     * 通用表结构修改方法
     * 添加列到现有表
     */
    public static void addColumn(SQLiteDatabase db, String tableName, String columnDefinition) {
        try {
            db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + columnDefinition);
            Log.d(TAG, "Added column to " + tableName + ": " + columnDefinition);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add column to " + tableName, e);
            throw e;
        }
    }

    /**
     * 通用表结构修改方法
     * 创建索引
     */
    public static void createIndex(SQLiteDatabase db, String tableName, String columnName) {
        String indexName = "INDEX_" + tableName + "_" + columnName.replace(",", "_");
        try {
            db.execSQL("CREATE INDEX IF NOT EXISTS " + indexName + " ON " + tableName + "(" + columnName + ")");
            Log.d(TAG, "Created index on " + tableName + "(" + columnName + ")");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create index on " + tableName, e);
            throw e;
        }
    }

    /**
     * 通用表结构修改方法
     * 删除表
     */
    public static void dropTable(SQLiteDatabase db, String tableName) {
        try {
            db.execSQL("DROP TABLE IF EXISTS " + tableName);
            Log.d(TAG, "Dropped table: " + tableName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to drop table: " + tableName, e);
            throw e;
        }
    }
}
