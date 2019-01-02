package com.georgek.sgfs.podcastplayer.model.response

import java.util.*

data class RssFeedResponse(
        var title: String = "",
        var description: String = "",
        var summary: String = "",
        var lastUpdated: Date = Date(),
        var episodeModels: MutableList<EpisodeResponseModel>? = null
) {
    data class EpisodeResponseModel(
            var title: String? = null,
            var link: String? = null,
            var description: String? = null,
            var guid: String? = null,
            var pubDate: String? = null,
            var duration: String? = null,
            var url: String? = null,
            var type: String? = null
    )
}