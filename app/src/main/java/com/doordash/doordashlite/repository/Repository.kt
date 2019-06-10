package com.doordash.doordashlite.repository

import com.doordash.doordashlite.model.IResponseListener
import com.doordash.doordashlite.model.Restaurant
import com.doordash.doordashlite.model.RestaurantDetails

interface Repository {
    fun listOfRestaurants(pageSize: Int): Listing<Restaurant>
    fun getRestaurantById(id: Int, listener: IResponseListener<RestaurantDetails>)

    enum class Type {
        IN_MEMORY_BY_PAGE,
    }
}