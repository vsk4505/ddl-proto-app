package com.doordash.doordashlite.repository.inMemory.byPage

import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import com.doordash.doordashlite.api.DoorDashLiteApi
import com.doordash.doordashlite.repository.NetworkState
import com.doordash.doordashlite.model.Restaurant
import com.doordash.doordashlite.util.Location
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.Executor

/**
 * A data source that uses the before/after keys calculated based on the number of objects in page requests.
 */
class PageKeyedDataSource(
    private val location: Location,
    private val doorDashLiteApi: DoorDashLiteApi,
    private val retryExecutor: Executor
) : PageKeyedDataSource<Int, Restaurant>() {

    // keep a function reference for the retry event
    private var retry: (() -> Any)? = null

    /**
     * There is no sync on the state because paging will always call loadInitial first then wait
     * for it to return some success value before calling loadAfter.
     */
    val networkState = MutableLiveData<NetworkState>()

    val initialLoad = MutableLiveData<NetworkState>()

    fun retryAllFailed() {
        val prevRetry = retry
        retry = null
        prevRetry?.let {
            retryExecutor.execute {
                it.invoke()
            }
        }
    }

    override fun loadBefore(
        params: LoadParams<Int>,
        callback: LoadCallback<Int, Restaurant>
    ) {
        // ignored, since we only ever append to our initial load
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, Restaurant>) {
        networkState.postValue(NetworkState.LOADING)
        doorDashLiteApi.getRestaurantsList(
            location.latitude,
            location.longitude,
            offset = params.key,
            limit = params.requestedLoadSize
        ).enqueue(
            object : retrofit2.Callback<List<Restaurant>> {
                override fun onFailure(call: Call<List<Restaurant>>, t: Throwable) {
                    retry = {
                        loadAfter(params, callback)
                    }
                    networkState.postValue(NetworkState.error(t.message ?: "unknown err"))
                }

                override fun onResponse(
                    call: Call<List<Restaurant>>,
                    response: Response<List<Restaurant>>
                ) {
                    if (response.isSuccessful) {
                        val data = response.body() ?: emptyList()
                        retry = null
                        val nextPageOffset = params.key + data.size
                        callback.onResult(data, nextPageOffset)
                        networkState.postValue(NetworkState.LOADED)
                    } else {
                        retry = {
                            loadAfter(params, callback)
                        }
                        networkState.postValue(
                            NetworkState.error("error code: ${response.code()}")
                        )
                    }
                }
            }
        )
    }

    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, Restaurant>
    ) {
        val request = doorDashLiteApi.getRestaurantsList(
            location.latitude,
            location.longitude,
            offset = 0,
            limit = params.requestedLoadSize
        )
        networkState.postValue(NetworkState.LOADING)
        initialLoad.postValue(NetworkState.LOADING)

        // triggered by a refresh, we better execute sync
        try {
            val response = request.execute()
            val data = response.body() ?: emptyList()
            retry = null
            networkState.postValue(NetworkState.LOADED)
            initialLoad.postValue(NetworkState.LOADED)
            val nextPageOffset = data.size
            callback.onResult(data, null, nextPageOffset)
        } catch (ioException: IOException) {
            retry = {
                loadInitial(params, callback)
            }
            val error = NetworkState.error(ioException.message ?: "unknown error")
            networkState.postValue(error)
            initialLoad.postValue(error)
        }
    }
}