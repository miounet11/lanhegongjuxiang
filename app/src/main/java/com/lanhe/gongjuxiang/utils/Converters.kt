package com.lanhe.gongjuxiang.utils

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

/**
 * Room数据库类型转换器
 * 用于转换复杂数据类型到数据库支持的基本类型
 */
class Converters {

    private val gson = Gson()

    // List<String> 转换器
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Map<String, Any> 转换器
    @TypeConverter
    fun fromStringMap(value: Map<String, Any>?): String {
        return gson.toJson(value ?: emptyMap<String, Any>())
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, Any> {
        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        return try {
            gson.fromJson(value, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // Date 转换器
    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }

    // List<OptimizationItem> 转换器
    @TypeConverter
    fun fromOptimizationItemList(items: List<OptimizationItem>?): String {
        return gson.toJson(items ?: emptyList<OptimizationItem>())
    }

    @TypeConverter
    fun toOptimizationItemList(value: String): List<OptimizationItem> {
        val listType = object : TypeToken<List<OptimizationItem>>() {}.type
        return try {
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 优化类型枚举转换器
    @TypeConverter
    fun fromOptimizationState(state: OptimizationState): String {
        return state.name
    }

    @TypeConverter
    fun toOptimizationState(value: String): OptimizationState {
        return try {
            OptimizationState.valueOf(value)
        } catch (e: Exception) {
            OptimizationState.Idle
        }
    }

    // 建议严重程度枚举转换器
    @TypeConverter
    fun fromTipSeverity(severity: TipSeverity): String {
        return severity.name
    }

    @TypeConverter
    fun toTipSeverity(value: String): TipSeverity {
        return try {
            TipSeverity.valueOf(value)
        } catch (e: Exception) {
            TipSeverity.LOW
        }
    }
}
