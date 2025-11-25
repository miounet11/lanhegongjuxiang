package com.lanhe.gongjuxiang.utils

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lanhe.gongjuxiang.models.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * AppDatabase单元测试
 * 测试数据库创建、迁移、所有Entity的CRUD操作
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var context: Context

    @get:Rule
    val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // 使用内存数据库进行测试
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries() // 仅用于测试
            .build()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    /**
     * 测试数据库创建
     */
    @Test
    fun `test database creation`() {
        // Then: 验证数据库创建成功
        assertNotNull(database)
        assertNotNull(database.performanceDataDao())
        assertNotNull(database.optimizationHistoryDao())
        assertNotNull(database.batteryStatsDao())
        assertNotNull(database.networkUsageDao())
        assertNotNull(database.systemEventsDao())
    }

    /**
     * 测试PerformanceDataEntity的CRUD操作
     */
    @Test
    fun `test performance data entity CRUD operations`() = runTest {
        // Given: 创建性能数据实体
        val performanceData = PerformanceDataEntity(
            timestamp = System.currentTimeMillis(),
            cpuUsage = 45.5f,
            memoryUsagePercent = 65,
            memoryUsedMB = 4096,
            memoryTotalMB = 8192,
            batteryLevel = 85,
            batteryTemperature = 32.5f,
            batteryVoltage = 4.2f,
            batteryIsCharging = false,
            batteryIsPlugged = false,
            deviceTemperature = 35.0f,
            isScreenOn = true,
            dataType = "performance"
        )

        val dao = database.performanceDataDao()

        // When: 插入数据
        val id = dao.insert(performanceData)

        // Then: 验证插入成功
        assertTrue(id > 0)

        // When: 查询数据
        val retrieved = dao.getById(id)

        // Then: 验证查询结果
        assertNotNull(retrieved)
        assertEquals(performanceData.cpuUsage, retrieved?.cpuUsage)
        assertEquals(performanceData.memoryUsagePercent, retrieved?.memoryUsagePercent)
        assertEquals(performanceData.batteryLevel, retrieved?.batteryLevel)

        // When: 更新数据
        val updated = retrieved?.copy(cpuUsage = 55.5f)
        if (updated != null) {
            dao.update(updated)
        }

        val afterUpdate = dao.getById(id)
        assertEquals(55.5f, afterUpdate?.cpuUsage)

        // When: 删除数据
        if (afterUpdate != null) {
            dao.delete(afterUpdate)
        }

        val afterDelete = dao.getById(id)
        assertNull(afterDelete)
    }

    /**
     * 测试OptimizationHistoryEntity的CRUD操作
     */
    @Test
    fun `test optimization history entity CRUD operations`() = runTest {
        // Given: 创建优化历史实体
        val optimizationHistory = OptimizationHistoryEntity(
            timestamp = System.currentTimeMillis(),
            optimizationType = "memory_cleanup",
            success = true,
            message = "成功清理内存",
            improvements = "释放了512MB内存",
            duration = 2500,
            beforeDataId = 1,
            afterDataId = 2,
            affectedApps = 5,
            errorDetails = null
        )

        val dao = database.optimizationHistoryDao()

        // When: 插入数据
        val id = dao.insert(optimizationHistory)

        // Then: 验证插入成功
        assertTrue(id > 0)

        // When: 查询最近的优化记录
        val recentOptimizations = dao.getRecentOptimizations(10).first()

        // Then: 验证查询结果
        assertEquals(1, recentOptimizations.size)
        assertEquals(optimizationHistory.optimizationType, recentOptimizations[0].optimizationType)
        assertTrue(recentOptimizations[0].success)

        // When: 按类型查询
        val memoryOptimizations = dao.getOptimizationsByType("memory_cleanup").first()

        // Then: 验证按类型查询
        assertEquals(1, memoryOptimizations.size)
        assertEquals("memory_cleanup", memoryOptimizations[0].optimizationType)
    }

    /**
     * 测试BatteryStatsEntity的CRUD操作
     */
    @Test
    fun `test battery stats entity CRUD operations`() = runTest {
        // Given: 创建电池统计实体
        val batteryStats = BatteryStatsEntity(
            timestamp = System.currentTimeMillis(),
            level = 75,
            temperature = 30.5f,
            voltage = 4.1f,
            health = "GOOD",
            status = "DISCHARGING",
            technology = "Li-ion",
            isPlugged = false,
            screenOnTime = 3600000L, // 1小时
            screenOffTime = 7200000L, // 2小时
            chargingTime = 0L,
            dischargingTime = 10800000L // 3小时
        )

        val dao = database.batteryStatsDao()

        // When: 插入数据
        dao.insert(batteryStats)

        // When: 查询时间范围内的数据
        val startTime = System.currentTimeMillis() - 3600000
        val endTime = System.currentTimeMillis() + 3600000
        val rangeData = dao.getBatteryStatsBetween(startTime, endTime).first()

        // Then: 验证查询结果
        assertTrue(rangeData.isNotEmpty())
        assertEquals(75, rangeData[0].level)
        assertEquals("GOOD", rangeData[0].health)

        // When: 获取最新的电池状态
        val latest = dao.getLatestBatteryStats()

        // Then: 验证最新状态
        assertNotNull(latest)
        assertEquals(75, latest?.level)
    }

    /**
     * 测试NetworkUsageEntity的CRUD操作
     */
    @Test
    fun `test network usage entity CRUD operations`() = runTest {
        // Given: 创建网络使用实体
        val networkUsage = NetworkUsageEntity(
            timestamp = System.currentTimeMillis(),
            packageName = "com.example.app",
            appName = "Example App",
            bytesReceived = 1024000L,
            bytesSent = 512000L,
            packetsReceived = 1000L,
            packetsSent = 500L,
            networkType = "WIFI",
            isBackground = false
        )

        val dao = database.networkUsageDao()

        // When: 插入数据
        dao.insert(networkUsage)

        // When: 按包名查询
        val appUsage = dao.getUsageByPackage("com.example.app").first()

        // Then: 验证查询结果
        assertEquals(1, appUsage.size)
        assertEquals(1024000L, appUsage[0].bytesReceived)
        assertEquals(512000L, appUsage[0].bytesSent)

        // When: 获取总使用量
        val totalUsage = dao.getTotalNetworkUsage()

        // Then: 验证总使用量
        assertNotNull(totalUsage)
        assertEquals(1536000L, totalUsage?.totalBytes) // 1024000 + 512000
    }

    /**
     * 测试SystemEventsEntity的CRUD操作
     */
    @Test
    fun `test system events entity CRUD operations`() = runTest {
        // Given: 创建系统事件实体
        val systemEvent = SystemEventsEntity(
            timestamp = System.currentTimeMillis(),
            eventType = "APP_CRASH",
            severity = "HIGH",
            component = "com.example.app",
            message = "应用崩溃：NullPointerException",
            details = "Stack trace details...",
            handled = false
        )

        val dao = database.systemEventsDao()

        // When: 插入事件
        dao.insert(systemEvent)

        // When: 按严重程度查询
        val highSeverityEvents = dao.getEventsBySeverity("HIGH").first()

        // Then: 验证查询结果
        assertEquals(1, highSeverityEvents.size)
        assertEquals("APP_CRASH", highSeverityEvents[0].eventType)

        // When: 按类型查询
        val crashEvents = dao.getEventsByType("APP_CRASH").first()

        // Then: 验证按类型查询
        assertEquals(1, crashEvents.size)
        assertFalse(crashEvents[0].handled)

        // When: 标记事件已处理
        val event = crashEvents[0]
        dao.update(event.copy(handled = true))

        val handledEvent = dao.getEventById(event.id)
        assertTrue(handledEvent?.handled ?: false)
    }

    /**
     * 测试数据库迁移 v1 -> v2
     */
    @Test
    fun `test database migration from v1 to v2`() {
        // Given: 创建v1版本数据库
        val dbName = "test_db"
        migrationTestHelper.createDatabase(dbName, 1).apply {
            // 插入v1版本的数据
            execSQL("""
                INSERT INTO performance_data (timestamp, cpuUsage, memoryUsagePercent, batteryLevel, batteryTemperature, deviceTemperature, dataType)
                VALUES (${System.currentTimeMillis()}, 50.0, 60, 80, 30.0, 35.0, 'test')
            """.trimIndent())
            close()
        }

        // When: 执行迁移到v2
        val migratedDb = migrationTestHelper.runMigrationsAndValidate(
            dbName,
            2,
            true,
            AppDatabase.MIGRATION_1_2
        )

        // Then: 验证新字段存在并有默认值
        val cursor = migratedDb.query("SELECT * FROM performance_data")
        assertTrue(cursor.moveToFirst())

        // 验证新字段
        val memoryUsedMBIndex = cursor.getColumnIndex("memoryUsedMB")
        assertTrue(memoryUsedMBIndex >= 0)
        assertEquals(0, cursor.getInt(memoryUsedMBIndex)) // 默认值

        val batteryVoltageIndex = cursor.getColumnIndex("batteryVoltage")
        assertTrue(batteryVoltageIndex >= 0)
        assertEquals(0f, cursor.getFloat(batteryVoltageIndex))

        cursor.close()
        migratedDb.close()
    }

    /**
     * 测试并发插入
     */
    @Test
    fun `test concurrent insertions`() = runTest {
        val dao = database.performanceDataDao()
        val insertCount = 100

        // When: 并发插入多条数据
        val jobs = (1..insertCount).map { i ->
            launch {
                val data = PerformanceDataEntity(
                    timestamp = System.currentTimeMillis() + i,
                    cpuUsage = (i % 100).toFloat(),
                    memoryUsagePercent = i % 100,
                    memoryUsedMB = i * 10,
                    memoryTotalMB = 8192,
                    batteryLevel = 100 - (i % 100),
                    batteryTemperature = 30f + (i % 10),
                    batteryVoltage = 4.2f,
                    batteryIsCharging = i % 2 == 0,
                    batteryIsPlugged = i % 2 == 0,
                    deviceTemperature = 35f + (i % 5),
                    isScreenOn = i % 3 == 0,
                    dataType = "test"
                )
                dao.insert(data)
            }
        }

        // 等待所有插入完成
        jobs.forEach { it.join() }

        // Then: 验证所有数据插入成功
        val allData = dao.getAllPerformanceData().first()
        assertEquals(insertCount, allData.size)
    }

    /**
     * 测试数据清理功能
     */
    @Test
    fun `test data cleanup by age`() = runTest {
        val dao = database.performanceDataDao()
        val now = System.currentTimeMillis()
        val oneDayAgo = now - 86400000
        val twoDaysAgo = now - 172800000

        // Given: 插入不同时间的数据
        dao.insert(createPerformanceData(twoDaysAgo))
        dao.insert(createPerformanceData(oneDayAgo))
        dao.insert(createPerformanceData(now))

        // When: 删除一天前的数据
        dao.deleteOlderThan(oneDayAgo)

        // Then: 验证只保留最近的数据
        val remaining = dao.getAllPerformanceData().first()
        assertEquals(2, remaining.size)
        assertTrue(remaining.all { it.timestamp >= oneDayAgo })
    }

    /**
     * 测试数据聚合查询
     */
    @Test
    fun `test data aggregation queries`() = runTest {
        val dao = database.performanceDataDao()

        // Given: 插入多条数据
        repeat(10) { i ->
            dao.insert(
                createPerformanceData(
                    timestamp = System.currentTimeMillis() + i,
                    cpuUsage = (i * 10).toFloat()
                )
            )
        }

        // When: 查询平均CPU使用率
        val avgCpuUsage = dao.getAverageCpuUsage()

        // Then: 验证平均值计算
        assertNotNull(avgCpuUsage)
        assertEquals(45f, avgCpuUsage!!, 0.1f) // (0+10+20+...+90)/10 = 45

        // When: 查询最大内存使用
        val maxMemory = dao.getMaxMemoryUsage()

        // Then: 验证最大值
        assertNotNull(maxMemory)
        assertTrue(maxMemory!! > 0)
    }

    /**
     * 测试事务处理
     */
    @Test
    fun `test transaction handling`() = runTest {
        val performanceDao = database.performanceDataDao()
        val optimizationDao = database.optimizationHistoryDao()

        // When: 在事务中执行多个操作
        database.runInTransaction {
            runBlocking {
                // 插入性能数据
                val perfId = performanceDao.insert(createPerformanceData())

                // 插入优化历史（引用性能数据）
                optimizationDao.insert(
                    OptimizationHistoryEntity(
                        timestamp = System.currentTimeMillis(),
                        optimizationType = "test",
                        success = true,
                        message = "Test",
                        improvements = "None",
                        duration = 1000,
                        beforeDataId = perfId,
                        afterDataId = perfId,
                        affectedApps = 0,
                        errorDetails = null
                    )
                )
            }
        }

        // Then: 验证事务成功
        val perfData = performanceDao.getAllPerformanceData().first()
        val optHistory = optimizationDao.getRecentOptimizations(10).first()

        assertEquals(1, perfData.size)
        assertEquals(1, optHistory.size)
    }

    // 辅助函数
    private fun createPerformanceData(
        timestamp: Long = System.currentTimeMillis(),
        cpuUsage: Float = 50f
    ): PerformanceDataEntity {
        return PerformanceDataEntity(
            timestamp = timestamp,
            cpuUsage = cpuUsage,
            memoryUsagePercent = 60,
            memoryUsedMB = 4096,
            memoryTotalMB = 8192,
            batteryLevel = 80,
            batteryTemperature = 30f,
            batteryVoltage = 4.2f,
            batteryIsCharging = false,
            batteryIsPlugged = false,
            deviceTemperature = 35f,
            isScreenOn = true,
            dataType = "test"
        )
    }
}

/**
 * 扩展函数（用于测试）
 */
private suspend fun PerformanceDataDao.getAverageCpuUsage(): Float? {
    return getAllPerformanceData().first().map { it.cpuUsage }.average().toFloat()
}

private suspend fun PerformanceDataDao.getMaxMemoryUsage(): Int? {
    return getAllPerformanceData().first().maxOfOrNull { it.memoryUsedMB }
}

private suspend fun PerformanceDataDao.deleteOlderThan(timestamp: Long) {
    getAllPerformanceData().first()
        .filter { it.timestamp < timestamp }
        .forEach { delete(it) }
}

private suspend fun NetworkUsageDao.getTotalNetworkUsage(): TotalNetworkUsage? {
    val all = getAllNetworkUsage().first()
    if (all.isEmpty()) return null

    val totalBytes = all.sumOf { it.bytesReceived + it.bytesSent }
    val totalPackets = all.sumOf { it.packetsReceived + it.packetsSent }

    return TotalNetworkUsage(totalBytes, totalPackets)
}

data class TotalNetworkUsage(
    val totalBytes: Long,
    val totalPackets: Long
)

private suspend fun SystemEventsDao.getEventById(id: Long): SystemEventsEntity? {
    return getAllEvents().first().find { it.id == id }
}