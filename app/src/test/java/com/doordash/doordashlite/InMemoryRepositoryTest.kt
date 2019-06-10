package com.doordash.doordashlite

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import com.doordash.doordashlite.model.Restaurant
import com.doordash.doordashlite.repository.Listing
import com.doordash.doordashlite.repository.NetworkState
import com.doordash.doordashlite.repository.Repository.Type.IN_MEMORY_BY_PAGE
import com.doordash.doordashlite.repository.Repository
import com.doordash.doordashlite.repository.inMemory.byPage.InMemoryByPageKeyRepository
import com.doordash.doordashlite.util.LocationHelper
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito
import java.util.concurrent.Executor

@RunWith(Parameterized::class)
class InMemoryRepositoryTest(type : Repository.Type) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = listOf(IN_MEMORY_BY_PAGE)
    }
    @Suppress("unused")
    @get:Rule // used to make all live data calls sync
    val instantExecutor = InstantTaskExecutorRule()
    private val location = LocationHelper.getLocation()
    private val testApi = TestDoorDashLiteApi()
    private val networkExecutor = Executor { command -> command.run() }
    private val repository = when(type) {
        IN_MEMORY_BY_PAGE -> InMemoryByPageKeyRepository(
                location = location,
                doorDashLiteApi = testApi,
                networkExecutor = networkExecutor)
        else -> throw IllegalArgumentException()
    }
    private val restaurantFactory = RestaurantFactory()

    private fun <T> PagedList<T>.loadAllData() {
        do {
            val oldSize = this.loadedCount
            this.loadAround(this.size - 1)
        } while (this.size != oldSize)
    }

    /**
     * asserts that empty list works fine
     */
    @Test
    fun emptyList() {
        val listing = repository.listOfRestaurants(10)
        val pagedList = getPagedList(listing)
        assertThat(pagedList.size, `is`(0))
    }

    /**
     * asserts that a list w/ single item is loaded properly
     */
    @Test
    fun oneItem() {
        val restaurant = restaurantFactory.createRestaurant()
        testApi.addRestaurant(restaurant)
        val listing = repository.listOfRestaurants(pageSize = 1)
        assertThat(getPagedList(listing), `is`(listOf(restaurant)))
    }

    /**
     * asserts loading a full list in multiple pages
     */
    @Test
    fun verifyCompleteList() {
        val restaurants = (0..10).map { restaurantFactory.createRestaurant() }
        restaurants.forEach(testApi::addRestaurant)
        val listing = repository.listOfRestaurants(pageSize = 3)
        // trigger loading of the whole list
        val pagedList = getPagedList(listing)
        pagedList.loadAllData()
        assertThat(pagedList, `is`(restaurants))
    }

    /**
     * asserts the failure message when the initial load cannot complete
     */
    @Test
    fun failToLoadInitial() {
        testApi.failureMsg = "Server error"
        val listing = repository.listOfRestaurants(pageSize = 3)
        // trigger load
        getPagedList(listing)
        assertThat(getNetworkState(listing), `is`(NetworkState.error("Server error")))
    }

    /**
     * asserts the retry logic when initial load request fails
     */
    @Test
    fun retryInInitialLoad() {
        testApi.addRestaurant(restaurantFactory.createRestaurant())
        testApi.failureMsg = "xxx"
        val listing = repository.listOfRestaurants(pageSize = 3)
        // trigger load
        val pagedList = getPagedList(listing)
        assertThat(pagedList.size, `is`(0))

        @Suppress("UNCHECKED_CAST")
        val networkObserver = Mockito.mock(Observer::class.java) as Observer<NetworkState>
        listing.networkState.observeForever(networkObserver)
        testApi.failureMsg = null
        listing.retry()
        assertThat(pagedList.size, `is`(1 ))
        assertThat(getNetworkState(listing), `is`(NetworkState.LOADED))
        val inOrder = Mockito.inOrder(networkObserver)
        inOrder.verify(networkObserver).onChanged(NetworkState.error("xxx"))
        inOrder.verify(networkObserver).onChanged(NetworkState.LOADING)
        inOrder.verify(networkObserver).onChanged(NetworkState.LOADED)
        inOrder.verifyNoMoreInteractions()
    }

    /**
     * asserts the retry logic when initial load succeeds but subsequent loads fails
     */
    @Test
    fun retryAfterInitialFails() {
        val restaurants = (0..10).map { restaurantFactory.createRestaurant() }
        restaurants.forEach(testApi::addRestaurant)
        val listing = repository.listOfRestaurants(pageSize = 2)
        val list = getPagedList(listing)
        assertThat("test sanity, we should not load everything",
                list.size < restaurants.size, `is`(true))
        assertThat(getNetworkState(listing), `is`(NetworkState.LOADED))
        testApi.failureMsg = "fail"
        list.loadAllData()
        assertThat(getNetworkState(listing), `is`(NetworkState.error("fail")))
        testApi.failureMsg = null
        listing.retry()
        list.loadAllData()
        assertThat(getNetworkState(listing), `is`(NetworkState.LOADED))
        assertThat(list, `is`(restaurants))
    }

    /**
     * asserts refresh loads the new data
     */
    @Test
    fun refresh() {
        val restaurantsV1 = (0..5).map { restaurantFactory.createRestaurant() }
        restaurantsV1.forEach(testApi::addRestaurant)
        val listing = repository.listOfRestaurants(pageSize = 5)
        val list = getPagedList(listing)
        list.loadAround(5)
        val restaurantsV2 = (0..10).map { restaurantFactory.createRestaurant() }
        testApi.clear()
        restaurantsV2.forEach(testApi::addRestaurant)

        @Suppress("UNCHECKED_CAST")
        val refreshObserver = Mockito.mock(Observer::class.java) as Observer<NetworkState>
        listing.refreshState.observeForever(refreshObserver)
        listing.refresh()

        val list2 = getPagedList(listing)
        list2.loadAround(5)
        assertThat(list2, `is`(restaurantsV2))
        val inOrder = Mockito.inOrder(refreshObserver)
        inOrder.verify(refreshObserver).onChanged(NetworkState.LOADED) // initial state
        inOrder.verify(refreshObserver).onChanged(NetworkState.LOADING)
        inOrder.verify(refreshObserver).onChanged(NetworkState.LOADED)
    }

    /**
     * asserts that refresh also works after failure
     */
    @Test
    fun refreshAfterFailure() {
        val restaurants = (0..5).map { restaurantFactory.createRestaurant() }
        restaurants.forEach(testApi::addRestaurant)

        testApi.failureMsg = "xx"
        val listing = repository.listOfRestaurants(pageSize = 5)
        getPagedList(listing)
        assertThat(getNetworkState(listing), `is`(NetworkState.error("xx")))
        testApi.failureMsg = null
        listing.refresh()
        // get the new list since refresh will create a new paged list
        assertThat(getPagedList(listing), `is`(restaurants))
    }

    /**
     * extract the latest paged list from the listing
     */
    private fun getPagedList(listing: Listing<Restaurant>): PagedList<Restaurant> {
        val observer = LoggingObserver<PagedList<Restaurant>>()
        listing.pagedList.observeForever(observer)
        assertThat(observer.value, `is`(notNullValue()))
        return observer.value!!
    }

    /**
     * extract the latest network state from the listing
     */
    private fun getNetworkState(listing: Listing<Restaurant>) : NetworkState? {
        val networkObserver = LoggingObserver<NetworkState>()
        listing.networkState.observeForever(networkObserver)
        return networkObserver.value
    }

    /**
     * simple observer that logs the latest value it receives
     */
    private class LoggingObserver<T> : Observer<T> {
        var value : T? = null
        override fun onChanged(t: T?) {
            this.value = t
        }
    }
}
