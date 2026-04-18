package com.yolomedia.ui.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.iv_image)
        private val btnMore: ImageView = itemView.findViewById(R.id.btn_more)

        fun bind(image: ImageItem, position: Int) {
            val context = itemView.context

            Glide.with(context)
                .load(image.uri)
                .transform(CenterCrop(), RoundedCorners(16))
                .placeholder(R.drawable.bg_card_light)
                .into(ivImage)

            ivImage.setOnClickListener { onImageClick(image, position) }

            btnMore.setOnClickListener { view ->
                showPopupMenu(view, image)
            }
        }

        private fun showPopupMenu(anchor: View, image: ImageItem) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.menu.add(0, 1, 0, anchor.context.getString(R.string.details))
            popup.menu.add(0, 2, 1, anchor.context.getString(R.string.move_to_safe))
            popup.menu.add(0, 3, 2, anchor.context.getString(R.string.share))
            popup.menu.add(0, 4, 3, anchor.context.getString(R.string.delete))

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> onDetailsClick(image)
                    2 -> onMoveToSafeClick(image)
                    3 -> onShareClick(image)
                    4 -> onDeleteClick(image)
                }
                true
            }
            popup.show()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ImageItem>() {
        override fun areItemsTheSame(oldItem: ImageItem, newItem: ImageItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ImageItem, newItem: ImageItem) =
            oldItem == newItem
    }
}
