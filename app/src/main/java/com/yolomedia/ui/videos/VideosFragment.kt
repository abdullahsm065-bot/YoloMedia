package com.yolomedia.ui.videos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.yolomedia.R
import com.yolomedia.data.model.MediaFolder
import com.yolomedia.data.model.SortOrder
import com.yolomedia.data.model.VideoItem
import com.yolomedia.data.preferences.AppPreferences
import com.yolomedia.ui.common.ModernDialog
import com.yolomedia.ui.player.VideoPlayerActivity
import com.yolomedia.ui.reels.ReelsActivity
import com.yolomedia.ui.theme.ThemeManager
import com.yolomedia.utils.FormatUtils
import com.yolomedia.viewmodel.SafeViewModel
import com.yolomedia.viewmodel.VideosViewModel
import java.io.File

class VideosFragment : Fragment() {

    private lateinit var viewModel: VideosViewModel
    private lateinit var safeViewModel: SafeViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvTitle: TextView
    private lateinit var btnBack: ImageView
    private lateinit var btnSort: ImageView
    private lateinit var selectionBar: LinearLayout
    private lateinit var tvSelectionCount: TextView
    private lateinit var btnCloseSelection: ImageView
    private lateinit var btnSelectAll: ImageView
    private lateinit var btnShareSelected: ImageView
    private lateinit var btnSafeSelected: ImageView
    private lateinit var btnDeleteSelected: ImageView

    private val folderAdapter = VideoFolderAdapter(
        onFolderClick = { folder -> viewModel.loadVideosInFolder(folder.path, folder.name) },
        onReelClick = { folder -> openFolderAsReels(folder) },
        onToggleReel = { folder -> showReelToggleDialog(folder) }
    )

    private val videoAdapter = VideoAdapter(
        onVideoClick = { video -> openVideoPlayer(video) },
        onDeleteClick = { video -> showDeleteDialog(video) },
        onDetailsClick = { video -> showDetailsDialog(video) },
        onMoveToSafeClick = { video -> showMoveToSafeDialog(video) },
        onShareClick = { video -> shareVideo(video) }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_videos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[VideosViewModel::class.java]
        safeViewModel = ViewModelProvider(requireActivity())[SafeViewModel::class.java]

        recyclerView = view.findViewById(R.id.recycler_view)
        progressBar = view.findViewById(R.id.progress_bar)
        emptyState = view.findViewById(R.id.empty_state)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        tvTitle = view.findViewById(R.id.tv_title)
        btnBack = view.findViewById(R.id.btn_back)
        btnSort = view.findViewById(R.id.btn_sort)
        selectionBar = view.findViewById(R.id.selection_bar)
        tvSelectionCount = view.findViewById(R.id.tv_selection_count)
        btnCloseSelection = view.findViewById(R.id.btn_close_selection)
        btnSelectAll = view.findViewById(R.id.btn_select_all)
        btnShareSelected = view.findViewById(R.id.btn_share_selected)
        btnSafeSelected = view.findViewById(R.id.btn_safe_selected)
        btnDeleteSelected = view.findViewById(R.id.btn_delete_selected)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = folderAdapter

        setupListeners()
        setupSelectionBar()
        observeData()
        applyTheme()

        viewModel.loadFolders()
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
    }

    fun handleBackPress(): Boolean {
        if (videoAdapter.isSelectionMode) {
            videoAdapter.exitSelectionMode()
            return true
        }
        if (viewModel.currentFolder.value != null) {
            viewModel.goBackToFolders()
            return true
        }
        return false
    }

