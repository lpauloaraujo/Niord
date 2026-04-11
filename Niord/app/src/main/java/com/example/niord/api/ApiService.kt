package com.example.niord.api

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse


class ApiService {
    suspend fun greet(): String {
        return ApiClient.apiService.get("greet").body<HttpResponse>().body<String>()
    }
}