package com.yolomedia.ui.safe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.yolomedia.R
import com.yolomedia.data.repository.SafeMediaItem
import com.yolomedia.ui.theme.ThemeManager
import com.yolomedia.utils.FormatUtils

class SafeMediaAdapter(
    private val onItemClick: (SafeMediaItem) -> Unit,
    private val onRestoreClick: (SafeMediaItem) -> Unit,
    private val onDeleteClick: (SafeMediaItem) -> Unit
) : ListAdapter<SafeMediaItem, SafeMediaAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_safe_media, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView as CardView
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val tvInfo: TextView = itemView.findViewById(R.id.tv_info)
        private val btnRestore: ImageView = itemView.findViewById(R.id.btn_restore)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete)

        fun bind(item: SafeMediaItem) {
            val context = itemView.context
            tvName.text = item.name

            val info = StringBuilder(FormatUtils.formatFileSize(item.size))
            if (item.isVideo && item.duration > 0) {
                info.append(" \u2022 ${FormatUtils.formatDuration(item.duration)}")
            }
            tvInfo.text = info.toString()

            cardView.setCardBackgroundColor(ThemeManager.getCardColor(context))
            tvName.setTextColor(ThemeManager.getTextPrimaryColor(context))
            tvInfo.setTextColor(ThemeManager.getTextSecondaryColor(context))

            val accentColor = ThemeManager.getAccentColor(context)
            btnRestore.setColorFilter(accentColor)
            btnDelete.setColorFilter(ThemeManager.getTextSecondaryColor(context))

            Glide.with(context)
                .load(item.uri)
                .transform(CenterCrop(), RoundedCorners(16))
                .placeholder(R.drawable.bg_card_light)
                .into(ivThumbnail)

            itemView.setOnClickListener { onItemClick(item) }
            btnRestore.setOnClickListener { onRestoreClick(item) }
            btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SafeMediaItem>() {
        override fun areItemsTheSame(oldItem: SafeMediaItem, newItem: SafeMediaItem) =
            oldItem.path == newItem.path

        override fun areContentsTheSame(oldItem: SafeMediaItem, newItem: SafeMediaItem) =
            oldItem == newItem
    }
}
