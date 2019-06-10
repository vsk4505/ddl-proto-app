package com.doordash.doordashlite.repository.inMemory.byPage

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import com.doordash.doordashlite.api.DoorDashLiteApi
import com.doordash.doordashlite.model.Restaurant
import com.doordash.doordashlite.util.Location
import java.util.concurrent.Executor

/**
 * A simple data source factory which also provides a way to observe the last created data source.
 * This allows us to channel its network request status etc back to the UI. See the Listing creation
 * in the Repository class.
 */
class DataSourceFactory(
    private val location: Location,
    private val doorDashLiteApi: DoorDashLiteApi,
    private val retryExecutor: Executor) : DataSource.Factory<Int, Restaurant>() {
    val sourceLiveData = MutableLiveData<PageKeyedDataSource>()
    override fun create(): DataSource<Int, Restaurant> {
        val source = PageKeyedDataSource(location, doorDashLiteApi, retryExecutor)
        sourceLiveData.postValue(source)
        return source
    }
}
