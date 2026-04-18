package com.yolomedia.ui.safe

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yolomedia.R
import com.yolomedia.data.model.SafeFolder
import com.yolomedia.data.repository.SafeMediaItem
import com.yolomedia.ui.common.ModernDialog
import com.yolomedia.ui.gallery.ImageViewerActivity
import com.yolomedia.ui.player.VideoPlayerActivity
import com.yolomedia.ui.theme.ThemeManager
import com.yolomedia.viewmodel.SafeViewModel

class SafeFragment : Fragment() {

    private lateinit var viewModel: SafeViewModel
    private lateinit var lockScreen: LinearLayout
    private lateinit var unlockedContent: LinearLayout
    private lateinit var numberPad: GridLayout
    private lateinit var recyclerSafe: RecyclerView
    private lateinit var emptyFolders: View
    private lateinit var emptyVideos: View
    private lateinit var emptyPhotos: View
    private lateinit var tabSelector: LinearLayout
    private lateinit var tabVideos: TextView
    private lateinit var tabPhotos: TextView
    private lateinit var btnSafeBack: ImageView
    private lateinit var tvSafeTitle: TextView
    private lateinit var btnAddFolder: ImageView
    private lateinit var btnLock: ImageView
    private lateinit var btnBiometric: LinearLayout
    private lateinit var tvPinError: TextView
    private lateinit var dots: List<View>

    private var currentPin = ""
    private var isSettingUp = false
    private var setupPin = ""
    private var isConfirmingPin = false
    private var showingVideos = true
    private var biometricAutoTriggered = false

    private val folderAdapter = SafeFolderAdapter(
        onFolderClick = { folder -> viewModel.openFolder(folder) },
        onDeleteClick = { folder -> showDeleteFolderDialog(folder) },
        onRenameClick = { folder -> showRenameFolderDialog(folder) }
    )

    private val mediaAdapter = SafeMediaAdapter(
        onItemClick = { item -> openSafeMedia(item) },
        onRestoreClick = { item -> showRestoreDialog(item) },
        onDeleteClick = { item -> showDeleteMediaDialog(item) }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_safe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SafeViewModel::class.java]

        bindViews(view)
        setupNumberPad()
        setupListeners()
        observeData()
        applyTheme()

