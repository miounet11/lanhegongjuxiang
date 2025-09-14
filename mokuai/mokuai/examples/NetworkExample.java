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

import com.hippo.ehviewer.module.network.NetworkManager;
import com.hippo.ehviewer.module.network.NetworkConfig;
import com.hippo.ehviewer.module.network.interfaces.INetworkCallback;

/**
 * 网络模块使用示例
 * 演示如何在项目中使用网络模块进行HTTP请求
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class NetworkExample {

    private static final String TAG = NetworkExample.class.getSimpleName();

    private final Context context;
    private final NetworkManager networkManager;

    public NetworkExample(Context context) {
        this.context = context;
        this.networkManager = NetworkManager.getInstance(context);

        // 配置网络模块
        configureNetwork();
    }

    /**
     * 配置网络模块
     */
    private void configureNetwork() {
        NetworkConfig config = new NetworkConfig.Builder()
            .setConnectTimeout(30000L)      // 30秒连接超时
            .setReadTimeout(60000L)         // 60秒读取超时
            .setWriteTimeout(60000L)        // 60秒写入超时
            .setRetryCount(3)               // 重试3次
            .enableCookie(true)             // 启用Cookie
            .setUserAgent("LanHe Browser/1.0 (Chromium)")
            .build();

        networkManager.setConfig(config);
    }

    /**
     * GET请求示例
     */
    public void getRequestExample() {
        Log.d(TAG, "Executing GET request example");

        String url = "https://httpbin.org/get";

        networkManager.get(url)
            .enqueue(new INetworkCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    Log.i(TAG, "GET request success: " + result);
                    // 处理成功结果
                    handleGetResponse(result);
                }

                @Override
                public void onFailure(Exception error) {
                    Log.e(TAG, "GET request failed", error);
                    // 处理错误
                    handleError(error);
                }

                @Override
                public void onCancel() {
                    Log.w(TAG, "GET request cancelled");
                }

                @Override
                public void onProgress(int progress, String message) {
                    Log.d(TAG, "GET progress: " + progress + "% - " + message);
                }
            });
    }

    /**
     * POST请求示例
     */
    public void postRequestExample() {
        Log.d(TAG, "Executing POST request example");

        String url = "https://httpbin.org/post";
        String jsonData = "{\"key\": \"value\", \"timestamp\": " + System.currentTimeMillis() + "}";

        networkManager.post(url, jsonData)
            .enqueue(new INetworkCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    Log.i(TAG, "POST request success: " + result);
                    handlePostResponse(result);
                }

                @Override
                public void onFailure(Exception error) {
                    Log.e(TAG, "POST request failed", error);
                    handleError(error);
                }

                @Override
                public void onCancel() {
                    Log.w(TAG, "POST request cancelled");
                }
            });
    }

    /**
     * 文件下载示例
     */
    public void downloadExample() {
        Log.d(TAG, "Executing download example");

        String downloadUrl = "https://httpbin.org/image/png";
        File destination = new File(context.getExternalFilesDir(null), "downloaded_image.png");

        networkManager.download(downloadUrl, destination, new INetworkCallback<File>() {
            @Override
            public void onSuccess(File result) {
                Log.i(TAG, "Download success: " + result.getAbsolutePath());
                handleDownloadSuccess(result);
            }

            @Override
            public void onFailure(Exception error) {
                Log.e(TAG, "Download failed", error);
                handleError(error);
            }

            @Override
            public void onCancel() {
                Log.w(TAG, "Download cancelled");
            }

            @Override
            public void onProgress(int progress, String message) {
                Log.d(TAG, "Download progress: " + progress + "% - " + message);
                updateDownloadProgress(progress, message);
            }
        });
    }

    /**
     * 并发请求示例
     */
    public void concurrentRequestsExample() {
        Log.d(TAG, "Executing concurrent requests example");

        String[] urls = {
            "https://httpbin.org/delay/1",
            "https://httpbin.org/delay/2",
            "https://httpbin.org/delay/3"
        };

        for (String url : urls) {
            networkManager.get(url)
                .enqueue(new INetworkCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        Log.i(TAG, "Concurrent request success for: " + url);
                    }

                    @Override
                    public void onFailure(Exception error) {
                        Log.e(TAG, "Concurrent request failed for: " + url, error);
                    }
                });
        }
    }

    /**
     * 带认证的请求示例
     */
    public void authenticatedRequestExample() {
        Log.d(TAG, "Executing authenticated request example");

        String url = "https://api.example.com/protected";

        // 注意：实际项目中需要实现认证头添加逻辑
        // 这里只是示例

        networkManager.get(url)
            .enqueue(new INetworkCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    Log.i(TAG, "Authenticated request success");
                    handleAuthenticatedResponse(result);
                }

                @Override
                public void onFailure(Exception error) {
                    Log.e(TAG, "Authenticated request failed", error);
                    handleAuthError(error);
                }
            });
    }

    /**
     * 处理GET响应
     */
    private void handleGetResponse(String response) {
        // 解析响应数据
        try {
            // 使用JSON解析库解析响应
            Log.d(TAG, "Handling GET response: " + response);
            // 处理业务逻辑...

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse GET response", e);
        }
    }

    /**
     * 处理POST响应
     */
    private void handlePostResponse(String response) {
        // 处理POST请求的响应
        Log.d(TAG, "Handling POST response: " + response);
        // 处理业务逻辑...
    }

    /**
     * 处理下载成功
     */
    private void handleDownloadSuccess(File file) {
        // 处理下载完成的文件
        Log.d(TAG, "Download completed: " + file.getName());
        // 可以在这里进行文件处理、重命名等操作
    }

    /**
     * 处理认证响应
     */
    private void handleAuthenticatedResponse(String response) {
        // 处理需要认证的API响应
        Log.d(TAG, "Authenticated response received");
        // 处理认证相关逻辑...
    }

    /**
     * 处理错误
     */
    private void handleError(Exception error) {
        // 统一的错误处理
        Log.e(TAG, "Network error occurred", error);

        // 可以根据错误类型进行不同处理
        if (error instanceof java.net.UnknownHostException) {
            // 网络不可用
            showNetworkError();
        } else if (error instanceof java.net.SocketTimeoutException) {
            // 请求超时
            showTimeoutError();
        } else {
            // 其他错误
            showGenericError();
        }
    }

    /**
     * 处理认证错误
     */
    private void handleAuthError(Exception error) {
        // 处理认证失败的情况
        Log.e(TAG, "Authentication failed", error);
        // 可以在这里触发重新登录流程
    }

    /**
     * 更新下载进度
     */
    private void updateDownloadProgress(int progress, String message) {
        // 更新UI进度
        Log.d(TAG, "Download progress updated: " + progress + "%");
        // 可以在这里更新进度条、显示下载信息等
    }

    /**
     * 显示网络错误
     */
    private void showNetworkError() {
        // 显示网络错误提示
        Log.w(TAG, "Network is unavailable");
    }

    /**
     * 显示超时错误
     */
    private void showTimeoutError() {
        // 显示超时错误提示
        Log.w(TAG, "Request timed out");
    }

    /**
     * 显示通用错误
     */
    private void showGenericError() {
        // 显示通用错误提示
        Log.w(TAG, "An error occurred");
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        networkManager.cleanup();
    }
}
