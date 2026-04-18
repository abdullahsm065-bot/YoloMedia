package com.yolomedia.ui.gallery

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.yolomedia.R
import com.yolomedia.data.model.ImageItem
import com.yolomedia.utils.FormatUtils
import com.yolomedia.viewmodel.GalleryViewModel
import com.yolomedia.viewmodel.SafeViewModel

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var ivImage: ImageView
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
    private var controlsVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        setContentView(R.layout.activity_image_viewer)

        safeViewModel = ViewModelProvider(this)[SafeViewModel::class.java]
        galleryViewModel = ViewModelProvider(this)[GalleryViewModel::class.java]

        ivImage = findViewById(R.id.iv_image)
        btnBack = findViewById(R.id.btn_back)
        btnShare = findViewById(R.id.btn_share)
        btnDelete = findViewById(R.id.btn_delete)
        btnInfo = findViewById(R.id.btn_info)
        btnMoveSafe = findViewById(R.id.btn_move_safe)
        bottomActions = findViewById(R.id.bottom_actions)
        topBar = findViewById(R.id.top_bar)
        tvImageName = findViewById(R.id.tv_image_name)

        imageUri = intent.getStringExtra("image_uri")
        imagePath = intent.getStringExtra("image_path")
        imageTitle = intent.getStringExtra("image_title")

        tvImageName.text = imageTitle ?: ""

        imageUri?.let { uri ->
            Glide.with(this)
                .load(Uri.parse(uri))
                .into(ivImage)
        }

        btnBack.setOnClickListener { finish() }

        ivImage.setOnClickListener { toggleControls() }

        btnShare.setOnClickListener {
            imageUri?.let { uri ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.share)))
            }
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.yes) { _, _ ->
                    imageUri?.let { uri ->
                        try {
                            contentResolver.delete(Uri.parse(uri), null, null)
                            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                            finish()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }

        btnInfo.setOnClickListener {
            showDetailsDialog()
        }

        btnMoveSafe.setOnClickListener {
            showMoveToSafeDialog()
        }

        // Tint icons white
        btnBack.setColorFilter(0xFFFFFFFF.toInt())

        // Entrance animation
        bottomActions.translationY = 200f
        bottomActions.animate().translationY(0f).setDuration(400).setInterpolator(DecelerateInterpolator()).start()
        topBar.alpha = 0f
        topBar.animate().alpha(1f).setDuration(300).start()
    }

    private fun toggleControls() {
        controlsVisible = !controlsVisible
        val targetAlpha = if (controlsVisible) 1f else 0f
        ObjectAnimator.ofFloat(topBar, "alpha", targetAlpha).setDuration(250).start()
        ObjectAnimator.ofFloat(bottomActions, "alpha", targetAlpha).setDuration(250).start()
    }

    private fun showDetailsDialog() {
        val details = StringBuilder().apply {
            append("${getString(R.string.detail_name)}: ${imageTitle ?: "Unknown"}\n\n")
            append("${getString(R.string.detail_path)}: ${imagePath ?: "Unknown"}")
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.details)
            .setMessage(details.toString())
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun showMoveToSafeDialog() {
        val folderNames = safeViewModel.getSafeFolderNames()

        if (folderNames.isEmpty()) {
            Toast.makeText(this, "Create a folder in Safe first", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.select_folder)
            .setItems(folderNames.toTypedArray()) { _, which ->
                val folderName = folderNames[which]
                AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_move_safe)
                    .setMessage("Move \"${imageTitle}\" to Safe folder \"$folderName\"?")
                    .setPositiveButton(R.string.yes) { _, _ ->
                        imageUri?.let { uri ->
                            val imageItem = ImageItem(
                                id = 0,
                                title = imageTitle ?: "",
                                path = imagePath ?: "",
                                uri = Uri.parse(uri),
                                size = 0,
                                dateAdded = 0,
                                dateModified = 0,
                                mimeType = "image/*"
                            )
                            safeViewModel.moveImageToSafe(imageItem, folderName)
                            Toast.makeText(this, getString(R.string.moved_successfully), Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    .setNegativeButton(R.string.no, null)
                    .show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun hideSystemUI() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
    }
}
