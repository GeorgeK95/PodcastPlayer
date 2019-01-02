package com.georgek.sgfs.podcastplayer.model.data

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import java.util.*

@Entity
data class Podcast(
        @PrimaryKey(autoGenerate = true) var id: Long? = null,
        var feedUrl: String = "",
        var feedTitle: String = "",
        var feedDesc: String = "",
        var imageUrl: String = "",
        var lastUpdated: Date = Date(),
        @Ignore var episodes: List<Episode> = listOf()
)