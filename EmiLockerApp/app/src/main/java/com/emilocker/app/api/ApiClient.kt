package com.emilocker.app.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var retrofit: Retrofit? = null

    fun getService(baseUrl: String): EmiApiService {
        if (retrofit == null || retrofit?.baseUrl()?.toString() != baseUrl) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            // Ensure baseUrl ends with '/'
            val safeUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

            retrofit = Retrofit.Builder()
                .baseUrl(safeUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(EmiApiService::class.java)
    }
}
