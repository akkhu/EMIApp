package com.emilocker.app.api

import com.emilocker.app.api.models.DeviceStatusResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface EmiApiService {
    @GET("mobile/{appId}/{imei}/status")
    suspend fun getDeviceStatus(
        @Path("appId") appId: String,
        @Path("imei") imei: String
    ): Response<DeviceStatusResponse>
}
