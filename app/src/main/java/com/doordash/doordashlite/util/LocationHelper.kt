package com.doordash.doordashlite.util

class LocationHelper {
    companion object {
        fun getLocation(): Location {
            return Location()
        }
    }
}

data class Location(val latitude: Float = 37.422740f, val longitude: Float = -122.139956f)