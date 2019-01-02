package com.georgek.sgfs.podcastplayer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.georgek.sgfs.podcastplayer.R
import com.georgek.sgfs.podcastplayer.db.PodcastPlayerDatabase
import com.georgek.sgfs.podcastplayer.repository.PodcastRepository
import com.georgek.sgfs.podcastplayer.ui.PodcastActivity
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

class EpisodeUpdateService : JobService() {
    companion object {
        val EPISODE_CHANNEL_ID = "podcastplayer_episodes_channel"
        val EXTRA_FEED_URL = "PodcastPlayerFeedUrl"
    }

    private fun displayNotification(podcastInfo: PodcastRepository.PodcastUpdateInfo) {
        val contentIntent = Intent(this, PodcastActivity::class.java)
        contentIntent.putExtra(EXTRA_FEED_URL, podcastInfo.feedUrl)
        val pendingContentIntent = PendingIntent.getActivity(this, 0,
                contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, EPISODE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_episode_icon)
                .setContentTitle(getString(R.string.episode_notification_title))
                .setContentText(getString(R.string.episode_notification_text, podcastInfo.newCount, podcastInfo.name))
                .setNumber(podcastInfo.newCount)
                .setAutoCancel(true)
                .setContentIntent(pendingContentIntent)
                .build()

        val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(podcastInfo.name, 0, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.getNotificationChannel(EPISODE_CHANNEL_ID) == null) {

            val channel = NotificationChannel(EPISODE_CHANNEL_ID, "Episodes",
                    NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        val db = PodcastPlayerDatabase.getInstance(this)
        val repo = PodcastRepository(FeedService.instance, db.podcastDao())

        launch(CommonPool) {
            repo.updatePodcastEpisodes { podcastUpdates ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel()
                }
                for (podcastUpdate in podcastUpdates) {
                    displayNotification(podcastUpdate)
                }

                jobFinished(jobParameters, false)
            }
        }
        return true
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        return true
    }
}