package com.georgek.sgfs.podcastplayer.ui

import android.arch.lifecycle.ViewModelProviders
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.widget.LinearLayoutManager
import android.text.method.ScrollingMovementMethod
import android.view.*
import com.bumptech.glide.Glide
import com.georgek.sgfs.podcastplayer.R
import com.georgek.sgfs.podcastplayer.adapter.EpisodeListAdapter
import com.georgek.sgfs.podcastplayer.model.view.PodcastViewModel
import com.georgek.sgfs.podcastplayer.service.PodcastPlayerMediaService
import kotlinx.android.synthetic.main.fragment_podcast_details.*
import android.support.v7.widget.DividerItemDecoration

class PodcastDetailsFragment : Fragment(), PodcastPlayerMediaService.EpisodeListAdapterListener {

    private lateinit var episodeListAdapter: EpisodeListAdapter
    private lateinit var podcastViewModel: PodcastViewModel

    private var listener: OnPodcastDetailsListener? = null

    private var menuItem: MenuItem? = null

    interface OnPodcastDetailsListener {
        fun onSubscribe()
        fun onUnsubscribe()
        fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData)
    }

    override fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData) {
        listener?.onShowEpisodePlayer(episodeViewData)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        setupViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_podcast_details, container, false)
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupControls()
        updateControls()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnPodcastDetailsListener) {
            listener = context
        } else {
            throw RuntimeException(context!!.toString() +
                    " must implement OnPodcastDetailsListener")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_feed_action -> {
                podcastViewModel.recentlyLoaded?.feedUrl?.let {
                    if (podcastViewModel.recentlyLoaded?.subscribed!!) {
                        listener?.onUnsubscribe()
                    } else {
                        listener?.onSubscribe()
                    }
                }
                return true
            }
            else ->
                return super.onOptionsItemSelected(item)
        }
    }

    private fun updateMenuItem() {
        val viewData = podcastViewModel.recentlyLoaded ?: return
        menuItem?.title = if (viewData.subscribed)
            getString(R.string.unsubscribe) else getString(R.string.subscribe)
    }

    private fun setupControls() {
        feedDescTextView.movementMethod = ScrollingMovementMethod()
        episodeRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(context)
        episodeRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
                episodeRecyclerView.context, layoutManager.orientation
        )

        episodeRecyclerView.addItemDecoration(dividerItemDecoration)

        episodeListAdapter = EpisodeListAdapter(podcastViewModel.recentlyLoaded?.episodes, this)
        episodeRecyclerView.adapter = episodeListAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu?,
                                     inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.menu_details, menu)

        menuItem = menu?.findItem(R.id.menu_feed_action)
        updateMenuItem()
    }

    private fun updateControls() {
        val viewData = podcastViewModel.recentlyLoaded ?: return
        feedTitleTextView.text = viewData.feedTitle
        feedDescTextView.text = viewData.feedDesc
        Glide.with(activity).load(viewData.imageUrl).into(feedImageView)
    }

    private fun setupViewModel() {
        podcastViewModel = ViewModelProviders.of(this.activity!!).get(PodcastViewModel::class.java)
    }

    companion object {
        fun newInstance(): PodcastDetailsFragment {
            return PodcastDetailsFragment()
        }
    }
}