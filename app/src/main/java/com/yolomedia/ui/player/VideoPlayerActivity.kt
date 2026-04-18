package com.yolomedia.ui.player

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.yolomedia.R

class VideoPlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var btnBack: ImageView
    private var videoUri: String? = null
    private var playbackPosition: Long = 0
    private var playWhenReady = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        setContentView(R.layout.activity_video_player)

        playerView = findViewById(R.id.player_view)
        btnBack = findViewById(R.id.btn_back)

        videoUri = intent.getStringExtra("video_uri")

        btnBack.setOnClickListener { finish() }

        if (savedInstanceState != null) {
            playbackPosition = savedInstanceState.getLong("playback_position", 0)
            playWhenReady = savedInstanceState.getBoolean("play_when_ready", true)
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        player?.let {
            outState.putLong("playback_position", it.currentPosition)
            outState.putBoolean("play_when_ready", it.playWhenReady)
        }
    }

    private fun initializePlayer() {
        val uri = videoUri ?: return

        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo
            val mediaItem = MediaItem.fromUri(Uri.parse(uri))
            exo.setMediaItem(mediaItem)
            exo.playWhenReady = playWhenReady
            exo.seekTo(playbackPosition)
            exo.prepare()

            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        exo.seekTo(0)
                        exo.pause()
                    }
                }
            })
        }
    }

    private fun releasePlayer() {
        player?.let {
            playbackPosition = it.currentPosition
            playWhenReady = it.playWhenReady
            it.release()
        }
        player = null
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
