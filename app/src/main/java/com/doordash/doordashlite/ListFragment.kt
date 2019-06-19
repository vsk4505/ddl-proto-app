package com.doordash.doordashlite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import com.doordash.doordashlite.glide.GlideApp
import com.doordash.doordashlite.repository.NetworkState
import com.doordash.doordashlite.repository.ui.SharedViewModel
import com.doordash.doordashlite.repository.ui.RestaurantsPagedListAdapter
import com.doordash.doordashlite.model.Restaurant
import kotlinx.android.synthetic.main.fragment_list.*

class ListFragment : Fragment(), RestaurantsPagedListAdapter.AdapterCallback {

    private var progressBar: ProgressBar? = null
    private lateinit var model: SharedViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_list, container, false)
        progressBar = view.findViewById(R.id.progress_bar)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model = getViewModel()
        initAdapter()
        initSwipeToRefresh()
        model.initList()
    }

    private fun getViewModel(): SharedViewModel {
        return ViewModelProviders.of(activity!!).get(SharedViewModel::class.java)
    }

    private fun initAdapter() {
        val glide = GlideApp.with(this)
        val adapterCallback = if (this is RestaurantsPagedListAdapter.AdapterCallback) this else null
        val adapter = RestaurantsPagedListAdapter(glide, adapterCallback) {
            model.retryList()
        }
        list.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        list.adapter = adapter
        model.restaurants.observe(this, Observer<PagedList<Restaurant>> {
            adapter.submitList(it)
        })
        model.networkState.observe(this, Observer {
            adapter.setNetworkState(it)
        })
        model.itemRefresh.observe(this, Observer { pos ->
            hideProgress()
            adapter.notifyItemChanged(pos)
        })
    }

    private fun initSwipeToRefresh() {
        model.refreshState.observe(this, Observer {
            swipe_refresh.isRefreshing = it == NetworkState.LOADING
        })
        swipe_refresh.setOnRefreshListener {
            model.refreshList()
        }
    }

    override fun onItemClicked(id: Int) {
        model.showDetails.postValue(id)
    }

    override fun onLikeClicked(pos: Int, id: Int) {
        showProgress()
        model?.storeFavoriteId(pos, id)
    }

    override fun isLiked(id: Int?): Boolean? {
        return model.isIdInFavorites(id)
    }

    private fun showProgress() {
        progressBar?.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        progressBar?.visibility = View.GONE
    }
}
