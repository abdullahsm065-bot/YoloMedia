package com.yolomedia.ui.gallery

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.yolomedia.R
import com.yolomedia.data.model.ImageItem
import com.yolomedia.ui.common.ModernDialog
import com.yolomedia.ui.common.ZoomableImageView
import com.yolomedia.utils.FormatUtils
import com.yolomedia.viewmodel.GalleryViewModel
import com.yolomedia.viewmodel.SafeViewModel

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var ivImage: ZoomableImageView
    private lateinit var btnBack: ImageView
    private lateinit var btnShare: LinearLayout
    private lateinit var btnDelete: LinearLayout
    private lateinit var btnInfo: LinearLayout
    private lateinit var btnMoveSafe: LinearLayout
    private lateinit var bottomActions: LinearLayout
    private lateinit var topBar: LinearLayout
    private lateinit var tvImageName: TextView
    private lateinit var safeViewModel: SafeViewModel
    private lateinit var galleryViewModel: GalleryViewModel

    private var imageUri: String? = null
    private var imagePath: String? = null
    private var imageTitle: String? = null
    private var imageSize: Long = 0
    private var imageDateAdded: Long = 0
    private var imageMimeType: String = "image/*"
    private var controlsVisible = true
    private var isSafeMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
        setContentView(R.layout.activity_image_viewer)

        safeViewModel = ViewModelProvider(this)[SafeViewModel::class.java]
        galleryViewModel = ViewModelProvider(this)[GalleryViewModel::class.java]

        bindViews()
        loadIntentData()
        loadImage()
        setupListeners()
        animateEntry()
    }

    private fun bindViews() {
        ivImage = findViewById(R.id.iv_image)
        btnBack = findViewById(R.id.btn_back)
        btnShare = findViewById(R.id.btn_share)
        btnDelete = findViewById(R.id.btn_delete)
        btnInfo = findViewById(R.id.btn_info)
        btnMoveSafe = findViewById(R.id.btn_move_safe)
        bottomActions = findViewById(R.id.bottom_actions)
        topBar = findViewById(R.id.top_bar)
        tvImageName = findViewById(R.id.tv_image_name)
    }

    private fun loadIntentData() {
        imageUri = intent.getStringExtra("image_uri")
        imagePath = intent.getStringExtra("image_path")
        imageTitle = intent.getStringExtra("image_title")
        imageSize = intent.getLongExtra("image_size", 0)
        imageDateAdded = intent.getLongExtra("image_date", 0)
        imageMimeType = intent.getStringExtra("image_mime") ?: "image/*"
        isSafeMode = intent.getBooleanExtra("safe_mode", false)
        tvImageName.text = imageTitle ?: ""
    }

    private fun loadImage() {
        imageUri?.let { uri ->
            Glide.with(this).load(Uri.parse(uri)).into(ivImage)
        }
        if (imagePath != null && imageUri == null) {
            Glide.with(this).load(java.io.File(imagePath!!)).into(ivImage)
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnBack.setColorFilter(0xFFFFFFFF.toInt())

        btnShare.setOnClickListener {
            val shareUri = if (imageUri != null) Uri.parse(imageUri) else {
                imagePath?.let { path ->
                    androidx.core.content.FileProvider.getUriForFile(
                        this, "$packageName.fileprovider", java.io.File(path)
                    )
                }
            }
            shareUri?.let { uri ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = imageMimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.share)))
            }
        }

        btnDelete.setOnClickListener {
            ModernDialog.confirm(
                context = this,
                title = getString(R.string.delete),
                message = getString(R.string.confirm_delete),
                positiveText = getString(R.string.yes),
                negativeText = getString(R.string.no),
                onPositive = {
                    if (isSafeMode) {
                        imagePath?.let { path ->
                            java.io.File(path).delete()
                            val metaPath = path.replace(".nomedia_photo", ".meta")
                            java.io.File(metaPath).delete()
                        }
                    } else {
                        imageUri?.let { uri ->
                            try {
                                contentResolver.delete(Uri.parse(uri), null, null)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
            )
        }

        btnInfo.setOnClickListener { showDetailsDialog() }

        if (isSafeMode) {
            btnMoveSafe.visibility = View.GONE
        } else {
            btnMoveSafe.setOnClickListener { showMoveToSafeDialog() }
        }
    }

    private fun animateEntry() {
        bottomActions.translationY = 120f
        bottomActions.alpha = 0f
        bottomActions.animate().translationY(0f).alpha(1f)
            .setDuration(350).setInterpolator(DecelerateInterpolator()).start()

        topBar.alpha = 0f
        topBar.animate().alpha(1f).setDuration(250).start()
    }

    fun toggleControls() {
        controlsVisible = !controlsVisible
        val target = if (controlsVisible) 1f else 0f
        topBar.animate().alpha(target).setDuration(200).start()
        bottomActions.animate().alpha(target).setDuration(200).start()
    }

    private fun showDetailsDialog() {
        val details = StringBuilder().apply {
            append("${getString(R.string.detail_name)}: ${imageTitle ?: "Unknown"}\n\n")
            append("${getString(R.string.detail_path)}: ${imagePath ?: "Unknown"}\n\n")
            if (imageSize > 0) {
                append("${getString(R.string.detail_size)}: ${FormatUtils.formatFileSize(imageSize)}\n\n")
            }
            if (imageDateAdded > 0) {
                append("${getString(R.string.detail_date)}: ${FormatUtils.formatDate(imageDateAdded)}\n\n")
            }
            append("${getString(R.string.detail_type)}: $imageMimeType")
        }

        ModernDialog.info(
            context = this,
            title = getString(R.string.details),
            message = details.toString()
        )
    }

    private fun showMoveToSafeDialog() {
        val folderNames = safeViewModel.getSafeFolderNames()

        if (folderNames.isEmpty()) {
            ModernDialog.input(
                context = this,
                title = getString(R.string.create_folder),
                hint = getString(R.string.folder_name),
                onConfirm = { name ->
                    if (name.isNotEmpty()) {
                        safeViewModel.createFolder(name)
                        performMoveToSafe(name)
                    }
                }
            )
            return
        }

        val options = folderNames.toMutableList()
        options.add("+ Create New Folder")

        ModernDialog.list(
            context = this,
            title = getString(R.string.select_folder),
            options = options.toTypedArray(),
            onSelect = { which ->
                if (which < folderNames.size) {
                    performMoveToSafe(folderNames[which])
                } else {
                    ModernDialog.input(
                        context = this,
                        title = getString(R.string.create_folder),
                        hint = getString(R.string.folder_name),
                        onConfirm = { name ->
                            if (name.isNotEmpty()) {
                                safeViewModel.createFolder(name)
                                performMoveToSafe(name)
                            }
                        }
                    )
                }
            }
        )
    }

    private fun performMoveToSafe(folderName: String) {
        ModernDialog.confirm(
            context = this,
            title = getString(R.string.confirm_move_safe),
            message = "Move \"${imageTitle}\" to Safe folder \"$folderName\"?",
            positiveText = getString(R.string.yes),
            negativeText = getString(R.string.no),
            onPositive = {
                imageUri?.let { uri ->
                    val imageItem = ImageItem(
                        id = 0,
                        title = imageTitle ?: "",
                        path = imagePath ?: "",
                        uri = Uri.parse(uri),
                        size = imageSize,
                        dateAdded = imageDateAdded,
                        dateModified = 0,
                        mimeType = imageMimeType
                    )
                    safeViewModel.moveImageToSafe(imageItem, folderName)
                    Toast.makeText(this, getString(R.string.moved_successfully), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )
    }
}
