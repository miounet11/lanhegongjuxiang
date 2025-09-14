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
 * 网络请求回调接口
 *
 * @param <T> 响应数据类型
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface INetworkCallback<T> {

    /**
     * 请求成功回调
     *
     * @param result 请求结果
     */
    void onSuccess(T result);

    /**
     * 请求失败回调
     *
     * @param error 错误信息
     */
    void onFailure(Exception error);

    /**
     * 请求取消回调
     */
    void onCancel();

    /**
     * 进度更新回调
     *
     * @param progress 进度值 (0-100)
     * @param message 进度消息
     */
    default void onProgress(int progress, String message) {
        // 默认空实现
    }
}
