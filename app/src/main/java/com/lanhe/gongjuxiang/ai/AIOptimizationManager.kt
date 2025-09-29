package com.lanhe.gongjuxiang.ai

import android.content.Context
import android.util.Log
import com.lanhe.gongjuxiang.utils.SystemMonitorHelper
import com.lanhe.gongjuxiang.utils.BatteryHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * AI智能优化管理器
 * 使用LLM进行智能系统分析和优化建议
 */
class AIOptimizationManager(private val context: Context) {

    companion object {
        private const val TAG = "AIOptimizationManager"
        private const val API_URL = "https://ttkk.inping.com/v1/chat/completions"
        private const val API_KEY = "sk-K3BXOY5MnB85PEmoMapZKJSRuSMGPSqIUhUvbNkrC443iZ9W"
        private const val MODEL = "grok-3"

        // AI功能类型
        const val FEATURE_SYSTEM_ANALYSIS = "system_analysis"
        const val FEATURE_PERFORMANCE_OPTIMIZE = "performance_optimize"
        const val FEATURE_BATTERY_OPTIMIZE = "battery_optimize"
        const val FEATURE_MEMORY_OPTIMIZE = "memory_optimize"
        const val FEATURE_NETWORK_OPTIMIZE = "network_optimize"
        const val FEATURE_APP_RECOMMEND = "app_recommend"
        const val FEATURE_SECURITY_CHECK = "security_check"
        const val FEATURE_USAGE_ANALYSIS = "usage_analysis"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val systemMonitor = SystemMonitorHelper(context)
    private val batteryHelper = BatteryHelper(context)

    data class OptimizationSuggestion(
        val title: String,
        val description: String,
        val priority: Priority,
        val impact: String,
        val actions: List<String>,
        val estimatedImprovement: String
    )

    enum class Priority {
        HIGH, MEDIUM, LOW
    }

    data class SystemStatus(
        val cpuUsage: Float,
        val memoryUsage: Float,
        val storageUsage: Float,
        val batteryLevel: Int,
        val batteryTemperature: Float,
        val networkType: String,
        val runningApps: Int,
        val uptime: Long
    )

    /**
     * 获取当前系统状态
     */
    suspend fun getCurrentSystemStatus(): SystemStatus = withContext(Dispatchers.IO) {
        val cpuUsage = systemMonitor.getCpuUsage()
        val memoryUsage = systemMonitor.getMemoryUsage().percent.toFloat()
        val storageUsage = systemMonitor.getStorageUsage().percent.toFloat()
        val batteryInfo = batteryHelper.getBatteryInfo()
        // Get network type from ConnectivityManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val networkType = when {
            capabilities == null -> "无网络"
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "蜂窝网络"
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> "以太网"
            else -> "其他"
        }

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningApps = activityManager.runningAppProcesses?.size ?: 0

        SystemStatus(
            cpuUsage = cpuUsage,
            memoryUsage = memoryUsage,
            storageUsage = storageUsage,
            batteryLevel = batteryInfo.level,
            batteryTemperature = batteryInfo.temperature,
            networkType = networkType,
            runningApps = runningApps,
            uptime = android.os.SystemClock.elapsedRealtime()
        )
    }

    /**
     * 进行AI智能分析
     */
    suspend fun performAIAnalysis(feature: String = FEATURE_SYSTEM_ANALYSIS): List<OptimizationSuggestion> {
        return withContext(Dispatchers.IO) {
            try {
                val systemStatus = getCurrentSystemStatus()
                val prompt = buildAnalysisPrompt(feature, systemStatus)
                val response = callLLMAPI(prompt)
                parseAIResponse(response)
            } catch (e: Exception) {
                Log.e(TAG, "AI analysis failed", e)
                getFallbackSuggestions(feature)
            }
        }
    }

