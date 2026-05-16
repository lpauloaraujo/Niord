package com.example.niord.api

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


object ApiClient {
    private const val BASE_URL = "http://192.168.100.25/"
    fun createHttpClient(context: Context): HttpClient {
        return HttpClient(OkHttp) {
            // 1. Serialization (JSON to Data Classes)
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }

            install(HttpCookies) {
                storage = PersistentCookieStorage(context)
            }

            // 2. Logging (Great for debugging)
            install(Logging) {
                level = LogLevel.BODY
            }

            // 3. Optional: Default Request (Base URL)
            defaultRequest {
                url(BASE_URL)
            }
        }
    }
}


class PersistentCookieStorage(private val context: Context) : CookiesStorage {
    private val sharedPrefs = context.getSharedPreferences("app_cookies", Context.MODE_PRIVATE)

    override suspend fun get(requestUrl: Url): List<Cookie> {
        // Logic to retrieve and parse cookies from SharedPreferences
        return emptyList() // Simplified for example
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        // Logic to save cookie string to SharedPreferences
    }

    override fun close() {}
}