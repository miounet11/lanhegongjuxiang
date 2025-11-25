package com.lanhe.gongjuxiang.utils

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 系统事件数据访问对象
 */
@Dao
interface SystemEventsDao {
    @Query("SELECT * FROM system_events ORDER BY timestamp DESC LIMIT :limit")
    fun getLatestEvents(limit: Int = 100): Flow<List<SystemEventsEntity>>

    @Query("SELECT * FROM system_events WHERE eventType = :eventType ORDER BY timestamp DESC LIMIT :limit")
    fun getEventsByType(eventType: String, limit: Int = 100): Flow<List<SystemEventsEntity>>

    @Query("SELECT * FROM system_events WHERE severity = :severity ORDER BY timestamp DESC LIMIT :limit")
    fun getEventsBySeverity(severity: String, limit: Int = 100): Flow<List<SystemEventsEntity>>

    @Query("SELECT * FROM system_events WHERE category = :category ORDER BY timestamp DESC LIMIT :limit")
    fun getEventsByCategory(category: String, limit: Int = 100): Flow<List<SystemEventsEntity>>

    @Query("SELECT * FROM system_events WHERE resolved = :resolved ORDER BY timestamp DESC")
    fun getEventsByResolvedStatus(resolved: Boolean): Flow<List<SystemEventsEntity>>

    @Query("SELECT * FROM system_events WHERE timestamp > :startTime AND timestamp < :endTime ORDER BY timestamp DESC")
    fun getEventsInTimeRange(startTime: Long, endTime: Long): Flow<List<SystemEventsEntity>>

    @Query("SELECT COUNT(*) FROM system_events WHERE severity = :severity AND resolved = false")
    suspend fun getUnresolvedCountBySeverity(severity: String): Int

    @Query("SELECT category, COUNT(*) as count FROM system_events WHERE timestamp > :startTime GROUP BY category")
    suspend fun getEventCategoryCounts(startTime: Long): List<EventCategoryCount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: SystemEventsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<SystemEventsEntity>)

    @Update
    suspend fun update(event: SystemEventsEntity)

    @Query("UPDATE system_events SET resolved = true, resolvedTimestamp = :timestamp WHERE id = :eventId")
    suspend fun markAsResolved(eventId: Long, timestamp: Long)

    @Delete
    suspend fun delete(event: SystemEventsEntity)

    @Query("DELETE FROM system_events WHERE timestamp < :timestamp")
    suspend fun deleteOldEvents(timestamp: Long)

    @Query("DELETE FROM system_events")
    suspend fun clearAll()
}

/**
 * 事件分类计数
 */
data class EventCategoryCount(
    val category: String,
    val count: Int
)