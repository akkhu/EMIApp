package com.emilocker.app.api.models
import com.google.gson.annotations.SerializedName

data class DeviceStatusResponse(
    @SerializedName("status") val status: String,
    @SerializedName("lastSyncTime") val lastSyncTime: String
)
