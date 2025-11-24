package com.lanhe.mokuai.proxy

import android.content.Context
import android.net.Proxy
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

/**
 * 代理选择器 - 智能代理管理和切换工具
 */
class ProxySelector(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "proxy_selector_prefs"
        private const val KEY_PROXIES = "proxy_list"
        private const val KEY_ACTIVE_PROXY = "active_proxy"
        private const val KEY_RULES = "proxy_rules"
        private const val KEY_STATISTICS = "proxy_statistics"
        private const val KEY_AUTO_SWITCH = "auto_switch_enabled"

        private const val TEST_URL = "https://www.google.com"
        private const val TEST_TIMEOUT = 5000 // 5秒超时
        private const val LATENCY_CHECK_INTERVAL = 60000L // 1分钟检查一次延迟
    }

    private val sharedPrefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 代理缓存
    private val proxyCache = ConcurrentHashMap<String, ProxyConfig>()
    private val latencyCache = ConcurrentHashMap<String, Long>()

    data class ProxyConfig(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val type: ProxyType,
        val host: String,
        val port: Int,
        val username: String? = null,
        val password: String? = null,
        val enabled: Boolean = true,
        val priority: Int = 0,
        val country: String? = null,
        val protocol: ProxyProtocol = ProxyProtocol.HTTP,
        val tags: List<String> = emptyList(),
        val customHeaders: Map<String, String> = emptyMap(),
        val bypassList: List<String> = emptyList()
    )

    enum class ProxyType {
        HTTP,
        HTTPS,
        SOCKS4,
        SOCKS5,
        SHADOWSOCKS,
        VMESS,
        SYSTEM,
        DIRECT
    }

    enum class ProxyProtocol {
        HTTP,
        HTTPS,
        SOCKS,
        SS,
        VMESS
    }

    data class ProxyRule(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val pattern: String,
        val proxyId: String?,
        val action: RuleAction,
        val enabled: Boolean = true,
        val priority: Int = 0
    )

    enum class RuleAction {
        USE_PROXY,      // 使用指定代理
        DIRECT,         // 直连
        REJECT,         // 拒绝连接
        AUTO_SELECT     // 自动选择最佳代理
    }

    data class ProxyStatistics(
        val proxyId: String,
        val totalConnections: Long = 0,
        val successfulConnections: Long = 0,
        val failedConnections: Long = 0,
        val totalBandwidth: Long = 0,
        val averageLatency: Long = 0,
        val lastUsed: Long = 0,
        val uptime: Long = 0
    )

    data class ProxyTestResult(
        val proxyId: String,
        val success: Boolean,
        val latency: Long = -1,
        val speed: Long = -1,
        val errorMessage: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class ProxyGroup(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val proxies: List<String>,
        val strategy: SelectionStrategy,
        val enabled: Boolean = true
    )

    enum class SelectionStrategy {
        ROUND_ROBIN,    // 轮询
        RANDOM,         // 随机
        LEAST_USED,     // 最少使用
        LOWEST_LATENCY, // 最低延迟
        FAILOVER        // 故障转移
    }

    /**
     * 获取所有代理配置
     */
    fun getAllProxies(): List<ProxyConfig> {
        val json = sharedPrefs.getString(KEY_PROXIES, "[]") ?: "[]"
        return parseProxiesFromJson(json)
    }

    /**
     * 添加代理配置
     */
    fun addProxy(proxy: ProxyConfig) {
        val proxies = getAllProxies().toMutableList()
        proxies.add(proxy)
        saveProxies(proxies)
        proxyCache[proxy.id] = proxy
    }

    /**
     * 更新代理配置
     */
    fun updateProxy(proxy: ProxyConfig) {
        val proxies = getAllProxies().toMutableList()
        val index = proxies.indexOfFirst { it.id == proxy.id }
        if (index != -1) {
            proxies[index] = proxy
            saveProxies(proxies)
            proxyCache[proxy.id] = proxy
        }
    }

    /**
     * 删除代理配置
     */
    fun deleteProxy(id: String) {
        val proxies = getAllProxies().toMutableList()
        proxies.removeAll { it.id == id }
        saveProxies(proxies)
        proxyCache.remove(id)

        // 如果删除的是当前活动代理，清除活动代理
        if (getActiveProxyId() == id) {
            setActiveProxy(null)
        }
    }

    /**
     * 设置活动代理
     */
    fun setActiveProxy(proxyId: String?) {
        sharedPrefs.edit().putString(KEY_ACTIVE_PROXY, proxyId).apply()

        if (proxyId != null) {
            updateProxyStatistics(proxyId) { stats ->
                stats.copy(lastUsed = System.currentTimeMillis())
            }
        }
    }

    /**
     * 获取当前活动代理
     */
    fun getActiveProxy(): ProxyConfig? {
        val proxyId = getActiveProxyId() ?: return null
        return getProxy(proxyId)
    }

    /**
     * 获取活动代理ID
     */
    private fun getActiveProxyId(): String? {
        return sharedPrefs.getString(KEY_ACTIVE_PROXY, null)
    }

    /**
     * 获取指定代理
     */
    fun getProxy(id: String): ProxyConfig? {
        return proxyCache[id] ?: getAllProxies().find { it.id == id }
    }

    /**
     * 测试代理连接
     */
    suspend fun testProxy(proxy: ProxyConfig): ProxyTestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            when (proxy.type) {
                ProxyType.HTTP, ProxyType.HTTPS -> testHttpProxy(proxy, startTime)
                ProxyType.SOCKS4, ProxyType.SOCKS5 -> testSocksProxy(proxy, startTime)
                ProxyType.DIRECT -> ProxyTestResult(
                    proxyId = proxy.id,
                    success = true,
                    latency = 0
                )
                ProxyType.SYSTEM -> testSystemProxy(startTime)
                else -> ProxyTestResult(
                    proxyId = proxy.id,
                    success = false,
                    errorMessage = "不支持的代理类型"
                )
            }
        } catch (e: Exception) {
            ProxyTestResult(
                proxyId = proxy.id,
                success = false,
                errorMessage = e.message
            )
        }
    }

    /**
     * 测试HTTP代理
     */
    private fun testHttpProxy(proxy: ProxyConfig, startTime: Long): ProxyTestResult {
        try {
            val proxyAddress = InetSocketAddress(proxy.host, proxy.port)
            val javaProxy = java.net.Proxy(java.net.Proxy.Type.HTTP, proxyAddress)

            val url = URL(TEST_URL)
            val connection = url.openConnection(javaProxy) as HttpURLConnection

            // 设置认证
            if (!proxy.username.isNullOrEmpty() && !proxy.password.isNullOrEmpty()) {
                val auth = "${proxy.username}:${proxy.password}"
                val encodedAuth = Base64.getEncoder().encodeToString(auth.toByteArray())
                connection.setRequestProperty("Proxy-Authorization", "Basic $encodedAuth")
            }

            // 设置自定义请求头
            proxy.customHeaders.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }

            connection.connectTimeout = TEST_TIMEOUT
            connection.readTimeout = TEST_TIMEOUT
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            val latency = System.currentTimeMillis() - startTime

            connection.disconnect()

            val success = responseCode in 200..299
            if (success) {
                latencyCache[proxy.id] = latency
            }

            return ProxyTestResult(
                proxyId = proxy.id,
                success = success,
                latency = if (success) latency else -1,
                errorMessage = if (!success) "HTTP $responseCode" else null
            )
        } catch (e: Exception) {
            return ProxyTestResult(
                proxyId = proxy.id,
                success = false,
                errorMessage = e.message
            )
        }
    }

    /**
     * 测试SOCKS代理
     */
    private fun testSocksProxy(proxy: ProxyConfig, startTime: Long): ProxyTestResult {
        try {
            val proxyAddress = InetSocketAddress(proxy.host, proxy.port)
            val javaProxy = java.net.Proxy(java.net.Proxy.Type.SOCKS, proxyAddress)

            // 设置SOCKS认证
            if (!proxy.username.isNullOrEmpty() && !proxy.password.isNullOrEmpty()) {
                Authenticator.setDefault(object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(proxy.username, proxy.password.toCharArray())
                    }
                })
            }

            val socket = Socket(javaProxy)
            socket.connect(InetSocketAddress("www.google.com", 80), TEST_TIMEOUT)

            val latency = System.currentTimeMillis() - startTime
            val success = socket.isConnected

            socket.close()

            if (success) {
                latencyCache[proxy.id] = latency
            }

            return ProxyTestResult(
                proxyId = proxy.id,
                success = success,
                latency = if (success) latency else -1
            )
        } catch (e: Exception) {
            return ProxyTestResult(
                proxyId = proxy.id,
                success = false,
                errorMessage = e.message
            )
        }
    }

    /**
     * 测试系统代理
     */
    private fun testSystemProxy(startTime: Long): ProxyTestResult {
        try {
            val url = URL(TEST_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = TEST_TIMEOUT
            connection.readTimeout = TEST_TIMEOUT

            val responseCode = connection.responseCode
            val latency = System.currentTimeMillis() - startTime

            connection.disconnect()

            return ProxyTestResult(
                proxyId = "system",
                success = responseCode in 200..299,
                latency = latency
            )
        } catch (e: Exception) {
            return ProxyTestResult(
                proxyId = "system",
                success = false,
                errorMessage = e.message
            )
        }
    }

    /**
     * 批量测试代理
     */
    suspend fun testProxies(proxies: List<ProxyConfig>): List<ProxyTestResult> = withContext(Dispatchers.IO) {
        proxies.map { proxy ->
            async { testProxy(proxy) }
        }.awaitAll()
    }

    /**
     * 自动选择最佳代理
     */
    suspend fun selectBestProxy(
        proxies: List<ProxyConfig> = getAllProxies(),
        strategy: SelectionStrategy = SelectionStrategy.LOWEST_LATENCY
    ): ProxyConfig? = withContext(Dispatchers.IO) {
        val enabledProxies = proxies.filter { it.enabled }
        if (enabledProxies.isEmpty()) return@withContext null

        when (strategy) {
            SelectionStrategy.LOWEST_LATENCY -> {
                val testResults = testProxies(enabledProxies)
                val successful = testResults.filter { it.success }
                if (successful.isEmpty()) return@withContext null

                val bestResult = successful.minByOrNull { it.latency }
                bestResult?.let { result ->
                    enabledProxies.find { it.id == result.proxyId }
                }
            }
            SelectionStrategy.RANDOM -> {
                enabledProxies.randomOrNull()
            }
            SelectionStrategy.ROUND_ROBIN -> {
                // 简单的轮询实现
                val lastUsed = getActiveProxyId()
                val index = enabledProxies.indexOfFirst { it.id == lastUsed }
                val nextIndex = (index + 1) % enabledProxies.size
                enabledProxies[nextIndex]
            }
            SelectionStrategy.LEAST_USED -> {
                val statistics = getAllStatistics()
                enabledProxies.minByOrNull { proxy ->
                    statistics[proxy.id]?.totalConnections ?: 0
                }
            }
            SelectionStrategy.FAILOVER -> {
                // 故障转移：选择第一个可用的代理
                for (proxy in enabledProxies) {
                    val result = testProxy(proxy)
                    if (result.success) {
                        return@withContext proxy
                    }
                }
                null
            }
        }
    }

    /**
     * 应用代理设置到系统
     */
    fun applyProxyToSystem(proxy: ProxyConfig?): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Android 5.0及以上版本
                when (proxy?.type) {
                    ProxyType.HTTP, ProxyType.HTTPS -> {
                        System.setProperty("http.proxyHost", proxy.host)
                        System.setProperty("http.proxyPort", proxy.port.toString())
                        System.setProperty("https.proxyHost", proxy.host)
                        System.setProperty("https.proxyPort", proxy.port.toString())

                        if (!proxy.username.isNullOrEmpty() && !proxy.password.isNullOrEmpty()) {
                            System.setProperty("http.proxyUser", proxy.username)
                            System.setProperty("http.proxyPassword", proxy.password)
                        }

                        // 设置绕过列表
                        if (proxy.bypassList.isNotEmpty()) {
                            System.setProperty("http.nonProxyHosts", proxy.bypassList.joinToString("|"))
                        }
                        true
                    }
                    ProxyType.SOCKS4, ProxyType.SOCKS5 -> {
                        System.setProperty("socksProxyHost", proxy.host)
                        System.setProperty("socksProxyPort", proxy.port.toString())

                        if (proxy.type == ProxyType.SOCKS5 && !proxy.username.isNullOrEmpty()) {
                            System.setProperty("java.net.socks.username", proxy.username)
                            System.setProperty("java.net.socks.password", proxy.password ?: "")
                        }
                        true
                    }
                    ProxyType.DIRECT, null -> {
                        clearSystemProxy()
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 清除系统代理设置
     */
    fun clearSystemProxy() {
        System.clearProperty("http.proxyHost")
        System.clearProperty("http.proxyPort")
        System.clearProperty("https.proxyHost")
        System.clearProperty("https.proxyPort")
        System.clearProperty("http.proxyUser")
        System.clearProperty("http.proxyPassword")
        System.clearProperty("http.nonProxyHosts")
        System.clearProperty("socksProxyHost")
        System.clearProperty("socksProxyPort")
        System.clearProperty("java.net.socks.username")
        System.clearProperty("java.net.socks.password")
    }

    // ========== 规则管理 ==========

    /**
     * 添加代理规则
     */
    fun addRule(rule: ProxyRule) {
        val rules = getRules().toMutableList()
        rules.add(rule)
        saveRules(rules.sortedByDescending { it.priority })
    }

    /**
     * 获取所有规则
     */
    fun getRules(): List<ProxyRule> {
        val json = sharedPrefs.getString(KEY_RULES, "[]") ?: "[]"
        return parseRulesFromJson(json)
    }

    /**
     * 删除规则
     */
    fun deleteRule(id: String) {
        val rules = getRules().toMutableList()
        rules.removeAll { it.id == id }
        saveRules(rules)
    }

    /**
     * 根据URL匹配代理规则
     */
    fun matchProxyRule(url: String): ProxyConfig? {
        val rules = getRules().filter { it.enabled }.sortedByDescending { it.priority }

        for (rule in rules) {
            if (url.matches(Regex(rule.pattern))) {
                return when (rule.action) {
                    RuleAction.USE_PROXY -> rule.proxyId?.let { getProxy(it) }
                    RuleAction.DIRECT -> ProxyConfig(
                        name = "Direct",
                        type = ProxyType.DIRECT,
                        host = "",
                        port = 0
                    )
                    RuleAction.REJECT -> null
                    RuleAction.AUTO_SELECT -> null // 需要自动选择
                }
            }
        }

        return getActiveProxy()
    }

    // ========== 统计管理 ==========

    /**
     * 获取代理统计信息
     */
    fun getProxyStatistics(proxyId: String): ProxyStatistics {
        val statistics = getAllStatistics()
        return statistics[proxyId] ?: ProxyStatistics(proxyId)
    }

    /**
     * 获取所有统计信息
     */
    private fun getAllStatistics(): Map<String, ProxyStatistics> {
        val json = sharedPrefs.getString(KEY_STATISTICS, "{}")
        val map = mutableMapOf<String, ProxyStatistics>()

        try {
            val jsonObject = JSONObject(json ?: "{}")
            jsonObject.keys().forEach { key ->
                val stats = jsonObject.getJSONObject(key)
                map[key] = ProxyStatistics(
                    proxyId = key,
                    totalConnections = stats.getLong("totalConnections"),
                    successfulConnections = stats.getLong("successfulConnections"),
                    failedConnections = stats.getLong("failedConnections"),
                    totalBandwidth = stats.getLong("totalBandwidth"),
                    averageLatency = stats.getLong("averageLatency"),
                    lastUsed = stats.getLong("lastUsed"),
                    uptime = stats.getLong("uptime")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return map
    }

    /**
     * 更新代理统计信息
     */
    private fun updateProxyStatistics(proxyId: String, update: (ProxyStatistics) -> ProxyStatistics) {
        val statistics = getAllStatistics().toMutableMap()
        val current = statistics[proxyId] ?: ProxyStatistics(proxyId)
        statistics[proxyId] = update(current)
        saveStatistics(statistics)
    }

    /**
     * 记录连接结果
     */
    fun recordConnection(proxyId: String, success: Boolean, latency: Long = 0, bandwidth: Long = 0) {
        updateProxyStatistics(proxyId) { stats ->
            val newTotal = stats.totalConnections + 1
            val newSuccess = if (success) stats.successfulConnections + 1 else stats.successfulConnections
            val newFailed = if (!success) stats.failedConnections + 1 else stats.failedConnections
            val newAvgLatency = if (success && latency > 0) {
                ((stats.averageLatency * stats.successfulConnections) + latency) / newSuccess
            } else {
                stats.averageLatency
            }

            stats.copy(
                totalConnections = newTotal,
                successfulConnections = newSuccess,
                failedConnections = newFailed,
                totalBandwidth = stats.totalBandwidth + bandwidth,
                averageLatency = newAvgLatency,
                lastUsed = System.currentTimeMillis()
            )
        }
    }

    // ========== 导入导出 ==========

    /**
     * 导出代理配置
     */
    fun exportProxies(): String {
        val proxies = getAllProxies()
        val json = JSONObject()

        val proxiesArray = JSONArray()
        proxies.forEach { proxy ->
            proxiesArray.put(proxyToJson(proxy))
        }

        json.put("version", 1)
        json.put("exportDate", System.currentTimeMillis())
        json.put("proxies", proxiesArray)

        return json.toString(2)
    }

    /**
     * 导入代理配置
     */
    fun importProxies(json: String): Boolean {
        return try {
            val jsonObject = JSONObject(json)
            val proxiesArray = jsonObject.getJSONArray("proxies")
            val proxies = mutableListOf<ProxyConfig>()

            for (i in 0 until proxiesArray.length()) {
                val proxyJson = proxiesArray.getJSONObject(i)
                proxies.add(jsonToProxy(proxyJson))
            }

            saveProxies(proxies)
            proxyCache.clear()
            proxies.forEach { proxyCache[it.id] = it }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 从订阅URL导入
     */
    suspend fun importFromSubscription(url: String): List<ProxyConfig> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val content = connection.getInputStream().bufferedReader().readText()
            parseSubscriptionContent(content)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 解析订阅内容
     */
    private fun parseSubscriptionContent(content: String): List<ProxyConfig> {
        // 这里需要根据不同的订阅格式进行解析
        // 支持常见的格式如：Clash、Surge、V2Ray等
        return emptyList()
    }

    // ========== 私有辅助方法 ==========

    private fun saveProxies(proxies: List<ProxyConfig>) {
        val jsonArray = JSONArray()
        proxies.forEach { proxy ->
            jsonArray.put(proxyToJson(proxy))
        }
        sharedPrefs.edit().putString(KEY_PROXIES, jsonArray.toString()).apply()
    }

    private fun saveRules(rules: List<ProxyRule>) {
        val jsonArray = JSONArray()
        rules.forEach { rule ->
            jsonArray.put(JSONObject().apply {
                put("id", rule.id)
                put("name", rule.name)
                put("pattern", rule.pattern)
                rule.proxyId?.let { put("proxyId", it) }
                put("action", rule.action.name)
                put("enabled", rule.enabled)
                put("priority", rule.priority)
            })
        }
        sharedPrefs.edit().putString(KEY_RULES, jsonArray.toString()).apply()
    }

    private fun saveStatistics(statistics: Map<String, ProxyStatistics>) {
        val json = JSONObject()
        statistics.forEach { (key, stats) ->
            json.put(key, JSONObject().apply {
                put("totalConnections", stats.totalConnections)
                put("successfulConnections", stats.successfulConnections)
                put("failedConnections", stats.failedConnections)
                put("totalBandwidth", stats.totalBandwidth)
                put("averageLatency", stats.averageLatency)
                put("lastUsed", stats.lastUsed)
                put("uptime", stats.uptime)
            })
        }
        sharedPrefs.edit().putString(KEY_STATISTICS, json.toString()).apply()
    }

    private fun parseProxiesFromJson(json: String): List<ProxyConfig> {
        val list = mutableListOf<ProxyConfig>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val proxyJson = jsonArray.getJSONObject(i)
                list.add(jsonToProxy(proxyJson))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseRulesFromJson(json: String): List<ProxyRule> {
        val list = mutableListOf<ProxyRule>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                list.add(ProxyRule(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    pattern = item.getString("pattern"),
                    proxyId = item.optString("proxyId", ""),
                    action = RuleAction.valueOf(item.getString("action")),
                    enabled = item.getBoolean("enabled"),
                    priority = item.getInt("priority")
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun proxyToJson(proxy: ProxyConfig): JSONObject {
        return JSONObject().apply {
            put("id", proxy.id)
            put("name", proxy.name)
            put("type", proxy.type.name)
            put("host", proxy.host)
            put("port", proxy.port)
            proxy.username?.let { put("username", it) }
            proxy.password?.let { put("password", it) }
            put("enabled", proxy.enabled)
            put("priority", proxy.priority)
            proxy.country?.let { put("country", it) }
            put("protocol", proxy.protocol.name)

            if (proxy.tags.isNotEmpty()) {
                put("tags", JSONArray(proxy.tags))
            }

            if (proxy.customHeaders.isNotEmpty()) {
                val headers = JSONObject()
                proxy.customHeaders.forEach { (key, value) ->
                    headers.put(key, value)
                }
                put("customHeaders", headers)
            }

            if (proxy.bypassList.isNotEmpty()) {
                put("bypassList", JSONArray(proxy.bypassList))
            }
        }
    }

    private fun jsonToProxy(json: JSONObject): ProxyConfig {
        val tags = mutableListOf<String>()
        if (json.has("tags")) {
            val tagsArray = json.getJSONArray("tags")
            for (i in 0 until tagsArray.length()) {
                tags.add(tagsArray.getString(i))
            }
        }

        val customHeaders = mutableMapOf<String, String>()
        if (json.has("customHeaders")) {
            val headers = json.getJSONObject("customHeaders")
            headers.keys().forEach { key ->
                customHeaders[key] = headers.getString(key)
            }
        }

        val bypassList = mutableListOf<String>()
        if (json.has("bypassList")) {
            val bypassArray = json.getJSONArray("bypassList")
            for (i in 0 until bypassArray.length()) {
                bypassList.add(bypassArray.getString(i))
            }
        }

        return ProxyConfig(
            id = json.getString("id"),
            name = json.getString("name"),
            type = ProxyType.valueOf(json.getString("type")),
            host = json.getString("host"),
            port = json.getInt("port"),
            username = json.optString("username", ""),
            password = json.optString("password", ""),
            enabled = json.getBoolean("enabled"),
            priority = json.getInt("priority"),
            country = json.optString("country", ""),
            protocol = ProxyProtocol.valueOf(json.optString("protocol", "HTTP")),
            tags = tags,
            customHeaders = customHeaders,
            bypassList = bypassList
        )
    }
}