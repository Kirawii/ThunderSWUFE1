package com.kirawii.thunderswufe.network;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kirawii.thunderswufe.BuildConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class NetworkModule {
    private static final NetworkModule INSTANCE = new NetworkModule();
    private static final Map<String, String> predefinedCookies = new HashMap<>();

    static {
        predefinedCookies.put("JSESSIONID", "d8b76fc3-beeb-491a-8f94-2c634f6d744c");
        predefinedCookies.put("SESSION", "ZmFjMzY0YWQtMDA2Ny00ZTRlLTkxYzYtYTRlZTkxNzk4MGNh");
        predefinedCookies.put("etToken", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMTEiLCJhY2NOdW0iOiIxMzEzMTciLCJwZXJDb2RlIjoiMTExIiwiZXhwIjoxNzc5MTAyOTA0LCJpYXQiOjE3NDc1NjY5MDQsImp0aSI6IjAwZmM3NDFjLTQ1ZmUtNDk0OC1iOWY1LTIyOGM3YzJlMmZlYiJ9.tpk_DTtgWDZogMse-rQWsfdK1GxV92ao-r_qFmHUjYo");
    }

    private NetworkModule() {}
    public static NetworkModule getInstance() { return INSTANCE; }

    private static class AppCookieJar implements CookieJar {
        private final Map<String, List<Cookie>> cookieStore = new HashMap<>();

        public AppCookieJar(Context context) {
            String domain = null;
            try {
                HttpUrl url = HttpUrl.parse(BuildConfig.BASE_URL);
                if (url != null) {
                    domain = url.host();
                }
            } catch (Exception ignored) {}
            if (domain != null && !domain.isEmpty()) {
                List<Cookie> cookiesForDomain = new java.util.ArrayList<>();
                for (Map.Entry<String, String> entry : predefinedCookies.entrySet()) {
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
            List<Cookie> existing = cookieStore.get(url.host());
            if (existing == null) existing = new java.util.ArrayList<>();
            existing.addAll(cookies);
            cookieStore.put(url.host(), existing);
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            List<Cookie> cookies = cookieStore.get(url.host());
            return cookies != null ? cookies : new java.util.ArrayList<>();
        }
    }

    public static OkHttpClient provideOkHttpClient(Context context) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        if (BuildConfig.DEBUG) {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }
        return new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .cookieJar(new AppCookieJar(context))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static Retrofit provideRetrofit(OkHttpClient okHttpClient) {
        Gson gson = new GsonBuilder().create();
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    public static ElectricityService provideElectricityService(Retrofit retrofit) {
        return retrofit.create(ElectricityService.class);
    }
}
