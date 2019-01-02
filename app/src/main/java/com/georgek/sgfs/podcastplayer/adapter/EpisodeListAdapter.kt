package com.georgek.sgfs.podcastplayer.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.georgek.sgfs.podcastplayer.R
import com.georgek.sgfs.podcastplayer.model.view.PodcastViewModel
import com.georgek.sgfs.podcastplayer.service.PodcastPlayerMediaService
import com.georgek.sgfs.podcastplayer.util.DateUtils
import com.georgek.sgfs.podcastplayer.util.HtmlUtils

class EpisodeListAdapter(
        private var episodeViewList: List<PodcastViewModel.EpisodeViewData>?,
        private val episodeListAdapterListener: PodcastPlayerMediaService.EpisodeListAdapterListener) :
        RecyclerView.Adapter<EpisodeListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeListAdapter.ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.episode_item, parent, false),
                episodeListAdapterListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val episodeViewList = episodeViewList ?: return
        val episodeView = episodeViewList[position]
        setHolderData(holder, episodeView)
    }

    override fun getItemCount(): Int {
        return episodeViewList?.size ?: 0
    }

    fun setViewData(episodeList: List<PodcastViewModel.EpisodeViewData>) {
        episodeViewList = episodeList
        this.notifyDataSetChanged()
    }

    private fun setHolderData(holder: ViewHolder, episodeView: PodcastViewModel.EpisodeViewData) {
        holder.episodeViewData = episodeView
        holder.titleTextView.text = episodeView.title
        holder.descTextView.text = HtmlUtils.htmlToSpannable(episodeView.description ?: "")
        holder.durationTextView.text = episodeView.duration
        holder.releaseDateTextView.text = episodeView.releaseDate?.let {
            DateUtils.dateToShortDate(it)
        }
    }

    class ViewHolder(
            v: View,
            private val episodeListAdapterListener: PodcastPlayerMediaService.EpisodeListAdapterListener
    ) : RecyclerView.ViewHolder(v) {
        var episodeViewData: PodcastViewModel.EpisodeViewData? = null
        val titleTextView: TextView = v.findViewById(R.id.titleView)
        val descTextView: TextView = v.findViewById(R.id.descView)
        val durationTextView: TextView = v.findViewById(R.id.durationView)
        val releaseDateTextView: TextView = v.findViewById(R.id.releaseDateView)

        init {
            v.setOnClickListener {
                episodeViewData?.let {
                    episodeListAdapterListener.onSelectedEpisode(it)
                }
            }
        }
    }
}
