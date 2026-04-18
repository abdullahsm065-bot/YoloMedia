package com.yolomedia.ui.videos

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
import com.yolomedia.data.model.MediaFolder
import com.yolomedia.ui.theme.ThemeManager
import com.yolomedia.utils.FormatUtils

class VideoFolderAdapter(
    private val onFolderClick: (MediaFolder) -> Unit
) : ListAdapter<MediaFolder, VideoFolderAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_folder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView as CardView
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        private val tvFolderName: TextView = itemView.findViewById(R.id.tv_folder_name)
        private val tvFolderInfo: TextView = itemView.findViewById(R.id.tv_folder_info)
        private val tvCount: TextView = itemView.findViewById(R.id.tv_count)

        fun bind(folder: MediaFolder) {
            val context = itemView.context
            tvFolderName.text = folder.name
            tvFolderInfo.text = "${folder.mediaCount} videos • ${FormatUtils.formatFileSize(folder.totalSize)}"
            tvCount.text = "${folder.mediaCount}"

            cardView.setCardBackgroundColor(ThemeManager.getCardColor(context))
            tvFolderName.setTextColor(ThemeManager.getTextPrimaryColor(context))
            tvFolderInfo.setTextColor(ThemeManager.getTextSecondaryColor(context))

            Glide.with(context)
                .load(folder.thumbnailUri)
                .transform(CenterCrop(), RoundedCorners(24))
                .placeholder(R.drawable.bg_card_light)
                .into(ivThumbnail)

            itemView.setOnClickListener { onFolderClick(folder) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MediaFolder>() {
        override fun areItemsTheSame(oldItem: MediaFolder, newItem: MediaFolder) =
            oldItem.path == newItem.path

        override fun areContentsTheSame(oldItem: MediaFolder, newItem: MediaFolder) =
            oldItem == newItem
    }
}
