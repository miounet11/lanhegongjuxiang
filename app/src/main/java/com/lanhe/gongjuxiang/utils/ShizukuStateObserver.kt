package com.lanhe.gongjuxiang.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.lanhe.gongjuxiang.utils.ShizukuState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Shizuku状态观察器
 * 用于在Activity或Fragment中观察Shizuku状态变化
 */
class ShizukuStateObserver(
    private val lifecycleOwner: LifecycleOwner,
    private val onStateChanged: (ShizukuState) -> Unit
) : LifecycleEventObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> startObserving()
            Lifecycle.Event.ON_STOP -> stopObserving()
            Lifecycle.Event.ON_DESTROY -> cleanup()
            else -> {}
        }
    }

    private fun startObserving() {
        scope.launch {
            ShizukuManager.shizukuState.collectLatest { state ->
                onStateChanged(state)
            }
        }
    }

    private fun stopObserving() {
        // 停止收集状态变化
    }

    private fun cleanup() {
        lifecycleOwner.lifecycle.removeObserver(this)
        // 清理协程作用域
    }
}