    private fun setupSelectionBar() {
        videoAdapter.onSelectionChanged = { count ->
            if (count > 0) {
                selectionBar.visibility = View.VISIBLE
                selectionBar.setBackgroundColor(ThemeManager.getSurfaceColor(requireContext()))
                tvSelectionCount.text = "$count selected"
                tvSelectionCount.setTextColor(ThemeManager.getTextPrimaryColor(requireContext()))
                ThemeManager.tintIcon(btnCloseSelection, requireContext())
                ThemeManager.tintIconAccent(btnSelectAll, requireContext())
                ThemeManager.tintIconAccent(btnShareSelected, requireContext())
                ThemeManager.tintIconAccent(btnSafeSelected, requireContext())
                btnDeleteSelected.setColorFilter(0xFFE53935.toInt())
            } else {
                selectionBar.visibility = View.GONE
            }
        }

        btnCloseSelection.setOnClickListener { videoAdapter.exitSelectionMode() }

        btnSelectAll.setOnClickListener { videoAdapter.selectAll() }

        btnDeleteSelected.setOnClickListener {
            val selected = videoAdapter.getSelectedItems()
            if (selected.isEmpty()) return@setOnClickListener
            ModernDialog.confirm(
                context = requireContext(),
                title = getString(R.string.delete),
                message = "Delete ${selected.size} selected video(s)?",
                positiveText = getString(R.string.yes),
                negativeText = getString(R.string.no),
                onPositive = {
                    selected.forEach { viewModel.deleteVideo(it) }
                    videoAdapter.exitSelectionMode()
                }
            )
        }

        btnShareSelected.setOnClickListener {
            val selected = videoAdapter.getSelectedItems()
            if (selected.isEmpty()) return@setOnClickListener
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "video/*"
                val uris = ArrayList<android.net.Uri>()
                selected.forEach { video ->
                    val file = File(video.path)
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )
                    uris.add(uri)
                }
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        }

        btnSafeSelected.setOnClickListener {
            val selected = videoAdapter.getSelectedItems()
            if (selected.isEmpty()) return@setOnClickListener
            val folderNames = safeViewModel.getSafeFolderNames()
            if (folderNames.isEmpty()) {
                ModernDialog.input(
                    context = requireContext(),
                    title = getString(R.string.create_folder),
                    hint = getString(R.string.folder_name),
                    onConfirm = { name ->
                        if (name.isNotEmpty()) {
                            safeViewModel.createFolder(name)
                            selected.forEach { safeViewModel.moveVideoToSafe(it, name) }
                            videoAdapter.exitSelectionMode()
                            viewModel.loadFolders()
                        }
                    }
                )
            } else {
                val options = folderNames.toMutableList()
                options.add("+ Create New Folder")
                ModernDialog.list(
                    context = requireContext(),
                    title = getString(R.string.select_folder),
                    options = options.toTypedArray(),
                    onSelect = { which ->
                        if (which < folderNames.size) {
                            selected.forEach { safeViewModel.moveVideoToSafe(it, folderNames[which]) }
                            videoAdapter.exitSelectionMode()
                            viewModel.loadFolders()
                        } else {
                            ModernDialog.input(
                                context = requireContext(),
                                title = getString(R.string.create_folder),
                                hint = getString(R.string.folder_name),
                                onConfirm = { name ->
                                    if (name.isNotEmpty()) {
                                        safeViewModel.createFolder(name)
                                        selected.forEach { safeViewModel.moveVideoToSafe(it, name) }
                                        videoAdapter.exitSelectionMode()
                                        viewModel.loadFolders()
                                    }
                                }
                            )
                        }
                    }
                )
            }
        }
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener {
            if (viewModel.currentFolder.value != null) {
                viewModel.loadVideosInFolder(
                    viewModel.currentFolder.value!!,
                    viewModel.currentFolderName.value ?: ""
                )
            } else {
                viewModel.loadFolders()
            }
        }

        btnBack.setOnClickListener {
            viewModel.goBackToFolders()
        }

