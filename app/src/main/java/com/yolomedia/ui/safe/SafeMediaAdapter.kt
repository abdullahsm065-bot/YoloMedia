package com.yolomedia.ui.safe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
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
) : ListAdapter<SafeMediaItem, RecyclerView.ViewHolder>(DiffCallback()) {

    var isGridMode = false

    private val selectedItems = mutableSetOf<String>()
    var isSelectionMode = false
        private set
    var onSelectionChanged: ((Int) -> Unit)? = null

    fun toggleSelection(path: String) {
        if (selectedItems.contains(path)) selectedItems.remove(path) else selectedItems.add(path)
        if (selectedItems.isEmpty()) exitSelectionMode()
        onSelectionChanged?.invoke(selectedItems.size)
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<SafeMediaItem> {
        return currentList.filter { selectedItems.contains(it.path) }
    }

    fun selectAll() {
        currentList.forEach { selectedItems.add(it.path) }
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

    override fun getItemViewType(position: Int): Int {
        return if (isGridMode) VIEW_TYPE_GRID else VIEW_TYPE_ROW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_GRID) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_safe_photo, parent, false)
            GridViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_safe_media, parent, false)
            RowViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is RowViewHolder -> holder.bind(item)
            is GridViewHolder -> holder.bind(item)
        }
    }

    inner class RowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView as CardView
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val tvInfo: TextView = itemView.findViewById(R.id.tv_info)
        private val btnRestore: ImageView = itemView.findViewById(R.id.btn_restore)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete)
        private val cbSelect: CheckBox = itemView.findViewById(R.id.cb_select)

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
            btnRestore.setColorFilter(ThemeManager.getAccentColor(context))
            btnDelete.setColorFilter(ThemeManager.getTextSecondaryColor(context))

            Glide.with(context)
                .load(item.uri)
                .transform(CenterCrop(), RoundedCorners(16))
                .placeholder(R.drawable.bg_card_light)
                .into(ivThumbnail)

            if (isSelectionMode) {
                cbSelect.visibility = View.VISIBLE
                cbSelect.isChecked = selectedItems.contains(item.path)
                btnRestore.visibility = View.GONE
                btnDelete.visibility = View.GONE

                val accentColor = ThemeManager.getAccentColor(context)
                cbSelect.buttonTintList = android.content.res.ColorStateList.valueOf(accentColor)

                if (selectedItems.contains(item.path)) {
                    cardView.setCardBackgroundColor(
                        ThemeManager.getAccentColor(context) and 0x00FFFFFF or 0x20000000
                    )
                }
            } else {
                cbSelect.visibility = View.GONE
                btnRestore.visibility = View.VISIBLE
                btnDelete.visibility = View.VISIBLE
            }

            itemView.setOnClickListener {
                if (isSelectionMode) toggleSelection(item.path) else onItemClick(item)
            }
            itemView.setOnLongClickListener {
                if (!isSelectionMode) { enterSelectionMode(); toggleSelection(item.path) }
                true
            }
            cbSelect.setOnClickListener { toggleSelection(item.path) }
            btnRestore.setOnClickListener { onRestoreClick(item) }
            btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    inner class GridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        private val btnRestore: ImageView = itemView.findViewById(R.id.btn_restore)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete)
        private val cbSelect: CheckBox = itemView.findViewById(R.id.cb_select)
        private val selectionOverlay: View = itemView.findViewById(R.id.selection_overlay)

        fun bind(item: SafeMediaItem) {
            val context = itemView.context

            Glide.with(context)
                .load(item.uri)
                .centerCrop()
                .placeholder(R.drawable.bg_card_light)
                .into(ivThumbnail)

            if (isSelectionMode) {
                cbSelect.visibility = View.VISIBLE
                cbSelect.isChecked = selectedItems.contains(item.path)
                selectionOverlay.visibility = if (selectedItems.contains(item.path)) View.VISIBLE else View.GONE
                btnRestore.visibility = View.GONE
                btnDelete.visibility = View.GONE

                val accentColor = ThemeManager.getAccentColor(context)
                cbSelect.buttonTintList = android.content.res.ColorStateList.valueOf(accentColor)
            } else {
                cbSelect.visibility = View.GONE
                selectionOverlay.visibility = View.GONE
                btnRestore.visibility = View.VISIBLE
                btnDelete.visibility = View.VISIBLE
            }

            itemView.setOnClickListener {
                if (isSelectionMode) toggleSelection(item.path) else onItemClick(item)
            }
            itemView.setOnLongClickListener {
                if (!isSelectionMode) { enterSelectionMode(); toggleSelection(item.path) }
                true
            }
            cbSelect.setOnClickListener { toggleSelection(item.path) }
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

    companion object {
        private const val VIEW_TYPE_ROW = 0
        private const val VIEW_TYPE_GRID = 1
    }
}
