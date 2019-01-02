package com.georgek.sgfs.podcastplayer.service

import com.georgek.sgfs.podcastplayer.model.response.PodcastResponseModel
import com.georgek.sgfs.podcastplayer.util.Constants.Companion.I_TUNES_BASE_URL
import com.georgek.sgfs.podcastplayer.util.Constants.Companion.SEARCH_MEDIA_PODCAST_URL
import com.georgek.sgfs.podcastplayer.util.Constants.Companion.TERM_STR
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesService {
    @GET(SEARCH_MEDIA_PODCAST_URL)
    fun searchPodcastByTerm(@Query(TERM_STR) term: String): Call<PodcastResponseModel>

    companion object {
        val instance: ItunesService by lazy {
            val retrofit = Retrofit.Builder()
                    .baseUrl(I_TUNES_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            retrofit.create<ItunesService>(ItunesService::class.java)
        }
    }
}