package com.ankit.jarvisfacescanner.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.TextureView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import java.util.concurrent.atomic.AtomicBoolean

@UnstableApi
class RtspStreamManager(
    private val context: Context,
    private val textureView: TextureView,
    private val onFrameSampled: (Bitmap) -> Unit,
    private val onStatusChanged: (String) -> Unit
) {
    private var exoPlayer: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val isRunning = AtomicBoolean(false)
    private var sampleIntervalMs = 250L

    private val frameSamplerRunnable = object : Runnable {
        override fun run() {
            if (isRunning.get() && textureView.isAvailable) {
                val bitmap = textureView.getBitmap(480, 270)
                if (bitmap != null) {
                    onFrameSampled(bitmap)
                }
            }
            if (isRunning.get()) {
                handler.postDelayed(this, sampleIntervalMs)
            }
        }
    }

    fun startStream(rtspUri: String) {
        stopStream()
        onStatusChanged("CONNECTING TO TAPO...")
        Log.i("RtspStreamManager", "Initiating RTSP connection to: $rtspUri")

        if (textureView.isAvailable) {
            initializePlayer(rtspUri)
        } else {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    initializePlayer(rtspUri)
                }
                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            }
        }
    }

    private fun initializePlayer(rtspUri: String) {
        try {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                setVideoTextureView(textureView)
                val mediaItem = MediaItem.fromUri(rtspUri)

                // Standard RTSP source with TCP transport for reliable local Wi-Fi transmission
                val rtspMediaSource = RtspMediaSource.Factory()
                    .setForceUseRtpTcp(true)
                    .setTimeoutMs(10000)
                    .createMediaSource(mediaItem)

                setMediaSource(rtspMediaSource)
                prepare()
                playWhenReady = true

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> onStatusChanged("TAPO BUFFERING...")
                            Player.STATE_READY -> {
                                onStatusChanged("TAPO C200 LIVE")
                                if (!isRunning.get()) {
                                    isRunning.set(true)
                                    handler.post(frameSamplerRunnable)
                                }
                            }
                            Player.STATE_ENDED -> onStatusChanged("TAPO STREAM ENDED")
                            Player.STATE_IDLE -> {
                                // Do not overwrite "CONNECTING..." immediately on initialization
                                Log.d("RtspStreamManager", "Player entered STATE_IDLE")
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        val errorDetail = when {
                            error.message?.contains("401", ignoreCase = true) == true -> "AUTH FAILED // CHECK PASSWORD"
                            error.message?.contains("refused", ignoreCase = true) == true -> "CONNECTION REFUSED // CHECK IP"
                            error.message?.contains("timeout", ignoreCase = true) == true -> "TIMEOUT // CHECK WI-FI"
                            else -> "RTSP ERR: ${error.errorCodeName}"
                        }
                        onStatusChanged(errorDetail)
                        Log.e("RtspStreamManager", "ExoPlayer RTSP Error: ${error.message}", error)

                        // Attempt auto-reconnect after 4 seconds
                        handler.postDelayed({
                            if (isRunning.get()) startStream(rtspUri)
                        }, 4000)
                    }
                })
            }
        } catch (e: Exception) {
            onStatusChanged("RTSP INIT ERROR: ${e.message}")
            Log.e("RtspStreamManager", "Failed to initialize ExoPlayer", e)
        }
    }

    fun stopStream() {
        isRunning.set(false)
        handler.removeCallbacks(frameSamplerRunnable)
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
    }
}
