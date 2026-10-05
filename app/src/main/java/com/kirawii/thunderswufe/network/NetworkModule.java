package com.kirawii.thunderswufe.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kirawii.thunderswufe.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class NetworkModule {
    private static volatile ElectricityService service;

    private NetworkModule() {}

    private static OkHttpClient provideOkHttpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        // BASIC logs method/status only; never log room fields, Authorization or cookies.
        loggingInterceptor.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BASIC : HttpLoggingInterceptor.Level.NONE);
        return new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private static Retrofit provideRetrofit(OkHttpClient okHttpClient) {
        Gson gson = new GsonBuilder().create();
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    public static ElectricityService provideElectricityService() {
        if (service == null) {
            synchronized (NetworkModule.class) {
                if (service == null) {
                    service = provideRetrofit(provideOkHttpClient()).create(ElectricityService.class);
                }
            }
        }
        return service;
    }
}
