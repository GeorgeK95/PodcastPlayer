package com.georgek.sgfs.podcastplayer.service

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

class PodcastPlayerMediaCallback(val context: Context,
                                 val mediaSession: MediaSessionCompat,
                                 var mediaPlayer: MediaPlayer? = null) :
        MediaSessionCompat.Callback() {

    private var mediaUri: Uri? = null
    private var newMedia: Boolean = false
    private var mediaExtras: Bundle? = null
    private var mediaNeedsPrepare: Boolean = false

    var listener: PodplayMediaListener? = null

    interface PodplayMediaListener {
        fun onStateChanged()
        fun onStopPlaying()
        fun onPausePlaying()
    }

    override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
        super.onPlayFromUri(uri, extras)
        println("Playing ${uri.toString()}")

        if (mediaUri == uri) {
            newMedia = false
            mediaExtras = null
        } else {
            mediaExtras = extras
            setNewMedia(uri)
        }

        onPlay()
    }

    override fun onPlay() {
        super.onPlay()

        if (ensureAudioFocus()) {
            mediaSession.isActive = true
            initializeMediaPlayer()
            prepareMedia()
            startPlaying()
        }
        println("onPlay called")

        initializeMediaPlayer()
        prepareMedia()
        startPlaying()
    }

    override fun onStop() {
        super.onStop()
        println("onStop called")
        stopPlaying()
    }

    override fun onPause() {
        super.onPause()
        println("onPause called")
        pausePlaying()
    }

    override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
        super.onCommand(command, extras, cb)

        when (command) {
            CMD_CHANGE_SPEED -> extras?.let { changeSpeed(it) }
        }
    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)

        mediaPlayer?.seekTo(pos.toInt())

        val playbackState: PlaybackStateCompat? = mediaSession.controller.playbackState

        if (playbackState != null) {
            setState(playbackState.state)
        } else {
            setState(PlaybackStateCompat.STATE_PAUSED)
        }
    }

    private fun changeSpeed(extras: Bundle) {
        var playbackState = PlaybackStateCompat.STATE_PAUSED

        if (mediaSession.controller.playbackState != null) {
            playbackState = mediaSession.controller.playbackState.state
        }

        setState(playbackState, extras.getFloat(CMD_EXTRA_SPEED))
    }

    private fun startPlaying() {
        mediaPlayer?.let { mediaPlayer ->
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start()
                setState(PlaybackStateCompat.STATE_PLAYING)
            }
        }
    }

    private fun pausePlaying() {
        removeAudioFocus()
        mediaPlayer?.let { mediaPlayer ->
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                setState(PlaybackStateCompat.STATE_PAUSED)
            }
        }

        listener?.onPausePlaying()
    }

    private fun stopPlaying() {
        removeAudioFocus()
        mediaSession.isActive = false
        mediaPlayer?.let { mediaPlayer ->
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
                setState(PlaybackStateCompat.STATE_STOPPED)
            }
        }

        listener?.onStopPlaying()
    }

    private fun prepareMedia() {
        if (newMedia) {
            newMedia = false
            mediaPlayer?.let { mediaPlayer ->
                mediaUri?.let {
                    if (mediaNeedsPrepare) {
                        mediaPlayer.reset()
                        mediaPlayer.setDataSource(context, mediaUri)
                        mediaPlayer.prepare()
                    }
                    mediaExtras?.let { mediaExtras ->
                        mediaSession.setMetadata(MediaMetadataCompat.Builder()
                                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                                        mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                                        mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                                        mediaExtras.getString(
                                                MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))
                                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer.duration.toLong())
                                .build())
                    }
                }
            }
        }
    }

    private fun initializeMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            mediaPlayer!!.setOnCompletionListener {
                setState(PlaybackStateCompat.STATE_PAUSED)
            }
            mediaNeedsPrepare = true
        }
    }

    private fun removeAudioFocus() {
        val audioManager = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocus(null)
    }

    private fun setNewMedia(uri: Uri?) {
        newMedia = true
        mediaUri = uri
    }

    private fun ensureAudioFocus(): Boolean {
        val audioManager = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = audioManager.requestAudioFocus(null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun setState(state: Int, newSpeed: Float? = null) {
        var position: Long = -1

        mediaPlayer?.let {
            position = it.getCurrentPosition().toLong()
        }

        var speed = 1.0f

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (newSpeed == null) {
                speed = mediaPlayer?.playbackParams?.speed ?: 1.0f
            } else {
                speed = newSpeed
            }
            mediaPlayer?.let {
                try {
                    it.playbackParams = it.playbackParams.setSpeed(speed)
                } catch (e: Exception) {
                    mediaPlayer?.reset()
                    mediaPlayer?.setDataSource(context, mediaUri)
                    mediaPlayer?.prepare()

                    it.playbackParams = it.playbackParams.setSpeed(speed)

                    mediaPlayer?.seekTo(position.toInt())

                    if (state == PlaybackStateCompat.STATE_PLAYING) {
                        mediaPlayer?.start()
                    }
                }
            }
        }

        val playbackState = PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                                PlaybackStateCompat.ACTION_STOP or
                                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                                PlaybackStateCompat.ACTION_PAUSE
                )
                .setState(state, position, speed)
                .build()

        mediaSession.setPlaybackState(playbackState)

        if (state == PlaybackStateCompat.STATE_PAUSED || state == PlaybackStateCompat.STATE_PLAYING) {
            listener?.onStateChanged()
        }
    }

    companion object {
        const val CMD_CHANGE_SPEED = "change_speed"
        const val CMD_EXTRA_SPEED = "speed"
    }
}