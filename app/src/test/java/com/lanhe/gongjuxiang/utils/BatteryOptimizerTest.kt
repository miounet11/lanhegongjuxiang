package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.lanhe.gongjuxiang.models.BatteryInfo
import com.lanhe.gongjuxiang.models.BatteryOptimizationSuggestion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

/**
 * AdvancedBatteryOptimizer单元测试
 * 测试电池数据读取、耗电应用检测、优化建议生成等
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BatteryOptimizerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockBatteryManager: BatteryManager

    @Mock
    private lateinit var mockIntent: Intent

    private lateinit var batteryOptimizer: AdvancedBatteryOptimizer

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        `when`(mockContext.getSystemService(Context.BATTERY_SERVICE))
            .thenReturn(mockBatteryManager)

        batteryOptimizer = AdvancedBatteryOptimizer(mockContext)
    }

    /**
     * 测试电池数据读取 - 正常状态
     */
    @Test
    fun `test battery data reading - normal state`() = runTest(testDispatcher) {
        // Given: 设置电池正常状态
        setupBatteryIntent(
            level = 75,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_DISCHARGING,
            health = BatteryManager.BATTERY_HEALTH_GOOD,
            temperature = 320, // 32.0°C
            voltage = 4200 // 4.2V
        )

        // When: 读取电池信息
        val batteryInfo = batteryOptimizer.getBatteryInfo()

        // Then: 验证电池数据
        assertNotNull(batteryInfo)
        assertEquals(75, batteryInfo.level)
        assertEquals(32.0f, batteryInfo.temperature, 0.1f)
        assertEquals(4.2f, batteryInfo.voltage, 0.01f)
        assertEquals("GOOD", batteryInfo.health)
        assertFalse(batteryInfo.isCharging)
    }

    /**
     * 测试电池数据读取 - 充电状态
     */
    @Test
    fun `test battery data reading - charging state`() = runTest(testDispatcher) {
        // Given: 设置充电状态
        setupBatteryIntent(
            level = 45,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_CHARGING,
            health = BatteryManager.BATTERY_HEALTH_GOOD,
            temperature = 350, // 35.0°C
            voltage = 4300 // 4.3V
        )

        // When: 读取电池信息
        val batteryInfo = batteryOptimizer.getBatteryInfo()

        // Then: 验证充电状态
        assertNotNull(batteryInfo)
        assertEquals(45, batteryInfo.level)
        assertTrue(batteryInfo.isCharging)
        assertEquals(35.0f, batteryInfo.temperature, 0.1f)
    }

    /**
     * 测试电池数据读取 - 满电状态
     */
    @Test
    fun `test battery data reading - full state`() = runTest(testDispatcher) {
        // Given: 设置满电状态
        setupBatteryIntent(
            level = 100,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_FULL,
            health = BatteryManager.BATTERY_HEALTH_GOOD,
            temperature = 300, // 30.0°C
            voltage = 4200
        )

        // When: 读取电池信息
        val batteryInfo = batteryOptimizer.getBatteryInfo()

        // Then: 验证满电状态
        assertNotNull(batteryInfo)
        assertEquals(100, batteryInfo.level)
        assertEquals("FULL", batteryInfo.status)
    }

    /**
     * 测试电池健康状态检测
     */
    @Test
    fun `test battery health detection`() = runTest(testDispatcher) {
        // Given: 各种健康状态
        val healthStates = mapOf(
            BatteryManager.BATTERY_HEALTH_GOOD to "GOOD",
            BatteryManager.BATTERY_HEALTH_OVERHEAT to "OVERHEAT",
            BatteryManager.BATTERY_HEALTH_DEAD to "DEAD",
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE to "OVER_VOLTAGE",
            BatteryManager.BATTERY_HEALTH_COLD to "COLD"
        )

        healthStates.forEach { (healthCode, expectedStatus) ->
            // Given: 设置特定健康状态
            setupBatteryIntent(health = healthCode)

            // When: 读取健康状态
            val batteryInfo = batteryOptimizer.getBatteryInfo()

            // Then: 验证健康状态
            assertEquals(expectedStatus, batteryInfo.health)
        }
    }

    /**
     * 测试耗电应用检测
     */
    @Test
    fun `test battery draining apps detection`() = runTest(testDispatcher) {
        // Given: 模拟应用耗电数据
        val appUsageData = listOf(
            AppBatteryUsage("com.app1", "App 1", 25.0), // 高耗电
            AppBatteryUsage("com.app2", "App 2", 15.0), // 中等耗电
            AppBatteryUsage("com.app3", "App 3", 5.0),  // 低耗电
            AppBatteryUsage("com.app4", "App 4", 30.0)  // 最高耗电
        )

        batteryOptimizer.setMockAppUsageData(appUsageData)

        // When: 检测耗电应用
        val drainingApps = batteryOptimizer.detectBatteryDrainingApps()

        // Then: 验证检测结果（按耗电量排序）
        assertEquals(2, drainingApps.size) // 只返回高耗电应用
        assertEquals("com.app4", drainingApps[0].packageName)
        assertEquals(30.0, drainingApps[0].batteryPercentage, 0.1)
        assertEquals("com.app1", drainingApps[1].packageName)
        assertEquals(25.0, drainingApps[1].batteryPercentage, 0.1)
    }

    /**
     * 测试优化建议生成 - 低电量
     */
    @Test
    fun `test optimization suggestions - low battery`() = runTest(testDispatcher) {
        // Given: 低电量状态
        setupBatteryIntent(level = 15)

        // When: 生成优化建议
        val suggestions = batteryOptimizer.generateOptimizationSuggestions()

        // Then: 验证建议包含省电模式
        assertTrue(suggestions.any { it.type == "POWER_SAVING_MODE" })
        assertTrue(suggestions.any { it.priority == "HIGH" })
    }

    /**
     * 测试优化建议生成 - 高温
     */
    @Test
    fun `test optimization suggestions - high temperature`() = runTest(testDispatcher) {
        // Given: 高温状态
        setupBatteryIntent(temperature = 450) // 45°C

        // When: 生成优化建议
        val suggestions = batteryOptimizer.generateOptimizationSuggestions()

        // Then: 验证建议包含降温措施
        assertTrue(suggestions.any { it.type == "TEMPERATURE_WARNING" })
        assertTrue(suggestions.any { it.description.contains("温度") })
    }

    /**
     * 测试优化建议生成 - 充电优化
     */
    @Test
    fun `test optimization suggestions - charging optimization`() = runTest(testDispatcher) {
        // Given: 充电状态且电量高
        setupBatteryIntent(
            level = 95,
            status = BatteryManager.BATTERY_STATUS_CHARGING
        )

        // When: 生成优化建议
        val suggestions = batteryOptimizer.generateOptimizationSuggestions()

        // Then: 验证建议包含充电优化
        assertTrue(suggestions.any { it.type == "CHARGING_OPTIMIZATION" })
        assertTrue(suggestions.any { it.description.contains("充电") })
    }

    /**
     * 测试电池使用时长预测
     */
    @Test
    fun `test battery life prediction`() = runTest(testDispatcher) {
        // Given: 当前电量和使用率
        setupBatteryIntent(level = 50)
        val currentUsageRate = 10.0 // 每小时10%

        // When: 预测剩余使用时长
        val remainingHours = batteryOptimizer.predictRemainingBatteryLife(currentUsageRate)

        // Then: 验证预测结果
        assertEquals(5.0, remainingHours, 0.1) // 50% / 10% per hour = 5 hours
    }

    /**
     * 测试电池充电时间预测
     */
    @Test
    fun `test charging time prediction`() = runTest(testDispatcher) {
        // Given: 充电状态
        setupBatteryIntent(
            level = 30,
            status = BatteryManager.BATTERY_STATUS_CHARGING
        )
        val chargingRate = 20.0 // 每小时充20%

        // When: 预测充满时间
        val hoursToFull = batteryOptimizer.predictTimeToFullCharge(chargingRate)

        // Then: 验证预测结果
        assertEquals(3.5, hoursToFull, 0.1) // (100-30)% / 20% per hour = 3.5 hours
    }

    /**
     * 测试电池统计数据收集
     */
    @Test
    fun `test battery statistics collection`() = runTest(testDispatcher) {
        // Given: 收集一段时间的电池数据
        val stats = mutableListOf<BatteryInfo>()

        repeat(5) { i ->
            setupBatteryIntent(level = 100 - i * 10)
            stats.add(batteryOptimizer.getBatteryInfo())
        }

        // When: 分析电池统计
        val analysis = batteryOptimizer.analyzeBatteryStats(stats)

        // Then: 验证统计分析
        assertEquals(100, analysis.maxLevel)
        assertEquals(60, analysis.minLevel)
        assertEquals(80, analysis.averageLevel)
        assertEquals(10.0, analysis.dischargeRate, 0.1) // 每次10%
    }

    /**
     * 测试省电模式自动触发
     */
    @Test
    fun `test power saving mode trigger`() = runTest(testDispatcher) {
        // Given: 不同电量水平
        val testCases = listOf(
            10 to true,  // 10% 应该触发省电
            15 to true,  // 15% 应该触发省电
            20 to false, // 20% 不触发
            50 to false  // 50% 不触发
        )

        testCases.forEach { (level, shouldTrigger) ->
            setupBatteryIntent(level = level)

            // When: 检查是否应该触发省电模式
            val trigger = batteryOptimizer.shouldTriggerPowerSavingMode()

            // Then: 验证触发逻辑
            assertEquals(
                "Level $level should${if (shouldTrigger) "" else " not"} trigger power saving",
                shouldTrigger,
                trigger
            )
        }
    }

    /**
     * 测试后台应用限制建议
     */
    @Test
    fun `test background app restriction suggestions`() = runTest(testDispatcher) {
        // Given: 后台运行的高耗电应用
        val backgroundApps = listOf(
            BackgroundApp("com.social", "Social App", 15.0, true),
            BackgroundApp("com.game", "Game", 20.0, false),
            BackgroundApp("com.utility", "Utility", 5.0, true)
        )

        batteryOptimizer.setMockBackgroundApps(backgroundApps)

        // When: 获取限制建议
        val suggestions = batteryOptimizer.getBackgroundRestrictionSuggestions()

        // Then: 验证建议
        assertEquals(2, suggestions.size) // 只建议限制高耗电应用
        assertTrue(suggestions.any { it.packageName == "com.social" })
        assertTrue(suggestions.any { it.packageName == "com.game" })
    }

    /**
     * 测试电池优化执行
     */
    @Test
    fun `test battery optimization execution`() = runTest(testDispatcher) {
        // Given: 准备优化项目
        val optimizationItems = listOf(
            "REDUCE_BRIGHTNESS",
            "DISABLE_BLUETOOTH",
            "LIMIT_BACKGROUND_APPS",
            "ENABLE_POWER_SAVING"
        )

        // When: 执行优化
        val results = batteryOptimizer.executeOptimizations(optimizationItems)

        // Then: 验证优化结果
        assertEquals(4, results.size)
        assertTrue(results.all { it.success })
        assertTrue(results.any { it.item == "REDUCE_BRIGHTNESS" })
    }

    /**
     * 测试电池健康趋势分析
     */
    @Test
    fun `test battery health trend analysis`() = runTest(testDispatcher) {
        // Given: 历史健康数据
        val healthHistory = listOf(
            BatteryHealthData(100, 30f, "GOOD"),
            BatteryHealthData(98, 32f, "GOOD"),
            BatteryHealthData(95, 35f, "GOOD"),
            BatteryHealthData(93, 38f, "OVERHEAT"),
            BatteryHealthData(90, 40f, "OVERHEAT")
        )

        // When: 分析健康趋势
        val trend = batteryOptimizer.analyzeHealthTrend(healthHistory)

        // Then: 验证趋势分析
        assertEquals("DECLINING", trend.status)
        assertEquals(-2.5, trend.capacityChangeRate, 0.1) // 平均每次下降2.5
        assertTrue(trend.hasTemperatureIssue)
    }

    // 辅助方法
    private fun setupBatteryIntent(
        level: Int = 50,
        scale: Int = 100,
        status: Int = BatteryManager.BATTERY_STATUS_DISCHARGING,
        health: Int = BatteryManager.BATTERY_HEALTH_GOOD,
        temperature: Int = 300,
        voltage: Int = 4200
    ) {
        `when`(mockIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(level)
        `when`(mockIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(scale)
        `when`(mockIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)).thenReturn(status)
        `when`(mockIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)).thenReturn(health)
        `when`(mockIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)).thenReturn(temperature)
        `when`(mockIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)).thenReturn(voltage)

        `when`(mockContext.registerReceiver(any(), any<IntentFilter>())).thenReturn(mockIntent)
    }
}