        if (!viewModel.preferences.isSafeSetup) {
            isSettingUp = true
            view.findViewById<TextView>(R.id.tv_title)?.text = getString(R.string.setup_pin)
        }
    }

    override fun onResume() {
        super.onResume()
        applyTheme()

        if (viewModel.isAuthenticated.value != true && !isSettingUp && !biometricAutoTriggered) {
            autoTriggerBiometric()
        }
    }

    private fun autoTriggerBiometric() {
        if (!viewModel.preferences.isSafeSetup || !viewModel.preferences.biometricEnabled) return

        val biometricManager = BiometricManager.from(requireContext())
        val canStrong = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        val canWeak = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        val canAuthenticate = canStrong == BiometricManager.BIOMETRIC_SUCCESS ||
                canWeak == BiometricManager.BIOMETRIC_SUCCESS

        if (canAuthenticate) {
            biometricAutoTriggered = true
            showBiometricPrompt()
        }
    }

    private fun openSafeMedia(item: SafeMediaItem) {
        if (item.isVideo) {
            val intent = Intent(requireContext(), VideoPlayerActivity::class.java).apply {
                putExtra("video_uri", item.uri.toString())
                putExtra("video_title", item.name)
            }
            startActivity(intent)
        } else {
            val intent = Intent(requireContext(), ImageViewerActivity::class.java).apply {
                putExtra("image_path", item.path)
                putExtra("image_title", item.name)
                putExtra("image_size", item.size)
                putExtra("safe_mode", true)
            }
            startActivity(intent)
        }
    }

    private fun bindViews(view: View) {
        lockScreen = view.findViewById(R.id.lock_screen)
        unlockedContent = view.findViewById(R.id.unlocked_content)
        numberPad = view.findViewById(R.id.number_pad)
        recyclerSafe = view.findViewById(R.id.recycler_safe)
        emptyFolders = view.findViewById(R.id.empty_folders)
        emptyVideos = view.findViewById(R.id.empty_safe_videos)
        emptyPhotos = view.findViewById(R.id.empty_safe_photos)
        tabSelector = view.findViewById(R.id.tab_selector)
        tabVideos = view.findViewById(R.id.tab_videos)
        tabPhotos = view.findViewById(R.id.tab_photos)
        btnSafeBack = view.findViewById(R.id.btn_safe_back)
        tvSafeTitle = view.findViewById(R.id.tv_safe_title)
        btnAddFolder = view.findViewById(R.id.btn_add_folder)
        btnLock = view.findViewById(R.id.btn_lock)
        btnBiometric = view.findViewById(R.id.btn_biometric)
        tvPinError = view.findViewById(R.id.tv_pin_error)

        dots = listOf(
            view.findViewById(R.id.dot1),
            view.findViewById(R.id.dot2),
            view.findViewById(R.id.dot3),
            view.findViewById(R.id.dot4)
        )
    }

    private fun setupNumberPad() {
        val context = requireContext()
        val density = context.resources.displayMetrics.density

        val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "DEL")

        numbers.forEach { num ->
            val btn = TextView(context).apply {
                text = if (num == "DEL") "\u232B" else num
                textSize = if (num == "DEL") 20f else 24f
                gravity = Gravity.CENTER
                val size = (64 * density).toInt()
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(
                        (8 * density).toInt(),
                        (6 * density).toInt(),
                        (8 * density).toInt(),
                        (6 * density).toInt()
                    )
                }
                setTextColor(ThemeManager.getTextPrimaryColor(context))

                if (num.isNotEmpty()) {
                    background = ThemeManager.createAccentDrawable(context, 32f).apply {
                        alpha = 25
                    }
                    isClickable = true
                    isFocusable = true

                    setOnClickListener {
                        if (num == "DEL") {
                            if (currentPin.isNotEmpty()) {
                                currentPin = currentPin.dropLast(1)
                                updatePinDots()
                            }
                        } else {
                            if (currentPin.length < 4) {
                                currentPin += num
                                updatePinDots()
                                if (currentPin.length == 4) {
                                    handlePinComplete()
                                }
                            }
                        }
                    }
                }
            }
            numberPad.addView(btn)
        }
    }

    private fun setupListeners() {
        btnBiometric.setOnClickListener { showBiometricPrompt() }

        btnAddFolder.setOnClickListener { showCreateFolderDialog() }

        btnLock.setOnClickListener {
            viewModel.lockSafe()
            biometricAutoTriggered = false
        }

        btnSafeBack.setOnClickListener { viewModel.goBack() }

        tabVideos.setOnClickListener {
            showingVideos = true
            updateTabs()
            loadCurrentContent()
        }

        tabPhotos.setOnClickListener {
            showingVideos = false
            updateTabs()
            loadCurrentContent()
        }

        updateBiometricVisibility()
    }

    private fun updateBiometricVisibility() {
        val biometricManager = BiometricManager.from(requireContext())
        val canStrong = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        val canWeak = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        val canAuthenticate = canStrong == BiometricManager.BIOMETRIC_SUCCESS ||
                canWeak == BiometricManager.BIOMETRIC_SUCCESS
        btnBiometric.visibility = if (canAuthenticate &&
            viewModel.preferences.isSafeSetup && viewModel.preferences.biometricEnabled) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun observeData() {
        viewModel.isAuthenticated.observe(viewLifecycleOwner) { authenticated ->
            if (authenticated) {
                lockScreen.visibility = View.GONE
                unlockedContent.visibility = View.VISIBLE
                viewModel.loadFolders()
            } else {
                lockScreen.visibility = View.VISIBLE
                unlockedContent.visibility = View.GONE
                currentPin = ""
                updatePinDots()
            }
        }

        viewModel.folders.observe(viewLifecycleOwner) { folders ->
            if (viewModel.currentFolder.value == null) {
                folderAdapter.submitList(folders)
                recyclerSafe.adapter = folderAdapter
                recyclerSafe.layoutManager = LinearLayoutManager(requireContext())
                emptyFolders.visibility = if (folders.isEmpty()) View.VISIBLE else View.GONE
                emptyVideos.visibility = View.GONE
                emptyPhotos.visibility = View.GONE
                tabSelector.visibility = View.GONE
                btnSafeBack.visibility = View.GONE
                btnAddFolder.visibility = View.VISIBLE
                tvSafeTitle.text = getString(R.string.safe_title)
            }
        }

        viewModel.currentFolder.observe(viewLifecycleOwner) { folder ->
            if (folder != null) {
                tvSafeTitle.text = folder.name
                btnSafeBack.visibility = View.VISIBLE
                tabSelector.visibility = View.VISIBLE
                btnAddFolder.visibility = View.GONE
                loadCurrentContent()
            } else {
                viewModel.loadFolders()
            }
        }

        viewModel.safeVideos.observe(viewLifecycleOwner) { videos ->
            if (showingVideos && viewModel.currentFolder.value != null) {
                mediaAdapter.submitList(videos)
                emptyVideos.visibility = if (videos.isEmpty()) View.VISIBLE else View.GONE
                emptyPhotos.visibility = View.GONE
            }
        }

        viewModel.safePhotos.observe(viewLifecycleOwner) { photos ->
            if (!showingVideos && viewModel.currentFolder.value != null) {
                mediaAdapter.submitList(photos)
                emptyPhotos.visibility = if (photos.isEmpty()) View.VISIBLE else View.GONE
                emptyVideos.visibility = View.GONE
            }
        }

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
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
                viewModel.clearOperationResult()
            }
        }
    }

    private fun loadCurrentContent() {
        viewModel.currentFolder.value ?: return
        emptyFolders.visibility = View.GONE

        if (showingVideos) {
            recyclerSafe.adapter = mediaAdapter
            recyclerSafe.layoutManager = LinearLayoutManager(requireContext())
            mediaAdapter.submitList(viewModel.safeVideos.value ?: emptyList())
            emptyVideos.visibility = if (viewModel.safeVideos.value.isNullOrEmpty()) View.VISIBLE else View.GONE
            emptyPhotos.visibility = View.GONE
        } else {
            recyclerSafe.adapter = mediaAdapter
            recyclerSafe.layoutManager = GridLayoutManager(requireContext(), 3)
            mediaAdapter.submitList(viewModel.safePhotos.value ?: emptyList())
            emptyPhotos.visibility = if (viewModel.safePhotos.value.isNullOrEmpty()) View.VISIBLE else View.GONE
            emptyVideos.visibility = View.GONE
        }
    }

    private fun updateTabs() {
        val context = requireContext()
        val accentColor = ThemeManager.getAccentColor(context)

        if (showingVideos) {
            tabVideos.background = ThemeManager.createAccentDrawable(context)
            tabVideos.setTextColor(0xFFFFFFFF.toInt())
            tabPhotos.background = ContextCompat.getDrawable(context, R.drawable.bg_tab_unselected)
            tabPhotos.setTextColor(accentColor)
        } else {
            tabPhotos.background = ThemeManager.createAccentDrawable(context)
            tabPhotos.setTextColor(0xFFFFFFFF.toInt())
            tabVideos.background = ContextCompat.getDrawable(context, R.drawable.bg_tab_unselected)
            tabVideos.setTextColor(accentColor)
        }
    }

    private fun updatePinDots() {
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                if (index < currentPin.length) R.drawable.bg_pin_dot_filled
                else R.drawable.bg_pin_dot
            )
        }
    }

    private fun handlePinComplete() {
        if (isSettingUp) {
            if (!isConfirmingPin) {
                setupPin = currentPin
                isConfirmingPin = true
                currentPin = ""
                updatePinDots()
                tvPinError.visibility = View.GONE
            } else {
                if (currentPin == setupPin) {
                    viewModel.setupPin(currentPin)
                    isSettingUp = false
                    isConfirmingPin = false
                } else {
                    tvPinError.text = getString(R.string.pin_mismatch)
                    tvPinError.visibility = View.VISIBLE
                    currentPin = ""
                    setupPin = ""
                    isConfirmingPin = false
                    updatePinDots()
                }
            }
        } else {
            if (viewModel.authenticate(currentPin)) {
                tvPinError.visibility = View.GONE
            } else {
                tvPinError.text = getString(R.string.wrong_pin)
                tvPinError.visibility = View.VISIBLE
                currentPin = ""
                updatePinDots()
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.authenticateWithBiometric()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        val biometricManager = BiometricManager.from(requireContext())
        val canStrong = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

        val promptInfo = if (canStrong) {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setNegativeButtonText(getString(R.string.biometric_prompt_cancel))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()
        } else {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setNegativeButtonText(getString(R.string.biometric_prompt_cancel))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()
        }

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showCreateFolderDialog() {
        ModernDialog.input(
            context = requireContext(),
            title = getString(R.string.create_folder),
            hint = getString(R.string.folder_name),
            onConfirm = { name ->
                if (name.isNotEmpty()) {
                    viewModel.createFolder(name)
                }
            }
        )
    }

    private fun showDeleteFolderDialog(folder: SafeFolder) {
        ModernDialog.confirm(
            context = requireContext(),
            title = getString(R.string.delete),
            message = "Delete folder \"${folder.name}\" and all its contents?",
            positiveText = getString(R.string.yes),
            negativeText = getString(R.string.no),
            onPositive = { viewModel.deleteFolder(folder.name) }
        )
    }

    private fun showRenameFolderDialog(folder: SafeFolder) {
        ModernDialog.input(
            context = requireContext(),
            title = getString(R.string.rename),
            hint = getString(R.string.folder_name),
            initialText = folder.name,
            onConfirm = { newName ->
                if (newName.isNotEmpty() && newName != folder.name) {
                    viewModel.renameFolder(folder.name, newName)
                }
            }
        )
    }

    private fun showRestoreDialog(item: SafeMediaItem) {
        ModernDialog.confirm(
            context = requireContext(),
            title = getString(R.string.restore),
            message = "Restore \"${item.name}\" to your gallery?",
            positiveText = getString(R.string.yes),
            negativeText = getString(R.string.no),
            onPositive = { viewModel.restoreMedia(item.path) }
        )
    }

    private fun showDeleteMediaDialog(item: SafeMediaItem) {
        ModernDialog.confirm(
            context = requireContext(),
            title = getString(R.string.delete),
            message = "Permanently delete \"${item.name}\"?",
            positiveText = getString(R.string.yes),
            negativeText = getString(R.string.no),
            onPositive = {
                java.io.File(item.path).delete()
                viewModel.currentFolder.value?.let { viewModel.openFolder(it) }
            }
        )
    }

    fun handleBackPress(): Boolean {
        if (viewModel.isAuthenticated.value == true) {
            if (viewModel.currentFolder.value != null) {
                viewModel.goBack()
                return true
            }
            viewModel.lockSafe()
            biometricAutoTriggered = false
            return true
        }
        return false
    }

    private fun applyTheme() {
        val context = context ?: return
        view?.setBackgroundColor(ThemeManager.getBackgroundColor(context))
        tvSafeTitle.setTextColor(ThemeManager.getTextPrimaryColor(context))
        ThemeManager.tintIcon(btnSafeBack, context)
        ThemeManager.tintIcon(btnAddFolder, context)
        ThemeManager.tintIcon(btnLock, context)
        updateTabs()
    }
}
