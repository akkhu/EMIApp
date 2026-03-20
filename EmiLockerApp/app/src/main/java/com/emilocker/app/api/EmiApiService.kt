package com.emilocker.app.api

import com.emilocker.app.api.models.DeviceStatusResponse
import com.emilocker.app.api.models.MobileRegisterRequest
import com.emilocker.app.api.models.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface EmiApiService {
    @GET("mobile/{appId}/{aadhaarNumber}/status")
    suspend fun getDeviceStatus(
        @Path("appId") appId: String,
        @Path("aadhaarNumber") aadhaarNumber: String
    ): Response<DeviceStatusResponse>

    @POST("mobile/register")
    suspend fun registerDevice(
        @Body request: MobileRegisterRequest
    ): Response<RegisterResponse>
}
