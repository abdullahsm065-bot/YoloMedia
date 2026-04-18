package com.yolomedia.ui.gallery

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.yolomedia.R
import com.yolomedia.data.model.ImageItem
import com.yolomedia.data.model.SortOrder
import com.yolomedia.data.preferences.AppPreferences
import com.yolomedia.ui.common.ModernDialog
import com.yolomedia.ui.theme.ThemeManager
import com.yolomedia.utils.FormatUtils
import com.yolomedia.viewmodel.GalleryViewModel
import com.yolomedia.viewmodel.SafeViewModel
import java.io.File

class GalleryFragment : Fragment() {

    private lateinit var viewModel: GalleryViewModel
    private lateinit var safeViewModel: SafeViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvTitle: TextView
    private lateinit var btnSort: ImageView
    private lateinit var preferences: AppPreferences
    private lateinit var selectionBar: LinearLayout
    private lateinit var tvSelectionCount: TextView
    private lateinit var btnCloseSelection: ImageView
    private lateinit var btnSelectAll: ImageView
    private lateinit var btnShareSelected: ImageView
    private lateinit var btnSafeSelected: ImageView
    private lateinit var btnDeleteSelected: ImageView

    private val imageAdapter = ImageAdapter(
        onImageClick = { image, _ -> openImageViewer(image) },
        onDeleteClick = { image -> showDeleteDialog(image) },
        onDetailsClick = { image -> showDetailsDialog(image) },
        onMoveToSafeClick = { image -> showMoveToSafeDialog(image) },
        onShareClick = { image -> shareImage(image) }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[GalleryViewModel::class.java]
        safeViewModel = ViewModelProvider(requireActivity())[SafeViewModel::class.java]
        preferences = AppPreferences(requireContext())

        recyclerView = view.findViewById(R.id.recycler_view)
        progressBar = view.findViewById(R.id.progress_bar)
        emptyState = view.findViewById(R.id.empty_state)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        tvTitle = view.findViewById(R.id.tv_title)
        btnSort = view.findViewById(R.id.btn_sort)
        selectionBar = view.findViewById(R.id.selection_bar)
        tvSelectionCount = view.findViewById(R.id.tv_selection_count)
        btnCloseSelection = view.findViewById(R.id.btn_close_selection)
        btnSelectAll = view.findViewById(R.id.btn_select_all)
        btnShareSelected = view.findViewById(R.id.btn_share_selected)
        btnSafeSelected = view.findViewById(R.id.btn_safe_selected)
        btnDeleteSelected = view.findViewById(R.id.btn_delete_selected)

        recyclerView.layoutManager = GridLayoutManager(requireContext(), preferences.gridColumnCount)
        recyclerView.adapter = imageAdapter

        setupListeners()
        setupSelectionBar()
        observeData()
        applyTheme()

        viewModel.loadImages()
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
        (recyclerView.layoutManager as? GridLayoutManager)?.spanCount = preferences.gridColumnCount
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener { viewModel.loadImages() }
        btnSort.setOnClickListener { showSortMenu(it) }
    }

