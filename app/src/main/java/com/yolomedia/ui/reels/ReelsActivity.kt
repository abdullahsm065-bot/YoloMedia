package com.yolomedia.ui.reels

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.viewpager2.widget.ViewPager2
import com.yolomedia.R
import com.yolomedia.data.model.VideoItem
import com.yolomedia.data.preferences.AppPreferences
import com.yolomedia.ui.common.ModernDialog
import com.yolomedia.utils.FormatUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReelsActivity : AppCompatActivity() {

    private lateinit var pager: ViewPager2
    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView
    private var adapter: ReelAdapter? = null
    private var videos: ArrayList<VideoItem> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_reels)

        pager = findViewById(R.id.reels_pager)
        btnBack = findViewById(R.id.btn_reels_back)
        tvTitle = findViewById(R.id.tv_reels_title)

        val folderName = intent.getStringExtra("folder_name") ?: "Reels"
        tvTitle.text = folderName

        @Suppress("DEPRECATION")
        videos = intent.getParcelableArrayListExtra<VideoItem>("videos") ?: arrayListOf()
        val startPosition = intent.getIntExtra("start_position", 0)

        adapter = ReelAdapter(
            videos = videos,
            onDeleteClick = { video -> showDeleteDialog(video) },
            onDetailsClick = { video -> showDetailsDialog(video) },
            onShareClick = { video -> shareVideo(video) },
            onMoveToSafeClick = { video -> showMoveToSafeDialog(video) },
            onRemoveFromReelClick = if (intent.getBooleanExtra("is_reel_folder", false)) {
                { video -> showRemoveFromReelDialog(video) }
            } else null
        )
        pager.adapter = adapter
        pager.offscreenPageLimit = 2

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            private var prevPos = -1

            override fun onPageSelected(position: Int) {
                if (prevPos >= 0) {
                    adapter?.pauseCurrent()
                }
                prevPos = position

                val prefs = AppPreferences(this@ReelsActivity)
                videos.getOrNull(position)?.let {
                    prefs.markVideoPlayed(it.uri)
                    prefs.addRecentlyViewed(it.uri)
                }
            }
        })

        if (startPosition in videos.indices) {
            pager.setCurrentItem(startPosition, false)
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun showDeleteDialog(video: VideoItem) {
        ModernDialog.confirm(
            context = this,
            title = "Delete Video",
            message = "Are you sure you want to delete \"${video.name}\"?",
            positiveText = "Delete",
            onPositive = {
                try {
                    val uri = Uri.parse(video.uri)
                    contentResolver.delete(uri, null, null)
                    Toast.makeText(this, "Video deleted", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showDetailsDialog(video: VideoItem) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val details = """
            Name: ${video.name}
            Duration: ${FormatUtils.formatDuration(video.duration)}
            Size: ${FormatUtils.formatFileSize(video.size)}
            Resolution: ${video.width}x${video.height}
            Date: ${dateFormat.format(Date(video.dateAdded * 1000))}
            Path: ${video.path}
        """.trimIndent()

        ModernDialog.info(
            context = this,
            title = "Video Details",
            message = details
        )
    }

    private fun shareVideo(video: VideoItem) {
        try {
            val file = File(video.path)
            val shareUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = video.mimeType.ifEmpty { "video/*" }
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Video"))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to share", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMoveToSafeDialog(video: VideoItem) {
        ModernDialog.info(
            context = this,
            title = "Move to Safe",
            message = "Open the Safe section to move videos securely."
        )
    }

    private fun showRemoveFromReelDialog(video: VideoItem) {
        ModernDialog.confirm(
            context = this,
            title = "Remove from Reel",
            message = "This video will still be in the folder but won't appear as a reel.",
            positiveText = "Remove",
            onPositive = {
                Toast.makeText(this, "Removed from reel view", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onPause() {
        super.onPause()
        adapter?.pauseCurrent()
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter?.releaseAll()
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
