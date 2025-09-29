package com.lanhe.gongjuxiang.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.shizuku.ShizukuManagerImpl
import com.lanhe.gongjuxiang.shizuku.ShizukuState
import com.lanhe.gongjuxiang.utils.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Shizuku权限请求对话框
 */
class ShizukuPermissionDialog(
    private val context: Context,
    private val onGranted: () -> Unit = {},
    private val onDenied: () -> Unit = {}
) {

    private var dialog: AlertDialog? = null
    private val shizukuManager = ShizukuManagerImpl(context)
    private val scope = CoroutineScope(Dispatchers.Main)

    fun show() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_shizuku_permission, null)

        // 初始化视图
        val statusIcon = view.findViewById<ImageView>(R.id.statusIcon)
        val statusText = view.findViewById<TextView>(R.id.statusText)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnGrant = view.findViewById<MaterialButton>(R.id.btnGrant)
        val tvHelp = view.findViewById<TextView>(R.id.tvHelp)

        // 监听Shizuku状态
        scope.launch {
            shizukuManager.shizukuState.collectLatest { state ->
                updateUI(state, statusIcon, statusText, btnGrant)
            }
        }

        // 设置按钮点击事件
        btnCancel.setOnClickListener {
            onDenied()
            dismiss()
        }

        btnGrant.setOnClickListener {
            when (shizukuManager.shizukuState.value) {
                ShizukuState.NotInstalled -> {
                    // 引导安装Shizuku
                    openShizukuDownloadPage()
                }
                ShizukuState.Unavailable -> {
                    // 提示启动Shizuku
                    showShizukuStartGuide()
                }
                ShizukuState.Denied -> {
                    // 请求权限
                    shizukuManager.requestPermission()
                }
                ShizukuState.Granted -> {
                    onGranted()
                    dismiss()
                }
                else -> {}
            }
        }

        tvHelp.setOnClickListener {
            showHelpDialog()
        }

        // 创建并显示对话框
        dialog = MaterialAlertDialogBuilder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog?.show()
    }

    /**
     * 更新UI状态
     */
    private fun updateUI(
        state: ShizukuState,
        statusIcon: ImageView,
        statusText: TextView,
        btnGrant: MaterialButton
    ) {
        when (state) {
            ShizukuState.NotInstalled -> {
                statusIcon.setImageResource(R.drawable.ic_error)
                statusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                statusText.text = "Shizuku未安装"
                btnGrant.text = "安装Shizuku"
                btnGrant.isEnabled = true
            }
            ShizukuState.Unavailable -> {
                statusIcon.setImageResource(R.drawable.ic_warning)
                statusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
                statusText.text = "Shizuku服务未运行"
                btnGrant.text = "启动指南"
                btnGrant.isEnabled = true
            }
            ShizukuState.Denied -> {
                statusIcon.setImageResource(R.drawable.ic_info)
                statusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
                statusText.text = "需要授权Shizuku权限"
                btnGrant.text = "授权"
                btnGrant.isEnabled = true
            }
            ShizukuState.Granted -> {
                statusIcon.setImageResource(R.drawable.ic_check)
                statusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                statusText.text = "Shizuku权限已授予"
                btnGrant.text = "继续"
                btnGrant.isEnabled = true
            }
            else -> {
                statusIcon.setImageResource(R.drawable.ic_info)
                statusText.text = "检查Shizuku状态..."
                btnGrant.text = "等待"
                btnGrant.isEnabled = false
            }
        }
    }

    /**
     * 打开Shizuku下载页面
     */
    private fun openShizukuDownloadPage() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://github.com/RikkaApps/Shizuku/releases")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * 显示Shizuku启动指南
     */
    private fun showShizukuStartGuide() {
        MaterialAlertDialogBuilder(context)
            .setTitle("启动Shizuku服务")
            .setMessage("""
                请按照以下步骤启动Shizuku：

                1. 打开Shizuku应用
                2. 选择启动方式：
                   • 无线调试（推荐，Android 11+）
                   • ADB命令（需要电脑）
                   • Root权限（需要Root）
                3. 按照应用内指引完成启动
                4. 返回本应用重新授权
            """.trimIndent())
            .setPositiveButton("打开Shizuku") { _, _ ->
                openShizukuApp()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 打开Shizuku应用
     */
    private fun openShizukuApp() {
        val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        if (intent != null) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    /**
     * 显示帮助对话框
     */
    private fun showHelpDialog() {
        MaterialAlertDialogBuilder(context)
            .setTitle("关于Shizuku")
            .setMessage("""
                Shizuku是一个开源的系统API调用框架，允许普通应用调用系统API而无需Root权限。

                主要特点：
                • 无需Root即可使用高级功能
                • 安全可靠，权限可控
                • 支持Android 6.0及以上版本

                使用场景：
                • 应用管理和控制
                • 系统设置修改
                • 进程和服务管理
                • 高级系统优化

                注意：Shizuku需要通过ADB、无线调试或Root方式启动服务。
            """.trimIndent())
            .setPositiveButton("了解更多") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://shizuku.rikka.app/")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    /**
     * 关闭对话框
     */
    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}