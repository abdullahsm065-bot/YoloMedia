package com.yolomedia.ui.reels

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.yolomedia.R
import com.yolomedia.data.model.VideoItem
import com.yolomedia.data.preferences.AppPreferences

class ReelsActivity : AppCompatActivity() {

    private lateinit var pager: ViewPager2
    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView
    private var adapter: ReelAdapter? = null

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

        val videos = intent.getParcelableArrayListExtra<VideoItem>("videos") ?: arrayListOf()
        val startPosition = intent.getIntExtra("start_position", 0)

        adapter = ReelAdapter(videos)
        pager.adapter = adapter
        pager.offscreenPageLimit = 1

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            private var prevPos = -1

            override fun onPageSelected(position: Int) {
                if (prevPos >= 0) {
                    adapter?.pauseCurrent()
                }
                prevPos = position

                val prefs = AppPreferences(this@ReelsActivity)
                videos.getOrNull(position)?.let { prefs.markVideoPlayed(it.uri) }
            }
        })

        if (startPosition in videos.indices) {
            pager.setCurrentItem(startPosition, false)
        }

        btnBack.setOnClickListener { finish() }
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
