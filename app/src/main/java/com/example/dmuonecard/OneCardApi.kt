
package com.example.dmuonecard

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface OneCardApi {
    @POST("api/scan/")
    suspend fun scanStudent(
        @Body request: ScanRequest
    ): Response<ScanResponse>

    @GET("api/meal-status/")
    suspend fun getMealStatus(): MealStatusResponse
}
