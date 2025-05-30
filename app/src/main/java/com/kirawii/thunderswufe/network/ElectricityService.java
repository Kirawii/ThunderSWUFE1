package com.kirawii.thunderswufe.network;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ElectricityService {
    @FormUrlEncoded
    @POST("easytong_app/GetPayAccInfoNew")
    Call<ElectricityResponse> getElectricityInfo(
        @Header("Authorization") String authorizationToken,
        @Header("User-Agent") String userAgent,
        @Header("Accept") String accept,
        @Header("Origin") String origin,
        @Header("Referer") String referer,
        @Header("h5req") String h5req,
        @Header("sec-ch-ua-platform") String secChUaPlatform,
        @Header("sec-ch-ua") String secChUa,
        @Header("sec-ch-ua-mobile") String secChUaMobile,
        @Header("x-requested-with") String xRequestedWith,
        @Header("sec-fetch-site") String secFetchSite,
        @Header("sec-fetch-mode") String secFetchMode,
        @Header("sec-fetch-dest") String secFetchDest,
        @Header("accept-language") String acceptLanguage,
        @FieldMap Map<String, String> requestFields
    );
} 