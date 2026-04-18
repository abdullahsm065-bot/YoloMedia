package com.yolomedia.ui.safe

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
import com.yolomedia.R
import com.yolomedia.data.model.SafeFolder
import com.yolomedia.ui.theme.ThemeManager

class SafeFolderAdapter(
    private val onFolderClick: (SafeFolder) -> Unit,
    private val onDeleteClick: (SafeFolder) -> Unit,
    private val onRenameClick: (SafeFolder) -> Unit
) : ListAdapter<SafeFolder, SafeFolderAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_safe_folder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView as CardView
        private val tvFolderName: TextView = itemView.findViewById(R.id.tv_folder_name)
        private val tvFolderInfo: TextView = itemView.findViewById(R.id.tv_folder_info)
        private val btnMore: ImageView = itemView.findViewById(R.id.btn_more)

        fun bind(folder: SafeFolder) {
            val context = itemView.context
            tvFolderName.text = folder.name
            tvFolderInfo.text = "${folder.videoCount} videos, ${folder.photoCount} photos"

            cardView.setCardBackgroundColor(ThemeManager.getCardColor(context))
            tvFolderName.setTextColor(ThemeManager.getTextPrimaryColor(context))
            tvFolderInfo.setTextColor(ThemeManager.getTextSecondaryColor(context))
            ThemeManager.tintIcon(btnMore, context)

            itemView.setOnClickListener { onFolderClick(folder) }

            btnMore.setOnClickListener { view ->
                val popup = PopupMenu(context, view)
                popup.menu.add(0, 1, 0, context.getString(R.string.rename))
                popup.menu.add(0, 2, 1, context.getString(R.string.delete))
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> onRenameClick(folder)
                        2 -> onDeleteClick(folder)
                    }
                    true
                }
                popup.show()
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SafeFolder>() {
        override fun areItemsTheSame(oldItem: SafeFolder, newItem: SafeFolder) =
            oldItem.path == newItem.path

        override fun areContentsTheSame(oldItem: SafeFolder, newItem: SafeFolder) =
            oldItem == newItem
    }
}
