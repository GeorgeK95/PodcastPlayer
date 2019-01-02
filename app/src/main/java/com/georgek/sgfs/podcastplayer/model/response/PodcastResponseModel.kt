package com.georgek.sgfs.podcastplayer.model.response

data class PodcastResponseModel(
        val resultCount: Int,
        val results: List<ITunesPodcastModel>
) {
    data class ITunesPodcastModel(
            val collectionCensoredName: String,
            val feedUrl: String,
            val artworkUrl100: String,
            val releaseDate: String
    )
}