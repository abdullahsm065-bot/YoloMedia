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
import com.yolomedia.data.preferences.AppPreferences
import com.yolomedia.ui.theme.ThemeManager

class SafeFolderAdapter(
    private val onFolderClick: (SafeFolder) -> Unit,
    private val onDeleteClick: (SafeFolder) -> Unit,
    private val onRenameClick: (SafeFolder) -> Unit,
    private val onToggleReel: ((SafeFolder) -> Unit)? = null,
    private val onReelClick: ((SafeFolder) -> Unit)? = null
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
            val prefs = AppPreferences(context)
            val isReel = prefs.isSafeFolderReel(folder.name)

            tvFolderName.text = folder.name
            val infoText = "${folder.videoCount} videos, ${folder.photoCount} photos"
            tvFolderInfo.text = if (isReel) "$infoText \u2022 Reel" else infoText

            cardView.setCardBackgroundColor(ThemeManager.getCardColor(context))
            tvFolderName.setTextColor(ThemeManager.getTextPrimaryColor(context))
            tvFolderInfo.setTextColor(ThemeManager.getTextSecondaryColor(context))
            ThemeManager.tintIcon(btnMore, context)

            itemView.setOnClickListener {
                if (isReel && onReelClick != null) {
                    onReelClick.invoke(folder)
                } else {
                    onFolderClick(folder)
                }
            }

            btnMore.setOnClickListener { view ->
                val popup = PopupMenu(context, view)
                popup.menu.add(0, 1, 0, context.getString(R.string.rename))
                popup.menu.add(0, 2, 1, context.getString(R.string.delete))
                val reelLabel = if (isReel) "Remove Reel Tag" else "Tag as Reel"
                popup.menu.add(0, 3, 2, reelLabel)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> onRenameClick(folder)
                        2 -> onDeleteClick(folder)
                        3 -> onToggleReel?.invoke(folder)
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
