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

import androidx.annotation.NonNull;

/**
 * 数据库配置类
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class DatabaseConfig {

    // 默认配置常量
    private static final String DEFAULT_DATABASE_NAME = "ehviewer.db";
    private static final int DEFAULT_DATABASE_VERSION = 1;
    private static final int DEFAULT_MAX_CONNECTION_POOL_SIZE = 5;
    private static final boolean DEFAULT_ENABLE_WAL = true;
    private static final boolean DEFAULT_ENABLE_FOREIGN_KEYS = true;
    private static final long DEFAULT_CACHE_SIZE = 10 * 1024 * 1024; // 10MB

    // 配置属性
    private String databaseName;
    private int databaseVersion;
    private int maxConnectionPoolSize;
    private boolean enableWAL;
    private boolean enableForeignKeys;
    private long cacheSize;

    /**
     * 默认构造函数，使用默认配置
     */
    public DatabaseConfig() {
        this.databaseName = DEFAULT_DATABASE_NAME;
        this.databaseVersion = DEFAULT_DATABASE_VERSION;
        this.maxConnectionPoolSize = DEFAULT_MAX_CONNECTION_POOL_SIZE;
        this.enableWAL = DEFAULT_ENABLE_WAL;
        this.enableForeignKeys = DEFAULT_ENABLE_FOREIGN_KEYS;
        this.cacheSize = DEFAULT_CACHE_SIZE;
    }

    /**
     * 私有构造函数，用于Builder模式
     */
    private DatabaseConfig(Builder builder) {
        this.databaseName = builder.databaseName;
        this.databaseVersion = builder.databaseVersion;
        this.maxConnectionPoolSize = builder.maxConnectionPoolSize;
        this.enableWAL = builder.enableWAL;
        this.enableForeignKeys = builder.enableForeignKeys;
        this.cacheSize = builder.cacheSize;
    }

    // Getter方法
    public String getDatabaseName() {
        return databaseName;
    }

    public int getDatabaseVersion() {
        return databaseVersion;
    }

    public int getMaxConnectionPoolSize() {
        return maxConnectionPoolSize;
    }

    public boolean isEnableWAL() {
        return enableWAL;
    }

    public boolean isEnableForeignKeys() {
        return enableForeignKeys;
    }

    public long getCacheSize() {
        return cacheSize;
    }

    /**
     * Builder模式构建DatabaseConfig
     */
    public static class Builder {
        private String databaseName = DEFAULT_DATABASE_NAME;
        private int databaseVersion = DEFAULT_DATABASE_VERSION;
        private int maxConnectionPoolSize = DEFAULT_MAX_CONNECTION_POOL_SIZE;
        private boolean enableWAL = DEFAULT_ENABLE_WAL;
        private boolean enableForeignKeys = DEFAULT_ENABLE_FOREIGN_KEYS;
        private long cacheSize = DEFAULT_CACHE_SIZE;

        /**
         * 设置数据库名称
         *
         * @param databaseName 数据库文件名
         * @return Builder实例
         */
        public Builder setDatabaseName(@NonNull String databaseName) {
            if (databaseName.trim().isEmpty()) {
                throw new IllegalArgumentException("Database name cannot be empty");
            }
            this.databaseName = databaseName;
            return this;
        }

        /**
         * 设置数据库版本
         *
         * @param databaseVersion 数据库版本号
         * @return Builder实例
         */
        public Builder setDatabaseVersion(int databaseVersion) {
            if (databaseVersion <= 0) {
                throw new IllegalArgumentException("Database version must be positive");
            }
            this.databaseVersion = databaseVersion;
            return this;
        }

        /**
         * 设置最大连接池大小
         *
         * @param maxConnectionPoolSize 最大连接池大小
         * @return Builder实例
         */
        public Builder setMaxConnectionPoolSize(int maxConnectionPoolSize) {
            if (maxConnectionPoolSize <= 0) {
                throw new IllegalArgumentException("Max connection pool size must be positive");
            }
            this.maxConnectionPoolSize = maxConnectionPoolSize;
            return this;
        }

        /**
         * 设置是否启用WAL模式
         *
         * @param enableWAL true启用WAL，false禁用WAL
         * @return Builder实例
         */
        public Builder enableWAL(boolean enableWAL) {
            this.enableWAL = enableWAL;
            return this;
        }

        /**
         * 设置是否启用外键约束
         *
         * @param enableForeignKeys true启用外键，false禁用外键
         * @return Builder实例
         */
        public Builder enableForeignKeys(boolean enableForeignKeys) {
            this.enableForeignKeys = enableForeignKeys;
            return this;
        }

        /**
         * 设置缓存大小
         *
         * @param cacheSize 缓存大小(字节)
         * @return Builder实例
         */
        public Builder setCacheSize(long cacheSize) {
            if (cacheSize <= 0) {
                throw new IllegalArgumentException("Cache size must be positive");
            }
            this.cacheSize = cacheSize;
            return this;
        }

        /**
         * 构建DatabaseConfig实例
         *
         * @return DatabaseConfig实例
         */
        public DatabaseConfig build() {
            return new DatabaseConfig(this);
        }
    }
}
