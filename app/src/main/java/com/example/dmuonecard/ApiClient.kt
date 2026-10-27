package com.example.dmuonecard

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(8, TimeUnit.SECONDS)
    .writeTimeout(5, TimeUnit.SECONDS)
    .callTimeout(10, TimeUnit.SECONDS)
    .build()

object ApiClient {
     private const val BASE_URL = "http://10.129.175.133:8000/"

    val api: OneCardApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OneCardApi::class.java)
    }
}
