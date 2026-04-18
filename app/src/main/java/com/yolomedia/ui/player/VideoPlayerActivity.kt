package com.yolomedia.ui.player

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.yolomedia.R
import com.yolomedia.utils.FormatUtils

class VideoPlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var btnBack: ImageView
    private lateinit var btnPlayPause: FrameLayout
    private lateinit var ivPlayPause: ImageView
    private lateinit var btnSeekBack: FrameLayout
    private lateinit var btnSeekForward: FrameLayout
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvVideoTitle: TextView
    private lateinit var btnSpeed: TextView
    private lateinit var controlsOverlay: FrameLayout
    private lateinit var topControls: LinearLayout
    private lateinit var centerControls: LinearLayout
    private lateinit var bottomControls: LinearLayout
    private lateinit var bufferingIndicator: ProgressBar

    private var videoUri: String? = null
    private var videoTitle: String? = null
    private var playbackPosition: Long = 0
    private var playWhenReady = true
    private var controlsVisible = true
    private var currentSpeedIndex = 2
    private val speeds = floatArrayOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 3.0f)
    private val speedLabels = arrayOf("0.25x", "0.5x", "1x", "1.5x", "2x", "3x")

    private val handler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable { hideControls() }
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        setContentView(R.layout.activity_video_player)

        bindViews()

        videoUri = intent.getStringExtra("video_uri")
        videoTitle = intent.getStringExtra("video_title")
        tvVideoTitle.text = videoTitle ?: ""

        setupListeners()

        if (savedInstanceState != null) {
            playbackPosition = savedInstanceState.getLong("playback_position", 0)
            playWhenReady = savedInstanceState.getBoolean("play_when_ready", true)
        }
    }

    private fun bindViews() {
        playerView = findViewById(R.id.player_view)
        btnBack = findViewById(R.id.btn_back)
        btnPlayPause = findViewById(R.id.btn_play_pause)
        ivPlayPause = findViewById(R.id.iv_play_pause)
        btnSeekBack = findViewById(R.id.btn_seek_back)
        btnSeekForward = findViewById(R.id.btn_seek_forward)
        seekBar = findViewById(R.id.seek_bar)
        tvCurrentTime = findViewById(R.id.tv_current_time)
        tvDuration = findViewById(R.id.tv_duration)
        tvVideoTitle = findViewById(R.id.tv_video_title)
        btnSpeed = findViewById(R.id.btn_speed)
        controlsOverlay = findViewById(R.id.controls_overlay)
        topControls = findViewById(R.id.top_controls)
        centerControls = findViewById(R.id.center_controls)
        bottomControls = findViewById(R.id.bottom_controls)
        bufferingIndicator = findViewById(R.id.buffering_indicator)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        playerView.setOnClickListener { toggleControls() }
        controlsOverlay.setOnClickListener { toggleControls() }

        btnPlayPause.setOnClickListener {
            player?.let { exo ->
                if (exo.isPlaying) exo.pause() else exo.play()
                updatePlayPauseIcon()
                resetHideTimer()
            }
        }

        btnSeekBack.setOnClickListener {
            player?.let { exo ->
                exo.seekTo(maxOf(0, exo.currentPosition - 10_000))
                resetHideTimer()
            }
        }

        btnSeekForward.setOnClickListener {
            player?.let { exo ->
                exo.seekTo(minOf(exo.duration, exo.currentPosition + 10_000))
                resetHideTimer()
            }
        }

        btnSpeed.setOnClickListener {
            currentSpeedIndex = (currentSpeedIndex + 1) % speeds.size
            player?.setPlaybackSpeed(speeds[currentSpeedIndex])
            btnSpeed.text = speedLabels[currentSpeedIndex]
            resetHideTimer()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player?.let { exo ->
                        val position = (progress.toLong() * exo.duration) / 1000
                        tvCurrentTime.text = FormatUtils.formatDuration(position)
                    }
                }
            }

            override fun onStartTrackingTouch(bar: SeekBar?) {
                handler.removeCallbacks(hideControlsRunnable)
            }

            override fun onStopTrackingTouch(bar: SeekBar?) {
                bar?.let { b ->
                    player?.let { exo ->
                        val position = (b.progress.toLong() * exo.duration) / 1000
                        exo.seekTo(position)
                    }
                }
                resetHideTimer()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(updateProgressRunnable)
        handler.removeCallbacks(hideControlsRunnable)
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
                    when (playbackState) {
                        Player.STATE_READY -> {
                            tvDuration.text = FormatUtils.formatDuration(exo.duration)
                            bufferingIndicator.visibility = View.GONE
                        }
                        Player.STATE_BUFFERING -> {
                            bufferingIndicator.visibility = View.VISIBLE
                        }
                        Player.STATE_ENDED -> {
                            exo.seekTo(0)
                            exo.pause()
                            updatePlayPauseIcon()
                            showControls()
                        }
                        else -> {
                            bufferingIndicator.visibility = View.GONE
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlayPauseIcon()
                    if (isPlaying) {
                        handler.post(updateProgressRunnable)
                        resetHideTimer()
                    } else {
                        handler.removeCallbacks(hideControlsRunnable)
                    }
                }
            })
        }

        handler.post(updateProgressRunnable)
        resetHideTimer()
    }

    private fun releasePlayer() {
        player?.let {
            playbackPosition = it.currentPosition
            playWhenReady = it.playWhenReady
            it.release()
        }
        player = null
    }

    private fun updatePlayPauseIcon() {
        val isPlaying = player?.isPlaying == true
        ivPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)

        val scaleX = ObjectAnimator.ofFloat(ivPlayPause, "scaleX", 0.7f, 1f)
        val scaleY = ObjectAnimator.ofFloat(ivPlayPause, "scaleY", 0.7f, 1f)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 200
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun updateProgress() {
        player?.let { exo ->
            if (exo.duration > 0) {
                val progress = ((exo.currentPosition * 1000) / exo.duration).toInt()
                seekBar.progress = progress
                tvCurrentTime.text = FormatUtils.formatDuration(exo.currentPosition)
            }
        }
    }

    private fun toggleControls() {
        if (controlsVisible) hideControls() else showControls()
    }

    private fun showControls() {
        controlsVisible = true
        animateView(topControls, true)
        animateView(centerControls, true)
        animateView(bottomControls, true)
        resetHideTimer()
    }

    private fun hideControls() {
        controlsVisible = false
        animateView(topControls, false)
        animateView(centerControls, false)
        animateView(bottomControls, false)
    }

    private fun animateView(view: View, show: Boolean) {
        view.animate()
            .alpha(if (show) 1f else 0f)
            .setDuration(250)
            .withStartAction { if (show) view.visibility = View.VISIBLE }
            .withEndAction { if (!show) view.visibility = View.GONE }
            .start()
    }

    private fun resetHideTimer() {
        handler.removeCallbacks(hideControlsRunnable)
        handler.postDelayed(hideControlsRunnable, 4000)
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
