package com.lanhe.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore扩展属性必须在顶层定义
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "module_shared_prefs"
)

/**
 * 模块间数据共享存储
 */
class ModuleDataStore private constructor(private val context: Context) {
    
    /**
     * 保存模块数据
     */
    suspend fun saveModuleData(moduleName: String, key: String, value: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey("${moduleName}_$key")] = value
        }
    }
    
    /**
     * 获取模块数据
     */
    fun getModuleData(moduleName: String, key: String): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey("${moduleName}_$key")]
        }
    }
    
    /**
     * 保存模块状态
     */
    suspend fun saveModuleStatus(moduleName: String, status: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey("${moduleName}_status")] = status
        }
    }
    
    /**
     * 获取模块状态
     */
    fun getModuleStatus(moduleName: String): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey("${moduleName}_status")]
        }
    }
    
    /**
     * 保存模块配置
     */
    suspend fun saveModuleConfig(moduleName: String, config: Map<String, Any>) {
        context.dataStore.edit { preferences ->
            config.forEach { (key, value) ->
                when (value) {
                    is String -> preferences[stringPreferencesKey("${moduleName}_config_$key")] = value
                    is Boolean -> preferences[booleanPreferencesKey("${moduleName}_config_$key")] = value
                    // 可以添加更多类型支持
                }
            }
        }
    }
    
    /**
     * 获取模块配置
     */
    fun getModuleConfig(moduleName: String, key: String): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey("${moduleName}_config_$key")]
        }
    }
    
    /**
     * 清除模块数据
     */
    suspend fun clearModuleData(moduleName: String) {
        context.dataStore.edit { preferences ->
            preferences.asMap().keys.filter { key ->
                val keyName = key.name
                keyName.startsWith("${moduleName}_")
            }.forEach { key ->
                preferences.remove(key)
            }
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: ModuleDataStore? = null
        
        fun getInstance(context: Context): ModuleDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ModuleDataStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
