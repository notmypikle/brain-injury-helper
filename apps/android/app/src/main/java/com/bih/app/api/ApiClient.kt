package com.bih.app.api

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Builds a [BihApi] for the configured base URL, attaching the API key
 *  header when one is set (matches BIH_API_KEY on the server). */
object ApiClient {

    fun create(baseUrl: String, apiKey: String): BihApi {
        val json = Json { ignoreUnknownKeys = true }

        val authInterceptor = Interceptor { chain ->
            val request = if (apiKey.isNotBlank()) {
                chain.request().newBuilder()
                    .header("X-API-Key", apiKey)
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BihApi::class.java)
    }
}
