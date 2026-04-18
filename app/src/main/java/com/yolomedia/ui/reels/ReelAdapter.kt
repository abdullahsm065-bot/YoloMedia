package com.yolomedia.ui.reels

import android.media.MediaPlayer
import android.net.Uri
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yolomedia.R
import com.yolomedia.data.model.VideoItem
import com.yolomedia.utils.FormatUtils

class ReelAdapter(
    private val videos: List<VideoItem>
) : RecyclerView.Adapter<ReelAdapter.ReelViewHolder>() {

    private var currentPlayingHolder: ReelViewHolder? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reel, parent, false)
        return ReelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReelViewHolder, position: Int) {
        holder.bind(videos[position])
    }

    override fun getItemCount(): Int = videos.size

    fun pauseCurrent() {
        currentPlayingHolder?.pause()
    }

    fun playAt(position: Int, holder: ReelViewHolder?) {
        currentPlayingHolder?.release()
        currentPlayingHolder = holder
    }

    override fun onViewRecycled(holder: ReelViewHolder) {
        super.onViewRecycled(holder)
        holder.release()
        if (currentPlayingHolder == holder) currentPlayingHolder = null
    }

    fun releaseAll() {
        currentPlayingHolder?.release()
        currentPlayingHolder = null
    }

    inner class ReelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val surface: SurfaceView = itemView.findViewById(R.id.reel_surface)
        private val loading: ProgressBar = itemView.findViewById(R.id.reel_loading)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_reel_title)
        private val tvInfo: TextView = itemView.findViewById(R.id.tv_reel_info)
        private val seekBar: SeekBar = itemView.findViewById(R.id.reel_seek)
        private val playIndicator: ImageView = itemView.findViewById(R.id.reel_play_indicator)

        private var mediaPlayer: MediaPlayer? = null
        private var isPrepared = false
        private var videoItem: VideoItem? = null
        private val handler = android.os.Handler(android.os.Looper.getMainLooper())
        private val progressUpdater = object : Runnable {
            override fun run() {
                mediaPlayer?.let { mp ->
                    if (isPrepared && mp.isPlaying && mp.duration > 0) {
                        seekBar.progress = ((mp.currentPosition.toLong() * 1000) / mp.duration).toInt()
                    }
                }
                handler.postDelayed(this, 300)
            }
        }

        fun bind(video: VideoItem) {
            videoItem = video
            tvTitle.text = video.name
            tvInfo.text = "${FormatUtils.formatDuration(video.duration)} • ${FormatUtils.formatFileSize(video.size)}"
            seekBar.progress = 0

            surface.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    initPlayer(holder, video)
                }
                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    release()
                }
            })

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser && isPrepared) {
                        mediaPlayer?.let { mp ->
                            mp.seekTo(((progress.toLong() * mp.duration) / 1000).toInt())
                        }
                    }
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })

            itemView.setOnClickListener {
                mediaPlayer?.let { mp ->
                    if (isPrepared) {
                        if (mp.isPlaying) {
                            mp.pause()
                            showPlayIndicator(false)
                        } else {
                            mp.start()
                            showPlayIndicator(true)
                        }
                    }
                }
            }
        }

        private fun initPlayer(holder: SurfaceHolder, video: VideoItem) {
            release()
            loading.visibility = View.VISIBLE

            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(itemView.context, Uri.parse(video.uri))
                    setDisplay(holder)
                    isLooping = true

                    setOnPreparedListener { mp ->
                        isPrepared = true
                        loading.visibility = View.GONE
                        mp.start()
                        handler.post(progressUpdater)
                        currentPlayingHolder = this@ReelViewHolder
                    }

                    setOnErrorListener { _, _, _ ->
                        loading.visibility = View.GONE
                        true
                    }

                    prepareAsync()
                }
            } catch (_: Exception) {
                loading.visibility = View.GONE
            }
        }

        private fun showPlayIndicator(isPlaying: Boolean) {
            playIndicator.setImageResource(if (isPlaying) R.drawable.ic_play else R.drawable.ic_pause)
            playIndicator.alpha = 0.8f
            playIndicator.scaleX = 0.5f
            playIndicator.scaleY = 0.5f
            playIndicator.animate()
                .scaleX(1f).scaleY(1f).alpha(0f)
                .setDuration(500)
                .start()
        }

        fun pause() {
            mediaPlayer?.let { if (isPrepared && it.isPlaying) it.pause() }
        }

        fun release() {
            handler.removeCallbacks(progressUpdater)
            mediaPlayer?.let {
                if (isPrepared) {
                    try { it.stop() } catch (_: Exception) {}
                }
                it.release()
            }
            mediaPlayer = null
            isPrepared = false
        }
    }
}
