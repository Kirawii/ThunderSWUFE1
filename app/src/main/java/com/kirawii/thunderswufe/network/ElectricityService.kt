package com.kirawii.thunderswufe.network

import retrofit2.Response
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface ElectricityService {
    @FormUrlEncoded
    @POST("easytong_app/GetPayAccInfoNew")
    suspend fun getElectricityInfo(
        @Header("Authorization") authorizationToken: String,
        @HeaderMap headers: Map<String, String>,
        @FieldMap requestFields: Map<String, String>
    ): Response<ElectricityResponse>
}