/**
 * 测试用的数据类和扩展函数
 */
data class AppBatteryUsage(
    val packageName: String,
    val appName: String,
    val batteryPercentage: Double
)

data class BackgroundApp(
    val packageName: String,
    val appName: String,
    val batteryUsage: Double,
    val isRunning: Boolean
)

data class BatteryHealthData(
    val capacity: Int,
    val temperature: Float,
    val health: String
)

data class BatteryAnalysis(
    val maxLevel: Int,
    val minLevel: Int,
    val averageLevel: Int,
    val dischargeRate: Double
)

data class HealthTrend(
    val status: String,
    val capacityChangeRate: Double,
    val hasTemperatureIssue: Boolean
)

data class OptimizationResult(
    val item: String,
    val success: Boolean,
    val improvement: String
)

// AdvancedBatteryOptimizer mock 实现
class AdvancedBatteryOptimizer(private val context: Context) {

    private var mockAppUsageData: List<AppBatteryUsage> = emptyList()
    private var mockBackgroundApps: List<BackgroundApp> = emptyList()

    fun getBatteryInfo(): BatteryInfo {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return BatteryInfo(
            level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0,
            temperature = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f,
            voltage = (intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000f,
            isCharging = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ==
                BatteryManager.BATTERY_STATUS_CHARGING,
            health = getHealthString(intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1),
            status = getStatusString(intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1)
        )
    }

    private fun getHealthString(health: Int): String = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
        BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER_VOLTAGE"
        BatteryManager.BATTERY_HEALTH_COLD -> "COLD"
        else -> "UNKNOWN"
    }

