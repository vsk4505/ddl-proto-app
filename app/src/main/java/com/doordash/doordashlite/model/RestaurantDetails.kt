package com.doordash.doordashlite.model

import com.google.gson.annotations.SerializedName

data class RestaurantDetails(
    @SerializedName("id") val id: Int,
    @SerializedName("cover_img_url") val coverImageUrl: String,
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: String
)