        btnSort.setOnClickListener { showSortMenu(it) }
    }

    private fun observeData() {
        viewModel.folders.observe(viewLifecycleOwner) { folders ->
            if (viewModel.currentFolder.value == null) {
                folderAdapter.submitList(folders)
                recyclerView.adapter = folderAdapter
                emptyState.visibility = if (folders.isEmpty()) View.VISIBLE else View.GONE
                swipeRefresh.isRefreshing = false
            }
        }

        viewModel.videos.observe(viewLifecycleOwner) { videos ->
            if (viewModel.currentFolder.value != null) {
                videoAdapter.submitList(videos)
                recyclerView.adapter = videoAdapter
                emptyState.visibility = if (videos.isEmpty()) View.VISIBLE else View.GONE
                swipeRefresh.isRefreshing = false
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            progressBar.visibility = if (loading && folderAdapter.currentList.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.currentFolder.observe(viewLifecycleOwner) { folder ->
            if (folder != null) {
                tvTitle.text = viewModel.currentFolderName.value ?: "Videos"
                btnBack.visibility = View.VISIBLE
            } else {
                tvTitle.text = getString(R.string.videos_title)
                btnBack.visibility = View.GONE
            }
        }
    }

    private fun applyTheme() {
        val context = context ?: return
        view?.setBackgroundColor(ThemeManager.getBackgroundColor(context))
        tvTitle.setTextColor(ThemeManager.getTextPrimaryColor(context))
        ThemeManager.tintIcon(btnBack, context)
        ThemeManager.tintIcon(btnSort, context)
        swipeRefresh.setColorSchemeColors(ThemeManager.getAccentColor(context))
    }

    private fun openVideoPlayer(video: VideoItem) {
        val intent = Intent(requireContext(), VideoPlayerActivity::class.java).apply {
            putExtra("video_uri", video.uri.toString())
            putExtra("video_title", video.title)
        }
        startActivity(intent)
    }

    private fun showDeleteDialog(video: VideoItem) {
        ModernDialog.confirm(
            context = requireContext(),
            title = getString(R.string.delete),
            message = getString(R.string.confirm_delete),
            positiveText = getString(R.string.yes),
            negativeText = getString(R.string.no),
            onPositive = { viewModel.deleteVideo(video) }
        )
    }

    private fun showDetailsDialog(video: VideoItem) {
        val details = StringBuilder().apply {
            append("${getString(R.string.detail_name)}: ${video.title}\n\n")
            append("${getString(R.string.detail_path)}: ${video.path}\n\n")
            append("${getString(R.string.detail_size)}: ${FormatUtils.formatFileSize(video.size)}\n\n")
            append("${getString(R.string.detail_duration)}: ${FormatUtils.formatDuration(video.duration)}\n\n")
            append("${getString(R.string.detail_resolution)}: ${FormatUtils.formatResolution(video.width, video.height)}\n\n")
            append("${getString(R.string.detail_date)}: ${FormatUtils.formatDate(video.dateAdded)}\n\n")
            append("${getString(R.string.detail_type)}: ${video.mimeType}")
        }

        ModernDialog.info(
            context = requireContext(),
            title = getString(R.string.details),
            message = details.toString()
        )
    }

    private fun showMoveToSafeDialog(video: VideoItem) {
        val folderNames = safeViewModel.getSafeFolderNames()

        if (folderNames.isEmpty()) {
            showCreateFolderForSafeDialog(video)
            return
        }

        val options = folderNames.toMutableList()
        options.add("+ Create New Folder")

        ModernDialog.list(
            context = requireContext(),
            title = getString(R.string.select_folder),
            options = options.toTypedArray(),
            onSelect = { which ->
                if (which < folderNames.size) {
                    confirmMoveToSafe(video, folderNames[which])
                } else {
                    showCreateFolderForSafeDialog(video)
                }
            }
        )
    }

    private fun showCreateFolderForSafeDialog(video: VideoItem) {
        ModernDialog.input(
            context = requireContext(),
            title = getString(R.string.create_folder),
            hint = getString(R.string.folder_name),
            onConfirm = { name ->
                if (name.isNotEmpty()) {
                    safeViewModel.createFolder(name)
                    confirmMoveToSafe(video, name)
                }
            }
        )
    }

    private fun confirmMoveToSafe(video: VideoItem, folderName: String) {
        ModernDialog.confirm(
            context = requireContext(),
            title = getString(R.string.confirm_move_safe),
            message = "Move \"${video.title}\" to Safe folder \"$folderName\"?",
            positiveText = getString(R.string.yes),
            negativeText = getString(R.string.no),
            onPositive = {
                safeViewModel.moveVideoToSafe(video, folderName)
                safeViewModel.operationResult.observe(viewLifecycleOwner) { result ->
                    if (result != null) {
                        val msg = when (result) {
                            is SafeViewModel.OperationResult.Success -> result.message
                            is SafeViewModel.OperationResult.Error -> result.message
                        }
                        ModernDialog.info(
                            context = requireContext(),
                            title = if (result is SafeViewModel.OperationResult.Success) "Success" else "Error",
                            message = msg
                        )
                        safeViewModel.clearOperationResult()
                        viewModel.loadFolders()
                    }
                }
            }
        )
    }

    private fun shareVideo(video: VideoItem) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = video.mimeType
            val file = File(video.path)
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun showSortMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, getString(R.string.sort_name_asc))
        popup.menu.add(0, 2, 1, getString(R.string.sort_name_desc))
        popup.menu.add(0, 3, 2, getString(R.string.sort_date_desc))
        popup.menu.add(0, 4, 3, getString(R.string.sort_date_asc))
        popup.menu.add(0, 5, 4, getString(R.string.sort_size_desc))
        popup.menu.add(0, 6, 5, getString(R.string.sort_size_asc))

        popup.setOnMenuItemClickListener { item ->
            val order = when (item.itemId) {
                1 -> SortOrder.NAME_ASC
                2 -> SortOrder.NAME_DESC
                3 -> SortOrder.DATE_DESC
                4 -> SortOrder.DATE_ASC
                5 -> SortOrder.SIZE_DESC
                6 -> SortOrder.SIZE_ASC
                else -> SortOrder.DATE_DESC
            }
            viewModel.setSortOrder(order)
            true
        }
        popup.show()
    }

    private fun openFolderAsReels(folder: MediaFolder) {
        viewModel.loadVideosInFolder(folder.path, folder.name)
        viewModel.videos.observe(viewLifecycleOwner) { videos ->
            if (videos.isNotEmpty() && viewModel.currentFolder.value == folder.path) {
                val intent = Intent(requireContext(), ReelsActivity::class.java).apply {
                    putParcelableArrayListExtra("videos", ArrayList(videos))
                    putExtra("folder_name", folder.name)
                    putExtra("start_position", 0)
                }
                startActivity(intent)
                viewModel.goBackToFolders()
            }
        }
    }

    private fun showReelToggleDialog(folder: MediaFolder) {
        val prefs = AppPreferences(requireContext())
        val isReel = prefs.isReelFolder(folder.path)
        val title = if (isReel) "Remove from Reels" else "Tag as Reel"
        val message = if (isReel)
            "Remove \"${folder.name}\" from Reels? It will open normally."
        else
            "Tag \"${folder.name}\" as Reel? Tapping it will open in vertical swipe mode like Instagram Reels."

        ModernDialog.confirm(
            context = requireContext(),
            title = title,
            message = message,
            positiveText = if (isReel) "Remove" else "Tag",
            negativeText = "Cancel",
            onPositive = {
                prefs.toggleReelFolder(folder.path)
                folderAdapter.notifyDataSetChanged()
            }
        )
    }

    fun refreshData() {
        if (viewModel.currentFolder.value != null) {
            viewModel.loadVideosInFolder(
                viewModel.currentFolder.value!!,
                viewModel.currentFolderName.value ?: ""
            )
        } else {
            viewModel.loadFolders()
        }
    }
}