    /**
     * 构建分析提示词
     */
    private fun buildAnalysisPrompt(feature: String, status: SystemStatus): String {
        val basePrompt = """
            作为Android系统优化专家，请分析以下系统状态并提供优化建议：

            系统状态：
            - CPU使用率: ${status.cpuUsage}%
            - 内存使用率: ${status.memoryUsage}%
            - 存储使用率: ${status.storageUsage}%
            - 电池电量: ${status.batteryLevel}%
            - 电池温度: ${status.batteryTemperature}°C
            - 网络类型: ${status.networkType}
            - 运行应用数: ${status.runningApps}
            - 系统运行时间: ${status.uptime / 1000 / 60}分钟

            请提供JSON格式的优化建议，格式如下：
            {
                "suggestions": [
                    {
                        "title": "建议标题",
                        "description": "详细描述",
                        "priority": "HIGH/MEDIUM/LOW",
                        "impact": "预期影响",
                        "actions": ["具体操作1", "具体操作2"],
                        "estimatedImprovement": "预估改善"
                    }
                ]
            }
        """.trimIndent()

        return when (feature) {
            FEATURE_PERFORMANCE_OPTIMIZE -> "$basePrompt\n重点关注性能优化。"
            FEATURE_BATTERY_OPTIMIZE -> "$basePrompt\n重点关注电池优化。"
            FEATURE_MEMORY_OPTIMIZE -> "$basePrompt\n重点关注内存优化。"
            FEATURE_NETWORK_OPTIMIZE -> "$basePrompt\n重点关注网络优化。"
            FEATURE_SECURITY_CHECK -> "$basePrompt\n重点关注安全检查。"
            else -> basePrompt
        }
    }

