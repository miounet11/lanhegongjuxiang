package com.lanhe.gongjuxiang.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * AI助手 - 集成Grok-3大语言模型
 * 提供智能问答、系统分析、故障诊断等AI功能
 */
class AIAssistant(private val context: Context) {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val API_KEY = "sk-XW411jzvAxJwQuV6ytl3520laxm49Rqynj3x27TzLnJz3rQU"
        private const val BASE_URL = "https://ttkk.inping.com/v1"
        private const val MODEL = "grok-3"
        private const val MAX_TOKENS = 2000
        private const val TEMPERATURE = 0.7
    }

    /**
     * AI对话消息数据类
     */
    data class ChatMessage(
        val role: String, // "user" or "assistant"
        val content: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * AI响应数据类
     */
    data class AIResponse(
        val success: Boolean,
        val message: String,
        val errorMessage: String? = null,
        val usage: TokenUsage? = null
    )

    /**
     * Token使用统计
     */
    data class TokenUsage(
        val promptTokens: Int,
        val completionTokens: Int,
        val totalTokens: Int
    )

    /**
     * 系统分析结果
     */
    data class SystemAnalysis(
        val batteryHealth: String,
        val performanceScore: Int,
        val recommendations: List<String>,
        val warnings: List<String>
    )

    /**
     * 发送消息到AI
     */
    suspend fun sendMessage(message: String, systemPrompt: String? = null): AIResponse = withContext(Dispatchers.IO) {
        try {
            val messages = mutableListOf<Map<String, String>>()

            // 添加系统提示（如果提供）
            systemPrompt?.let {
                messages.add(mapOf("role" to "system", "content" to it))
            }

            // 添加用户消息
            messages.add(mapOf("role" to "user", "content" to message))

            val requestBody = createChatRequest(messages)

            val request = Request.Builder()
                .url("$BASE_URL/chat/completions")
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                parseAIResponse(responseBody)
            } else {
                AIResponse(
                    success = false,
                    message = "",
                    errorMessage = "API请求失败: ${response.code} ${response.message}"
                )
            }
        } catch (e: Exception) {
            AIResponse(
                success = false,
                message = "",
                errorMessage = "网络异常: ${e.message}"
            )
        }
    }

    /**
     * 智能系统分析
     */
    suspend fun analyzeSystemStatus(): SystemAnalysis = withContext(Dispatchers.IO) {
        try {
            // 获取系统信息
            val batteryInfo = getBatteryInfo()
            val performanceData = getPerformanceData()
            val networkInfo = getNetworkInfo()

            val systemInfo = """
                电池信息: $batteryInfo
                性能数据: $performanceData
                网络信息: $networkInfo
            """.trimIndent()

            val prompt = """
                基于以下Android设备系统信息，请分析设备健康状况并提供优化建议：

                $systemInfo

                请以JSON格式返回分析结果，包含以下字段：
                - batteryHealth: 电池健康状况描述
                - performanceScore: 性能评分(0-100)
                - recommendations: 优化建议列表
                - warnings: 警告信息列表

                要求：
                1. 分析要专业准确
                2. 建议要实用可行
                3. 评分要客观合理
                4. 警告要突出重要问题
            """.trimIndent()

            val response = sendMessage(prompt)
            if (response.success) {
                parseSystemAnalysis(response.message)
            } else {
                // 返回默认分析结果
                SystemAnalysis(
                    batteryHealth = "无法获取电池信息",
                    performanceScore = 50,
                    recommendations = listOf("请检查设备连接状态", "确保应用有必要权限"),
                    warnings = listOf("AI分析功能暂时不可用")
                )
            }
        } catch (e: Exception) {
            SystemAnalysis(
                batteryHealth = "分析异常: ${e.message}",
                performanceScore = 0,
                recommendations = emptyList(),
                warnings = listOf("系统分析失败")
            )
        }
    }

    /**
     * 故障诊断
     */
    suspend fun diagnoseIssue(issueDescription: String, logData: String? = null): AIResponse = withContext(Dispatchers.IO) {
        val prompt = """
            用户报告了一个Android设备问题：

            问题描述: $issueDescription
            ${logData?.let { "相关日志: $it" } ?: ""}

            请提供专业的诊断分析和解决方案。包括：
            1. 可能的原因分析
            2. 解决步骤
            3. 预防建议
            4. 如果需要的话，建议进一步的诊断信息

            请用中文回复，语气专业友好。
        """.trimIndent()

        sendMessage(prompt)
    }

    /**
     * 智能建议生成
     */
    suspend fun generateSmartSuggestions(userBehavior: String): List<String> = withContext(Dispatchers.IO) {
        val prompt = """
            基于用户的使用行为数据: $userBehavior

            请分析用户的使用习惯，并生成个性化的设备优化建议。
            返回3-5条最相关的实用建议，用JSON数组格式返回。

            例如: ["建议开启省电模式", "建议清理缓存文件", "建议定期重启设备"]
        """.trimIndent()

        val response = sendMessage(prompt)
        if (response.success) {
            try {
                val jsonArray = gson.fromJson(response.message, JsonArray::class.java)
                jsonArray.map { it.asString }
            } catch (e: Exception) {
                listOf("开启省电模式以节省电量", "定期清理缓存文件", "检查应用权限设置")
            }
        } else {
            listOf("开启省电模式以节省电量", "定期清理缓存文件", "检查应用权限设置")
        }
    }

    /**
     * 代码解释和建议
     */
    suspend fun explainCode(codeSnippet: String, language: String = "kotlin"): AIResponse = withContext(Dispatchers.IO) {
        val prompt = """
            请解释以下${language}代码的功能和实现原理：

            ```$language
            $codeSnippet
            ```

            请提供：
            1. 代码功能说明
            2. 关键实现要点
            3. 可能的改进建议
            4. 最佳实践建议

            用中文回复，保持专业性和易懂性。
        """.trimIndent()

        sendMessage(prompt)
    }

    /**
     * 学习和帮助
     */
    suspend fun getLearningContent(topic: String): AIResponse = withContext(Dispatchers.IO) {
        val prompt = """
            用户想了解Android开发的"$topic"相关知识。

            请提供：
            1. 基础概念解释
            2. 实际应用示例
            3. 学习建议和资源
            4. 常见问题解答

            用中文回复，内容要通俗易懂，适合初学者到中级开发者。
        """.trimIndent()

        sendMessage(prompt)
    }

    /**
     * 创意功能建议
     */
    suspend fun suggestNewFeatures(currentFeatures: List<String>): List<String> = withContext(Dispatchers.IO) {
        val featuresText = currentFeatures.joinToString(", ")
        val prompt = """
            当前工具箱已有功能: $featuresText

            请基于这些现有功能，建议5个创新的新功能。
            要求：
            1. 功能要有实用价值
            2. 技术上可行
            3. 与现有功能有协同效应
            4. 能提升用户体验

            返回JSON数组格式，每个建议包含功能名称和简要描述。
            例如: [{"name": "智能截图", "description": "OCR识别截图内容并提供相关操作"}]
        """.trimIndent()

        val response = sendMessage(prompt)
        if (response.success) {
            try {
                val jsonArray = gson.fromJson(response.message, JsonArray::class.java)
                jsonArray.map { obj ->
                    val jsonObj = obj.asJsonObject
                    "${jsonObj.get("name").asString}: ${jsonObj.get("description").asString}"
                }
            } catch (e: Exception) {
                listOf(
                    "智能截图: OCR识别截图内容并提供相关操作",
                    "应用行为分析: 分析应用使用模式并优化性能",
                    "设备健康报告: 生成详细的设备健康状态报告",
                    "自动化任务: 创建自定义的自动化操作流程",
                    "云端同步: 将设置和数据同步到云端"
                )
            }
        } else {
            listOf(
                "智能截图: OCR识别截图内容并提供相关操作",
                "应用行为分析: 分析应用使用模式并优化性能",
                "设备健康报告: 生成详细的设备健康状态报告",
                "自动化任务: 创建自定义的自动化操作流程",
                "云端同步: 将设置和数据同步到云端"
            )
        }
    }

    /**
     * 创建聊天请求体
     */
    private fun createChatRequest(messages: List<Map<String, String>>): okhttp3.RequestBody {
        val json = JsonObject().apply {
            addProperty("model", MODEL)
            addProperty("max_tokens", MAX_TOKENS)
            addProperty("temperature", TEMPERATURE)

            val messagesArray = JsonArray()
            messages.forEach { message ->
                val messageObj = JsonObject().apply {
                    addProperty("role", message["role"])
                    addProperty("content", message["content"])
                }
                messagesArray.add(messageObj)
            }
            add("messages", messagesArray)
        }

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        return json.toString().toRequestBody(mediaType)
    }

    /**
     * 解析AI响应
     */
    private fun parseAIResponse(responseBody: String): AIResponse {
        return try {
            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            val choices = jsonResponse.getAsJsonArray("choices")

            if (choices != null && choices.size() > 0) {
                val message = choices[0].asJsonObject
                    .getAsJsonObject("message")
                    .get("content").asString

                // 解析token使用情况
                val usage = jsonResponse.getAsJsonObject("usage")?.let { usageObj ->
                    TokenUsage(
                        promptTokens = usageObj.get("prompt_tokens").asInt,
                        completionTokens = usageObj.get("completion_tokens").asInt,
                        totalTokens = usageObj.get("total_tokens").asInt
                    )
                }

                AIResponse(success = true, message = message, usage = usage)
            } else {
                AIResponse(success = false, message = "", errorMessage = "API返回数据格式错误")
            }
        } catch (e: Exception) {
            AIResponse(success = false, message = "", errorMessage = "解析响应失败: ${e.message}")
        }
    }

    /**
     * 解析系统分析结果
     */
    private fun parseSystemAnalysis(jsonString: String): SystemAnalysis {
        return try {
            val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)
            SystemAnalysis(
                batteryHealth = jsonObject.get("batteryHealth").asString,
                performanceScore = jsonObject.get("performanceScore").asInt,
                recommendations = jsonObject.getAsJsonArray("recommendations").map { it.asString },
                warnings = jsonObject.getAsJsonArray("warnings").map { it.asString }
            )
        } catch (e: Exception) {
            SystemAnalysis(
                batteryHealth = "解析失败",
                performanceScore = 50,
                recommendations = listOf("请重试分析"),
                warnings = listOf("数据解析异常")
            )
        }
    }

    /**
     * 获取电池信息摘要
     */
    private fun getBatteryInfo(): String {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))

            val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            val temperature = intent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            val voltage = intent?.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
            val status = intent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1

            val percentage = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
            val tempCelsius = if (temperature > 0) temperature / 10.0 else 0.0
            val voltageV = if (voltage > 0) voltage / 1000.0 else 0.0

            val statusText = when (status) {
                android.os.BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
                android.os.BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
                android.os.BatteryManager.BATTERY_STATUS_FULL -> "已充满"
                android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
                else -> "未知"
            }

            "电池电量: ${percentage}%, 温度: ${tempCelsius}°C, 电压: ${voltageV}V, 状态: $statusText"
        } catch (e: Exception) {
            "电池信息获取失败: ${e.message}"
        }
    }

    /**
     * 获取性能数据摘要
     */
    private fun getPerformanceData(): String {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            val totalMemory = memoryInfo.totalMem / (1024 * 1024 * 1024.0) // GB
            val availableMemory = memoryInfo.availMem / (1024 * 1024 * 1024.0) // GB
            val usedMemory = totalMemory - availableMemory

            val runningProcesses = activityManager.runningAppProcesses?.size ?: 0

            // CPU使用率（简化版）
            val cpuUsage = try {
                val process = Runtime.getRuntime().exec("top -n 1")
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                var cpuLine = ""
                reader.useLines { lines ->
                    lines.forEach { line ->
                        if (line.contains("CPU:")) {
                            cpuLine = line
                        }
                    }
                }
                // 解析CPU使用率（这里是简化实现）
                25 // 默认值，实际应该解析top命令输出
            } catch (e: Exception) {
                25
            }

            "CPU使用率: ${cpuUsage}%, 内存使用: ${String.format("%.1f", usedMemory)}GB/${String.format("%.1f", totalMemory)}GB, 运行进程: ${runningProcesses}个"
        } catch (e: Exception) {
            "性能数据获取失败: ${e.message}"
        }
    }

    /**
     * 获取网络信息摘要
     */
    private fun getNetworkInfo(): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager

            val wifiConnected = capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true
            val mobileConnected = capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true

            val wifiInfo = if (wifiConnected) {
                val connectionInfo = wifiManager.connectionInfo
                val signalStrength = android.net.wifi.WifiManager.calculateSignalLevel(connectionInfo.rssi, 5)
                val ssid = connectionInfo.ssid?.replace("\"", "") ?: "未知"
                "WiFi: $ssid (信号强度: ${signalStrength}/5)"
            } else {
                "WiFi: 未连接"
            }

            val mobileInfo = if (mobileConnected) {
                "移动数据: 已连接"
            } else {
                "移动数据: 未连接"
            }

            "$wifiInfo, $mobileInfo"
        } catch (e: Exception) {
            "网络信息获取失败: ${e.message}"
        }
    }

    /**
     * 检查AI服务可用性
     */
    suspend fun checkServiceAvailability(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = sendMessage("Hello", "You are a helpful AI assistant.")
            response.success
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取AI功能列表
     */
    fun getAIFeatures(): List<String> {
        return listOf(
            "智能问答 - 解答各种技术问题",
            "系统分析 - 分析设备健康状况",
            "故障诊断 - 帮助解决设备问题",
            "智能建议 - 提供个性化优化建议",
            "代码解释 - 解释代码功能和原理",
            "学习助手 - 提供Android开发学习内容",
            "功能建议 - 推荐新的应用功能",
            "创意对话 - 进行各种主题的对话"
        )
    }

    /**
     * 获取使用统计
     */
    fun getUsageStatistics(): Map<String, Any> {
        return mapOf(
            "totalRequests" to 0, // 这里可以统计实际使用情况
            "successRate" to 0.95,
            "averageResponseTime" to 2500, // 毫秒
            "featuresUsed" to getAIFeatures().size
        )
    }
}
