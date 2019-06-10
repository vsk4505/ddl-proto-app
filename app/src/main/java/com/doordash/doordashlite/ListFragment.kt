package com.doordash.doordashlite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import com.doordash.doordashlite.glide.GlideApp
import com.doordash.doordashlite.repository.Repository
import com.doordash.doordashlite.repository.NetworkState
import com.doordash.doordashlite.repository.ui.SharedViewModel
import com.doordash.doordashlite.repository.ui.RestaurantsPagedListAdapter
import com.doordash.doordashlite.model.Restaurant
import kotlinx.android.synthetic.main.fragment_list.*

class ListFragment : Fragment() {

    private lateinit var model: SharedViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model = getViewModel()
        initAdapter()
        initSwipeToRefresh()
        model.initList()
    }

    private fun getViewModel(): SharedViewModel {
        return ViewModelProviders.of(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                val repoType = Repository.Type.values()[Repository.Type.IN_MEMORY_BY_PAGE.ordinal]
                val repo = ServiceLocator.instance(context!!)
                    .getRepository(repoType)
                return SharedViewModel(repo) as T
            }
        })[SharedViewModel::class.java]
    }

    private fun initAdapter() {
        val glide = GlideApp.with(this)
        val adapterCallback = if (activity is RestaurantsPagedListAdapter.AdapterCallback) activity as RestaurantsPagedListAdapter.AdapterCallback else null
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
    }

    private fun initSwipeToRefresh() {
        model.refreshState.observe(this, Observer {
            swipe_refresh.isRefreshing = it == NetworkState.LOADING
        })
        swipe_refresh.setOnRefreshListener {
            model.refreshList()
        }
    }
}
