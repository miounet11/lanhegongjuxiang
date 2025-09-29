package com.lanhe.mokuai.urlopen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Browser
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabColorSchemeParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import java.util.regex.Pattern

/**
 * URL打开器 - 智能URL处理和跳转工具
 */
class UrlOpener(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "url_opener_prefs"
        private const val KEY_HISTORY = "url_history"
        private const val KEY_RULES = "url_rules"
        private const val KEY_SHORTCUTS = "url_shortcuts"
        private const val MAX_HISTORY_SIZE = 100

        // URL协议
        private val SUPPORTED_SCHEMES = setOf(
            "http", "https", "ftp", "ftps", "file",
            "mailto", "tel", "sms", "geo", "market",
            "intent", "content", "magnet", "thunder"
        )

        // 常见URL模式
        private val URL_PATTERNS = mapOf(
            "video" to Pattern.compile(".*\\.(mp4|avi|mkv|mov|wmv|flv|webm|m3u8)$", Pattern.CASE_INSENSITIVE),
            "audio" to Pattern.compile(".*\\.(mp3|wav|flac|aac|ogg|wma|m4a)$", Pattern.CASE_INSENSITIVE),
            "image" to Pattern.compile(".*\\.(jpg|jpeg|png|gif|webp|bmp|svg)$", Pattern.CASE_INSENSITIVE),
            "document" to Pattern.compile(".*\\.(pdf|doc|docx|xls|xlsx|ppt|pptx|txt)$", Pattern.CASE_INSENSITIVE),
            "archive" to Pattern.compile(".*\\.(zip|rar|7z|tar|gz|bz2)$", Pattern.CASE_INSENSITIVE),
            "apk" to Pattern.compile(".*\\.apk$", Pattern.CASE_INSENSITIVE)
        )

        // 深度链接模式
        private val DEEPLINK_PATTERNS = mapOf(
            "youtube" to "(?:https?://)?(?:www\\.)?(?:youtube\\.com|youtu\\.be)/.*",
            "twitter" to "(?:https?://)?(?:www\\.)?twitter\\.com/.*",
            "instagram" to "(?:https?://)?(?:www\\.)?instagram\\.com/.*",
            "facebook" to "(?:https?://)?(?:www\\.)?facebook\\.com/.*",
            "tiktok" to "(?:https?://)?(?:www\\.)?tiktok\\.com/.*",
            "bilibili" to "(?:https?://)?(?:www\\.)?bilibili\\.com/.*",
            "weibo" to "(?:https?://)?(?:www\\.)?weibo\\.com/.*",
            "taobao" to "(?:https?://)?(?:.*\\.)?taobao\\.com/.*",
            "jd" to "(?:https?://)?(?:.*\\.)?jd\\.com/.*"
        )
    }

    private val sharedPrefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    data class UrlInfo(
        val originalUrl: String,
        val scheme: String,
        val host: String?,
        val path: String?,
        val queryParams: Map<String, String>,
        val fragment: String?,
        val type: UrlType,
        val isDeepLink: Boolean,
        val appPackage: String? = null
    )

    enum class UrlType {
        WEB, VIDEO, AUDIO, IMAGE, DOCUMENT, ARCHIVE, APPLICATION, EMAIL, PHONE, MAP, STORE, DEEPLINK, OTHER
    }

    data class OpenConfig(
        val useCustomTabs: Boolean = true,
        val customTabColor: Int? = null,
        val showTitle: Boolean = true,
        val enableUrlBar: Boolean = true,
        val enableShare: Boolean = true,
        val instantApps: Boolean = true,
        val animations: Boolean = true,
        val forceExternal: Boolean = false,
        val chooserTitle: String? = null
    )

    data class UrlRule(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val pattern: String,
        val action: RuleAction,
        val targetApp: String? = null,
        val replacement: String? = null,
        val enabled: Boolean = true,
        val priority: Int = 0
    )

    enum class RuleAction {
        BLOCK,           // 阻止打开
        REDIRECT,        // 重定向到其他URL
        OPEN_WITH,       // 使用指定应用打开
        TRANSFORM,       // 转换URL格式
        COPY_TO_CLIPBOARD // 复制到剪贴板
    }

    data class UrlShortcut(
        val id: String = UUID.randomUUID().toString(),
        val keyword: String,
        val url: String,
        val description: String? = null,
        val icon: String? = null,
        val usageCount: Int = 0,
        val lastUsed: Long = 0
    )

    data class UrlHistory(
        val url: String,
        val title: String? = null,
        val timestamp: Long = System.currentTimeMillis(),
        val openCount: Int = 1
    )

    /**
     * 打开URL
     */
    suspend fun openUrl(url: String, config: OpenConfig = OpenConfig()): Boolean = withContext(Dispatchers.Main) {
        try {
            val processedUrl = processUrl(url)
            val urlInfo = analyzeUrl(processedUrl)

            // 应用规则
            val rule = findMatchingRule(processedUrl)
            if (rule != null && rule.enabled) {
                return@withContext applyRule(processedUrl, rule)
            }

            // 根据URL类型选择打开方式
            when (urlInfo.type) {
                UrlType.WEB, UrlType.VIDEO, UrlType.IMAGE, UrlType.DOCUMENT -> {
                    if (config.useCustomTabs && !config.forceExternal) {
                        openWithCustomTabs(processedUrl, config)
                    } else {
                        openWithBrowser(processedUrl, config)
                    }
                }
                UrlType.EMAIL -> openEmail(processedUrl)
                UrlType.PHONE -> openPhone(processedUrl)
                UrlType.MAP -> openMap(processedUrl)
                UrlType.STORE -> openStore(processedUrl)
                UrlType.DEEPLINK -> openDeepLink(urlInfo, config)
                UrlType.APPLICATION -> openApplication(processedUrl)
                else -> openWithDefault(processedUrl, config)
            }

            // 保存到历史记录
            saveToHistory(processedUrl)
            true
        } catch (e: Exception) {
            handleOpenError(e, url)
            false
        }
    }

    /**
     * 分析URL
     */
    fun analyzeUrl(url: String): UrlInfo {
        val uri = Uri.parse(url)
        val scheme = uri.scheme ?: "http"
        val host = uri.host
        val path = uri.path
        val fragment = uri.fragment

        // 解析查询参数
        val queryParams = mutableMapOf<String, String>()
        uri.queryParameterNames.forEach { name ->
            uri.getQueryParameter(name)?.let { value ->
                queryParams[name] = value
            }
        }

        // 判断URL类型
        val type = detectUrlType(url, scheme)

        // 检查是否是深度链接
        val (isDeepLink, appPackage) = checkDeepLink(url)

        return UrlInfo(
            originalUrl = url,
            scheme = scheme,
            host = host,
            path = path,
            queryParams = queryParams,
            fragment = fragment,
            type = type,
            isDeepLink = isDeepLink,
            appPackage = appPackage
        )
    }

    /**
     * 批量打开URL
     */
    suspend fun openMultipleUrls(urls: List<String>, delayMs: Long = 500) = withContext(Dispatchers.IO) {
        urls.forEach { url ->
            withContext(Dispatchers.Main) {
                openUrl(url)
            }
            if (delayMs > 0) {
                kotlinx.coroutines.delay(delayMs)
            }
        }
    }

    /**
     * 使用Custom Tabs打开
     */
    private fun openWithCustomTabs(url: String, config: OpenConfig) {
        val builder = CustomTabsIntent.Builder()

        // 设置颜色
        config.customTabColor?.let { color ->
            val params = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(color)
                .build()
            builder.setDefaultColorSchemeParams(params)
        }

        // 设置选项
        builder.setShowTitle(config.showTitle)
        builder.setUrlBarHidingEnabled(!config.enableUrlBar)
        builder.setShareState(if (config.enableShare) {
            CustomTabsIntent.SHARE_STATE_ON
        } else {
            CustomTabsIntent.SHARE_STATE_OFF
        })
        builder.setInstantAppsEnabled(config.instantApps)

        // 设置动画
        if (config.animations) {
            builder.setStartAnimations(context, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            builder.setExitAnimations(context, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }

    /**
     * 使用浏览器打开
     */
    private fun openWithBrowser(url: String, config: OpenConfig) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addCategory(Intent.CATEGORY_BROWSABLE)

        if (config.chooserTitle != null) {
            val chooser = Intent.createChooser(intent, config.chooserTitle)
            context.startActivity(chooser)
        } else {
            context.startActivity(intent)
        }
    }

    /**
     * 打开邮件
     */
    private fun openEmail(url: String) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_SENDTO, uri)

        // 解析邮件参数
        uri.getQueryParameter("subject")?.let {
            intent.putExtra(Intent.EXTRA_SUBJECT, it)
        }
        uri.getQueryParameter("body")?.let {
            intent.putExtra(Intent.EXTRA_TEXT, it)
        }
        uri.getQueryParameter("cc")?.let {
            intent.putExtra(Intent.EXTRA_CC, arrayOf(it))
        }
        uri.getQueryParameter("bcc")?.let {
            intent.putExtra(Intent.EXTRA_BCC, arrayOf(it))
        }

        context.startActivity(Intent.createChooser(intent, "选择邮件应用"))
    }

    /**
     * 打开电话
     */
    private fun openPhone(url: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse(url))
        context.startActivity(intent)
    }

    /**
     * 打开地图
     */
    private fun openMap(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.setPackage("com.google.android.apps.maps")

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // 如果Google Maps未安装，使用网页版
            val webUrl = url.replace("geo:", "https://maps.google.com/maps?q=")
            openWithBrowser(webUrl, OpenConfig())
        }
    }

    /**
     * 打开应用商店
     */
    private fun openStore(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

        // 尝试使用Google Play
        intent.setPackage("com.android.vending")

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // 使用网页版
            val webUrl = url.replace("market://", "https://play.google.com/store/apps/")
            openWithBrowser(webUrl, OpenConfig())
        }
    }

    /**
     * 打开深度链接
     */
    private fun openDeepLink(urlInfo: UrlInfo, config: OpenConfig) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlInfo.originalUrl))

        urlInfo.appPackage?.let {
            intent.setPackage(it)
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // 应用未安装，使用浏览器打开
            openWithBrowser(urlInfo.originalUrl, config)
        }
    }

    /**
     * 打开APK文件
     */
    private fun openApplication(url: String) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        context.startActivity(intent)
    }

    /**
     * 默认方式打开
     */
    private fun openWithDefault(url: String, config: OpenConfig) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

        if (config.chooserTitle != null) {
            val chooser = Intent.createChooser(intent, config.chooserTitle)
            context.startActivity(chooser)
        } else {
            context.startActivity(intent)
        }
    }

    /**
     * 处理URL（清理和标准化）
     */
    private fun processUrl(url: String): String {
        var processed = url.trim()

        // 添加协议
        if (!processed.contains("://")) {
            processed = when {
                processed.startsWith("//") -> "https:$processed"
                processed.contains("@") && !processed.contains("/") -> "mailto:$processed"
                processed.matches(Regex("^\\+?[0-9\\-()\\s]+$")) -> "tel:${processed.replace(Regex("[\\s()-]"), "")}"
                else -> "https://$processed"
            }
        }

        // URL解码
        if (processed.contains("%")) {
            try {
                processed = URLDecoder.decode(processed, "UTF-8")
            } catch (e: Exception) {
                // 忽略解码错误
            }
        }

        return processed
    }

    /**
     * 检测URL类型
     */
    private fun detectUrlType(url: String, scheme: String): UrlType {
        return when (scheme) {
            "mailto" -> UrlType.EMAIL
            "tel", "sms" -> UrlType.PHONE
            "geo" -> UrlType.MAP
            "market" -> UrlType.STORE
            "file", "content" -> detectFileType(url)
            "http", "https" -> detectWebContentType(url)
            else -> UrlType.OTHER
        }
    }

    /**
     * 检测文件类型
     */
    private fun detectFileType(url: String): UrlType {
        URL_PATTERNS.forEach { (type, pattern) ->
            if (pattern.matcher(url).find()) {
                return when (type) {
                    "video" -> UrlType.VIDEO
                    "audio" -> UrlType.AUDIO
                    "image" -> UrlType.IMAGE
                    "document" -> UrlType.DOCUMENT
                    "archive" -> UrlType.ARCHIVE
                    "apk" -> UrlType.APPLICATION
                    else -> UrlType.OTHER
                }
            }
        }
        return UrlType.OTHER
    }

    /**
     * 检测Web内容类型
     */
    private fun detectWebContentType(url: String): UrlType {
        // 先检查是否是深度链接
        DEEPLINK_PATTERNS.forEach { (_, pattern) ->
            if (url.matches(Regex(pattern))) {
                return UrlType.DEEPLINK
            }
        }

        // 检查文件扩展名
        val fileType = detectFileType(url)
        if (fileType != UrlType.OTHER) {
            return fileType
        }

        return UrlType.WEB
    }

    /**
     * 检查深度链接
     */
    private fun checkDeepLink(url: String): Pair<Boolean, String?> {
        val deepLinkApps = mapOf(
            "youtube" to "com.google.android.youtube",
            "twitter" to "com.twitter.android",
            "instagram" to "com.instagram.android",
            "facebook" to "com.facebook.katana",
            "tiktok" to "com.zhiliaoapp.musically",
            "bilibili" to "tv.danmaku.bili",
            "weibo" to "com.sina.weibo",
            "taobao" to "com.taobao.taobao",
            "jd" to "com.jingdong.app.mall"
        )

        deepLinkApps.forEach { (key, packageName) ->
            val pattern = DEEPLINK_PATTERNS[key] ?: return@forEach
            if (url.matches(Regex(pattern))) {
                // 检查应用是否已安装
                if (isAppInstalled(packageName)) {
                    return Pair(true, packageName)
                }
            }
        }

        return Pair(false, null)
    }

    /**
     * 检查应用是否安装
     */
    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 处理打开错误
     */
    private fun handleOpenError(error: Exception, url: String) {
        val message = when (error) {
            is ActivityNotFoundException -> "没有找到可以打开此链接的应用"
            is SecurityException -> "没有权限打开此链接"
            else -> "无法打开链接: ${error.message}"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // ========== 规则管理 ==========

    /**
     * 添加URL规则
     */
    fun addRule(rule: UrlRule) {
        val rules = getRules().toMutableList()
        rules.add(rule)
        saveRules(rules.sortedByDescending { it.priority })
    }

    /**
     * 获取所有规则
     */
    fun getRules(): List<UrlRule> {
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
     * 更新规则
     */
    fun updateRule(rule: UrlRule) {
        val rules = getRules().toMutableList()
        val index = rules.indexOfFirst { it.id == rule.id }
        if (index != -1) {
            rules[index] = rule
            saveRules(rules.sortedByDescending { it.priority })
        }
    }

    /**
     * 查找匹配的规则
     */
    private fun findMatchingRule(url: String): UrlRule? {
        return getRules()
            .filter { it.enabled }
            .sortedByDescending { it.priority }
            .firstOrNull { rule ->
                try {
                    url.matches(Regex(rule.pattern))
                } catch (e: Exception) {
                    false
                }
            }
    }

    /**
     * 应用规则
     */
    private fun applyRule(url: String, rule: UrlRule): Boolean {
        return when (rule.action) {
            RuleAction.BLOCK -> {
                Toast.makeText(context, "链接已被阻止: ${rule.name}", Toast.LENGTH_SHORT).show()
                false
            }
            RuleAction.REDIRECT -> {
                rule.replacement?.let { replacement ->
                    val newUrl = url.replace(Regex(rule.pattern), replacement)
                    openWithBrowser(newUrl, OpenConfig())
                    true
                } ?: false
            }
            RuleAction.OPEN_WITH -> {
                rule.targetApp?.let { packageName ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    intent.setPackage(packageName)
                    try {
                        context.startActivity(intent)
                        true
                    } catch (e: Exception) {
                        false
                    }
                } ?: false
            }
            RuleAction.TRANSFORM -> {
                rule.replacement?.let { template ->
                    val transformed = transformUrl(url, template)
                    openWithBrowser(transformed, OpenConfig())
                    true
                } ?: false
            }
            RuleAction.COPY_TO_CLIPBOARD -> {
                copyToClipboard(url)
                Toast.makeText(context, "链接已复制到剪贴板", Toast.LENGTH_SHORT).show()
                true
            }
        }
    }

    /**
     * 转换URL
     */
    private fun transformUrl(url: String, template: String): String {
        val uri = Uri.parse(url)
        var result = template

        // 替换占位符
        result = result.replace("{scheme}", uri.scheme ?: "")
        result = result.replace("{host}", uri.host ?: "")
        result = result.replace("{path}", uri.path ?: "")
        result = result.replace("{query}", uri.query ?: "")
        result = result.replace("{fragment}", uri.fragment ?: "")

        // 替换查询参数
        uri.queryParameterNames.forEach { name ->
            uri.getQueryParameter(name)?.let { value ->
                result = result.replace("{$name}", value)
            }
        }

        return result
    }

    /**
     * 复制到剪贴板
     */
    private fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("URL", text)
        clipboard.setPrimaryClip(clip)
    }

    // ========== 快捷方式管理 ==========

    /**
     * 添加快捷方式
     */
    fun addShortcut(shortcut: UrlShortcut) {
        val shortcuts = getShortcuts().toMutableList()
        shortcuts.add(shortcut)
        saveShortcuts(shortcuts)
    }

    /**
     * 获取所有快捷方式
     */
    fun getShortcuts(): List<UrlShortcut> {
        val json = sharedPrefs.getString(KEY_SHORTCUTS, "[]") ?: "[]"
        return parseShortcutsFromJson(json)
    }

    /**
     * 使用快捷方式
     */
    suspend fun useShortcut(keyword: String): Boolean {
        val shortcut = getShortcuts().firstOrNull {
            it.keyword.equals(keyword, ignoreCase = true)
        }

        return if (shortcut != null) {
            // 更新使用统计
            updateShortcutUsage(shortcut.id)
            openUrl(shortcut.url)
        } else {
            false
        }
    }

    /**
     * 更新快捷方式使用统计
     */
    private fun updateShortcutUsage(id: String) {
        val shortcuts = getShortcuts().toMutableList()
        val index = shortcuts.indexOfFirst { it.id == id }
        if (index != -1) {
            shortcuts[index] = shortcuts[index].copy(
                usageCount = shortcuts[index].usageCount + 1,
                lastUsed = System.currentTimeMillis()
            )
            saveShortcuts(shortcuts)
        }
    }

    // ========== 历史记录管理 ==========

    /**
     * 保存到历史记录
     */
    private fun saveToHistory(url: String) {
        val history = getHistory().toMutableList()

        // 查找是否已存在
        val existing = history.indexOfFirst { it.url == url }
        if (existing != -1) {
            // 更新访问次数
            history[existing] = history[existing].copy(
                timestamp = System.currentTimeMillis(),
                openCount = history[existing].openCount + 1
            )
        } else {
            // 添加新记录
            history.add(0, UrlHistory(url = url))

            // 限制历史记录数量
            if (history.size > MAX_HISTORY_SIZE) {
                history.subList(MAX_HISTORY_SIZE, history.size).clear()
            }
        }

        saveHistory(history)
    }

    /**
     * 获取历史记录
     */
    fun getHistory(): List<UrlHistory> {
        val json = sharedPrefs.getString(KEY_HISTORY, "[]") ?: "[]"
        return parseHistoryFromJson(json)
    }

    /**
     * 清除历史记录
     */
    fun clearHistory() {
        sharedPrefs.edit().remove(KEY_HISTORY).apply()
    }

    // ========== JSON序列化 ==========

    private fun saveRules(rules: List<UrlRule>) {
        val json = convertRulesToJson(rules)
        sharedPrefs.edit().putString(KEY_RULES, json).apply()
    }

    private fun saveShortcuts(shortcuts: List<UrlShortcut>) {
        val json = convertShortcutsToJson(shortcuts)
        sharedPrefs.edit().putString(KEY_SHORTCUTS, json).apply()
    }

    private fun saveHistory(history: List<UrlHistory>) {
        val json = convertHistoryToJson(history)
        sharedPrefs.edit().putString(KEY_HISTORY, json).apply()
    }

    private fun parseRulesFromJson(json: String): List<UrlRule> {
        val list = mutableListOf<UrlRule>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                list.add(
                    UrlRule(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        pattern = item.getString("pattern"),
                        action = RuleAction.valueOf(item.getString("action")),
                        targetApp = item.optString("targetApp", null),
                        replacement = item.optString("replacement", null),
                        enabled = item.getBoolean("enabled"),
                        priority = item.getInt("priority")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseShortcutsFromJson(json: String): List<UrlShortcut> {
        val list = mutableListOf<UrlShortcut>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                list.add(
                    UrlShortcut(
                        id = item.getString("id"),
                        keyword = item.getString("keyword"),
                        url = item.getString("url"),
                        description = item.optString("description", null),
                        icon = item.optString("icon", null),
                        usageCount = item.getInt("usageCount"),
                        lastUsed = item.getLong("lastUsed")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseHistoryFromJson(json: String): List<UrlHistory> {
        val list = mutableListOf<UrlHistory>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                list.add(
                    UrlHistory(
                        url = item.getString("url"),
                        title = item.optString("title", null),
                        timestamp = item.getLong("timestamp"),
                        openCount = item.getInt("openCount")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun convertRulesToJson(rules: List<UrlRule>): String {
        val jsonArray = JSONArray()
        rules.forEach { rule ->
            val json = JSONObject().apply {
                put("id", rule.id)
                put("name", rule.name)
                put("pattern", rule.pattern)
                put("action", rule.action.name)
                rule.targetApp?.let { put("targetApp", it) }
                rule.replacement?.let { put("replacement", it) }
                put("enabled", rule.enabled)
                put("priority", rule.priority)
            }
            jsonArray.put(json)
        }
        return jsonArray.toString()
    }

    private fun convertShortcutsToJson(shortcuts: List<UrlShortcut>): String {
        val jsonArray = JSONArray()
        shortcuts.forEach { shortcut ->
            val json = JSONObject().apply {
                put("id", shortcut.id)
                put("keyword", shortcut.keyword)
                put("url", shortcut.url)
                shortcut.description?.let { put("description", it) }
                shortcut.icon?.let { put("icon", it) }
                put("usageCount", shortcut.usageCount)
                put("lastUsed", shortcut.lastUsed)
            }
            jsonArray.put(json)
        }
        return jsonArray.toString()
    }

    private fun convertHistoryToJson(history: List<UrlHistory>): String {
        val jsonArray = JSONArray()
        history.forEach { item ->
            val json = JSONObject().apply {
                put("url", item.url)
                item.title?.let { put("title", it) }
                put("timestamp", item.timestamp)
                put("openCount", item.openCount)
            }
            jsonArray.put(json)
        }
        return jsonArray.toString()
    }
}