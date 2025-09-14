package com.lanhe.gongjuxiang.browser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lanhe.gongjuxiang.R

/**
 * 浏览器设置适配器
 */
class BrowserSettingsAdapter(
    private val settingsItems: List<BrowserSettingItem>,
    private val onItemClick: (BrowserSettingItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when (settingsItems[position].type) {
            SettingType.SWITCH -> VIEW_TYPE_SWITCH
            SettingType.BUTTON -> VIEW_TYPE_BUTTON
            SettingType.CATEGORY -> VIEW_TYPE_CATEGORY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            VIEW_TYPE_SWITCH -> {
                val view = layoutInflater.inflate(R.layout.item_browser_setting_switch, parent, false)
                SwitchViewHolder(view)
            }
            VIEW_TYPE_BUTTON -> {
                val view = layoutInflater.inflate(R.layout.item_browser_setting_button, parent, false)
                ButtonViewHolder(view)
            }
            VIEW_TYPE_CATEGORY -> {
                val view = layoutInflater.inflate(R.layout.item_browser_setting_category, parent, false)
                CategoryViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = settingsItems[position]

        when (holder) {
            is SwitchViewHolder -> holder.bind(item)
            is ButtonViewHolder -> holder.bind(item)
            is CategoryViewHolder -> holder.bind(item)
        }
    }

    override fun getItemCount(): Int = settingsItems.size

    // 开关设置项ViewHolder
    inner class SwitchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.iv_icon)
        private val titleTextView: TextView = itemView.findViewById(R.id.tv_title)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.tv_description)
        private val switchView: Switch = itemView.findViewById(R.id.switch_setting)

        fun bind(item: BrowserSettingItem) {
            iconImageView.setImageResource(item.icon)
            titleTextView.text = item.title
            descriptionTextView.text = item.description
            switchView.isChecked = item.isEnabled

            itemView.setOnClickListener {
                switchView.isChecked = !switchView.isChecked
                item.isEnabled = switchView.isChecked
                onItemClick(item)
            }

            switchView.setOnCheckedChangeListener { _, isChecked ->
                item.isEnabled = isChecked
                onItemClick(item)
            }
        }
    }

    // 按钮设置项ViewHolder
    inner class ButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.iv_icon)
        private val titleTextView: TextView = itemView.findViewById(R.id.tv_title)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.tv_description)

        fun bind(item: BrowserSettingItem) {
            iconImageView.setImageResource(item.icon)
            titleTextView.text = item.title
            descriptionTextView.text = item.description

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    // 分类设置项ViewHolder
    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.iv_icon)
        private val titleTextView: TextView = itemView.findViewById(R.id.tv_title)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.tv_description)

        fun bind(item: BrowserSettingItem) {
            iconImageView.setImageResource(item.icon)
            titleTextView.text = item.title
            descriptionTextView.text = item.description

            // 分类项通常不响应点击
            itemView.setOnClickListener(null)
        }
    }

    companion object {
        private const val VIEW_TYPE_SWITCH = 0
        private const val VIEW_TYPE_BUTTON = 1
        private const val VIEW_TYPE_CATEGORY = 2
    }
}
