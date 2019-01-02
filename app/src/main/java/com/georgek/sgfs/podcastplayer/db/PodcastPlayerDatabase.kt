package com.georgek.sgfs.podcastplayer.db

import android.arch.persistence.room.*
import android.content.Context
import com.georgek.sgfs.podcastplayer.model.data.Episode
import com.georgek.sgfs.podcastplayer.model.data.Podcast
import java.util.*

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return (date?.time)
    }
}

@Database(entities = [Podcast::class, Episode::class], version = 1)
@TypeConverters(Converters::class)
abstract class PodcastPlayerDatabase : RoomDatabase() {

    abstract fun podcastDao(): PodcastDao

    companion object {
        private const val APP_TITLE = "PodcastPlayer"

        private var instance: PodcastPlayerDatabase? = null

        fun getInstance(context: Context): PodcastPlayerDatabase {
            if (instance == null) {
                instance = Room.databaseBuilder(context.applicationContext,
                        PodcastPlayerDatabase::class.java, APP_TITLE).build()
            }

            return instance as PodcastPlayerDatabase
        }
    }
}