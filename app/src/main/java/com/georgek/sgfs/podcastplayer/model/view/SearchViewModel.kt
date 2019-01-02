package com.georgek.sgfs.podcastplayer.model.view

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import com.georgek.sgfs.podcastplayer.model.response.PodcastResponseModel
import com.georgek.sgfs.podcastplayer.repository.ItunesRepository
import com.georgek.sgfs.podcastplayer.util.DateUtils.DateFormatter.jsonDateToShortDate

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    var repo: ItunesRepository? = null

    fun search(query: String, callback: (List<PodcastSummaryViewData>) -> Unit) {
        repo!!.searchByTerm(query) { results ->
            if (results == null) {
                callback(emptyList())
            } else {
                val searchViews = results.map { currentItem ->
                    itunesPodcastToPodcastSummaryViewData(currentItem)
                }
                callback(searchViews)
            }
        }
    }

    private fun itunesPodcastToPodcastSummaryViewData(itunesPodcast: PodcastResponseModel.ITunesPodcastModel):
            PodcastSummaryViewData {
        return PodcastSummaryViewData(
                itunesPodcast.collectionCensoredName,
                jsonDateToShortDate(itunesPodcast.releaseDate),
                itunesPodcast.artworkUrl100,
                itunesPodcast.feedUrl)
    }

    data class PodcastSummaryViewData(
            var name: String? = "",
            var lastUpdated: String? = "",
            var imageUrl: String? = "",
            var feedUrl: String? = ""
    )
}