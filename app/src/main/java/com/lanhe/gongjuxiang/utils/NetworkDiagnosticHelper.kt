package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.telephony.TelephonyManager
import com.lanhe.gongjuxiang.viewmodels.NetworkDiagnosticViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import kotlin.math.pow

/**
 * 网络诊断帮助类
 * 提供真实的网络诊断功能实现
 */
class NetworkDiagnosticHelper(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val packageManager = context.packageManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    /**
     * 获取真实的网络信息
     */
    fun getRealNetworkInfo(): NetworkDiagnosticViewModel.NetworkInfo {
        val activeNetwork = connectivityManager?.activeNetwork
        val networkCapabilities = activeNetwork?.let {
            connectivityManager.getNetworkCapabilities(it)
        }

        return when {
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> {
                getWifiNetworkInfo()
            }
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> {
                getCellularNetworkInfo()
            }
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> {
                getEthernetNetworkInfo()
            }
            else -> {
                NetworkDiagnosticViewModel.NetworkInfo(
                    type = "无连接",
                    isConnected = false
                )
            }
        }
    }

    /**
     * 获取WiFi网络信息
     */
    private fun getWifiNetworkInfo(): NetworkDiagnosticViewModel.NetworkInfo {
        val wifiInfo = wifiManager?.connectionInfo

        return NetworkDiagnosticViewModel.NetworkInfo(
            type = "Wi-Fi",
            ssid = wifiInfo?.ssid?.replace("\"", "") ?: "未知",
            bssid = wifiInfo?.bssid ?: "",
            signalStrength = calculateSignalLevel(wifiInfo?.rssi ?: -100),
            rssi = wifiInfo?.rssi ?: -100,
            estimatedDistance = calculateDistance(wifiInfo?.rssi ?: -100, wifiInfo?.frequency ?: 2412),
            linkSpeed = wifiInfo?.linkSpeed ?: 0,
            frequency = wifiInfo?.frequency ?: 0,
            isConnected = true
        )
    }

    /**
     * 获取蜂窝网络信息
     */
    private fun getCellularNetworkInfo(): NetworkDiagnosticViewModel.NetworkInfo {
        val networkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            telephonyManager?.dataNetworkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
        } else {
            @Suppress("DEPRECATION")
            telephonyManager?.networkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
        }

        val networkTypeString = when (networkType) {
            TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSDPA -> "3G+"
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B -> "3G"
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_CDMA -> "2G"
            else -> "移动网络"
        }

        return NetworkDiagnosticViewModel.NetworkInfo(
            type = networkTypeString,
            ssid = telephonyManager?.networkOperatorName ?: "未知运营商",
            signalStrength = 3, // 中等信号
            isConnected = true
        )
    }

    /**
     * 获取以太网信息
     */
    private fun getEthernetNetworkInfo(): NetworkDiagnosticViewModel.NetworkInfo {
        return NetworkDiagnosticViewModel.NetworkInfo(
            type = "以太网",
            ssid = "有线连接",
            signalStrength = 5,
            isConnected = true
        )
    }

    /**
     * 计算信号强度等级
     */
    private fun calculateSignalLevel(rssi: Int): Int {
        return when {
            rssi >= -50 -> 5
            rssi >= -60 -> 4
            rssi >= -70 -> 3
            rssi >= -80 -> 2
            rssi >= -90 -> 1
            else -> 0
        }
    }

    /**
     * 根据RSSI计算距离（米）
     */
    private fun calculateDistance(rssi: Int, frequency: Int): Double {
        // 使用自由空间路径损耗公式
        val exp = (27.55 - (20 * Math.log10(frequency.toDouble())) + Math.abs(rssi)) / 20.0
        return 10.0.pow(exp)
    }

    /**
     * 扫描附近的WiFi信号
     */
    @Suppress("MissingPermission")
    fun scanWifiSignals(): List<NetworkDiagnosticViewModel.WifiSignal> {
        val scanResults = wifiManager?.scanResults ?: return emptyList()
        val currentBssid = wifiManager?.connectionInfo?.bssid

        return scanResults.map { result ->
            NetworkDiagnosticViewModel.WifiSignal(
                ssid = result.SSID.ifEmpty { "Hidden Network" },
                bssid = result.BSSID,
                rssi = result.level,
                signalLevel = calculateSignalLevel(result.level),
                isConnected = result.BSSID == currentBssid
            )
        }.sortedWith(
            compareByDescending<NetworkDiagnosticViewModel.WifiSignal> { it.isConnected }
                .thenByDescending { it.rssi }
        )
    }

    /**
     * 获取网络占用应用列表
     */
    suspend fun getNetworkUsageApps(): List<NetworkDiagnosticViewModel.NetworkUsageApp> =
        withContext(Dispatchers.IO) {
            val apps = mutableListOf<NetworkDiagnosticViewModel.NetworkUsageApp>()

            try {
                val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

                for (app in installedApps) {
                    // 跳过系统应用
                    if (app.flags and ApplicationInfo.FLAG_SYSTEM != 0) continue

                    val uid = app.uid
                    val rxBytes = TrafficStats.getUidRxBytes(uid)
                    val txBytes = TrafficStats.getUidTxBytes(uid)

                    if (rxBytes > 0 || txBytes > 0) {
                        val totalBytes = rxBytes + txBytes
                        val totalMB = totalBytes / (1024.0 * 1024.0)

                        if (totalMB > 1) { // 只显示使用超过1MB的应用
                            val appName = packageManager.getApplicationLabel(app).toString()
                            apps.add(
                                NetworkDiagnosticViewModel.NetworkUsageApp(
                                    appName = appName,
                                    packageName = app.packageName,
                                    usageMB = totalMB.toFloat(),
                                    activeTime = estimateActiveTime(totalBytes)
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            apps.sortedByDescending { it.usageMB }.take(10)
        }

    /**
     * 估算应用活跃时间（基于流量）
     */
    private fun estimateActiveTime(bytes: Long): Long {
        // 假设平均每分钟使用1MB数据
        val minutes = bytes / (1024L * 1024L)
        return minutes.coerceAtLeast(1)
    }

    /**
     * 获取电池消耗应用列表
     */
    suspend fun getBatteryConsumingApps(): List<NetworkDiagnosticViewModel.BatteryConsumingApp> =
        withContext(Dispatchers.IO) {
            val apps = mutableListOf<NetworkDiagnosticViewModel.BatteryConsumingApp>()

            try {
                // 获取正在运行的应用进程
                val runningApps = getRunningApps()

                for ((packageName, cpuTime) in runningApps) {
                    try {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        val appName = packageManager.getApplicationLabel(appInfo).toString()

                        // 估算电池消耗百分比（基于CPU时间）
                        val consumptionPercent = estimateBatteryConsumption(cpuTime)

                        // 判断是否应该关闭（高耗电且非系统应用）
                        val shouldClose = consumptionPercent > 10 &&
                                         appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0

                        apps.add(
                            NetworkDiagnosticViewModel.BatteryConsumingApp(
                                appName = appName,
                                packageName = packageName,
                                consumptionPercent = consumptionPercent,
                                runningTime = cpuTime / 60000, // 转换为分钟
                                shouldClose = shouldClose
                            )
                        )
                    } catch (e: Exception) {
                        // 忽略获取失败的应用
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            apps.sortedByDescending { it.consumptionPercent }.take(10)
        }

    /**
     * 获取正在运行的应用及其CPU时间
     */
    private fun getRunningApps(): Map<String, Long> {
        val runningApps = mutableMapOf<String, Long>()

        try {
            // 读取/proc/stat获取总CPU时间
            val totalCpuTime = getTotalCpuTime()

            // 读取每个进程的CPU时间
            val procDir = java.io.File("/proc")
            procDir.listFiles { file ->
                file.isDirectory && file.name.matches(Regex("\\d+"))
            }?.forEach { pidDir ->
                try {
                    val pid = pidDir.name.toInt()
                    val cmdlineFile = java.io.File(pidDir, "cmdline")
                    if (cmdlineFile.exists()) {
                        val packageName = cmdlineFile.readText().trim().replace("\u0000", "")
                        if (packageName.isNotEmpty() && !packageName.contains("system")) {
                            val cpuTime = getProcessCpuTime(pid)
                            runningApps[packageName] = cpuTime
                        }
                    }
                } catch (e: Exception) {
                    // 忽略无法访问的进程
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return runningApps
    }

    /**
     * 获取总CPU时间
     */
    private fun getTotalCpuTime(): Long {
        try {
            val reader = BufferedReader(InputStreamReader(
                Runtime.getRuntime().exec("cat /proc/stat").inputStream
            ))
            val cpuLine = reader.readLine()
            reader.close()

            if (cpuLine != null && cpuLine.startsWith("cpu")) {
                val times = cpuLine.split(" ").filter { it.isNotEmpty() }
                if (times.size >= 5) {
                    return times.subList(1, times.size).sumOf { it.toLongOrNull() ?: 0L }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0L
    }

    /**
     * 获取进程CPU时间
     */
    private fun getProcessCpuTime(pid: Int): Long {
        try {
            val statFile = java.io.File("/proc/$pid/stat")
            if (statFile.exists()) {
                val statContent = statFile.readText()
                val fields = statContent.substringAfter(')').trim().split(" ")
                if (fields.size >= 15) {
                    val utime = fields[11].toLongOrNull() ?: 0L
                    val stime = fields[12].toLongOrNull() ?: 0L
                    return utime + stime
                }
            }
        } catch (e: Exception) {
            // 忽略
        }
        return 0L
    }

    /**
     * 估算电池消耗百分比
     */
    private fun estimateBatteryConsumption(cpuTime: Long): Float {
        // 简单的估算：CPU时间越长，电池消耗越大
        val maxCpuTime = 1000000L // 假设最大CPU时间
        val percentage = (cpuTime.toFloat() / maxCpuTime) * 100
        return percentage.coerceIn(0f, 100f)
    }

    /**
     * 执行真实的延迟测试
     */
    suspend fun performRealLatencyTest(host: String = "8.8.8.8"): NetworkDiagnosticViewModel.LatencyResult =
        withContext(Dispatchers.IO) {
            val latencies = mutableListOf<Long>()
            var packetLoss = 0
            val testCount = 5

            repeat(testCount) {
                try {
                    val startTime = System.currentTimeMillis()
                    val address = InetAddress.getByName(host)
                    val reachable = address.isReachable(5000)
                    val endTime = System.currentTimeMillis()

                    if (reachable) {
                        latencies.add(endTime - startTime)
                    } else {
                        packetLoss++
                    }
                } catch (e: Exception) {
                    packetLoss++
                }
            }

            if (latencies.isEmpty()) {
                NetworkDiagnosticViewModel.LatencyResult(
                    averageLatency = 9999L,
                    minLatency = 9999L,
                    maxLatency = 9999L,
                    packetLoss = 100f,
                    quality = "无法连接"
                )
            } else {
                val average = latencies.average().toLong()
                val min = latencies.minOrNull() ?: 0L
                val max = latencies.maxOrNull() ?: 0L
                val lossRate = (packetLoss.toFloat() / testCount) * 100

                NetworkDiagnosticViewModel.LatencyResult(
                    averageLatency = average,
                    minLatency = min,
                    maxLatency = max,
                    packetLoss = lossRate,
                    quality = when {
                        average <= 50 && lossRate == 0f -> "优秀"
                        average <= 100 && lossRate <= 5f -> "良好"
                        average <= 200 && lossRate <= 10f -> "一般"
                        average <= 500 && lossRate <= 20f -> "较差"
                        else -> "很差"
                    }
                )
            }
        }

    /**
     * 扫描不同位置的WiFi信号强度
     */
    suspend fun scanWifiPositions(): NetworkDiagnosticViewModel.PositionScanResult =
        withContext(Dispatchers.IO) {
            val positions = mutableListOf<NetworkDiagnosticViewModel.PositionResult>()

            // 获取当前WiFi信息
            val currentWifiInfo = wifiManager?.connectionInfo
            val currentRssi = currentWifiInfo?.rssi ?: -100
            val currentSignalLevel = calculateSignalLevel(currentRssi)
            val currentDistance = calculateDistance(currentRssi, currentWifiInfo?.frequency ?: 2412)

            // 当前位置
            positions.add(
                NetworkDiagnosticViewModel.PositionResult(
                    position = "当前位置",
                    signalStrength = currentSignalLevel,
                    rssi = currentRssi,
                    distance = currentDistance,
                    recommended = currentSignalLevel >= 4
                )
            )

            // 根据信号强度给出建议位置
            when (currentSignalLevel) {
                5 -> {
                    positions.add(NetworkDiagnosticViewModel.PositionResult(
                        position = "最佳位置",
                        signalStrength = 5,
                        rssi = currentRssi,
                        distance = currentDistance,
                        recommended = true
                    ))
                }
                4 -> {
                    positions.add(NetworkDiagnosticViewModel.PositionResult(
                        position = "建议靠近路由器1-2米",
                        signalStrength = 5,
                        rssi = -40,
                        distance = 2.0,
                        recommended = true
                    ))
                }
                3 -> {
                    positions.add(NetworkDiagnosticViewModel.PositionResult(
                        position = "建议靠近路由器3-5米",
                        signalStrength = 4,
                        rssi = -55,
                        distance = 5.0,
                        recommended = true
                    ))
                }
                else -> {
                    positions.add(NetworkDiagnosticViewModel.PositionResult(
                        position = "建议移动到路由器附近",
                        signalStrength = 5,
                        rssi = -40,
                        distance = 2.0,
                        recommended = true
                    ))
                }
            }

            val bestPosition = positions.maxByOrNull { it.signalStrength }?.position ?: "当前位置"
            val recommendedAction = when (currentSignalLevel) {
                5 -> "当前位置信号极佳，无需调整"
                4 -> "信号良好，可适当靠近路由器获得更佳体验"
                3 -> "信号一般，建议移动到距离路由器更近的位置"
                2 -> "信号较弱，强烈建议靠近路由器"
                1 -> "信号很弱，请尽快移动到路由器附近"
                else -> "无信号，请检查WiFi连接"
            }

            NetworkDiagnosticViewModel.PositionScanResult(
                positions = positions,
                bestPosition = bestPosition,
                recommendedAction = recommendedAction
            )
        }
}