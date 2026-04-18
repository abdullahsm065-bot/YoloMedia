package com.yolomedia.ui.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.yolomedia.R
import com.yolomedia.data.model.ImageItem

class ImageAdapter(
    private val onImageClick: (ImageItem, Int) -> Unit,
    private val onDeleteClick: (ImageItem) -> Unit,
    private val onDetailsClick: (ImageItem) -> Unit,
    private val onMoveToSafeClick: (ImageItem) -> Unit,
    private val onShareClick: (ImageItem) -> Unit
) : ListAdapter<ImageItem, ImageAdapter.ViewHolder>(DiffCallback()) {

    private var animationsEnabled = true
    private var lastAnimatedPosition = -1

    fun setAnimationsEnabled(enabled: Boolean) {
        animationsEnabled = enabled
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
        if (animationsEnabled && position > lastAnimatedPosition) {
            val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.item_fall_down)
            animation.startOffset = (position % 6 * 50).toLong()
            holder.itemView.startAnimation(animation)
            lastAnimatedPosition = position
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.iv_image)

        fun bind(image: ImageItem, position: Int) {
            val context = itemView.context

            Glide.with(context)
                .load(image.uri)
                .transform(CenterCrop(), RoundedCorners(20))
                .placeholder(R.drawable.bg_card_light)
                .into(ivImage)

            ivImage.setOnClickListener { onImageClick(image, position) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ImageItem>() {
        override fun areItemsTheSame(oldItem: ImageItem, newItem: ImageItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ImageItem, newItem: ImageItem) =
            oldItem == newItem
    }
}
