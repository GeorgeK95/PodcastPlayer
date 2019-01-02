package com.georgek.sgfs.podcastplayer.model.view

import android.app.Application
import android.app.SharedElementCallback
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import com.georgek.sgfs.podcastplayer.model.data.Episode
import com.georgek.sgfs.podcastplayer.model.data.Podcast
import com.georgek.sgfs.podcastplayer.repository.PodcastRepository
import com.georgek.sgfs.podcastplayer.util.DateUtils
import java.util.*

class PodcastViewModel(application: Application) : AndroidViewModel(application) {

    private var activePodcast: Podcast? = null

    var podcastRepository: PodcastRepository? = null
    var recentlyLoaded: PodcastViewData? = null
    var livePodcastData: LiveData<List<SearchViewModel.PodcastSummaryViewData>>? = null

    var activeEpisodeViewData: EpisodeViewData? = null

    fun setActivePodcast(feedUrl: String, callback: (SearchViewModel.PodcastSummaryViewData?) -> Unit) {
        val repo = podcastRepository ?: return
        repo.getPodcast(feedUrl) { podcast ->
            if (podcast == null) {
                callback(null)
            } else {
                recentlyLoaded = podcastToPodcastViewData(podcast)
                activePodcast = podcast
                callback(podcastToSummaryView(podcast))
            }
        }
    }

    fun getPodcasts(): LiveData<List<SearchViewModel.PodcastSummaryViewData>>? {
        val repo = podcastRepository ?: return null

        if (livePodcastData == null) {
            val liveData = repo.getAll()
            livePodcastData = Transformations.map(liveData) { podcastList ->
                podcastList.map { podcast ->
                    podcastToSummaryView(podcast)
                }
            }
        }

        return livePodcastData
    }

    fun getPodcast(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData, callback: (PodcastViewData?) -> Unit) {
        val repo = podcastRepository ?: return
        val feedUrl = podcastSummaryViewData.feedUrl ?: return

        repo.getPodcast(feedUrl) {
            it?.let {
                it.feedTitle = podcastSummaryViewData.name ?: ""
                it.imageUrl = podcastSummaryViewData.imageUrl ?: ""
                recentlyLoaded = podcastToPodcastViewData(it)
                activePodcast = it
                callback(recentlyLoaded)
            }
        }
    }

    fun saveActivePodcast() {
        val repo = podcastRepository ?: return
        activePodcast?.let {
            it.episodes = it.episodes.drop(1)
            repo.save(it)
        }
    }

    fun deleteActivePodcast() {
        val repo = podcastRepository ?: return
        activePodcast?.let {
            repo.delete(it)
        }
    }

    private fun podcastToSummaryView(podcast: Podcast): SearchViewModel.PodcastSummaryViewData {
        return SearchViewModel.PodcastSummaryViewData(
                podcast.feedTitle,
                DateUtils.dateToShortDate(podcast.lastUpdated),
                podcast.imageUrl,
                podcast.feedUrl)
    }

    private fun episodesToEpisodesViewData(episodes: List<Episode>): List<EpisodeViewData> {
        return episodes.map {
            val isVideo = it.mimeType.startsWith(VIDEO)
            EpisodeViewData(it.guid, it.title, it.description, it.mediaUrl, it.releaseDate, it.duration, isVideo)
        }
    }

    private fun podcastToPodcastViewData(podcast: Podcast): PodcastViewData {
        return PodcastViewData(
                podcast.id != null,
                podcast.feedTitle,
                podcast.feedUrl,
                podcast.feedDesc,
                podcast.imageUrl,
                episodesToEpisodesViewData(podcast.episodes)
        )
    }

    data class PodcastViewData(
            var subscribed: Boolean = false,
            var feedTitle: String? = "",
            var feedUrl: String? = "",
            var feedDesc: String? = "",
            var imageUrl: String? = "",
            var episodes: List<EpisodeViewData>
    )

    data class EpisodeViewData(
            var guid: String? = "",
            var title: String? = "",
            var description: String? = "",
            var mediaUrl: String? = "",
            var releaseDate: Date? = null,
            var duration: String? = "",
            var isVideo: Boolean = false
    )

    companion object {
        const val VIDEO ="video"
    }
}