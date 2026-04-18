package com.yolomedia.ui.videos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.yolomedia.R
import com.yolomedia.data.model.VideoItem
import com.yolomedia.ui.theme.ThemeManager
import com.yolomedia.utils.FormatUtils

class VideoAdapter(
    private val onVideoClick: (VideoItem) -> Unit,
    private val onDeleteClick: (VideoItem) -> Unit,
    private val onDetailsClick: (VideoItem) -> Unit,
    private val onMoveToSafeClick: (VideoItem) -> Unit,
    private val onShareClick: (VideoItem) -> Unit
) : ListAdapter<VideoItem, VideoAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView as CardView
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvInfo: TextView = itemView.findViewById(R.id.tv_info)
        private val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
        private val btnMore: ImageView = itemView.findViewById(R.id.btn_more)

        fun bind(video: VideoItem) {
            val context = itemView.context

            tvTitle.text = video.title
            tvInfo.text = FormatUtils.formatFileSize(video.size)
            tvDuration.text = FormatUtils.formatDuration(video.duration)

            cardView.setCardBackgroundColor(ThemeManager.getCardColor(context))
            tvTitle.setTextColor(ThemeManager.getTextPrimaryColor(context))
            tvInfo.setTextColor(ThemeManager.getTextSecondaryColor(context))
            ThemeManager.tintIcon(btnMore, context)

            Glide.with(context)
                .load(video.uri)
                .transform(CenterCrop(), RoundedCorners(20))
                .placeholder(R.drawable.bg_card_light)
                .into(ivThumbnail)

            itemView.setOnClickListener { onVideoClick(video) }

            btnMore.setOnClickListener { view ->
                showPopupMenu(view, video)
            }
        }

        private fun showPopupMenu(anchor: View, video: VideoItem) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.menu.add(0, 1, 0, anchor.context.getString(R.string.details))
            popup.menu.add(0, 2, 1, anchor.context.getString(R.string.move_to_safe))
            popup.menu.add(0, 3, 2, anchor.context.getString(R.string.share))
            popup.menu.add(0, 4, 3, anchor.context.getString(R.string.delete))

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> onDetailsClick(video)
                    2 -> onMoveToSafeClick(video)
                    3 -> onShareClick(video)
                    4 -> onDeleteClick(video)
                }
                true
            }
            popup.show()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<VideoItem>() {
        override fun areItemsTheSame(oldItem: VideoItem, newItem: VideoItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: VideoItem, newItem: VideoItem) =
            oldItem == newItem
    }
}
