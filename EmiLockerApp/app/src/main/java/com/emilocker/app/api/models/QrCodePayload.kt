package com.emilocker.app.api.models
import com.google.gson.annotations.SerializedName

data class QrCodePayload(
    @SerializedName("action") val action: String,
    @SerializedName("appId") val appId: String,
    @SerializedName("imei") val imei: String,
    @SerializedName("serverUrl") val serverUrl: String
)
