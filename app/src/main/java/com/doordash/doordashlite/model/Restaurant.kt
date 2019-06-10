package com.doordash.doordashlite.model

import com.google.gson.annotations.SerializedName

data class Restaurant(
    @SerializedName("id") val id: Int,
    @SerializedName("cover_img_url") val coverImageUrl: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("status_type") val statusType: String,
    @SerializedName("status") val status: String
)