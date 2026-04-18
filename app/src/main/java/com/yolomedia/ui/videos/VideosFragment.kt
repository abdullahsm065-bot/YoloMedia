package com.yolomedia.ui.videos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.yolomedia.R
import com.yolomedia.data.model.SortOrder
import com.yolomedia.data.model.VideoItem
import com.yolomedia.ui.player.VideoPlayerActivity
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

    private val folderAdapter = VideoFolderAdapter { folder ->
        viewModel.loadVideosInFolder(folder.path, folder.name)
    }

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

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = folderAdapter

        setupListeners()
        observeData()
        applyTheme()

        viewModel.loadFolders()
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
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
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(R.string.confirm_delete)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.deleteVideo(video)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showDetailsDialog(video: VideoItem) {
        val context = requireContext()
        val details = StringBuilder().apply {
            append("${getString(R.string.detail_name)}: ${video.title}\n\n")
            append("${getString(R.string.detail_path)}: ${video.path}\n\n")
            append("${getString(R.string.detail_size)}: ${FormatUtils.formatFileSize(video.size)}\n\n")
            append("${getString(R.string.detail_duration)}: ${FormatUtils.formatDuration(video.duration)}\n\n")
            append("${getString(R.string.detail_resolution)}: ${FormatUtils.formatResolution(video.width, video.height)}\n\n")
            append("${getString(R.string.detail_date)}: ${FormatUtils.formatDate(video.dateAdded)}\n\n")
            append("${getString(R.string.detail_type)}: ${video.mimeType}")
        }

        AlertDialog.Builder(context)
            .setTitle(R.string.details)
            .setMessage(details.toString())
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun showMoveToSafeDialog(video: VideoItem) {
        val context = requireContext()
        val folderNames = safeViewModel.getSafeFolderNames()

        if (folderNames.isEmpty()) {
            showCreateFolderForSafeDialog(video)
            return
        }

        val options = folderNames.toMutableList()
        options.add("+ Create New Folder")

        AlertDialog.Builder(context)
            .setTitle(R.string.select_folder)
            .setItems(options.toTypedArray()) { _, which ->
                if (which < folderNames.size) {
                    confirmMoveToSafe(video, folderNames[which])
                } else {
                    showCreateFolderForSafeDialog(video)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCreateFolderForSafeDialog(video: VideoItem) {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.folder_name)
            setPadding(64, 32, 64, 16)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.create_folder)
            .setView(input)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    safeViewModel.createFolder(name)
                    confirmMoveToSafe(video, name)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmMoveToSafe(video: VideoItem, folderName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_move_safe)
            .setMessage("Move \"${video.title}\" to Safe folder \"$folderName\"?")
            .setPositiveButton(R.string.yes) { _, _ ->
                safeViewModel.moveVideoToSafe(video, folderName)
                safeViewModel.operationResult.observe(viewLifecycleOwner) { result ->
                    if (result != null) {
                        val msg = when (result) {
                            is SafeViewModel.OperationResult.Success -> result.message
                            is SafeViewModel.OperationResult.Error -> result.message
                        }
                        AlertDialog.Builder(requireContext())
                            .setMessage(msg)
                            .setPositiveButton(R.string.ok, null)
                            .show()
                        safeViewModel.clearOperationResult()
                        viewModel.loadFolders()
                    }
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
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
