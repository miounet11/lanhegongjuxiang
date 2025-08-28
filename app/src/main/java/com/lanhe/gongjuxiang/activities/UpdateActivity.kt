package com.lanhe.gongjuxiang.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.utils.UpdateChecker

/**
 * NEURAL 更新检查界面
 * 显示最新版本信息和更新选项
 */
class UpdateActivity : AppCompatActivity() {

    private lateinit var updateChecker: UpdateChecker
    private lateinit var cardUpdateAvailable: MaterialCardView
    private lateinit var cardUpToDate: MaterialCardView
    private lateinit var cardError: MaterialCardView

    private lateinit var tvCurrentVersion: MaterialTextView
    private lateinit var tvLatestVersion: MaterialTextView
    private lateinit var tvReleaseDate: MaterialTextView
    private lateinit var tvLoadingMessage: MaterialTextView
    private lateinit var tvUpdateMessage: MaterialTextView
    private lateinit var tvUpToDateMessage: MaterialTextView
    private lateinit var tvErrorMessage: MaterialTextView

    private lateinit var btnDownloadUpdate: MaterialButton
    private lateinit var btnViewChangelog: MaterialButton
    private lateinit var btnOpenGitHub: MaterialButton
    private lateinit var btnRetryCheck: MaterialButton
    private lateinit var btnDismiss: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update)

        // 设置标题
        title = "NEURAL 更新检查"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initializeViews()
        initializeUpdateChecker()
        checkForUpdates()
    }

    private fun initializeViews() {
        cardUpdateAvailable = findViewById(R.id.cardUpdateAvailable)
        cardUpToDate = findViewById(R.id.cardUpToDate)
        cardError = findViewById(R.id.cardError)

        tvCurrentVersion = findViewById(R.id.tvCurrentVersion)
        tvLatestVersion = findViewById(R.id.tvLatestVersion)
        tvReleaseDate = findViewById(R.id.tvReleaseDate)
        tvLoadingMessage = findViewById(R.id.tvUpdateMessage) // 用于加载状态
        tvUpdateMessage = findViewById(R.id.tvUpdateMessage) // 用于更新信息
        tvUpToDateMessage = findViewById(R.id.tvUpToDateMessage) // 用于已是最新版本
        tvErrorMessage = findViewById(R.id.tvErrorMessage)

        btnDownloadUpdate = findViewById(R.id.btnDownloadUpdate)
        btnViewChangelog = findViewById(R.id.btnViewChangelog)
        btnOpenGitHub = findViewById(R.id.btnOpenGitHub)
        btnRetryCheck = findViewById(R.id.btnRetryCheck)
        btnDismiss = findViewById(R.id.btnDismiss)

        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        btnDownloadUpdate.setOnClickListener {
            // 下载更新逻辑会在UpdateChecker中处理
        }

        btnViewChangelog.setOnClickListener {
            openGitHubRepo()
        }

        btnOpenGitHub.setOnClickListener {
            openGitHubRepo()
        }

        btnRetryCheck.setOnClickListener {
            checkForUpdates()
        }

        btnDismiss.setOnClickListener {
            finish()
        }
    }

    private fun initializeUpdateChecker() {
        updateChecker = UpdateChecker(this)
    }

    private fun checkForUpdates() {
        showLoadingState()

        updateChecker.checkForUpdates { result ->
            when (result) {
                is UpdateChecker.UpdateResult.UpdateAvailable -> {
                    showUpdateAvailable(result.versionInfo)
                }
                is UpdateChecker.UpdateResult.UpToDate -> {
                    showUpToDate()
                }
                is UpdateChecker.UpdateResult.Error -> {
                    showError(result.message)
                }
            }
        }
    }

    private fun showLoadingState() {
        cardUpdateAvailable.visibility = View.GONE
        cardUpToDate.visibility = View.GONE
        cardError.visibility = View.GONE

        // 显示加载状态
        tvLoadingMessage.visibility = View.VISIBLE
        tvUpdateMessage.visibility = View.GONE
        cardUpdateAvailable.visibility = View.VISIBLE
    }

    private fun showUpdateAvailable(versionInfo: UpdateChecker.VersionInfo) {
        hideAllCards()

        // 设置版本信息
        tvCurrentVersion.text = "当前版本: ${getCurrentVersion()}"
        tvLatestVersion.text = "最新版本: ${versionInfo.version}"
        tvReleaseDate.text = "发布日期: ${versionInfo.releaseDate}"

        // 隐藏加载消息，显示更新消息
        tvLoadingMessage.visibility = View.GONE
        tvUpdateMessage.visibility = View.VISIBLE

        // 设置更新消息
        val message = buildString {
            append("🎯 发现新版本 ${versionInfo.version}！\n\n")
            append("✨ 新版本特性:\n")
            versionInfo.releaseNotes.forEach { note ->
                append("• $note\n")
            }
        }
        tvUpdateMessage.text = message

        cardUpdateAvailable.visibility = View.VISIBLE

        // 设置下载按钮点击事件
        btnDownloadUpdate.setOnClickListener {
            updateChecker.downloadUpdate(versionInfo)
        }
    }

    private fun showUpToDate() {
        hideAllCards()
        tvUpToDateMessage.text = "✅ NEURAL 已是最新版本\n\n您的系统运行在最优状态！"
        cardUpToDate.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        hideAllCards()
        tvErrorMessage.text = "❌ 更新检查失败\n\n$message\n\n请检查网络连接后重试"
        cardError.visibility = View.VISIBLE
    }

    private fun hideAllCards() {
        cardUpdateAvailable.visibility = View.GONE
        cardUpToDate.visibility = View.GONE
        cardError.visibility = View.GONE
    }

    private fun getCurrentVersion(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun openGitHubRepo() {
        updateChecker.openGitHubRepo()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        updateChecker.cleanup()
    }
}
