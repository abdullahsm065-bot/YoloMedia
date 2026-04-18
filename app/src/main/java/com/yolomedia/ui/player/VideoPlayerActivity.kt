package com.yolomedia.ui.player

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
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
import kotlin.math.abs

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
    private var wasPlayingBeforeSeek = false

    private val speeds = floatArrayOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)
    private val speedLabels = arrayOf("0.25x", "0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x", "3x")
    private val aspectModes = arrayOf("Fit", "Fill", "Crop", "16:9", "4:3")
    private var currentAspect = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable { hideControls() }
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            mainHandler.postDelayed(this, 300)
        }
    }

    private var retriever: MediaMetadataRetriever? = null
    private var scrubThread: HandlerThread? = null
    private var scrubHandler: Handler? = null
    private var lastScrubTime = 0L

    private lateinit var gestureDetector: GestureDetector
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private var videoScale = 1f
    private var audioManager: AudioManager? = null
    private var orientationSet = false

    private var isVerticalSwipe = false
    private var isSwipingBrightness = false
    private var isSwipingVolume = false
    private var swipeStartBrightness = -1f
    private var swipeStartVolume = -1
    private var swipeStartY = 0f
    private var isLongPressing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
        setContentView(R.layout.activity_video_player)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        scrubThread = HandlerThread("FrameScrubber").also { it.start() }
        scrubHandler = Handler(scrubThread!!.looper)

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
        if (prefs.autoRotateVideo) detectVideoOrientation()
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
            val ew = if (rotation == 90 || rotation == 270) h else w
            val eh = if (rotation == 90 || rotation == 270) w else h
            if (ew > 0 && eh > 0) {
                requestedOrientation = if (ew > eh)
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                else
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
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
            override fun surfaceCreated(holder: SurfaceHolder) { initializePlayer(holder) }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) { releasePlayer() }
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

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                if (videoScale < 1f) {
                    videoScale = 1f
                    surfaceView.animate().scaleX(1f).scaleY(1f)
                        .translationX(0f).translationY(0f)
                        .setDuration(200).setInterpolator(DecelerateInterpolator()).start()
                }
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
                            showGestureInfo("−${seekDuration / 1000}s")
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
                                .setDuration(200).setInterpolator(DecelerateInterpolator()).start()
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
                        isLongPressing = true
                        mp.setPlaybackParams(mp.playbackParams.setSpeed(2f))
                        showGestureInfo("2× Speed")
                    }
                }
            }
        })
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        controlsOverlay.setOnTouchListener { _, event ->
            val prefs = AppPreferences(this)
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)

            if (prefs.videoGesturesEnabled && !controlsLocked && !scaleGestureDetector.isInProgress) {
                handleSwipeGesture(event)
            }

            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                if (isLongPressing) {
                    isLongPressing = false
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) {
                            mp.setPlaybackParams(mp.playbackParams.setSpeed(speeds[currentSpeedIndex]))
                        }
                    }
                    hideGestureInfo()
                }
                resetSwipeState()
            }
            true
        }

        btnPlayPause.setOnClickListener {
            if (!controlsLocked) { togglePlayPause(); resetHideTimer() }
        }

        val prefs = AppPreferences(this)
        val seekMs = prefs.doubleTapSeekDuration * 1000

        btnSeekBack.setOnClickListener {
            if (!controlsLocked) {
                mediaPlayer?.let { mp ->
                    mp.seekTo(maxOf(0, mp.currentPosition - seekMs))
                    animateButton(it)
                    resetHideTimer()
                }
            }
        }

        btnSeekForward.setOnClickListener {
            if (!controlsLocked) {
                mediaPlayer?.let { mp ->
                    if (isPrepared) mp.seekTo(minOf(mp.duration, mp.currentPosition + seekMs))
                    animateButton(it)
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
            requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
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
                        mp.seekTo(position.toInt())
                        scrubFrameAsync(position)
                    }
                }
            }

            override fun onStartTrackingTouch(bar: SeekBar?) {
                isSeeking = true
                wasPlayingBeforeSeek = mediaPlayer?.isPlaying == true
                mediaPlayer?.pause()
                mainHandler.removeCallbacks(hideControlsRunnable)
            }

            override fun onStopTrackingTouch(bar: SeekBar?) {
                isSeeking = false
                previewFrame.visibility = View.GONE
                if (wasPlayingBeforeSeek) {
                    mediaPlayer?.start()
                    mediaPlayer?.setPlaybackParams(mediaPlayer!!.playbackParams.setSpeed(speeds[currentSpeedIndex]))
                }
                updatePlayPauseIcon()
                resetHideTimer()
            }
        })
    }

    private fun handleSwipeGesture(event: MotionEvent) {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                swipeStartY = event.y
                isVerticalSwipe = false
                isSwipingBrightness = false
                isSwipingVolume = false
                val lp = window.attributes
                swipeStartBrightness = if (lp.screenBrightness < 0) 0.5f else lp.screenBrightness
                swipeStartVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
            }
            MotionEvent.ACTION_MOVE -> {
                if (scaleGestureDetector.isInProgress || event.pointerCount > 1) return

                val dy = swipeStartY - event.y
                if (!isVerticalSwipe && abs(dy) > 30) {
                    isVerticalSwipe = true
                    isSwipingBrightness = event.x < screenWidth / 2f
                    isSwipingVolume = event.x >= screenWidth / 2f
                }

                if (isVerticalSwipe) {
                    val fraction = dy / (screenHeight * 0.6f)
                    if (isSwipingBrightness) {
                        val newBrightness = (swipeStartBrightness + fraction).coerceIn(0.01f, 1f)
                        val lp = window.attributes
                        lp.screenBrightness = newBrightness
                        window.attributes = lp
                        showGestureInfo("☀ ${(newBrightness * 100).toInt()}%")
                    } else if (isSwipingVolume) {
                        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
                        val newVol = (swipeStartVolume + fraction * maxVol).toInt().coerceIn(0, maxVol)
                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                        showGestureInfo("🔊 ${(newVol * 100) / maxVol}%")
                    }
                }

                if (videoScale > 1.1f) {
                    surfaceView.translationX += event.x - swipeStartY
                    surfaceView.translationY += event.y - swipeStartY
                }
            }
        }
    }

    private fun resetSwipeState() {
        isVerticalSwipe = false
        isSwipingBrightness = false
        isSwipingVolume = false
    }

    private fun animateButton(view: View) {
        view.animate().scaleX(0.8f).scaleY(0.8f).setDuration(80)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(120)
                    .setInterpolator(OvershootInterpolator(3f)).start()
            }.start()
    }

    private fun scrubFrameAsync(positionMs: Long) {
        val now = System.currentTimeMillis()
        if (now - lastScrubTime < 50) return
        lastScrubTime = now

        scrubHandler?.post {
            try {
                retriever?.let { r ->
                    val bitmap: Bitmap? = r.getFrameAtTime(
                        positionMs * 1000,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    bitmap?.let { bmp ->
                        mainHandler.post {
                            previewFrame.setImageBitmap(bmp)
                            previewFrame.visibility = View.VISIBLE
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun showGestureInfo(text: String) {
        gestureInfoView.text = text
        gestureInfoView.alpha = 1f
        gestureInfoView.visibility = View.VISIBLE
        gestureInfoView.scaleX = 0.85f
        gestureInfoView.scaleY = 0.85f
        gestureInfoView.animate().scaleX(1f).scaleY(1f).setDuration(120)
            .setInterpolator(OvershootInterpolator(2f)).start()
        mainHandler.removeCallbacksAndMessages("gesture_hide")
        mainHandler.postDelayed({ hideGestureInfo() }, 900)
    }

    private fun hideGestureInfo() {
        gestureInfoView.animate().alpha(0f).setDuration(180).withEndAction {
            gestureInfoView.visibility = View.GONE
        }.start()
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
            try { retriever?.setDataSource(this, Uri.parse(uri)) } catch (_: Exception) {}

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@VideoPlayerActivity, Uri.parse(uri))
                setDisplay(holder)

                setOnPreparedListener { mp ->
                    isPrepared = true
                    bufferingIndicator.visibility = View.GONE
                    tvDuration.text = FormatUtils.formatDuration(mp.duration.toLong())

                    if (!orientationSet && AppPreferences(this@VideoPlayerActivity).autoRotateVideo) {
                        val vw = mp.videoWidth
                        val vh = mp.videoHeight
                        if (vw > 0 && vh > 0) {
                            requestedOrientation = if (vw > vh)
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            else
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
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
                    mainHandler.post(updateProgressRunnable)
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
            val vw = mp.videoWidth
            val vh = mp.videoHeight
            if (vw == 0 || vh == 0) return

            val sw = resources.displayMetrics.widthPixels.toFloat()
            val sh = resources.displayMetrics.heightPixels.toFloat()
            val params = surfaceView.layoutParams as FrameLayout.LayoutParams

            when (currentAspect) {
                0 -> { val r = minOf(sw / vw, sh / vh); params.width = (vw * r).toInt(); params.height = (vh * r).toInt() }
                1 -> { params.width = sw.toInt(); params.height = sh.toInt() }
                2 -> { val r = maxOf(sw / vw, sh / vh); params.width = (vw * r).toInt(); params.height = (vh * r).toInt() }
                3 -> { params.width = sw.toInt(); params.height = (sw * 9 / 16).toInt() }
                4 -> { params.width = sw.toInt(); params.height = (sw * 3 / 4).toInt() }
            }
            params.gravity = Gravity.CENTER
            surfaceView.layoutParams = params
        }
    }

    private fun releasePlayer() {
        mainHandler.removeCallbacks(updateProgressRunnable)
        mainHandler.removeCallbacks(hideControlsRunnable)
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

    override fun onDestroy() {
        super.onDestroy()
        scrubThread?.quitSafely()
        scrubThread = null
        scrubHandler = null
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
        val playing = mediaPlayer?.isPlaying == true
        ivPlayPause.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(ivPlayPause, "scaleX", 0.7f, 1f),
                ObjectAnimator.ofFloat(ivPlayPause, "scaleY", 0.7f, 1f)
            )
            duration = 180
            interpolator = OvershootInterpolator(2f)
            start()
        }
    }

    private fun updateProgress() {
        if (isSeeking) return
        mediaPlayer?.let { mp ->
            if (isPrepared && mp.duration > 0) {
                seekBar.progress = ((mp.currentPosition.toLong() * 1000) / mp.duration).toInt()
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
            fadeView(topControls, true)
            fadeView(centerControls, true)
            fadeView(bottomControls, true)
        }
        btnLock.visibility = View.VISIBLE
        resetHideTimer()
    }

    private fun hideControls() {
        controlsVisible = false
        fadeView(topControls, false)
        fadeView(centerControls, false)
        fadeView(bottomControls, false)
        if (!controlsLocked) btnLock.visibility = View.GONE
    }

    private fun fadeView(view: View, show: Boolean) {
        view.animate()
            .alpha(if (show) 1f else 0f)
            .setDuration(180)
            .setInterpolator(DecelerateInterpolator())
            .withStartAction { if (show) view.visibility = View.VISIBLE }
            .withEndAction { if (!show) view.visibility = View.GONE }
            .start()
    }

    private fun resetHideTimer() {
        mainHandler.removeCallbacks(hideControlsRunnable)
        mainHandler.postDelayed(hideControlsRunnable, 4000)
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