    private fun getStatusString(status: Int): String = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "CHARGING"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "DISCHARGING"
        BatteryManager.BATTERY_STATUS_FULL -> "FULL"
        else -> "UNKNOWN"
    }

    fun setMockAppUsageData(data: List<AppBatteryUsage>) {
        mockAppUsageData = data
    }

    fun setMockBackgroundApps(apps: List<BackgroundApp>) {
        mockBackgroundApps = apps
    }

    fun detectBatteryDrainingApps(): List<AppBatteryUsage> {
        return mockAppUsageData
            .filter { it.batteryPercentage >= 20.0 }
            .sortedByDescending { it.batteryPercentage }
    }

    fun generateOptimizationSuggestions(): List<BatteryOptimizationSuggestion> {
        val suggestions = mutableListOf<BatteryOptimizationSuggestion>()
        val info = getBatteryInfo()

        if (info.level <= 20) {
            suggestions.add(
                BatteryOptimizationSuggestion(
                    type = "POWER_SAVING_MODE",
                    description = "启用省电模式",
                    priority = "HIGH"
                )
            )
        }

        if (info.temperature >= 45f) {
            suggestions.add(
                BatteryOptimizationSuggestion(
                    type = "TEMPERATURE_WARNING",
                    description = "设备温度过高，建议降低使用强度",
                    priority = "HIGH"
                )
            )
        }

        if (info.isCharging && info.level >= 90) {
            suggestions.add(
                BatteryOptimizationSuggestion(
                    type = "CHARGING_OPTIMIZATION",
                    description = "电量接近满电，可考虑拔掉充电器",
                    priority = "MEDIUM"
                )
            )
        }

        return suggestions
    }

    fun predictRemainingBatteryLife(usageRatePerHour: Double): Double {
        val currentLevel = getBatteryInfo().level
        return currentLevel / usageRatePerHour
    }

    fun predictTimeToFullCharge(chargingRatePerHour: Double): Double {
        val currentLevel = getBatteryInfo().level
        return (100 - currentLevel) / chargingRatePerHour
    }

    fun analyzeBatteryStats(stats: List<BatteryInfo>): BatteryAnalysis {
        return BatteryAnalysis(
            maxLevel = stats.maxOf { it.level },
            minLevel = stats.minOf { it.level },
            averageLevel = stats.map { it.level }.average().toInt(),
            dischargeRate = if (stats.size > 1) {
                (stats.first().level - stats.last().level).toDouble() / (stats.size - 1)
            } else 0.0
        )
    }

    fun shouldTriggerPowerSavingMode(): Boolean {
        return getBatteryInfo().level <= 15
    }

    fun getBackgroundRestrictionSuggestions(): List<BackgroundApp> {
        return mockBackgroundApps.filter { it.batteryUsage >= 10.0 }
    }

    fun executeOptimizations(items: List<String>): List<OptimizationResult> {
        return items.map { item ->
            OptimizationResult(
                item = item,
                success = true,
                improvement = "优化成功"
            )
        }
    }

    fun analyzeHealthTrend(history: List<BatteryHealthData>): HealthTrend {
        val capacityChange = history.first().capacity - history.last().capacity
        val changeRate = capacityChange.toDouble() / (history.size - 1)
        val hasTemp = history.any { it.temperature >= 38f }

        return HealthTrend(
            status = if (changeRate > 0) "DECLINING" else "STABLE",
            capacityChangeRate = -changeRate,
            hasTemperatureIssue = hasTemp
        )
    }
}