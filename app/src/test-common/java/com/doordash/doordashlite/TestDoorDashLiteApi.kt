package com.doordash.doordashlite

import com.doordash.doordashlite.api.DoorDashLiteApi
import com.doordash.doordashlite.model.Restaurant
import com.doordash.doordashlite.model.RestaurantDetails
import retrofit2.Call
import retrofit2.mock.Calls
import java.io.IOException

class TestDoorDashLiteApi : DoorDashLiteApi {
    private val restaurantsList = mutableListOf<Restaurant>()
    var failureMsg: String? = null
    fun addRestaurant(restaurant: Restaurant) {
        restaurantsList.add(restaurant)
    }

    fun clear() {
        restaurantsList.clear()
    }

    private fun findRestaurants(offset: Int, limit: Int): List<Restaurant> {
        return restaurantsList.subList(offset, Math.min(restaurantsList.size, offset + limit))
    }

    override fun getRestaurantsList(
        latitude: Float,
        longitude: Float,
        offset: Int,
        limit: Int
    ): Call<List<Restaurant>> {
        failureMsg?.let {
            return Calls.failure(IOException(it))
        }
        val items = findRestaurants(offset, limit)
        return Calls.response(items)
    }

    override fun getRestaurantById(restaurantId: Int): Call<RestaurantDetails?> {
        TODO()
    }
}