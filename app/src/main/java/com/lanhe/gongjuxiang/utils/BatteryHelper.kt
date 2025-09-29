package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Battery information helper for real battery data
 */
class BatteryHelper(private val context: Context) {

    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    data class BatteryInfo(
        val level: Int,
        val temperature: Float,
        val voltage: Float,
        val current: Int,
        val health: String,
        val status: String,
        val plugged: String,
        val technology: String,
        val capacity: Long,
        val chargeCounter: Long,
        val energyCounter: Long,
        val isCharging: Boolean
    )

    /**
     * Get real battery information from system
     */
    suspend fun getBatteryInfo(): BatteryInfo = withContext(Dispatchers.IO) {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = level * 100 / scale.toFloat()

        val temperature = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0f
        val voltage = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000.0f

        val status = when (batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
            BatteryManager.BATTERY_STATUS_FULL -> "已充满"
            else -> "未知"
        }

        val health = when (batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "过热"
            BatteryManager.BATTERY_HEALTH_DEAD -> "损坏"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "过压"
            BatteryManager.BATTERY_HEALTH_COLD -> "过冷"
            else -> "未知"
        }

        val plugged = when (batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC充电"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB充电"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "无线充电"
            else -> "未充电"
        }

        val technology = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "锂离子"

        // Get current from BatteryManager (API 21+)
        val current = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        } else {
            readBatteryCurrentFromSysfs()
        }

        // Get capacity (API 21+)
        val capacity = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.toLong() ?: 0L
        } else {
            readBatteryCapacityFromSysfs()
        }

        // Get charge counter (API 21+)
        val chargeCounter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)?.toLong() ?: 0L
        } else {
            0L
        }

        // Get energy counter (API 21+)
        val energyCounter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)?.toLong() ?: 0L
        } else {
            0L
        }

        val isCharging = status == "充电中"

        BatteryInfo(
            level = batteryPct.toInt(),
            temperature = temperature,
            voltage = voltage,
            current = current / 1000, // Convert to mA
            health = health,
            status = status,
            plugged = plugged,
            technology = technology,
            capacity = capacity,
            chargeCounter = chargeCounter,
            energyCounter = energyCounter,
            isCharging = isCharging
        )
    }

    /**
     * Read battery current from sysfs for older devices
     */
    private fun readBatteryCurrentFromSysfs(): Int {
        return try {
            val files = listOf(
                "/sys/class/power_supply/battery/current_now",
                "/sys/class/power_supply/battery/batt_current",
                "/sys/class/power_supply/battery/current_avg"
            )

            for (file in files) {
                val f = File(file)
                if (f.exists() && f.canRead()) {
                    val value = f.readText().trim().toIntOrNull()
                    if (value != null) return value
                }
            }
            0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Read battery capacity from sysfs for older devices
     */
    private fun readBatteryCapacityFromSysfs(): Long {
        return try {
            val files = listOf(
                "/sys/class/power_supply/battery/charge_full",
                "/sys/class/power_supply/battery/charge_full_design",
                "/sys/class/power_supply/battery/full_bat"
            )

            for (file in files) {
                val f = File(file)
                if (f.exists() && f.canRead()) {
                    val value = f.readText().trim().toLongOrNull()
                    if (value != null) return value / 1000 // Convert to mAh
                }
            }
            4000L // Default typical capacity
        } catch (e: Exception) {
            4000L
        }
    }

    /**
     * Get battery health percentage based on charge counter and design capacity
     */
    fun getBatteryHealthPercentage(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val currentCapacity = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)?.toLong() ?: 0L
            val designCapacity = readBatteryCapacityFromSysfs()

            if (currentCapacity > 0 && designCapacity > 0) {
                ((currentCapacity.toFloat() / (designCapacity * 1000)) * 100).toInt().coerceIn(0, 100)
            } else {
                95 // Default healthy battery
            }
        } else {
            95
        }
    }

    /**
     * Get battery cycle count (estimated based on charge counter)
     */
    fun getBatteryCycleCount(): Int {
        // This is an estimation as Android doesn't provide direct cycle count
        // We estimate based on total charge delivered
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val chargeCounter = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)?.toLong() ?: 0L
            val designCapacity = readBatteryCapacityFromSysfs()

            if (chargeCounter > 0 && designCapacity > 0) {
                // Rough estimation: total charge / design capacity
                (chargeCounter / (designCapacity * 1000)).toInt()
            } else {
                readCycleCountFromSysfs()
            }
        } else {
            readCycleCountFromSysfs()
        }
    }

    /**
     * Try to read cycle count from sysfs (device-specific)
     */
    private fun readCycleCountFromSysfs(): Int {
        return try {
            val files = listOf(
                "/sys/class/power_supply/battery/cycle_count",
                "/sys/class/power_supply/battery/battery_cycle",
                "/sys/class/power_supply/bms/battery_cycle"
            )

            for (file in files) {
                val f = File(file)
                if (f.exists() && f.canRead()) {
                    val value = f.readText().trim().toIntOrNull()
                    if (value != null && value > 0) return value
                }
            }
            // If no cycle count available, estimate based on usage
            100 // Default estimate
        } catch (e: Exception) {
            100
        }
    }

    /**
     * Optimize battery settings (requires Shizuku or root)
     */
    suspend fun optimizeBatterySettings(): Boolean = withContext(Dispatchers.IO) {
        try {
            // These optimizations work on some devices without root

            // Try to disable battery optimization for our app
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent().apply {
                    action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = android.net.Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get power profile information
     */
    fun getPowerProfile(): Map<String, Double> {
        val profile = mutableMapOf<String, Double>()

        try {
            // Try to access PowerProfile via reflection (hidden API)
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val powerProfile = powerProfileClass.getConstructor(Context::class.java).newInstance(context)

            val methods = mapOf(
                "battery.capacity" to "getBatteryCapacity",
                "cpu.idle" to "getAveragePower",
                "cpu.active" to "getAveragePower",
                "screen.on" to "getAveragePower",
                "wifi.on" to "getAveragePower",
                "bluetooth.on" to "getAveragePower"
            )

            for ((key, methodName) in methods) {
                try {
                    val method = if (key == "battery.capacity") {
                        powerProfileClass.getMethod(methodName)
                    } else {
                        powerProfileClass.getMethod(methodName, String::class.java)
                    }

                    val value = if (key == "battery.capacity") {
                        method.invoke(powerProfile) as Double
                    } else {
                        method.invoke(powerProfile, key.substringBefore(".")) as Double
                    }

                    profile[key] = value
                } catch (e: Exception) {
                    // Method not available
                }
            }
        } catch (e: Exception) {
            // PowerProfile not accessible, use defaults
            profile["battery.capacity"] = readBatteryCapacityFromSysfs().toDouble()
        }

        return profile
    }
}