package com.doordash.doordashlite.api

import android.util.Log
import com.doordash.doordashlite.BuildConfig
import com.doordash.doordashlite.model.Restaurant
import com.doordash.doordashlite.model.RestaurantDetails
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API communication setup
 */
interface DoorDashLiteApi {
    /**
     * API to get list of restaurants details for given lat, lng, offset and limit
     */
    @GET("/v2/restaurant")
    fun getRestaurantsList(
        @Query("lat") latitude: Float,
        @Query("lng") longitude: Float,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): Call<List<Restaurant>>

    /**
     * API to get restaurant details for given restaurant_id
     */
    @GET("/v2/restaurant/{restaurant_id}")
    fun getRestaurantById(
        @Path("restaurant_id") restaurantId: Int
    ): Call<RestaurantDetails?>

    companion object {
        private const val BASE_URL = "https://api.doordash.com"
        fun create(): DoorDashLiteApi = create(HttpUrl.parse(BASE_URL)!!)

        private fun create(httpUrl: HttpUrl): DoorDashLiteApi {
            val logger = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger {
                Log.d("DoorDashAPI", it)
            })
            if (BuildConfig.DEBUG) {
                logger.level = HttpLoggingInterceptor.Level.BASIC
            } else {
                logger.level = HttpLoggingInterceptor.Level.NONE
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()

            return Retrofit.Builder()
                .baseUrl(httpUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DoorDashLiteApi::class.java)
        }
    }
}
