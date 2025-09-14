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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.module.network.exception.NetworkException;
import com.hippo.ehviewer.module.network.interfaces.INetworkCallback;
import com.hippo.ehviewer.module.network.interfaces.INetworkConfig;
import com.hippo.ehviewer.module.network.utils.NetworkUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 网络管理器
 * 负责管理所有网络请求，提供统一的网络接口
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class NetworkManager {

    private static final String TAG = NetworkManager.class.getSimpleName();

    // 单例模式实现
    private static volatile NetworkManager instance;

    // OkHttp客户端
    private OkHttpClient httpClient;

    // 配置
    private INetworkConfig config;

    // 主线程Handler
    private Handler mainHandler;

    // 上下文
    private Context context;

    /**
     * 获取NetworkManager实例
     *
     * @param context Android上下文
     * @return NetworkManager实例
     */
    public static NetworkManager getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (NetworkManager.class) {
                if (instance == null) {
                    instance = new NetworkManager(context.getApplicationContext());
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
    private NetworkManager(@NonNull Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.config = new NetworkConfig(); // 默认配置
        initHttpClient();
    }

    /**
     * 初始化OkHttp客户端
     */
    private void initHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(config.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getReadTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(config.getWriteTimeout(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true);

        // 设置User-Agent
        if (config.getUserAgent() != null) {
            builder.addInterceptor(chain -> {
                Request original = chain.request();
                Request request = original.newBuilder()
                        .header("User-Agent", config.getUserAgent())
                        .build();
                return chain.proceed(request);
            });
        }

        // 添加日志拦截器（调试模式）
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            builder.addInterceptor(new okhttp3.logging.HttpLoggingInterceptor()
                    .setLevel(okhttp3.logging.HttpLoggingInterceptor.Level.BODY));
        }

        this.httpClient = builder.build();
    }

    /**
     * GET请求
     *
     * @param url 请求URL
     * @return Call对象，可用于取消请求
     */
    public Call get(@NonNull String url) {
        return get(url, null);
    }

    /**
     * GET请求
     *
     * @param url 请求URL
     * @param callback 回调接口
     * @return Call对象，可用于取消请求
     */
    public Call get(@NonNull String url, @Nullable INetworkCallback<String> callback) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Call call = httpClient.newCall(request);

        if (callback != null) {
            call.enqueue(new NetworkCallbackAdapter(callback));
        }

        return call;
    }

    /**
     * POST请求
     *
     * @param url 请求URL
     * @param body 请求体
     * @return Call对象，可用于取消请求
     */
    public Call post(@NonNull String url, @NonNull String body) {
        return post(url, body, null);
    }

    /**
     * POST请求
     *
     * @param url 请求URL
     * @param body 请求体
     * @param callback 回调接口
     * @return Call对象，可用于取消请求
     */
    public Call post(@NonNull String url, @NonNull String body, @Nullable INetworkCallback<String> callback) {
        RequestBody requestBody = RequestBody.create(body, MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        Call call = httpClient.newCall(request);

        if (callback != null) {
            call.enqueue(new NetworkCallbackAdapter(callback));
        }

        return call;
    }

    /**
     * PUT请求
     *
     * @param url 请求URL
     * @param body 请求体
     * @return Call对象，可用于取消请求
     */
    public Call put(@NonNull String url, @NonNull String body) {
        return put(url, body, null);
    }

    /**
     * PUT请求
     *
     * @param url 请求URL
     * @param body 请求体
     * @param callback 回调接口
     * @return Call对象，可用于取消请求
     */
    public Call put(@NonNull String url, @NonNull String body, @Nullable INetworkCallback<String> callback) {
        RequestBody requestBody = RequestBody.create(body, MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .put(requestBody)
                .build();

        Call call = httpClient.newCall(request);

        if (callback != null) {
            call.enqueue(new NetworkCallbackAdapter(callback));
        }

        return call;
    }

    /**
     * DELETE请求
     *
     * @param url 请求URL
     * @return Call对象，可用于取消请求
     */
    public Call delete(@NonNull String url) {
        return delete(url, null);
    }

    /**
     * DELETE请求
     *
     * @param url 请求URL
     * @param callback 回调接口
     * @return Call对象，可用于取消请求
     */
    public Call delete(@NonNull String url, @Nullable INetworkCallback<String> callback) {
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        Call call = httpClient.newCall(request);

        if (callback != null) {
            call.enqueue(new NetworkCallbackAdapter(callback));
        }

        return call;
    }

    /**
     * 文件下载
     *
     * @param url 下载URL
     * @param destination 目标文件
     * @param callback 回调接口
     * @return Call对象，可用于取消请求
     */
    public Call download(@NonNull String url, @NonNull File destination, @Nullable INetworkCallback<File> callback) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Call call = httpClient.newCall(request);

        if (callback != null) {
            call.enqueue(new DownloadCallbackAdapter(destination, callback));
        }

        return call;
    }

    /**
     * 设置网络配置
     *
     * @param config 网络配置
     */
    public void setConfig(@NonNull INetworkConfig config) {
        this.config = config;
        // 重新初始化HttpClient以应用新配置
        initHttpClient();
    }

    /**
     * 获取当前配置
     *
     * @return 网络配置
     */
    public INetworkConfig getConfig() {
        return config;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        // 清理连接池
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }

    /**
     * 网络回调适配器
     */
    private class NetworkCallbackAdapter implements Callback {

        private final INetworkCallback<String> callback;

        public NetworkCallbackAdapter(INetworkCallback<String> callback) {
            this.callback = callback;
        }

        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
            mainHandler.post(() -> {
                NetworkException exception = NetworkUtils.createNetworkException(e);
                callback.onFailure(exception);
            });
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            mainHandler.post(() -> {
                try {
                    if (response.isSuccessful()) {
                        String body = response.body() != null ? response.body().string() : "";
                        callback.onSuccess(body);
                    } else {
                        NetworkException exception = new NetworkException(
                                NetworkException.ERROR_SERVER,
                                "HTTP " + response.code() + ": " + response.message()
                        );
                        callback.onFailure(exception);
                    }
                } catch (IOException e) {
                    NetworkException exception = NetworkUtils.createNetworkException(e);
                    callback.onFailure(exception);
                } finally {
                    response.close();
                }
            });
        }
    }

    /**
     * 下载回调适配器
     */
    private class DownloadCallbackAdapter implements Callback {

        private final File destination;
        private final INetworkCallback<File> callback;

        public DownloadCallbackAdapter(File destination, INetworkCallback<File> callback) {
            this.destination = destination;
            this.callback = callback;
        }

        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
            mainHandler.post(() -> {
                NetworkException exception = NetworkUtils.createNetworkException(e);
                callback.onFailure(exception);
            });
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            mainHandler.post(() -> {
                try {
                    if (response.isSuccessful()) {
                        // TODO: 实现文件下载逻辑
                        callback.onSuccess(destination);
                    } else {
                        NetworkException exception = new NetworkException(
                                NetworkException.ERROR_SERVER,
                                "HTTP " + response.code() + ": " + response.message()
                        );
                        callback.onFailure(exception);
                    }
                } catch (Exception e) {
                    NetworkException exception = NetworkUtils.createNetworkException(e);
                    callback.onFailure(exception);
                } finally {
                    response.close();
                }
            });
        }
    }
}
