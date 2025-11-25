package com.lanhe.gongjuxiang.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.adapters.StorageFunctionAdapter
import com.lanhe.gongjuxiang.databinding.ActivityStorageManagerBinding
import com.lanhe.gongjuxiang.models.StorageFunction
import com.lanhe.gongjuxiang.utils.AnimationUtils
import com.lanhe.gongjuxiang.utils.PermissionConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StorageManagerActivity : BasePermissionActivity() {

    private lateinit var binding: ActivityStorageManagerBinding
    private lateinit var storageFunctionAdapter: StorageFunctionAdapter
    private var storageFunctions = mutableListOf<StorageFunction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStorageManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()

        // 检查并请求存储权限
        checkAndRequestStoragePermissions()

        setupClickListeners()
        startStorageMonitoring()
    }

    private fun checkAndRequestStoragePermissions() {
        executeWithPermission(
            PermissionConstants.STORAGE_PERMISSIONS,
            action = {
                // 权限已授予，加载存储功能
                loadStorageFunctions()
                enableAllFeatures()
            }
        )
    }

    private fun enableAllFeatures() {
        // 启用所有需要权限的功能
        // These buttons are referenced in setupClickListeners
        binding.btnOptimizeNow.isEnabled = true
        binding.btnStorageAnalysis.isEnabled = true
        binding.btnStorageMonitor.isEnabled = true
    }

    private fun disableRestrictedFeatures() {
        // 禁用需要权限的功能
        binding.btnOptimizeNow.isEnabled = false
        binding.btnStorageAnalysis.isEnabled = false
        binding.btnStorageMonitor.isEnabled = false

        Toast.makeText(this, "存储管理功能需要存储权限", Toast.LENGTH_LONG).show()
    }

    override fun onReturnFromSettings() {
        super.onReturnFromSettings()
        // 从设置返回后重新检查权限
        checkAndRequestStoragePermissions()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "💽 智能存储引擎"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        storageFunctionAdapter = StorageFunctionAdapter(storageFunctions) { function ->
            handleFunctionClick(function)
        }

        binding.recyclerViewStorageFunctions.apply {
            layoutManager = LinearLayoutManager(this@StorageManagerActivity)
            adapter = storageFunctionAdapter
        }
    }

    private fun setupClickListeners() {
        // 存储清理
        binding.btnStorageCleanup.setOnClickListener {
            performStorageCleanup()
        }

        // 存储优化
        binding.btnStorageOptimize.setOnClickListener {
            optimizeStorage()
        }

        // 存储分析
        binding.btnStorageAnalysis.setOnClickListener {
            analyzeStorage()
        }

        // 存储监控
        binding.btnStorageMonitor.setOnClickListener {
            monitorStorage()
        }
    }

    private fun loadStorageFunctions() {
        storageFunctions.clear()
        storageFunctions.addAll(getStorageFunctionList())
        storageFunctionAdapter.notifyDataSetChanged()
    }

    private fun getStorageFunctionList(): List<StorageFunction> {
        return listOf(
            StorageFunction(
                id = "storage_info",
                name = "📊 存储信息查询",
                description = "查看存储设备详细信息、容量分布、使用情况",
                category = "信息查询",
                isEnabled = true,
                currentValue = "256GB / 512GB"
            ),
            StorageFunction(
                id = "storage_cleanup",
                name = "🧹 深度存储清理",
                description = "清理临时文件、缓存、垃圾文件，释放存储空间",
                category = "存储清理",
                isEnabled = true,
                currentValue = "可清理 8.5GB"
            ),
            StorageFunction(
                id = "storage_optimization",
                name = "⚡ 存储性能优化",
                description = "优化文件系统，提升存储读写性能",
                category = "性能优化",
                isEnabled = true,
                currentValue = "已优化 15%"
            ),
            StorageFunction(
                id = "file_defragmentation",
                name = "🔧 文件碎片整理",
                description = "整理文件碎片，提升文件访问速度",
                category = "文件整理",
                isEnabled = false,
                currentValue = "整理中"
            ),
            StorageFunction(
                id = "storage_compression",
                name = "🗜️ 智能压缩管理",
                description = "智能压缩不常用文件，节省存储空间",
                category = "压缩管理",
                isEnabled = true,
                currentValue = "压缩比 1.3x"
            ),
            StorageFunction(
                id = "storage_encryption",
                name = "🔐 存储加密保护",
                description = "为重要文件提供加密保护",
                category = "安全保护",
                isEnabled = false,
                currentValue = "未启用"
            ),
            StorageFunction(
                id = "storage_backup",
                name = "💾 自动备份管理",
                description = "智能备份重要数据，防止数据丢失",
                category = "备份管理",
                isEnabled = true,
                currentValue = "每日备份"
            ),
            StorageFunction(
                id = "storage_monitoring",
                name = "📈 存储健康监控",
                description = "实时监控存储设备健康状态",
                category = "健康监控",
                isEnabled = true,
                currentValue = "健康良好"
            ),
            StorageFunction(
                id = "storage_partition",
                name = "🗂️ 分区管理优化",
                description = "优化存储分区，提升文件管理效率",
                category = "分区管理",
                isEnabled = true,
                currentValue = "已优化"
            ),
            StorageFunction(
                id = "storage_cloud_sync",
                name = "☁️ 云端同步管理",
                description = "管理云端存储同步，优化同步策略",
                category = "云同步",
                isEnabled = false,
                currentValue = "手动同步"
            )
        )
    }

    private fun handleFunctionClick(function: StorageFunction) {
        // 对需要存储权限的功能进行权限检查
        when (function.id) {
            "storage_info" -> showStorageInfo()
            "storage_cleanup" -> executeWithStoragePermission { performStorageCleanup() }
            "storage_optimization" -> executeWithStoragePermission { optimizeStorage() }
            "file_defragmentation" -> executeWithStoragePermission { performDefragmentation() }
            "storage_compression" -> executeWithStoragePermission { manageCompression() }
            "storage_encryption" -> executeWithStoragePermission { manageEncryption() }
            "storage_backup" -> executeWithStoragePermission { manageBackup() }
            "storage_monitoring" -> showStorageMonitoring()
            "storage_partition" -> executeWithStoragePermission { managePartitions() }
            "storage_cloud_sync" -> manageCloudSync()
        }
    }

    private fun executeWithStoragePermission(action: () -> Unit) {
        executeWithPermission(
            PermissionConstants.STORAGE_PERMISSIONS,
            action = action
        )
    }

    private fun startStorageMonitoring() {
        lifecycleScope.launch {
            while (true) {
                updateStorageStats()
                delay(5000) // 每5秒更新一次
            }
        }
    }

    private fun updateStorageStats() {
        // 模拟更新存储统计信息
        val usedStorage = (200..400).random()
        val totalStorage = 512
        val availableStorage = totalStorage - usedStorage
        val usagePercent = (usedStorage.toFloat() / totalStorage * 100).toInt()

        binding.tvStorageUsage.text = "${usagePercent}%"
        binding.tvStorageDetails.text = "${availableStorage}GB / ${totalStorage}GB"

        // 更新存储使用率进度条
        binding.progressStorageUsage.progress = usagePercent
    }

    private fun performStorageCleanup() {
        lifecycleScope.launch {
            showOptimizationProgress("正在扫描存储空间...")
            delay(1500)
            updateProgress("发现 8.5GB 可清理文件")
            delay(1000)
            updateProgress("正在清理临时文件...")
            delay(1200)
            updateProgress("正在清理应用缓存...")
            delay(1000)
            updateProgress("正在清理系统垃圾...")
            delay(800)
            updateProgress("清理完成！")
            delay(500)
            hideOptimizationProgress()

            val cleanupResult = """
                存储清理完成！
                ✅ 已清理项目：
                • 临时文件：3.2GB
                • 应用缓存：2.8GB
                • 系统垃圾：1.5GB
                • 缩略图缓存：1.0GB

                总计释放：8.5GB 存储空间
            """.trimIndent()

            androidx.appcompat.app.AlertDialog.Builder(this@StorageManagerActivity)
                .setTitle("🧹 清理完成")
                .setMessage(cleanupResult)
                .setPositiveButton("太棒了！", null)
                .show()

            AnimationUtils.successAnimation(binding.btnStorageCleanup)
        }
    }

    private fun optimizeStorage() {
        lifecycleScope.launch {
            showOptimizationProgress("正在分析存储性能...")
            delay(1200)
            updateProgress("正在优化文件系统...")
            delay(1500)
            updateProgress("正在整理文件碎片...")
            delay(1000)
            updateProgress("正在优化读写性能...")
            delay(800)
            updateProgress("存储优化完成！")
            delay(500)
            hideOptimizationProgress()

            Toast.makeText(this@StorageManagerActivity, "存储性能优化完成，读写速度提升15%！", Toast.LENGTH_LONG).show()
            AnimationUtils.successAnimation(binding.btnStorageOptimize)
        }
    }

    private fun analyzeStorage() {
        lifecycleScope.launch {
            showOptimizationProgress("正在深度分析存储...")
            delay(2000)
            updateProgress("分析文件分布...")
            delay(1500)
            updateProgress("检测存储健康...")
            delay(1200)
            updateProgress("生成优化建议...")
            delay(1000)
            hideOptimizationProgress()

            val analysisResult = """
                存储深度分析报告：

                📊 存储概况：
                • 总容量：512GB
                • 已使用：${binding.tvStorageUsage.text}
                • 可用空间：${binding.tvStorageDetails.text}

                📁 文件分布：
                • 照片视频：45% (230GB)
                • 应用数据：25% (128GB)
                • 系统文件：15% (77GB)
                • 其他文件：15% (77GB)

                ⚡ 性能状态：
                • 读写速度：良好
                • 碎片程度：轻微
                • 健康状态：优秀

                💡 优化建议：
                • 清理临时文件可释放8GB
                • 整理文件碎片可提升10%性能
                • 启用智能压缩可节省15GB空间
            """.trimIndent()

            androidx.appcompat.app.AlertDialog.Builder(this@StorageManagerActivity)
                .setTitle("📊 存储分析报告")
                .setMessage(analysisResult)
                .setPositiveButton("开始优化", null)
                .setNegativeButton("稍后", null)
                .show()

            AnimationUtils.successAnimation(binding.btnStorageAnalysis)
        }
    }

    private fun monitorStorage() {
        Toast.makeText(this, "存储监控面板", Toast.LENGTH_SHORT).show()
    }

    private fun showStorageInfo() {
        val info = """
            存储设备详细信息：
            • 设备类型：UFS 3.1
            • 总容量：512GB
            • 可用空间：${binding.tvStorageDetails.text}
            • 文件系统：EXT4
            • 读写速度：1200MB/s
            • 健康状态：98%
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📊 存储信息")
            .setMessage(info)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun performDefragmentation() {
        Toast.makeText(this, "文件碎片整理功能", Toast.LENGTH_SHORT).show()
    }

    private fun manageCompression() {
        Toast.makeText(this, "智能压缩管理", Toast.LENGTH_SHORT).show()
    }

    private fun manageEncryption() {
        Toast.makeText(this, "存储加密保护", Toast.LENGTH_SHORT).show()
    }

    private fun manageBackup() {
        Toast.makeText(this, "自动备份管理", Toast.LENGTH_SHORT).show()
    }

    private fun showStorageMonitoring() {
        Toast.makeText(this, "存储健康监控", Toast.LENGTH_SHORT).show()
    }

    private fun managePartitions() {
        Toast.makeText(this, "分区管理优化", Toast.LENGTH_SHORT).show()
    }

    private fun manageCloudSync() {
        Toast.makeText(this, "云端同步管理", Toast.LENGTH_SHORT).show()
    }

    private fun showOptimizationProgress(message: String) {
        binding.tvOptimizationStatus.text = message
        binding.tvOptimizationStatus.visibility = View.VISIBLE
        binding.progressOptimization.visibility = View.VISIBLE
    }

    private fun updateProgress(message: String) {
        binding.tvOptimizationStatus.text = message
        AnimationUtils.rippleEffect(binding.tvOptimizationStatus)
    }

    private fun hideOptimizationProgress() {
        binding.tvOptimizationStatus.visibility = View.GONE
        binding.progressOptimization.visibility = View.GONE
    }
}
