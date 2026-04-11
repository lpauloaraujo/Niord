package com.example.niord.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ApiClient {
    private const val BASE_URL = "http://192.168.100.25/"
    val apiService = HttpClient(OkHttp) {
        // 1. Serialization (JSON to Data Classes)
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
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