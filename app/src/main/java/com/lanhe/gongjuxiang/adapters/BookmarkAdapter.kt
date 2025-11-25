package com.lanhe.gongjuxiang.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lanhe.gongjuxiang.databinding.ItemBookmarkBinding
import com.lanhe.mokuai.bookmark.BookmarkManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 书签列表适配器
 */
class BookmarkAdapter(
    private val onBookmarkClick: (BookmarkManager.Bookmark) -> Unit,
    private val onBookmarkLongClick: (BookmarkManager.Bookmark) -> Boolean
) : ListAdapter<BookmarkManager.Bookmark, BookmarkAdapter.BookmarkViewHolder>(BookmarkDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val binding = ItemBookmarkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookmarkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookmarkViewHolder(
        private val binding: ItemBookmarkBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bookmark: BookmarkManager.Bookmark) {
            binding.apply {
                // 书签标题
                tvBookmarkTitle.text = bookmark.title.ifEmpty { "无标题" }

                // 书签URL
                tvBookmarkUrl.text = bookmark.url

                // 书签图标 - 根据是否收藏显示不同图标
                if (bookmark.isFavorite) {
                    ivBookmarkIcon.setImageResource(android.R.drawable.star_on)
                } else {
                    ivBookmarkIcon.setImageResource(android.R.drawable.star_off)
                }

                // 点击事件
                root.setOnClickListener {
                    onBookmarkClick(bookmark)
                }

                root.setOnLongClickListener {
                    onBookmarkLongClick(bookmark)
                }
            }
        }
    }

    class BookmarkDiffCallback : DiffUtil.ItemCallback<BookmarkManager.Bookmark>() {
        override fun areItemsTheSame(
            oldItem: BookmarkManager.Bookmark,
            newItem: BookmarkManager.Bookmark
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: BookmarkManager.Bookmark,
            newItem: BookmarkManager.Bookmark
        ): Boolean {
            return oldItem == newItem
        }
    }
}
