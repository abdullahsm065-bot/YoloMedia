package com.yolomedia.ui.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.yolomedia.R
import com.yolomedia.data.model.ImageItem
import com.yolomedia.ui.theme.ThemeManager

class ImageAdapter(
    private val onImageClick: (ImageItem, Int) -> Unit,
    private val onDeleteClick: (ImageItem) -> Unit,
    private val onDetailsClick: (ImageItem) -> Unit,
    private val onMoveToSafeClick: (ImageItem) -> Unit,
    private val onShareClick: (ImageItem) -> Unit
) : ListAdapter<ImageItem, ImageAdapter.ViewHolder>(DiffCallback()) {

    private var animationsEnabled = true
    private var lastAnimatedPosition = -1

    private val selectedItems = mutableSetOf<Long>()
    var isSelectionMode = false
        private set
    var onSelectionChanged: ((Int) -> Unit)? = null

    fun setAnimationsEnabled(enabled: Boolean) {
        animationsEnabled = enabled
    }

    fun toggleSelection(id: Long) {
        if (selectedItems.contains(id)) selectedItems.remove(id) else selectedItems.add(id)
        if (selectedItems.isEmpty()) exitSelectionMode()
        onSelectionChanged?.invoke(selectedItems.size)
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<ImageItem> {
        return currentList.filter { selectedItems.contains(it.id) }
    }

    fun selectAll() {
        currentList.forEach { selectedItems.add(it.id) }
        onSelectionChanged?.invoke(selectedItems.size)
        notifyDataSetChanged()
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedItems.clear()
        onSelectionChanged?.invoke(0)
        notifyDataSetChanged()
    }

    fun enterSelectionMode() {
        isSelectionMode = true
        notifyDataSetChanged()
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
        private val cbSelect: CheckBox = itemView.findViewById(R.id.cb_select)
        private val selectionOverlay: View = itemView.findViewById(R.id.selection_overlay)

        fun bind(image: ImageItem, position: Int) {
            val context = itemView.context

            Glide.with(context)
                .load(image.uri)
                .transform(CenterCrop(), RoundedCorners(20))
                .placeholder(R.drawable.bg_card_light)
                .into(ivImage)

            if (isSelectionMode) {
                cbSelect.visibility = View.VISIBLE
                cbSelect.isChecked = selectedItems.contains(image.id)
                selectionOverlay.visibility = if (selectedItems.contains(image.id)) View.VISIBLE else View.GONE

                val accentColor = ThemeManager.getAccentColor(context)
                cbSelect.buttonTintList = android.content.res.ColorStateList.valueOf(accentColor)
            } else {
                cbSelect.visibility = View.GONE
                selectionOverlay.visibility = View.GONE
            }

            ivImage.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(image.id)
                } else {
                    onImageClick(image, position)
                }
            }

            ivImage.setOnLongClickListener {
                if (!isSelectionMode) {
                    enterSelectionMode()
                    toggleSelection(image.id)
                }
                true
            }

            cbSelect.setOnClickListener { toggleSelection(image.id) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ImageItem>() {
        override fun areItemsTheSame(oldItem: ImageItem, newItem: ImageItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ImageItem, newItem: ImageItem) =
            oldItem == newItem
    }
}
