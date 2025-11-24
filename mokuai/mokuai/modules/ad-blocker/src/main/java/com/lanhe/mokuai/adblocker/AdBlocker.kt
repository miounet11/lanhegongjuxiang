package com.lanhe.mokuai.adblocker

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * 广告拦截器 - 网页和应用广告过滤工具
 */
class AdBlocker(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "ad_blocker_prefs"
        private const val KEY_RULES = "blocking_rules"
        private const val KEY_WHITELIST = "whitelist"
        private const val KEY_STATISTICS = "statistics"
        private const val KEY_CUSTOM_FILTERS = "custom_filters"
        private const val KEY_ENABLED = "enabled"

        // 默认拦截规则源
        private val DEFAULT_FILTER_LISTS = listOf(
            "https://easylist.to/easylist/easylist.txt",
            "https://easylist.to/easylist/easyprivacy.txt",
            "https://raw.githubusercontent.com/cjx82630/cjxlist/master/cjx-annoyance.txt"
        )

        // 常见广告域名
        private val COMMON_AD_HOSTS = setOf(
            "doubleclick.net", "googleadservices.com", "googlesyndication.com",
            "google-analytics.com", "googletagmanager.com", "googletagservices.com",
            "facebook.com/tr", "amazon-adsystem.com", "adsystem.com",
            "adsrvr.org", "adzerk.net", "outbrain.com", "taboola.com",
            "scorecardresearch.com", "quantserve.com", "adsafeprotected.com",
            "moatads.com", "rubiconproject.com", "pubmatic.com", "openx.net",
            "criteo.com", "casalemedia.com", "amazon-adsystem.com", "yieldmo.com",
            "sharethrough.com", "spotxchange.com", "teads.tv", "smaato.net"
        )

        // 常见跟踪器
        private val COMMON_TRACKERS = setOf(
            "google-analytics.com", "googletagmanager.com", "facebook.com/tr",
            "analytics.twitter.com", "mc.yandex.ru", "analytics.yahoo.com",
            "scorecardresearch.com", "quantserve.com", "omtrdc.net",
            "demdex.net", "adsrvr.org", "doubleclick.net/pagead/id",
            "amazon-adsystem.com/pixels", "facebook.com/tr", "linkedin.com/px"
        )

        // 空响应
        private val EMPTY_RESPONSE = WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream("".toByteArray())
        )
    }

    private val sharedPrefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 缓存已编译的规则
    private val compiledRules = ConcurrentHashMap<String, Pattern>()
    private val hostBlocklist = mutableSetOf<String>()
    private val urlBlocklist = mutableSetOf<String>()
    private val elementHidingRules = mutableListOf<ElementHidingRule>()
    private val whitelistRules = mutableSetOf<String>()

    data class BlockingRule(
        val id: String = UUID.randomUUID().toString(),
        val rule: String,
        val type: RuleType,
        val enabled: Boolean = true,
        val source: String = "custom",
        val priority: Int = 0
    )

    enum class RuleType {
        DOMAIN,          // 域名拦截
        URL_PATTERN,     // URL模式匹配
        CSS_SELECTOR,    // CSS选择器隐藏
        SCRIPT_BLOCK,    // 脚本拦截
        IMAGE_BLOCK,     // 图片拦截
        FRAME_BLOCK,     // 框架拦截
        POPUP_BLOCK,     // 弹窗拦截
        COOKIE_BLOCK     // Cookie拦截
    }

    data class ElementHidingRule(
        val domain: String?,
        val selector: String
    )

    data class FilterList(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val url: String,
        val enabled: Boolean = true,
        val lastUpdate: Long = 0,
        val ruleCount: Int = 0
    )

    data class BlockingStatistics(
        val totalBlocked: Long = 0,
        val domainsBlocked: Int = 0,
        val trackersBlocked: Int = 0,
        val adsBlocked: Int = 0,
        val popupsBlocked: Int = 0,
        val savedBandwidth: Long = 0,
        val lastReset: Long = System.currentTimeMillis()
    )

    data class WhitelistEntry(
        val id: String = UUID.randomUUID().toString(),
        val pattern: String,
        val type: WhitelistType,
        val enabled: Boolean = true
    )

    enum class WhitelistType {
        DOMAIN,      // 整个域名
        PAGE,        // 特定页面
        TEMPORARY    // 临时白名单
    }

    init {
        loadRules()
    }

    /**
     * 检查是否启用广告拦截
     */
    fun isEnabled(): Boolean {
        return sharedPrefs.getBoolean(KEY_ENABLED, true)
    }

    /**
     * 设置广告拦截状态
     */
    fun setEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * 检查URL是否应该被拦截
     */
    fun shouldBlock(url: String): Boolean {
        if (!isEnabled()) return false

        val uri = Uri.parse(url)
        val host = uri.host ?: return false

        // 检查白名单
        if (isWhitelisted(url)) {
            return false
        }

        // 检查域名黑名单
        if (isHostBlocked(host)) {
            incrementBlockedCount("domain")
            return true
        }

        // 检查URL模式
        if (isUrlBlocked(url)) {
            incrementBlockedCount("url")
            return true
        }

        // 检查是否是广告或跟踪器
        if (isAdOrTracker(host, url)) {
            incrementBlockedCount("ad_tracker")
            return true
        }

        return false
    }

    /**
     * 拦截WebView请求
     */
    fun interceptRequest(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()

        if (shouldBlock(url)) {
            // 根据资源类型返回不同的空响应
            return when {
                url.endsWith(".js") -> WebResourceResponse("application/javascript", "UTF-8", ByteArrayInputStream("".toByteArray()))
                url.endsWith(".css") -> WebResourceResponse("text/css", "UTF-8", ByteArrayInputStream("".toByteArray()))
                url.endsWith(".png") || url.endsWith(".jpg") || url.endsWith(".gif") -> {
                    WebResourceResponse("image/png", "binary", ByteArrayInputStream(ByteArray(0)))
                }
                else -> EMPTY_RESPONSE
            }
        }

        return null
    }

    /**
     * 获取元素隐藏CSS
     */
    fun getElementHidingCSS(url: String): String {
        val uri = Uri.parse(url)
        val host = uri.host ?: return ""

        val rules = elementHidingRules.filter { rule ->
            rule.domain == null || host.contains(rule.domain)
        }

        return if (rules.isNotEmpty()) {
            rules.joinToString(",") { it.selector } + " { display: none !important; }"
        } else {
            ""
        }
    }

    /**
     * 获取JavaScript注入代码
     */
    fun getBlockingScript(): String {
        return """
            (function() {
                // 拦截弹窗
                window.open = function() { console.log('Popup blocked'); return null; };
                window.alert = function() { console.log('Alert blocked'); };
                window.confirm = function() { console.log('Confirm blocked'); return false; };

                // 拦截追踪器
                if (window.ga) window.ga = function() {};
                if (window.gtag) window.gtag = function() {};
                if (window._gaq) window._gaq = { push: function() {} };

                // 拦截Facebook像素
                if (window.fbq) window.fbq = function() {};

                // 拦截Google Ads
                if (window.adsbygoogle) window.adsbygoogle = { push: function() {} };

                // 移除广告元素
                const adSelectors = [
                    '[id*="ad"]', '[id*="Ad"]', '[id*="AD"]',
                    '[class*="ad"]', '[class*="Ad"]', '[class*="AD"]',
                    '[id*="banner"]', '[class*="banner"]',
                    '[id*="popup"]', '[class*="popup"]',
                    'iframe[src*="doubleclick"]',
                    'iframe[src*="googlesyndication"]'
                ];

                function removeAds() {
                    adSelectors.forEach(selector => {
                        document.querySelectorAll(selector).forEach(el => {
                            if (el.offsetWidth > 0 && el.offsetHeight > 0) {
                                el.remove();
                            }
                        });
                    });
                }

                // 初次执行
                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', removeAds);
                } else {
                    removeAds();
                }

                // 监听动态添加的元素
                const observer = new MutationObserver(removeAds);
                observer.observe(document.body, {
                    childList: true,
                    subtree: true
                });
            })();
        """.trimIndent()
    }

    // ========== 规则管理 ==========

    /**
     * 加载规则
     */
    private fun loadRules() {
        val rulesJson = sharedPrefs.getString(KEY_RULES, null)
        if (rulesJson != null) {
            parseRules(rulesJson)
        } else {
            loadDefaultRules()
        }
    }

    /**
     * 加载默认规则
     */
    private fun loadDefaultRules() {
        // 添加常见广告域名
        hostBlocklist.addAll(COMMON_AD_HOSTS)
        hostBlocklist.addAll(COMMON_TRACKERS)

        // 添加常见广告URL模式
        urlBlocklist.addAll(listOf(
            "*/ads/*", "*/ad/*", "*/advertisement/*",
            "*/banner/*", "*/popup/*", "*/popunder/*",
            "*tracking*", "*analytics*", "*metrics*",
            "*.doubleclick.*", "*.googlesyndication.*",
            "*/pagead/*", "*/pubads/*"
        ))

        // 添加元素隐藏规则
        elementHidingRules.addAll(listOf(
            ElementHidingRule(null, ".ad"),
            ElementHidingRule(null, ".ads"),
            ElementHidingRule(null, ".advertisement"),
            ElementHidingRule(null, "#ad"),
            ElementHidingRule(null, "#ads"),
            ElementHidingRule(null, "[id^='ad-']"),
            ElementHidingRule(null, "[class^='ad-']"),
            ElementHidingRule(null, "[id*='google_ads']"),
            ElementHidingRule(null, "[class*='google_ads']")
        ))
    }

    /**
     * 更新过滤器列表
     */
    suspend fun updateFilterLists(lists: List<FilterList>) = withContext(Dispatchers.IO) {
        lists.filter { it.enabled }.forEach { list ->
            try {
                val rules = downloadFilterList(list.url)
                parseFilterListRules(rules)
                updateFilterListInfo(list.copy(
                    lastUpdate = System.currentTimeMillis(),
                    ruleCount = rules.lines().size
                ))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        saveRules()
    }

    /**
     * 下载过滤器列表
     */
    private suspend fun downloadFilterList(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection()
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        BufferedReader(InputStreamReader(connection.getInputStream())).use { reader ->
            reader.readText()
        }
    }

    /**
     * 解析过滤器列表规则
     */
    private fun parseFilterListRules(content: String) {
        content.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() || trimmed.startsWith("!") -> {
                    // 注释或空行，忽略
                }
                trimmed.startsWith("@@") -> {
                    // 白名单规则
                    whitelistRules.add(trimmed.substring(2))
                }
                trimmed.startsWith("##") -> {
                    // 全局元素隐藏规则
                    elementHidingRules.add(ElementHidingRule(null, trimmed.substring(2)))
                }
                trimmed.contains("##") -> {
                    // 域名特定元素隐藏规则
                    val parts = trimmed.split("##")
                    if (parts.size == 2) {
                        elementHidingRules.add(ElementHidingRule(parts[0], parts[1]))
                    }
                }
                trimmed.startsWith("||") -> {
                    // 域名规则
                    val domain = trimmed.substring(2).replace("^", "")
                    hostBlocklist.add(domain)
                }
                else -> {
                    // URL规则
                    urlBlocklist.add(trimmed)
                }
            }
        }
    }

    /**
     * 添加自定义规则
     */
    fun addCustomRule(rule: BlockingRule) {
        when (rule.type) {
            RuleType.DOMAIN -> hostBlocklist.add(rule.rule)
            RuleType.URL_PATTERN -> urlBlocklist.add(rule.rule)
            RuleType.CSS_SELECTOR -> elementHidingRules.add(ElementHidingRule(null, rule.rule))
            else -> { /* 其他类型 */ }
        }
        saveRules()
    }

    /**
     * 删除自定义规则
     */
    fun removeCustomRule(rule: String) {
        hostBlocklist.remove(rule)
        urlBlocklist.remove(rule)
        elementHidingRules.removeAll { it.selector == rule }
        whitelistRules.remove(rule)
        saveRules()
    }

    /**
     * 保存规则
     */
    private fun saveRules() {
        val json = JSONObject().apply {
            put("hosts", JSONArray(hostBlocklist.toList()))
            put("urls", JSONArray(urlBlocklist.toList()))
            put("whitelist", JSONArray(whitelistRules.toList()))

            val hidingArray = JSONArray()
            elementHidingRules.forEach { rule ->
                hidingArray.put(JSONObject().apply {
                    rule.domain?.let { put("domain", it) }
                    put("selector", rule.selector)
                })
            }
            put("hiding", hidingArray)
        }
        sharedPrefs.edit().putString(KEY_RULES, json.toString()).apply()
    }

    /**
     * 解析规则
     */
    private fun parseRules(json: String) {
        try {
            val jsonObject = JSONObject(json)

            // 解析域名黑名单
            val hosts = jsonObject.getJSONArray("hosts")
            for (i in 0 until hosts.length()) {
                hostBlocklist.add(hosts.getString(i))
            }

            // 解析URL黑名单
            val urls = jsonObject.getJSONArray("urls")
            for (i in 0 until urls.length()) {
                urlBlocklist.add(urls.getString(i))
            }

            // 解析白名单
            val whitelist = jsonObject.getJSONArray("whitelist")
            for (i in 0 until whitelist.length()) {
                whitelistRules.add(whitelist.getString(i))
            }

            // 解析元素隐藏规则
            val hiding = jsonObject.getJSONArray("hiding")
            for (i in 0 until hiding.length()) {
                val rule = hiding.getJSONObject(i)
                elementHidingRules.add(ElementHidingRule(
                    domain = rule.optString("domain", ""),
                    selector = rule.getString("selector")
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            loadDefaultRules()
        }
    }

    // ========== 白名单管理 ==========

    /**
     * 添加到白名单
     */
    fun addToWhitelist(pattern: String, type: WhitelistType = WhitelistType.DOMAIN) {
        val entry = WhitelistEntry(pattern = pattern, type = type)
        whitelistRules.add(pattern)
        saveWhitelist(entry)
    }

    /**
     * 从白名单移除
     */
    fun removeFromWhitelist(pattern: String) {
        whitelistRules.remove(pattern)
        saveRules()
    }

    /**
     * 检查是否在白名单中
     */
    private fun isWhitelisted(url: String): Boolean {
        val uri = Uri.parse(url)
        val host = uri.host ?: return false

        return whitelistRules.any { pattern ->
            when {
                pattern.contains("*") -> {
                    val regex = pattern.replace("*", ".*")
                    url.matches(Regex(regex))
                }
                else -> host.contains(pattern) || url.contains(pattern)
            }
        }
    }

    private fun saveWhitelist(entry: WhitelistEntry) {
        val whitelist = getWhitelist().toMutableList()
        whitelist.add(entry)
        val json = JSONArray()
        whitelist.forEach { item ->
            json.put(JSONObject().apply {
                put("id", item.id)
                put("pattern", item.pattern)
                put("type", item.type.name)
                put("enabled", item.enabled)
            })
        }
        sharedPrefs.edit().putString(KEY_WHITELIST, json.toString()).apply()
    }

    fun getWhitelist(): List<WhitelistEntry> {
        val json = sharedPrefs.getString(KEY_WHITELIST, "[]") ?: "[]"
        val list = mutableListOf<WhitelistEntry>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                list.add(WhitelistEntry(
                    id = item.getString("id"),
                    pattern = item.getString("pattern"),
                    type = WhitelistType.valueOf(item.getString("type")),
                    enabled = item.getBoolean("enabled")
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // ========== 检测方法 ==========

    /**
     * 检查域名是否被拦截
     */
    private fun isHostBlocked(host: String): Boolean {
        // 完全匹配
        if (hostBlocklist.contains(host)) {
            return true
        }

        // 子域名匹配
        return hostBlocklist.any { blockedHost ->
            host.endsWith(".$blockedHost") || host == blockedHost
        }
    }

    /**
     * 检查URL是否被拦截
     */
    private fun isUrlBlocked(url: String): Boolean {
        return urlBlocklist.any { pattern ->
            when {
                pattern.contains("*") -> {
                    val regex = pattern.replace("*", ".*")
                    url.matches(Regex(regex))
                }
                else -> url.contains(pattern)
            }
        }
    }

    /**
     * 检查是否是广告或跟踪器
     */
    private fun isAdOrTracker(host: String, url: String): Boolean {
        // 检查常见广告域名
        if (COMMON_AD_HOSTS.any { adHost ->
                host.contains(adHost) || host.endsWith(".$adHost")
            }) {
            return true
        }

        // 检查常见跟踪器
        if (COMMON_TRACKERS.any { tracker ->
                host.contains(tracker) || url.contains(tracker)
            }) {
            return true
        }

        // 检查URL特征
        val adKeywords = listOf("ad", "ads", "advertisement", "banner", "popup", "tracking", "analytics")
        return adKeywords.any { keyword ->
            url.contains("/$keyword/") || url.contains(".$keyword.") || url.contains("?$keyword=")
        }
    }

    // ========== 统计管理 ==========

    /**
     * 增加拦截计数
     */
    private fun incrementBlockedCount(type: String) {
        val stats = getStatistics()
        val updated = when (type) {
            "domain" -> stats.copy(
                totalBlocked = stats.totalBlocked + 1,
                domainsBlocked = stats.domainsBlocked + 1
            )
            "ad_tracker" -> stats.copy(
                totalBlocked = stats.totalBlocked + 1,
                adsBlocked = stats.adsBlocked + 1,
                trackersBlocked = stats.trackersBlocked + 1
            )
            "popup" -> stats.copy(
                totalBlocked = stats.totalBlocked + 1,
                popupsBlocked = stats.popupsBlocked + 1
            )
            else -> stats.copy(totalBlocked = stats.totalBlocked + 1)
        }
        saveStatistics(updated)
    }

    /**
     * 获取统计信息
     */
    fun getStatistics(): BlockingStatistics {
        val json = sharedPrefs.getString(KEY_STATISTICS, null)
        return if (json != null) {
            try {
                val obj = JSONObject(json)
                BlockingStatistics(
                    totalBlocked = obj.getLong("totalBlocked"),
                    domainsBlocked = obj.getInt("domainsBlocked"),
                    trackersBlocked = obj.getInt("trackersBlocked"),
                    adsBlocked = obj.getInt("adsBlocked"),
                    popupsBlocked = obj.getInt("popupsBlocked"),
                    savedBandwidth = obj.getLong("savedBandwidth"),
                    lastReset = obj.getLong("lastReset")
                )
            } catch (e: Exception) {
                BlockingStatistics()
            }
        } else {
            BlockingStatistics()
        }
    }

    /**
     * 保存统计信息
     */
    private fun saveStatistics(stats: BlockingStatistics) {
        val json = JSONObject().apply {
            put("totalBlocked", stats.totalBlocked)
            put("domainsBlocked", stats.domainsBlocked)
            put("trackersBlocked", stats.trackersBlocked)
            put("adsBlocked", stats.adsBlocked)
            put("popupsBlocked", stats.popupsBlocked)
            put("savedBandwidth", stats.savedBandwidth)
            put("lastReset", stats.lastReset)
        }
        sharedPrefs.edit().putString(KEY_STATISTICS, json.toString()).apply()
    }

    /**
     * 重置统计
     */
    fun resetStatistics() {
        saveStatistics(BlockingStatistics())
    }

    /**
     * 更新过滤器列表信息
     */
    private fun updateFilterListInfo(filterList: FilterList) {
        // 这里可以保存过滤器列表的更新信息
    }
}