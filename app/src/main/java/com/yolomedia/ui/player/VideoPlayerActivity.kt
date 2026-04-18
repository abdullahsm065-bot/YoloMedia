package com.yolomedia.ui.player

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yolomedia.R
import com.yolomedia.data.preferences.AppPreferences
import com.yolomedia.utils.FormatUtils

class VideoPlayerActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var surfaceView: SurfaceView
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
    private lateinit var btnLock: ImageView
    private lateinit var btnRotate: ImageView
    private lateinit var btnLoop: ImageView
    private lateinit var btnAspect: ImageView
    private lateinit var gestureInfoView: TextView
    private lateinit var previewFrame: ImageView

    private var videoUri: String? = null
    private var videoTitle: String? = null
    private var playbackPosition: Int = 0
    private var playWhenReady = true
    private var controlsVisible = true
    private var isPrepared = false
    private var isSeeking = false
    private var controlsLocked = false
    private var isLooping = false
    private var currentSpeedIndex = 3

    private val speeds = floatArrayOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)
    private val speedLabels = arrayOf("0.25x", "0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x", "3x")
    private val aspectModes = arrayOf("Fit", "Fill", "Crop", "16:9", "4:3")
    private var currentAspect = 0

    private val handler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable { hideControls() }
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 250)
        }
    }

    private var retriever: MediaMetadataRetriever? = null
    private lateinit var gestureDetector: GestureDetector
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private var videoScale = 1f
    private var audioManager: AudioManager? = null
    private var orientationSet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
        setContentView(R.layout.activity_video_player)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        bindViews()

        videoUri = intent.getStringExtra("video_uri")
        videoTitle = intent.getStringExtra("video_title")
        tvVideoTitle.text = videoTitle ?: ""

        val prefs = AppPreferences(this)
        currentSpeedIndex = speeds.indexOfFirst { it == prefs.defaultPlaybackSpeed }.coerceAtLeast(3)
        isLooping = prefs.loopVideos

        setupGestures()
        setupListeners()
        setupSurface()

        if (savedInstanceState != null) {
            playbackPosition = savedInstanceState.getInt("playback_position", 0)
            playWhenReady = savedInstanceState.getBoolean("play_when_ready", true)
        }

        markVideoAsPlayed()
        detectVideoOrientation()
    }

    private fun markVideoAsPlayed() {
        videoUri?.let { AppPreferences(this).markVideoPlayed(it) }
    }

    private fun detectVideoOrientation() {
        if (orientationSet) return
        try {
            val r = MediaMetadataRetriever()
            r.setDataSource(this, Uri.parse(videoUri))
            val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            r.release()

            val effectiveW = if (rotation == 90 || rotation == 270) h else w
            val effectiveH = if (rotation == 90 || rotation == 270) w else h

            if (effectiveW > 0 && effectiveH > 0) {
                requestedOrientation = if (effectiveW > effectiveH) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                }
                orientationSet = true
            }
        } catch (_: Exception) {}
    }

    private fun bindViews() {
        surfaceView = findViewById(R.id.surface_view)
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
        btnLock = findViewById(R.id.btn_lock)
        btnRotate = findViewById(R.id.btn_rotate)
        btnLoop = findViewById(R.id.btn_loop)
        btnAspect = findViewById(R.id.btn_aspect)
        gestureInfoView = findViewById(R.id.gesture_info)
        previewFrame = findViewById(R.id.preview_frame)
    }

    private fun setupSurface() {
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                initializePlayer(holder)
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                releasePlayer()
            }
        })
    }

    private fun setupGestures() {
        val prefs = AppPreferences(this)
        val seekDuration = prefs.doubleTapSeekDuration * 1000

        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (!prefs.videoGesturesEnabled) return false
                videoScale = (videoScale * detector.scaleFactor).coerceIn(0.5f, 5f)
                surfaceView.scaleX = videoScale
                surfaceView.scaleY = videoScale
                return true
            }
        })

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (controlsLocked) return false
                val screenWidth = resources.displayMetrics.widthPixels
                when {
                    e.x < screenWidth / 3f -> {
                        mediaPlayer?.let { mp ->
                            mp.seekTo(maxOf(0, mp.currentPosition - seekDuration))
                            showGestureInfo("-${seekDuration / 1000}s")
                        }
                    }
                    e.x > screenWidth * 2 / 3f -> {
                        mediaPlayer?.let { mp ->
                            if (isPrepared) mp.seekTo(minOf(mp.duration, mp.currentPosition + seekDuration))
                            showGestureInfo("+${seekDuration / 1000}s")
                        }
                    }
                    else -> {
                        if (videoScale != 1f) {
                            videoScale = 1f
                            surfaceView.animate().scaleX(1f).scaleY(1f)
                                .translationX(0f).translationY(0f)
                                .setDuration(250).setInterpolator(DecelerateInterpolator()).start()
                        } else {
                            togglePlayPause()
                        }
                    }
                }
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (!controlsLocked) toggleControls()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (controlsLocked || !prefs.videoGesturesEnabled) return
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        mp.setPlaybackParams(mp.playbackParams.setSpeed(2f))
                        showGestureInfo("2x Speed")
                    }
                }
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (controlsLocked || scaleGestureDetector.isInProgress || !prefs.videoGesturesEnabled) return false
                if (e1 == null) return false

                if (videoScale > 1.1f) {
                    surfaceView.translationX -= distanceX
                    surfaceView.translationY -= distanceY
                    return true
                }

                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels
                val deltaY = e1.y - e2.y

                if (e1.x < screenWidth / 3f) {
                    adjustBrightness(deltaY / screenHeight)
                    return true
                } else if (e1.x > screenWidth * 2 / 3f) {
                    adjustVolume(deltaY / screenHeight)
                    return true
                }
                return false
            }
        })
    }

    private fun adjustBrightness(change: Float) {
        val lp = window.attributes
        var brightness = lp.screenBrightness
        if (brightness < 0) brightness = 0.5f
        brightness = (brightness + change * 0.5f).coerceIn(0.01f, 1f)
        lp.screenBrightness = brightness
        window.attributes = lp
        showGestureInfo("Brightness: ${(brightness * 100).toInt()}%")
    }

    private fun adjustVolume(change: Float) {
        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val newVol = (curVol + change * maxVol * 0.3f).toInt().coerceIn(0, maxVol)
        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
        showGestureInfo("Volume: ${(newVol * 100) / maxVol}%")
    }

    private fun showGestureInfo(text: String) {
        gestureInfoView.text = text
        gestureInfoView.alpha = 1f
        gestureInfoView.visibility = View.VISIBLE
        gestureInfoView.scaleX = 0.8f
        gestureInfoView.scaleY = 0.8f
        gestureInfoView.animate().scaleX(1f).scaleY(1f).setDuration(150)
            .setInterpolator(OvershootInterpolator(2f)).start()
        handler.removeCallbacksAndMessages(gestureInfoView)
        handler.postDelayed({
            gestureInfoView.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f)
                .setDuration(200).withEndAction {
                    gestureInfoView.visibility = View.GONE
                }.start()
        }, 800)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        controlsOverlay.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        val currentSpeed = speeds[currentSpeedIndex]
                        if (mp.playbackParams.speed != currentSpeed) {
                            mp.setPlaybackParams(mp.playbackParams.setSpeed(currentSpeed))
                        }
                    }
                }
            }
            true
        }

        btnPlayPause.setOnClickListener {
            if (!controlsLocked) {
                togglePlayPause()
                resetHideTimer()
            }
        }

        val prefs = AppPreferences(this)
        val seekMs = prefs.doubleTapSeekDuration * 1000

        btnSeekBack.setOnClickListener {
            if (!controlsLocked) {
                mediaPlayer?.let { mp ->
                    mp.seekTo(maxOf(0, mp.currentPosition - seekMs))
                    animateSeekButton(btnSeekBack)
                    resetHideTimer()
                }
            }
        }

        btnSeekForward.setOnClickListener {
            if (!controlsLocked) {
                mediaPlayer?.let { mp ->
                    if (isPrepared) mp.seekTo(minOf(mp.duration, mp.currentPosition + seekMs))
                    animateSeekButton(btnSeekForward)
                    resetHideTimer()
                }
            }
        }

        btnSpeed.setOnClickListener {
            if (!controlsLocked) {
                currentSpeedIndex = (currentSpeedIndex + 1) % speeds.size
                mediaPlayer?.setPlaybackParams(mediaPlayer!!.playbackParams.setSpeed(speeds[currentSpeedIndex]))
                btnSpeed.text = speedLabels[currentSpeedIndex]
                resetHideTimer()
            }
        }

        btnLock.setOnClickListener {
            controlsLocked = !controlsLocked
            btnLock.alpha = if (controlsLocked) 1f else 0.8f
            topControls.visibility = if (controlsLocked) View.GONE else View.VISIBLE
            centerControls.visibility = if (controlsLocked) View.GONE else View.VISIBLE
            bottomControls.visibility = if (controlsLocked) View.GONE else View.VISIBLE
            if (controlsLocked) {
                Toast.makeText(this, "Controls locked", Toast.LENGTH_SHORT).show()
            } else {
                showControls()
            }
        }

        btnRotate.setOnClickListener {
            requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }

        btnLoop.setOnClickListener {
            isLooping = !isLooping
            mediaPlayer?.isLooping = isLooping
            btnLoop.alpha = if (isLooping) 1f else 0.5f
            Toast.makeText(this, if (isLooping) "Loop ON" else "Loop OFF", Toast.LENGTH_SHORT).show()
        }

        btnAspect.setOnClickListener {
            currentAspect = (currentAspect + 1) % aspectModes.size
            applyAspectRatio()
            Toast.makeText(this, "Aspect: ${aspectModes[currentAspect]}", Toast.LENGTH_SHORT).show()
        }

        btnLoop.alpha = if (isLooping) 1f else 0.5f

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && isPrepared) {
                    mediaPlayer?.let { mp ->
                        val position = (progress.toLong() * mp.duration) / 1000
                        tvCurrentTime.text = FormatUtils.formatDuration(position)
                        scrubFrame(position)
                    }
                }
            }

            override fun onStartTrackingTouch(bar: SeekBar?) {
                isSeeking = true
                handler.removeCallbacks(hideControlsRunnable)
            }

            override fun onStopTrackingTouch(bar: SeekBar?) {
                isSeeking = false
                bar?.let { b ->
                    mediaPlayer?.let { mp ->
                        val position = (b.progress.toLong() * mp.duration) / 1000
                        mp.seekTo(position.toInt())
                    }
                }
                previewFrame.visibility = View.GONE
                resetHideTimer()
            }
        })
    }

    private fun animateSeekButton(view: View) {
        view.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(150)
                    .setInterpolator(OvershootInterpolator(3f)).start()
            }.start()
    }

    private fun scrubFrame(positionMs: Long) {
        try {
            retriever?.let { r ->
                val bitmap = r.getFrameAtTime(
                    positionMs * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
                bitmap?.let {
                    previewFrame.setImageBitmap(it)
                    previewFrame.visibility = View.VISIBLE
                }
            }
        } catch (_: Exception) {}
    }

    private fun togglePlayPause() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
            } else {
                mp.start()
                mp.setPlaybackParams(mp.playbackParams.setSpeed(speeds[currentSpeedIndex]))
            }
            updatePlayPauseIcon()
            resetHideTimer()
        }
    }

    private fun initializePlayer(holder: SurfaceHolder) {
        val uri = videoUri ?: return
        bufferingIndicator.visibility = View.VISIBLE

        try {
            retriever = MediaMetadataRetriever()
            try {
                retriever?.setDataSource(this, Uri.parse(uri))
            } catch (_: Exception) {}

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@VideoPlayerActivity, Uri.parse(uri))
                setDisplay(holder)

                setOnPreparedListener { mp ->
                    isPrepared = true
                    bufferingIndicator.visibility = View.GONE
                    tvDuration.text = FormatUtils.formatDuration(mp.duration.toLong())

                    if (!orientationSet) {
                        val vw = mp.videoWidth
                        val vh = mp.videoHeight
                        if (vw > 0 && vh > 0) {
                            requestedOrientation = if (vw > vh) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                            }
                            orientationSet = true
                        }
                    }

                    if (playbackPosition > 0) mp.seekTo(playbackPosition)
                    if (playWhenReady) {
                        mp.start()
                        mp.setPlaybackParams(mp.playbackParams.setSpeed(speeds[currentSpeedIndex]))
                    }
                    mp.isLooping = isLooping
                    btnSpeed.text = speedLabels[currentSpeedIndex]
                    updatePlayPauseIcon()
                    handler.post(updateProgressRunnable)
                    resetHideTimer()
                    applyAspectRatio()
                }

                setOnCompletionListener { mp ->
                    if (!isLooping) {
                        mp.seekTo(0)
                        updatePlayPauseIcon()
                        showControls()
                    }
                }

                setOnErrorListener { _, _, _ ->
                    bufferingIndicator.visibility = View.GONE
                    Toast.makeText(this@VideoPlayerActivity, "Playback error", Toast.LENGTH_SHORT).show()
                    true
                }

                setOnBufferingUpdateListener { _, percent ->
                    seekBar.secondaryProgress = percent * 10
                }

                setOnInfoListener { _, what, _ ->
                    when (what) {
                        MediaPlayer.MEDIA_INFO_BUFFERING_START -> bufferingIndicator.visibility = View.VISIBLE
                        MediaPlayer.MEDIA_INFO_BUFFERING_END -> bufferingIndicator.visibility = View.GONE
                    }
                    false
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            bufferingIndicator.visibility = View.GONE
            Toast.makeText(this, "Cannot play this video", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyAspectRatio() {
        mediaPlayer?.let { mp ->
            if (!isPrepared) return
            val videoWidth = mp.videoWidth
            val videoHeight = mp.videoHeight
            if (videoWidth == 0 || videoHeight == 0) return

            val screenWidth = resources.displayMetrics.widthPixels.toFloat()
            val screenHeight = resources.displayMetrics.heightPixels.toFloat()
            val params = surfaceView.layoutParams as FrameLayout.LayoutParams

            when (currentAspect) {
                0 -> {
                    val ratio = minOf(screenWidth / videoWidth, screenHeight / videoHeight)
                    params.width = (videoWidth * ratio).toInt()
                    params.height = (videoHeight * ratio).toInt()
                }
                1 -> {
                    params.width = screenWidth.toInt()
                    params.height = screenHeight.toInt()
                }
                2 -> {
                    val ratio = maxOf(screenWidth / videoWidth, screenHeight / videoHeight)
                    params.width = (videoWidth * ratio).toInt()
                    params.height = (videoHeight * ratio).toInt()
                }
                3 -> {
                    params.width = screenWidth.toInt()
                    params.height = (screenWidth * 9 / 16).toInt()
                }
                4 -> {
                    params.width = screenWidth.toInt()
                    params.height = (screenWidth * 3 / 4).toInt()
                }
            }
            params.gravity = android.view.Gravity.CENTER
            surfaceView.layoutParams = params
        }
    }

    private fun releasePlayer() {
        handler.removeCallbacks(updateProgressRunnable)
        handler.removeCallbacks(hideControlsRunnable)
        mediaPlayer?.let {
            if (isPrepared) {
                playbackPosition = it.currentPosition
                playWhenReady = it.isPlaying
            }
            it.release()
        }
        mediaPlayer = null
        isPrepared = false
        try { retriever?.release() } catch (_: Exception) {}
        retriever = null
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mediaPlayer?.let {
            if (isPrepared) {
                outState.putInt("playback_position", it.currentPosition)
                outState.putBoolean("play_when_ready", it.isPlaying)
            }
        }
    }

    private fun updatePlayPauseIcon() {
        val isPlaying = mediaPlayer?.isPlaying == true
        ivPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        val scaleX = ObjectAnimator.ofFloat(ivPlayPause, "scaleX", 0.6f, 1f)
        val scaleY = ObjectAnimator.ofFloat(ivPlayPause, "scaleY", 0.6f, 1f)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 200
            interpolator = OvershootInterpolator(2f)
            start()
        }
    }

    private fun updateProgress() {
        if (isSeeking) return
        mediaPlayer?.let { mp ->
            if (isPrepared && mp.duration > 0) {
                val progress = ((mp.currentPosition.toLong() * 1000) / mp.duration).toInt()
                seekBar.progress = progress
                tvCurrentTime.text = FormatUtils.formatDuration(mp.currentPosition.toLong())
            }
        }
    }

    private fun toggleControls() {
        if (controlsVisible) hideControls() else showControls()
    }

    private fun showControls() {
        controlsVisible = true
        if (!controlsLocked) {
            animateView(topControls, true)
            animateView(centerControls, true)
            animateView(bottomControls, true)
        }
        btnLock.visibility = View.VISIBLE
        resetHideTimer()
    }

    private fun hideControls() {
        controlsVisible = false
        animateView(topControls, false)
        animateView(centerControls, false)
        animateView(bottomControls, false)
        if (!controlsLocked) btnLock.visibility = View.GONE
    }

    private fun animateView(view: View, show: Boolean) {
        view.animate()
            .alpha(if (show) 1f else 0f)
            .setDuration(200)
            .setInterpolator(DecelerateInterpolator())
            .withStartAction { if (show) view.visibility = View.VISIBLE }
            .withEndAction { if (!show) view.visibility = View.GONE }
            .start()
    }

    private fun resetHideTimer() {
        handler.removeCallbacks(hideControlsRunnable)
        handler.postDelayed(hideControlsRunnable, 4000)
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
