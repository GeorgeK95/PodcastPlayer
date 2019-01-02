package com.georgek.sgfs.podcastplayer.ui

import android.app.SearchManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.firebase.jobdispatcher.*
import com.georgek.sgfs.podcastplayer.R
import com.georgek.sgfs.podcastplayer.adapter.PodcastListAdapter
import com.georgek.sgfs.podcastplayer.db.PodcastPlayerDatabase
import com.georgek.sgfs.podcastplayer.model.view.PodcastViewModel
import com.georgek.sgfs.podcastplayer.model.view.SearchViewModel
import com.georgek.sgfs.podcastplayer.repository.ItunesRepository
import com.georgek.sgfs.podcastplayer.repository.PodcastRepository
import com.georgek.sgfs.podcastplayer.service.EpisodeUpdateService
import com.georgek.sgfs.podcastplayer.service.FeedService
import com.georgek.sgfs.podcastplayer.service.ItunesService
import kotlinx.android.synthetic.main.activity_podcast.*

class PodcastActivity : AppCompatActivity(), PodcastListAdapter.PodcastListAdapterListener, PodcastDetailsFragment.OnPodcastDetailsListener {
    private lateinit var searchViewModel: SearchViewModel

    private lateinit var podcastViewModel: PodcastViewModel
    private lateinit var podcastListAdapter: PodcastListAdapter
    private lateinit var searchMenuItem: MenuItem
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast)
        setupToolbar()
        setupViewModels()
        updateControls()
        setupPodcastListView()
        handleIntent(intent)
        addBackStackListener()
        scheduleJobs()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)

        searchMenuItem = menu.findItem(R.id.search_item)
        val searchView = searchMenuItem.actionView as SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        if (supportFragmentManager.backStackEntryCount > 0)
            podcastRecyclerView.visibility = View.INVISIBLE

        if (podcastRecyclerView.visibility == View.INVISIBLE) {
            searchMenuItem.isVisible = false

            searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                    return true
                }

                override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                    showSubscribedPodcasts()
                    return true
                }
            })
        }

        return true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData) {
        podcastViewModel.activeEpisodeViewData = episodeViewData
        showPlayerFragment()
    }

    override fun onSubscribe() {
        podcastViewModel.saveActivePodcast()
        supportFragmentManager.popBackStack()
    }

    override fun onUnsubscribe() {
        podcastViewModel.deleteActivePodcast()
        supportFragmentManager.popBackStack()
    }

    override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {
        val feedUrl = podcastSummaryViewData.feedUrl ?: return
        showProgressBar()

        podcastViewModel.getPodcast(podcastSummaryViewData) {
            hideProgressBar()
            if (it != null)
                showDetailsFragment()
            else
                showError("Error loading feed $feedUrl")
        }
    }

    private fun showPlayerFragment() {
        val episodePlayerFragment = createEpisodePlayerFragment()

        supportFragmentManager.beginTransaction().replace(
                R.id.podcastDetailsContainer,
                episodePlayerFragment,
                TAG_PLAYER_FRAGMENT
        ).addToBackStack(TAG_PLAYER_FRAGMENT).commit()

        podcastRecyclerView.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }

    private fun createEpisodePlayerFragment(): EpisodePlayerFragment {
        var episodePlayerFragment = supportFragmentManager.findFragmentByTag(TAG_PLAYER_FRAGMENT)
                as EpisodePlayerFragment?

        if (episodePlayerFragment == null)
            episodePlayerFragment = EpisodePlayerFragment.newInstance()

        return episodePlayerFragment
    }

    private fun scheduleJobs() {
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(this))

        val oneHourInSeconds = 10//60 * 60
        val tenMinutesInSeconds = 20//60 * 10

        val episodeUpdateJob = dispatcher.newJobBuilder()
                .setService(EpisodeUpdateService::class.java)
                .setTag(TAG_EPISODE_UPDATE_JOB)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(oneHourInSeconds, (oneHourInSeconds + tenMinutesInSeconds)))
                .setLifetime(Lifetime.FOREVER)
                .setConstraints(
                        Constraint.ON_UNMETERED_NETWORK
//                        Constraint.DEVICE_CHARGING
                )
                .build()

        dispatcher.mustSchedule(episodeUpdateJob)
    }

    private fun setupPodcastListView() {
        podcastViewModel.getPodcasts()?.observe(this, Observer {
            if (it != null) {
                showSubscribedPodcasts()
            }
        })
    }

    private fun showSubscribedPodcasts() {
        val podcasts = podcastViewModel.getPodcasts()?.value
        if (podcasts != null) {
            toolbar.title = getString(R.string.subscribed_podcasts)
            podcastListAdapter.setSearchData(podcasts)
        }
    }

    private fun showDetailsFragment() {
        val podcastDetailsFragment = createPodcastDetailsFragment()

        supportFragmentManager.beginTransaction().add(
                R.id.podcastDetailsContainer,
                podcastDetailsFragment, TAG_DETAILS_FRAGMENT)
                .addToBackStack(TAG_DETAILS_FRAGMENT).commit()

        podcastRecyclerView.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }

    private fun createPodcastDetailsFragment(): PodcastDetailsFragment {
        var podcastDetailsFragment = supportFragmentManager
                .findFragmentByTag(TAG_DETAILS_FRAGMENT) as PodcastDetailsFragment?

        if (podcastDetailsFragment == null) podcastDetailsFragment = PodcastDetailsFragment.newInstance()

        return podcastDetailsFragment
    }

    private fun searchProcess(term: String) {
        showProgressBar()
        searchViewModel.search(term) { results ->
            hideProgressBar()
            toolbar.title = getString(R.string.search_results)
            podcastListAdapter.setSearchData(results)
        }
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
    }

    private fun updateControls() {
        podcastRecyclerView.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this)
        podcastRecyclerView.layoutManager = layoutManager
        val dividerItemDecoration =
                android.support.v7.widget.DividerItemDecoration(
                        podcastRecyclerView.context, layoutManager.orientation)
        podcastRecyclerView.addItemDecoration(dividerItemDecoration)
        podcastListAdapter = PodcastListAdapter(null, this, this)
        podcastRecyclerView.adapter = podcastListAdapter
    }

    private fun setupViewModels() {
        searchViewModel = ViewModelProviders.of(this).get(SearchViewModel::class.java)
        searchViewModel.repo = ItunesRepository(ItunesService.instance)

        podcastViewModel = ViewModelProviders.of(this).get(PodcastViewModel::class.java)

        val rssService = FeedService.instance

        val db = PodcastPlayerDatabase.getInstance(this)
        val podcastDao = db.podcastDao()
        podcastViewModel.podcastRepository = PodcastRepository(rssService, podcastDao)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            searchProcess(query)
        }

        val podcastFeedUrl = intent.getStringExtra(EpisodeUpdateService.EXTRA_FEED_URL)
        if (podcastFeedUrl != null) {
            podcastViewModel.setActivePodcast(podcastFeedUrl) {
                it?.let { podcastSummaryView -> onShowDetails(podcastSummaryView) }
            }
        }
    }

    private fun addBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                podcastRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok_button), null)
                .create()
                .show()
    }

    companion object {
        private val TAG_DETAILS_FRAGMENT = "DetailsFragment"
        private val TAG_EPISODE_UPDATE_JOB = "com.georgek.sgfs.podcastplayer.episodes"
        private const val TAG_PLAYER_FRAGMENT = "PlayerFragment"
    }
}
