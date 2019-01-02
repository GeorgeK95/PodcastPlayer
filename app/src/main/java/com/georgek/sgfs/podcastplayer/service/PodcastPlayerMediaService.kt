package com.georgek.sgfs.podcastplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.os.Build
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.ContextCompat.getSystemService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.georgek.sgfs.podcastplayer.R
import com.georgek.sgfs.podcastplayer.model.view.PodcastViewModel
import com.georgek.sgfs.podcastplayer.ui.PodcastActivity

class PodcastPlayerMediaService : MediaBrowserServiceCompat(), PodcastPlayerMediaCallback.PodplayMediaListener {

    private lateinit var mediaSession: MediaSessionCompat

    interface EpisodeListAdapterListener {
        fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData)
    }

    override fun onCreate() {
        super.onCreate()
        createMediaSession()
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        if (parentId.equals(PODCAST_PLAYER_EMPTY_ROOT_MEDIA_ID)) {
            result.sendResult(null)
        }
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        return MediaBrowserServiceCompat.BrowserRoot(PODCAST_PLAYER_EMPTY_ROOT_MEDIA_ID, null)
    }

    override fun onStateChanged() {
        displayNotification()
    }

    override fun onStopPlaying() {
        stopSelf()
        stopForeground(true)
    }

    override fun onPausePlaying() {
        stopSelf()
        stopForeground(false)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        mediaSession.controller.transportControls.stop()
    }

    private fun displayNotification() {
        if (mediaSession.controller.metadata == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val mediaDescription = mediaSession.controller.metadata.description

        Glide.with(this)
                .asBitmap()
                .load(mediaDescription.iconUri)
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap?, transition: Transition<in Bitmap>?) {
                        val notification = createNotification(mediaDescription, resource)

                        ContextCompat.startForegroundService(this@PodcastPlayerMediaService,
                                Intent(this@PodcastPlayerMediaService, PodcastPlayerMediaService::class.java))

                        startForeground(PODCAST_PLAYER_NOTIFICATION_ID, notification)
                    }
                })
    }


    private fun createNotification(mediaDescription: MediaDescriptionCompat, bitmap: Bitmap?): Notification {
        val notificationIntent = getNotificationIntent()
        val (pauseAction, playAction) = getPausePlayActions()

        val notification = NotificationCompat.Builder(this, PodcastPlayerMediaService.PODCAST_PLAYER_CHANNEL_ID)

        val actionStopIntent: PendingIntent = MediaButtonReceiver
                .buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP)

        notification
                .setContentTitle(mediaDescription.title)
                .setContentText(mediaDescription.subtitle)
                .setLargeIcon(bitmap)
                .setContentIntent(notificationIntent)
                .setDeleteIntent(actionStopIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_episode_icon)
                .addAction(if (isPlaying()) pauseAction else playAction)
                .setStyle(
                        android.support.v4.media.app.NotificationCompat.MediaStyle()
                                .setMediaSession(mediaSession.sessionToken)
                                .setShowActionsInCompactView(0)
                                .setShowCancelButton(true)
                                .setCancelButtonIntent(actionStopIntent)
                )

        return notification.build()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(PodcastPlayerMediaService.PODCAST_PLAYER_CHANNEL_ID) == null) {
            val channel = NotificationChannel(PodcastPlayerMediaService.PODCAST_PLAYER_CHANNEL_ID, "Player", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getNotificationIntent(): PendingIntent {
        val openActivityIntent = Intent(this, PodcastActivity::class.java)
        openActivityIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(this, 0, openActivityIntent,
                PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun isPlaying(): Boolean {
        if (mediaSession.controller.playbackState != null) {
            return mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING
        }

        return false
    }

    private fun getPausePlayActions(): Pair<NotificationCompat.Action, NotificationCompat.Action> {
        val pauseAction = NotificationCompat.Action(
                R.drawable.ic_pause_white, getString(R.string.pause),
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_PAUSE))
        val playAction = NotificationCompat.Action(
                R.drawable.ic_play_arrow_white, getString(R.string.play),
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_PLAY))
        return Pair(pauseAction, playAction)
    }

    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(this, "PodcastPlayerMediaService")
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        setSessionToken(mediaSession.sessionToken)

        val callBack = PodcastPlayerMediaCallback(this, mediaSession)
        callBack.listener = this
        mediaSession.setCallback(callBack)
    }

    companion object {
        private const val PODCAST_PLAYER_EMPTY_ROOT_MEDIA_ID = "podcast_player_empty_root_media_id"
        const val PODCAST_PLAYER_CHANNEL_ID = "podcast_player_channel"
        const val PODCAST_PLAYER_NOTIFICATION_ID = 1
    }
}