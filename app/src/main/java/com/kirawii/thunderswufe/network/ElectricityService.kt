package com.kirawii.thunderswufe.network

import retrofit2.Response
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit service interface for interacting with the electricity API.
 */
interface ElectricityService {

    /**
     * Fetches electricity account information.
     * Assumes the API expects 'application/x-www-form-urlencoded' content type.
     */
    @FormUrlEncoded
    @POST("easytong_app/GetPayAccInfoNew") // Relative path to the endpoint
    suspend fun getElectricityInfo(
        @Header("Authorization") authorizationToken: String,
        @Header("User-Agent") userAgent: String, // As per Python script, passed explicitly
        @Header("Accept") accept: String = "application/json, text/plain, */*", // Default from Python
        @Header("Origin") origin: String, // As per Python script
        @Header("Referer") referer: String, // As per Python script
        // Cookies should be handled by OkHttp's CookieJar configured in NetworkModule

        // + Add new headers based on successful request log
        @Header("h5req") h5req: String = "Y",
        @Header("sec-ch-ua-platform") secChUaPlatform: String = "\"Android\"", // Encapsulate in escaped quotes
        @Header("sec-ch-ua") secChUa: String = "\"Android WebView\";v=\"135\", \"Not-A.Brand\";v=\"8\", \"Chromium\";v=\"135\"", // Encapsulate in escaped quotes
        @Header("sec-ch-ua-mobile") secChUaMobile: String = "?1",
        @Header("x-requested-with") xRequestedWith: String = "cn.com.yunma.school.app",
        @Header("sec-fetch-site") secFetchSite: String = "same-origin",
        @Header("sec-fetch-mode") secFetchMode: String = "cors",
        @Header("sec-fetch-dest") secFetchDest: String = "empty",
        @Header("accept-language") acceptLanguage: String = "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",

        @FieldMap requestFields: Map<String, String> // All form fields including dynamic Time & Sign
    ): Response<ElectricityResponse>

    // TODO: Add other API endpoints here if needed
}