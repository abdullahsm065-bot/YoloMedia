package com.yolomedia.ui.videos

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yolomedia.R
import com.yolomedia.data.model.MediaFolder
import com.yolomedia.ui.theme.ThemeManager
import com.yolomedia.utils.FormatUtils

class VideoFolderAdapter(
    private val onFolderClick: (MediaFolder) -> Unit
) : ListAdapter<MediaFolder, VideoFolderAdapter.ViewHolder>(DiffCallback()) {

    private var animationsEnabled = true
    private var lastAnimatedPosition = -1

    fun setAnimationsEnabled(enabled: Boolean) {
        animationsEnabled = enabled
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_folder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
        if (animationsEnabled && position > lastAnimatedPosition) {
            val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.item_fall_down)
            animation.startOffset = (position * 60).toLong()
            holder.itemView.startAnimation(animation)
            lastAnimatedPosition = position
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView as CardView
        private val folderIconBg: View = itemView.findViewById(R.id.folder_icon_bg)
        private val ivFolderIcon: ImageView = itemView.findViewById(R.id.iv_folder_icon)
        private val tvFolderName: TextView = itemView.findViewById(R.id.tv_folder_name)
        private val tvFolderInfo: TextView = itemView.findViewById(R.id.tv_folder_info)
        private val tvCount: TextView = itemView.findViewById(R.id.tv_count)

        fun bind(folder: MediaFolder) {
            val context = itemView.context
            val accentColor = ThemeManager.getAccentColor(context)

            tvFolderName.text = folder.name
            tvFolderInfo.text = "${folder.mediaCount} videos \u2022 ${FormatUtils.formatFileSize(folder.totalSize)}"
            tvCount.text = "${folder.mediaCount}"

            cardView.setCardBackgroundColor(ThemeManager.getCardColor(context))
            tvFolderName.setTextColor(ThemeManager.getTextPrimaryColor(context))
            tvFolderInfo.setTextColor(ThemeManager.getTextSecondaryColor(context))

            ivFolderIcon.setColorFilter(accentColor)

            val bgDrawable = GradientDrawable().apply {
                setColor((accentColor and 0x00FFFFFF) or 0x1A000000)
                cornerRadius = 16f * context.resources.displayMetrics.density
            }
            folderIconBg.background = bgDrawable

            val countBg = tvCount.background
            if (countBg is GradientDrawable) {
                countBg.setColor(accentColor)
            }

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
