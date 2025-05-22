package com.kirawii.thunderswufe.network

import android.content.Context
import com.google.gson.GsonBuilder
import com.kirawii.thunderswufe.BuildConfig

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private val predefinedCookies = mapOf(
        "JSESSIONID" to "d8b76fc3-beeb-491a-8f94-2c634f6d744c",
        "SESSION" to "ZmFjMzY0YWQtMDA2Ny00ZTRlLTkxYzYtYTRlZTkxNzk4MGNh",
        "etToken" to "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMTEiLCJhY2NOdW0iOiIxMzEzMTciLCJwZXJDb2RlIjoiMTExIiwiZXhwIjoxNzc5MTAyOTA0LCJpYXQiOjE3NDc1NjY5MDQsImp0aSI6IjAwZmM3NDFjLTQ1ZmUtNDk0OC1iOWY1LTIyOGM3YzJlMmZlYiJ9.tpk_DTtgWDZogMse-rQWsfdK1GxV92ao-r_qFmHUjYo"
    )

    private class AppCookieJar(context: Context) : CookieJar {
        private val cookieStore = mutableMapOf<String, List<Cookie>>()

        init {
            val domain = BuildConfig.BASE_URL.toHttpUrlOrNull()?.host ?: ""
            if (domain.isNotEmpty()) {
                val cookiesForDomain = predefinedCookies.map { (name, value) ->
                    Cookie.Builder()
                        .name(name)
                        .value(value)
                        .domain(domain)
                        .path("/")
                        .build()
                }
                cookieStore[domain] = cookiesForDomain
            }
        }

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = (cookieStore[url.host] ?: emptyList()) + cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    fun provideOkHttpClient(context: Context): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .cookieJar(AppCookieJar(context))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val gson = GsonBuilder().create()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun provideElectricityService(retrofit: Retrofit): ElectricityService {
        return retrofit.create(ElectricityService::class.java)
    }
}
