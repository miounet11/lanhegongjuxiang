package com.lanhe.gongjuxiang.utils

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

/**
 * Room数据库类型转换器
 * 用于转换复杂数据类型到数据库支持的基本类型
 *
 * 注意：避免在这里引用自定义类型，改用JSON字符串存储
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
}
