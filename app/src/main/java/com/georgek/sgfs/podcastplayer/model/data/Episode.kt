package com.georgek.sgfs.podcastplayer.model.data

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import java.util.*

@Entity(
        foreignKeys = [
            ForeignKey(
                    entity = Podcast::class,
                    parentColumns = ["id"],
                    childColumns = ["podcastId"],
                    onDelete = ForeignKey.CASCADE
            )
        ],
        indices = [Index("podcastId")]
)
data class Episode(
        @PrimaryKey var guid: String = "",
        var podcastId: Long? = null,
        var title: String = "",
        var description: String = "",
        var mediaUrl: String = "",
        var mimeType: String = "",
        var releaseDate: Date = Date(),
        var duration: String = ""
)