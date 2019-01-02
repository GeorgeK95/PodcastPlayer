package com.georgek.sgfs.podcastplayer.repository

import com.georgek.sgfs.podcastplayer.model.response.PodcastResponseModel
import com.georgek.sgfs.podcastplayer.service.ItunesService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ItunesRepository(private val itunesService: ItunesService) {
    fun searchByTerm(term: String, callBack: (List<PodcastResponseModel.ITunesPodcastModel>?) -> Unit) {
        val podcastCall = itunesService.searchPodcastByTerm(term)

        podcastCall.enqueue(object : Callback<PodcastResponseModel> {
            override fun onFailure(call: Call<PodcastResponseModel>?, t: Throwable?) {
                callBack(null)
            }

            override fun onResponse(call: Call<PodcastResponseModel>?, response: Response<PodcastResponseModel>?) {
                val body = response?.body()
                callBack(body?.results)
            }
        })
    }
}