    private fun observeData() {
        viewModel.images.observe(viewLifecycleOwner) { images ->
            imageAdapter.submitList(images)
            emptyState.visibility = if (images.isEmpty()) View.VISIBLE else View.GONE
            swipeRefresh.isRefreshing = false
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            progressBar.visibility = if (loading && imageAdapter.currentList.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun applyTheme() {
        val context = context ?: return
        view?.setBackgroundColor(ThemeManager.getBackgroundColor(context))
        tvTitle.setTextColor(ThemeManager.getTextPrimaryColor(context))
        ThemeManager.tintIcon(btnSort, context)
        swipeRefresh.setColorSchemeColors(ThemeManager.getAccentColor(context))
    }

    private fun openImageViewer(image: ImageItem) {
        val intent = Intent(requireContext(), ImageViewerActivity::class.java).apply {
            putExtra("image_uri", image.uri.toString())
            putExtra("image_title", image.title)
            putExtra("image_path", image.path)
        }
        startActivity(intent)
    }

    private fun showDeleteDialog(image: ImageItem) {
        ModernDialog.confirm(
            context = requireContext(),
            title = getString(R.string.delete),
            message = getString(R.string.confirm_delete),
            positiveText = getString(R.string.yes),
            negativeText = getString(R.string.no),
            onPositive = { viewModel.deleteImage(image) }
        )
    }

    private fun showDetailsDialog(image: ImageItem) {
        val details = StringBuilder().apply {
            append("${getString(R.string.detail_name)}: ${image.title}\n\n")
            append("${getString(R.string.detail_path)}: ${image.path}\n\n")
            append("${getString(R.string.detail_size)}: ${FormatUtils.formatFileSize(image.size)}\n\n")
            append("${getString(R.string.detail_resolution)}: ${FormatUtils.formatResolution(image.width, image.height)}\n\n")
            append("${getString(R.string.detail_date)}: ${FormatUtils.formatDate(image.dateAdded)}\n\n")
            append("${getString(R.string.detail_type)}: ${image.mimeType}")
        }

        ModernDialog.info(
            context = requireContext(),
            title = getString(R.string.details),
            message = details.toString()
        )
    }

    private fun showMoveToSafeDialog(image: ImageItem) {
        val folderNames = safeViewModel.getSafeFolderNames()

        if (folderNames.isEmpty()) {
            showCreateFolderForSafeDialog(image)
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
                    confirmMoveToSafe(image, folderNames[which])
                } else {
                    showCreateFolderForSafeDialog(image)
                }
            }
        )
    }

    private fun showCreateFolderForSafeDialog(image: ImageItem) {
        ModernDialog.input(
            context = requireContext(),
            title = getString(R.string.create_folder),
            hint = getString(R.string.folder_name),
            onConfirm = { name ->
                if (name.isNotEmpty()) {
                    safeViewModel.createFolder(name)
                    confirmMoveToSafe(image, name)
                }
            }
        )
    }

    private fun confirmMoveToSafe(image: ImageItem, folderName: String) {
        ModernDialog.confirm(
            context = requireContext(),
            title = getString(R.string.confirm_move_safe),
            message = "Move \"${image.title}\" to Safe folder \"$folderName\"?",
            positiveText = getString(R.string.yes),
            negativeText = getString(R.string.no),
            onPositive = {
                safeViewModel.moveImageToSafe(image, folderName)
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
                        viewModel.loadImages()
                    }
                }
            }
        )
    }

    private fun shareImage(image: ImageItem) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = image.mimeType
            val file = File(image.path)
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

    fun handleBackPress(): Boolean {
        if (imageAdapter.isSelectionMode) {
            imageAdapter.exitSelectionMode()
            return true
        }
        return false
    }

    private fun setupSelectionBar() {
        imageAdapter.onSelectionChanged = { count ->
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

        btnCloseSelection.setOnClickListener { imageAdapter.exitSelectionMode() }
        btnSelectAll.setOnClickListener { imageAdapter.selectAll() }

        btnDeleteSelected.setOnClickListener {
            val selected = imageAdapter.getSelectedItems()
            if (selected.isEmpty()) return@setOnClickListener
            ModernDialog.confirm(
                context = requireContext(),
                title = getString(R.string.delete),
                message = "Delete ${selected.size} selected photo(s)?",
                positiveText = getString(R.string.yes),
                negativeText = getString(R.string.no),
                onPositive = {
                    selected.forEach { viewModel.deleteImage(it) }
                    imageAdapter.exitSelectionMode()
                }
            )
        }

        btnShareSelected.setOnClickListener {
            val selected = imageAdapter.getSelectedItems()
            if (selected.isEmpty()) return@setOnClickListener
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                val uris = ArrayList<android.net.Uri>()
                selected.forEach { image ->
                    val file = File(image.path)
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
            val selected = imageAdapter.getSelectedItems()
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
                            selected.forEach { safeViewModel.moveImageToSafe(it, name) }
                            imageAdapter.exitSelectionMode()
                            viewModel.loadImages()
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
                            selected.forEach { safeViewModel.moveImageToSafe(it, folderNames[which]) }
                            imageAdapter.exitSelectionMode()
                            viewModel.loadImages()
                        } else {
                            ModernDialog.input(
                                context = requireContext(),
                                title = getString(R.string.create_folder),
                                hint = getString(R.string.folder_name),
                                onConfirm = { name ->
                                    if (name.isNotEmpty()) {
                                        safeViewModel.createFolder(name)
                                        selected.forEach { safeViewModel.moveImageToSafe(it, name) }
                                        imageAdapter.exitSelectionMode()
                                        viewModel.loadImages()
                                    }
                                }
                            )
                        }
                    }
                )
            }
        }
    }

    fun refreshData() {
        viewModel.loadImages()
    }
}
