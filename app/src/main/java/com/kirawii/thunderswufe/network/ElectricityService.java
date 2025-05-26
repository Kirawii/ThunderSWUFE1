package com.kirawii.thunderswufe.network;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;
import java.util.Map;

/**
 * Retrofit service interface for interacting with the electricity API.
 */
public interface ElectricityService {
    /**
     * Fetches electricity account information.
     * Assumes the API expects 'application/x-www-form-urlencoded' content type.
     */
    @FormUrlEncoded
    @POST("easytong_app/GetPayAccInfoNew")
    Single<ElectricityResponse> getElectricityInfo(
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

    /**
     * 获取当前电量数据
     * @return 电量数据
     */
    Single<ElectricityResponse> getCurrentElectricityData();
    
    /**
     * 关闭服务，释放资源
     */
    default void close() {
        // 默认实现为空
    }

    class Headers {
        public static final String DEFAULT_ACCEPT = "application/json, text/plain, */*";
        public static final String DEFAULT_H5REQ = "Y";
        public static final String DEFAULT_SEC_CH_UA_PLATFORM = "\"Android\"";
        public static final String DEFAULT_SEC_CH_UA = "\"Android WebView\";v=\"135\", \"Not-A.Brand\";v=\"8\", \"Chromium\";v=\"135\"";
        public static final String DEFAULT_SEC_CH_UA_MOBILE = "?1";
        public static final String DEFAULT_X_REQUESTED_WITH = "cn.com.yunma.school.app";
        public static final String DEFAULT_SEC_FETCH_SITE = "same-origin";
        public static final String DEFAULT_SEC_FETCH_MODE = "cors";
        public static final String DEFAULT_SEC_FETCH_DEST = "empty";
        public static final String DEFAULT_ACCEPT_LANGUAGE = "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7";
        public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 15; V2241HA Build/AP3A.240905.015.A2; wv) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/135.0.7049.111 Mobile Safari/537.36 " +
                "ZJYXYwebviewbroswer ZJYXYAndroid tourCustomer/yunmaapp.NET/7.1.5/ym-30a974936ba5e48e03b0775175a54a30";
    }
} 