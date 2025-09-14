/*
 * Copyright 2024 LanHe Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lanhe.gongjuxiang.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.viewmodels.BrowserViewModel

/**
 * 书签适配器
 * 用于显示书签列表
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
class BookmarkAdapter(
    private val bookmarks: List<BrowserViewModel.BookmarkInfo>,
    private val onBookmarkClick: (BrowserViewModel.BookmarkInfo) -> Unit
) : RecyclerView.Adapter<BookmarkAdapter.BookmarkViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmark, parent, false)
        return BookmarkViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        val bookmark = bookmarks[position]
        holder.bind(bookmark, onBookmarkClick)
    }

    override fun getItemCount(): Int = bookmarks.size

    /**
     * 书签ViewHolder
     */
    class BookmarkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvTitle: TextView = itemView.findViewById(R.id.tvBookmarkTitle)
        private val tvUrl: TextView = itemView.findViewById(R.id.tvBookmarkUrl)

        fun bind(bookmark: BrowserViewModel.BookmarkInfo, onBookmarkClick: (BrowserViewModel.BookmarkInfo) -> Unit) {
            tvTitle.text = bookmark.title
            tvUrl.text = bookmark.url

            itemView.setOnClickListener {
                onBookmarkClick(bookmark)
            }

            // 长按删除书签
            itemView.setOnLongClickListener {
                // TODO: 显示删除确认对话框
                true
            }
        }
    }
}
