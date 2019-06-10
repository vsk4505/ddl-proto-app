package com.doordash.doordashlite

import com.doordash.doordashlite.model.Restaurant
import java.util.concurrent.atomic.AtomicInteger

class RestaurantFactory {
    private val counter = AtomicInteger(0)
    fun createRestaurant() : Restaurant {
        val id = counter.incrementAndGet()
        return Restaurant(
                id = id,
                coverImageUrl = "",
                name = "name_$id",
                description = "description $id",
                statusType = "open",
                status = ""
        )
    }
}