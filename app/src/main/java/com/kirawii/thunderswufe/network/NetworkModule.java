package com.kirawii.thunderswufe.network;

import android.content.Context;
import com.google.gson.GsonBuilder;
import com.kirawii.thunderswufe.BuildConfig;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NetworkModule {
    private static final Map<String, String> PREDEFINED_COOKIES = new HashMap<String, String>() {{
        put("JSESSIONID", "d8b76fc3-beeb-491a-8f94-2c634f6d744c");
        put("SESSION", "ZmFjMzY0YWQtMDA2Ny00ZTRlLTkxYzYtYTRlZTkxNzk4MGNh");
        put("etToken", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMTEiLCJhY2NOdW0iOiIxMzEzMTciLCJwZXJDb2RlIjoiMTExIiwiZXhwIjoxNzc5MTAyOTA0LCJpYXQiOjE3NDc1NjY5MDQsImp0aSI6IjAwZmM3NDFjLTQ1ZmUtNDk0OC1iOWY1LTIyOGM3YzJlMmZlYiJ9.tpk_DTtgWDZogMse-rQWsfdK1GxV92ao-r_qFmHUjYo");
    }};

    private static class AppCookieJar implements CookieJar {
        private final Map<String, List<Cookie>> cookieStore = new HashMap<>();

        public AppCookieJar(Context context) {
            String domain = HttpUrl.parse(BuildConfig.BASE_URL) != null ? 
                    HttpUrl.parse(BuildConfig.BASE_URL).host() : "";
            
            if (!domain.isEmpty()) {
                List<Cookie> cookiesForDomain = new ArrayList<>();
                for (Map.Entry<String, String> entry : PREDEFINED_COOKIES.entrySet()) {
                    Cookie cookie = new Cookie.Builder()
                            .name(entry.getKey())
                            .value(entry.getValue())
                            .domain(domain)
                            .path("/")
                            .build();
                    cookiesForDomain.add(cookie);
                }
                cookieStore.put(domain, cookiesForDomain);
            }
        }

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            cookieStore.put(url.host(), new ArrayList<>(cookies));
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            List<Cookie> cookies = cookieStore.get(url.host());
            return cookies != null ? new ArrayList<>(cookies) : new ArrayList<>();
        }
    }

    public static OkHttpClient provideOkHttpClient(Context context) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(BuildConfig.DEBUG ? 
                HttpLoggingInterceptor.Level.BODY : 
                HttpLoggingInterceptor.Level.NONE);

        return new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .cookieJar(new AppCookieJar(context))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static Retrofit provideRetrofit(OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create()))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }

    public static ElectricityService provideElectricityService(Retrofit retrofit) {
        return retrofit.create(ElectricityService.class);
    }
} 