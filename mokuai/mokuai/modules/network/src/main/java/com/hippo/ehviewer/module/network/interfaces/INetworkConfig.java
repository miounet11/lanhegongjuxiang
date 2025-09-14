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

package com.hippo.ehviewer.module.network.interfaces;

/**
 * 网络配置接口
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface INetworkConfig {

    /**
     * 获取连接超时时间
     *
     * @return 连接超时时间(毫秒)
     */
    long getConnectTimeout();

    /**
     * 获取读取超时时间
     *
     * @return 读取超时时间(毫秒)
     */
    long getReadTimeout();

    /**
     * 获取写入超时时间
     *
     * @return 写入超时时间(毫秒)
     */
    long getWriteTimeout();

    /**
     * 获取重试次数
     *
     * @return 重试次数
     */
    int getRetryCount();

    /**
     * 是否启用Cookie
     *
     * @return true启用Cookie，false禁用Cookie
     */
    boolean isCookieEnabled();

    /**
     * 获取User-Agent
     *
     * @return User-Agent字符串
     */
    String getUserAgent();

    /**
     * 获取代理配置
     *
     * @return 代理配置
     */
    ProxyConfig getProxyConfig();

    /**
     * 代理配置类
     */
    class ProxyConfig {
        private final String host;
        private final int port;
        private final String type;

        public ProxyConfig(String host, int port, String type) {
            this.host = host;
            this.port = port;
            this.type = type;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getType() {
            return type;
        }
    }
}
