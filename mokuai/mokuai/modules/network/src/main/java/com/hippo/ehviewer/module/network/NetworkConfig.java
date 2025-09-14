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

package com.hippo.ehviewer.module.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.module.network.interfaces.INetworkConfig;

/**
 * 网络配置实现类
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class NetworkConfig implements INetworkConfig {

    // 默认配置常量
    private static final long DEFAULT_CONNECT_TIMEOUT = 30000; // 30秒
    private static final long DEFAULT_READ_TIMEOUT = 60000;    // 60秒
    private static final long DEFAULT_WRITE_TIMEOUT = 60000;   // 60秒
    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final boolean DEFAULT_COOKIE_ENABLED = true;
    private static final String DEFAULT_USER_AGENT = "LanHe Browser/1.0 (Chromium)";

    // 配置属性
    private long connectTimeout;
    private long readTimeout;
    private long writeTimeout;
    private int retryCount;
    private boolean cookieEnabled;
    private String userAgent;
    private ProxyConfig proxyConfig;

    /**
     * 默认构造函数，使用默认配置
     */
    public NetworkConfig() {
        this.connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        this.readTimeout = DEFAULT_READ_TIMEOUT;
        this.writeTimeout = DEFAULT_WRITE_TIMEOUT;
        this.retryCount = DEFAULT_RETRY_COUNT;
        this.cookieEnabled = DEFAULT_COOKIE_ENABLED;
        this.userAgent = DEFAULT_USER_AGENT;
        this.proxyConfig = null;
    }

    /**
     * 私有构造函数，用于Builder模式
     */
    private NetworkConfig(Builder builder) {
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.writeTimeout = builder.writeTimeout;
        this.retryCount = builder.retryCount;
        this.cookieEnabled = builder.cookieEnabled;
        this.userAgent = builder.userAgent;
        this.proxyConfig = builder.proxyConfig;
    }

    @Override
    public long getConnectTimeout() {
        return connectTimeout;
    }

    @Override
    public long getReadTimeout() {
        return readTimeout;
    }

    @Override
    public long getWriteTimeout() {
        return writeTimeout;
    }

    @Override
    public int getRetryCount() {
        return retryCount;
    }

    @Override
    public boolean isCookieEnabled() {
        return cookieEnabled;
    }

    @Override
    public String getUserAgent() {
        return userAgent;
    }

    @Override
    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    /**
     * Builder模式构建NetworkConfig
     */
    public static class Builder {
        private long connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private long readTimeout = DEFAULT_READ_TIMEOUT;
        private long writeTimeout = DEFAULT_WRITE_TIMEOUT;
        private int retryCount = DEFAULT_RETRY_COUNT;
        private boolean cookieEnabled = DEFAULT_COOKIE_ENABLED;
        private String userAgent = DEFAULT_USER_AGENT;
        private ProxyConfig proxyConfig;

        /**
         * 设置连接超时时间
         *
         * @param connectTimeout 连接超时时间(毫秒)
         * @return Builder实例
         */
        public Builder setConnectTimeout(long connectTimeout) {
            if (connectTimeout <= 0) {
                throw new IllegalArgumentException("Connect timeout must be positive");
            }
            this.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * 设置读取超时时间
         *
         * @param readTimeout 读取超时时间(毫秒)
         * @return Builder实例
         */
        public Builder setReadTimeout(long readTimeout) {
            if (readTimeout <= 0) {
                throw new IllegalArgumentException("Read timeout must be positive");
            }
            this.readTimeout = readTimeout;
            return this;
        }

        /**
         * 设置写入超时时间
         *
         * @param writeTimeout 写入超时时间(毫秒)
         * @return Builder实例
         */
        public Builder setWriteTimeout(long writeTimeout) {
            if (writeTimeout <= 0) {
                throw new IllegalArgumentException("Write timeout must be positive");
            }
            this.writeTimeout = writeTimeout;
            return this;
        }

        /**
         * 设置重试次数
         *
         * @param retryCount 重试次数
         * @return Builder实例
         */
        public Builder setRetryCount(int retryCount) {
            if (retryCount < 0) {
                throw new IllegalArgumentException("Retry count must be non-negative");
            }
            this.retryCount = retryCount;
            return this;
        }

        /**
         * 设置是否启用Cookie
         *
         * @param cookieEnabled true启用Cookie，false禁用Cookie
         * @return Builder实例
         */
        public Builder enableCookie(boolean cookieEnabled) {
            this.cookieEnabled = cookieEnabled;
            return this;
        }

        /**
         * 设置User-Agent
         *
         * @param userAgent User-Agent字符串
         * @return Builder实例
         */
        public Builder setUserAgent(@Nullable String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * 设置代理配置
         *
         * @param proxyConfig 代理配置
         * @return Builder实例
         */
        public Builder setProxyConfig(@Nullable ProxyConfig proxyConfig) {
            this.proxyConfig = proxyConfig;
            return this;
        }

        /**
         * 构建NetworkConfig实例
         *
         * @return NetworkConfig实例
         */
        public NetworkConfig build() {
            return new NetworkConfig(this);
        }
    }
}
