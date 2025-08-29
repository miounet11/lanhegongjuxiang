package com.lanhe.gongjuxiang.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.databinding.ItemAppBinding
import com.lanhe.gongjuxiang.models.AppInfo
import com.lanhe.gongjuxiang.utils.AnimationUtils
import java.text.SimpleDateFormat
import java.util.*

class AppAdapter(
    private val context: Context,
    private val appList: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onUninstallClick: (AppInfo) -> Unit,
    private val onInstallClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    inner class AppViewHolder(private val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(appInfo: AppInfo) {
            binding.apply {
                // 设置应用图标
                appInfo.icon?.let { icon ->
                    ivAppIcon.setImageDrawable(icon)
                }

                // 设置应用信息
                tvAppName.text = appInfo.appName
                tvPackageName.text = appInfo.packageName
                tvVersion.text = "v${appInfo.versionName}"

                // 设置应用大小
                tvAppSize.text = formatFileSize(appInfo.size)

                // 设置安装时间
                tvInstallTime.text = formatDate(appInfo.installTime)

                // 设置系统应用标识
                if (appInfo.isSystemApp) {
                    tvAppType.text = "系统应用"
                    tvAppType.setTextColor(context.getColor(R.color.primary))
                } else {
                    tvAppType.text = "用户应用"
                    tvAppType.setTextColor(context.getColor(R.color.text_secondary))
                }

                // 点击事件
                root.setOnClickListener {
                    AnimationUtils.buttonPressFeedback(it)
                    onAppClick(appInfo)
                }

                btnUninstall.setOnClickListener {
                    AnimationUtils.buttonPressFeedback(it)
                    onUninstallClick(appInfo)
                }

                btnInstall.setOnClickListener {
                    AnimationUtils.buttonPressFeedback(it)
                    onInstallClick(appInfo)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(appList[position])
    }

    override fun getItemCount(): Int = appList.size

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
