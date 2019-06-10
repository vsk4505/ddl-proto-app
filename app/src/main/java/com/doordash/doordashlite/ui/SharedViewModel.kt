package com.doordash.doordashlite.repository.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.switchMap
import androidx.lifecycle.ViewModel
import com.doordash.doordashlite.model.IResponseListener
import com.doordash.doordashlite.model.Restaurant
import com.doordash.doordashlite.repository.Repository
import com.doordash.doordashlite.model.RestaurantDetails
import com.doordash.doordashlite.repository.Listing

class SharedViewModel(private val repository: Repository) : ViewModel() {
    val PAGE_SIZE: Int = 30
    val details = MutableLiveData<RestaurantDetails>()
    val detailsError = MutableLiveData<String>()
    private var repoResult = MutableLiveData<Listing<Restaurant>>()
    val restaurants = switchMap(repoResult) { it.pagedList }!!
    val networkState = switchMap(repoResult) { it.networkState }!!
    val refreshState = switchMap(repoResult) { it.refreshState }!!

    fun initList() {
        if (repoResult?.value == null) {
            repoResult?.value = repository.listOfRestaurants(PAGE_SIZE)
        }
    }

    fun refreshList() {
        repoResult?.value?.refresh?.invoke()
    }

    fun retryList() {
        val listing = repoResult?.value
        listing?.retry?.invoke()
    }

    fun getDetails(id: Int) {
        repository.getRestaurantById(id, object : IResponseListener<RestaurantDetails> {
            override fun onSuccess(obj: RestaurantDetails?) {
                details.value = obj ?: details.value
            }

            override fun onError(message: String?) {
                detailsError.value = message
            }
        })
    }
}
