package com.doordash.doordashlite.repository.inMemory.byPage

import androidx.lifecycle.Transformations
import androidx.annotation.MainThread
import androidx.paging.toLiveData
import com.doordash.doordashlite.api.DoorDashLiteApi
import com.doordash.doordashlite.model.IResponseListener
import com.doordash.doordashlite.repository.Repository
import com.doordash.doordashlite.repository.Listing
import com.doordash.doordashlite.model.Restaurant
import com.doordash.doordashlite.model.RestaurantDetails
import com.doordash.doordashlite.util.Location
import retrofit2.Call
import retrofit2.Response
import java.util.concurrent.Executor

/**
 * Repository implementation that returns a Listing that loads data directly from network by using
 * the previous / next offset values based on the list of objects returned in the query.
 */
class InMemoryByPageKeyRepository(
    private val location: Location,
    private val doorDashLiteApi: DoorDashLiteApi,
    private val networkExecutor: Executor
) : Repository {

    @MainThread
    override fun listOfRestaurants(pageSize: Int): Listing<Restaurant> {
        val sourceFactory = DataSourceFactory(location, doorDashLiteApi, networkExecutor)

        // We use toLiveData Kotlin extension function here, we could also use LivePagedListBuilder
        val livePagedList = sourceFactory.toLiveData(
            pageSize = pageSize,
            // provide custom executor for network requests, otherwise it will default to
            // Arch Components' IO pool which is also used for disk access
            fetchExecutor = networkExecutor
        )

        val refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) {
            it.initialLoad
        }
        return Listing(
            pagedList = livePagedList,
            networkState = Transformations.switchMap(sourceFactory.sourceLiveData) {
                it.networkState
            },
            retry = {
                sourceFactory.sourceLiveData.value?.retryAllFailed()
            },
            refresh = {
                sourceFactory.sourceLiveData.value?.invalidate()
            },
            refreshState = refreshState
        )
    }

    override fun getRestaurantById(id: Int, listener: IResponseListener<RestaurantDetails>) {
        doorDashLiteApi.getRestaurantById(restaurantId = id).enqueue(object : retrofit2.Callback<RestaurantDetails?> {
            override fun onResponse(call: Call<RestaurantDetails?>, response: Response<RestaurantDetails?>) {
                if (response.isSuccessful) {
                    listener.onSuccess(response.body())
                } else {
                    listener.onError("Server error")
                }
            }

            override fun onFailure(call: Call<RestaurantDetails?>, t: Throwable) {
                listener.onError(t.message)
            }
        })
    }
}