    /**
     * 调用LLM API
     */
    private suspend fun callLLMAPI(prompt: String): String = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "你是一个专业的Android系统优化助手，精通系统性能调优。")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.7)
            put("max_tokens", 1000)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }

                val responseBody = response.body?.string() ?: ""
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.getJSONArray("choices")

                if (choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    val message = firstChoice.getJSONObject("message")
                    return@withContext message.getString("content")
                }

                throw IOException("No response from AI")
            }
        } catch (e: Exception) {
            Log.e(TAG, "LLM API call failed", e)
            throw e
        }
    }

    /**
     * 解析AI响应
     */
    private suspend fun parseAIResponse(response: String): List<OptimizationSuggestion> {
        return try {
            // 尝试从响应中提取JSON
            val jsonStart = response.indexOf("{")
            val jsonEnd = response.lastIndexOf("}") + 1

            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val jsonStr = response.substring(jsonStart, jsonEnd)
                val json = JSONObject(jsonStr)
                val suggestions = json.getJSONArray("suggestions")

                val result = mutableListOf<OptimizationSuggestion>()

                for (i in 0 until suggestions.length()) {
                    val suggestion = suggestions.getJSONObject(i)
                    val actions = mutableListOf<String>()

                    val actionsArray = suggestion.optJSONArray("actions")
                    if (actionsArray != null) {
                        for (j in 0 until actionsArray.length()) {
                            actions.add(actionsArray.getString(j))
                        }
                    }

                    result.add(
                        OptimizationSuggestion(
                            title = suggestion.optString("title", "优化建议"),
                            description = suggestion.optString("description", ""),
                            priority = Priority.valueOf(suggestion.optString("priority", "MEDIUM")),
                            impact = suggestion.optString("impact", ""),
                            actions = actions,
                            estimatedImprovement = suggestion.optString("estimatedImprovement", "")
                        )
                    )
                }

                return result
            }

            // 如果无法解析JSON，返回基于文本的建议
            getFallbackSuggestionsFromText(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI response", e)
            emptyList()
        }
    }

    /**
     * 从纯文本响应生成建议
     */
    private suspend fun getFallbackSuggestionsFromText(text: String): List<OptimizationSuggestion> {
        val lines = text.split("\n").filter { it.isNotBlank() }
        val suggestions = mutableListOf<OptimizationSuggestion>()

        lines.forEach { line ->
            if (line.contains("建议") || line.contains("优化")) {
                suggestions.add(
                    OptimizationSuggestion(
                        title = line.take(50),
                        description = line,
                        priority = Priority.MEDIUM,
                        impact = "可能改善系统性能",
                        actions = listOf("执行AI建议的优化"),
                        estimatedImprovement = "视具体情况而定"
                    )
                )
            }
        }

        return suggestions.ifEmpty { getFallbackSuggestions(FEATURE_SYSTEM_ANALYSIS) }
    }

    /**
     * 获取备用建议（当AI服务不可用时）
     */
    private suspend fun getFallbackSuggestions(feature: String): List<OptimizationSuggestion> {
        val systemStatus = try {
            getCurrentSystemStatus()
        } catch (e: Exception) {
            null
        }

        val suggestions = mutableListOf<OptimizationSuggestion>()

        // 基于系统状态生成智能建议
        systemStatus?.let { status ->
            // CPU优化建议
            if (status.cpuUsage > 80) {
                suggestions.add(
                    OptimizationSuggestion(
                        title = "CPU负载过高",
                        description = "检测到CPU使用率达到${status.cpuUsage.toInt()}%，建议关闭不必要的后台应用",
                        priority = Priority.HIGH,
                        impact = "降低CPU负载，提升系统响应速度",
                        actions = listOf(
                            "关闭后台应用",
                            "清理进程缓存",
                            "禁用不必要的系统动画"
                        ),
                        estimatedImprovement = "CPU使用率降低20-30%"
                    )
                )
            }

            // 内存优化建议
            if (status.memoryUsage > 75) {
                suggestions.add(
                    OptimizationSuggestion(
                        title = "内存使用率偏高",
                        description = "当前内存使用率${status.memoryUsage.toInt()}%，建议释放内存",
                        priority = Priority.HIGH,
                        impact = "释放内存，提升应用运行流畅度",
                        actions = listOf(
                            "清理应用缓存",
                            "结束不常用的后台进程",
                            "重启部分系统服务"
                        ),
                        estimatedImprovement = "可释放15-25%内存"
                    )
                )
            }

            // 电池优化建议
            if (status.batteryTemperature > 35) {
                suggestions.add(
                    OptimizationSuggestion(
                        title = "电池温度偏高",
                        description = "电池温度达到${status.batteryTemperature}°C，建议降低设备负载",
                        priority = Priority.HIGH,
                        impact = "保护电池健康，延长电池寿命",
                        actions = listOf(
                            "降低屏幕亮度",
                            "关闭高耗电应用",
                            "开启省电模式"
                        ),
                        estimatedImprovement = "降低温度5-10°C"
                    )
                )
            }

            // 存储优化建议
            if (status.storageUsage > 85) {
                suggestions.add(
                    OptimizationSuggestion(
                        title = "存储空间不足",
                        description = "存储使用率达到${status.storageUsage.toInt()}%，建议清理存储",
                        priority = Priority.MEDIUM,
                        impact = "释放存储空间，提升系统性能",
                        actions = listOf(
                            "清理应用缓存",
                            "删除临时文件",
                            "移除不常用应用"
                        ),
                        estimatedImprovement = "可释放10-20%存储空间"
                    )
                )
            }
        }

        // 如果没有特定问题，提供通用建议
        if (suggestions.isEmpty()) {
            suggestions.add(
                OptimizationSuggestion(
                    title = "系统运行良好",
                    description = "当前系统状态正常，建议定期维护",
                    priority = Priority.LOW,
                    impact = "保持系统最佳状态",
                    actions = listOf(
                        "定期清理缓存",
                        "更新系统和应用",
                        "监控系统资源使用"
                    ),
                    estimatedImprovement = "持续优化系统性能"
                )
            )
        }

        return suggestions
    }

    /**
     * 执行优化建议
     */
    suspend fun executeOptimization(suggestion: OptimizationSuggestion): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Executing optimization: ${suggestion.title}")

                // 根据建议的操作执行相应的优化
                suggestion.actions.forEach { action ->
                    when {
                        action.contains("清理") || action.contains("缓存") -> {
                            cleanCache()
                        }
                        action.contains("关闭") || action.contains("结束") -> {
                            killBackgroundProcesses()
                        }
                        action.contains("省电") -> {
                            enablePowerSaving()
                        }
                        action.contains("降低亮度") -> {
                            reduceBrightness()
                        }
                    }
                }

                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to execute optimization", e)
                false
            }
        }
    }

    private fun cleanCache() {
        // 实现缓存清理逻辑
        context.cacheDir.deleteRecursively()
        context.externalCacheDir?.deleteRecursively()
    }

    private fun killBackgroundProcesses() {
        // 实现后台进程清理逻辑
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        activityManager.runningAppProcesses?.forEach { process ->
            if (process.importance >= android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                activityManager.killBackgroundProcesses(process.processName)
            }
        }
    }

    private fun enablePowerSaving() {
        // 实现省电模式逻辑
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable power saving", e)
        }
    }

    private fun reduceBrightness() {
        // 实现降低亮度逻辑
        try {
            android.provider.Settings.System.putInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                50 // 设置为较低亮度
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reduce brightness", e)
        }
    }
}