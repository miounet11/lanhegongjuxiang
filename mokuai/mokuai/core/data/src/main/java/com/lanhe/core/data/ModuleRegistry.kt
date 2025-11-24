package com.lanhe.core.data

import com.lanhe.core.common.api.ModuleApi
import com.lanhe.core.common.api.ModuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 模块注册管理器
 * 负责管理所有模块的生命周期和状态
 *
 * 注: 移除了Hilt注解(@Singleton, @Inject)以避免依赖问题
 */
class ModuleRegistry {

    companion object {
        @Volatile
        private var INSTANCE: ModuleRegistry? = null

        fun getInstance(): ModuleRegistry {
            return INSTANCE ?: synchronized(this) {
                ModuleRegistry().also { INSTANCE = it }
            }
        }
    }
    
    private val modules = mutableMapOf<String, ModuleApi>()
    private val _moduleStates = MutableStateFlow<Map<String, ModuleStatus>>(emptyMap())
    val moduleStates: StateFlow<Map<String, ModuleStatus>> = _moduleStates.asStateFlow()
    
    /**
     * 注册模块
     */
    fun registerModule(module: ModuleApi) {
        modules[module.getModuleName()] = module
        updateModuleState(module.getModuleName(), ModuleStatus.UNINITIALIZED)
    }
    
    /**
     * 注销模块
     */
    fun unregisterModule(moduleName: String) {
        modules.remove(moduleName)
        updateModuleState(moduleName, ModuleStatus.DISABLED)
    }
    
    /**
     * 获取模块
     */
    fun getModule(moduleName: String): ModuleApi? {
        return modules[moduleName]
    }
    
    /**
     * 初始化所有模块
     */
    suspend fun initializeAllModules(): Map<String, Result<Unit>> {
        val results = mutableMapOf<String, Result<Unit>>()
        
        modules.forEach { (name, module) ->
            updateModuleState(name, ModuleStatus.INITIALIZING)
            val result = module.initialize()
            results[name] = result
            
            if (result.isSuccess) {
                updateModuleState(name, ModuleStatus.INITIALIZED)
            } else {
                updateModuleState(name, ModuleStatus.ERROR)
            }
        }
        
        return results
    }
    
    /**
     * 初始化特定模块
     */
    suspend fun initializeModule(moduleName: String): Result<Unit> {
        val module = modules[moduleName] ?: return Result.failure(
            IllegalArgumentException("Module $moduleName not found")
        )
        
        updateModuleState(moduleName, ModuleStatus.INITIALIZING)
        val result = module.initialize()
        
        if (result.isSuccess) {
            updateModuleState(moduleName, ModuleStatus.INITIALIZED)
        } else {
            updateModuleState(moduleName, ModuleStatus.ERROR)
        }
        
        return result
    }
    
    /**
     * 清理所有模块
     */
    suspend fun cleanupAllModules(): Map<String, Result<Unit>> {
        val results = mutableMapOf<String, Result<Unit>>()
        
        modules.forEach { (name, module) ->
            results[name] = module.cleanup()
        }
        
        return results
    }
    
    /**
     * 获取所有已注册的模块列表
     */
    fun getAllModules(): List<String> {
        return modules.keys.toList()
    }
    
    /**
     * 检查模块是否已注册
     */
    fun isModuleRegistered(moduleName: String): Boolean {
        return modules.containsKey(moduleName)
    }
    
    /**
     * 获取模块依赖关系
     */
    fun getModuleDependencies(moduleName: String): List<String> {
        // 这里可以定义模块间的依赖关系
        val dependencyMap = mapOf(
            "performance-monitor" to listOf("core:common"),
            "memory-manager" to listOf("performance-monitor"),
            "network" to listOf("core:common"),
            "filesystem" to listOf("core:common"),
            "database" to listOf("core:common"),
            "analytics" to listOf("network", "database"),
            "crash" to listOf("core:common"),
            "ui" to listOf("core:common")
        )
        
        return dependencyMap[moduleName] ?: emptyList()
    }
    
    private fun updateModuleState(moduleName: String, status: ModuleStatus) {
        val currentState = _moduleStates.value.toMutableMap()
        currentState[moduleName] = status
        _moduleStates.value = currentState
    }
}
