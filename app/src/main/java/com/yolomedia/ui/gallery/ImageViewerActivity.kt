package com.yolomedia.ui.gallery

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.yolomedia.R

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var ivImage: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var btnShare: ImageView
    private lateinit var btnDelete: ImageView
    private lateinit var btnInfo: ImageView
    private lateinit var bottomActions: LinearLayout
    private var imageUri: String? = null
    private var imagePath: String? = null
    private var controlsVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        setContentView(R.layout.activity_image_viewer)

        ivImage = findViewById(R.id.iv_image)
        btnBack = findViewById(R.id.btn_back)
        btnShare = findViewById(R.id.btn_share)
        btnDelete = findViewById(R.id.btn_delete)
        btnInfo = findViewById(R.id.btn_info)
        bottomActions = findViewById(R.id.bottom_actions)

        imageUri = intent.getStringExtra("image_uri")
        imagePath = intent.getStringExtra("image_path")

        imageUri?.let { uri ->
            Glide.with(this)
                .load(Uri.parse(uri))
                .into(ivImage)
        }

        btnBack.setOnClickListener { finish() }

        ivImage.setOnClickListener {
            controlsVisible = !controlsVisible
            val visibility = if (controlsVisible) View.VISIBLE else View.GONE
            btnBack.visibility = visibility
            bottomActions.visibility = visibility
        }

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
            imageUri?.let { uri ->
                try {
                    contentResolver.delete(Uri.parse(uri), null, null)
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Tint icons white for dark background
        btnBack.setColorFilter(0xFFFFFFFF.toInt())
        btnShare.setColorFilter(0xFFFFFFFF.toInt())
        btnDelete.setColorFilter(0xFFFFFFFF.toInt())
        btnInfo.setColorFilter(0xFFFFFFFF.toInt())
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
