package com.example.niord.api

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType


class ApiService {
    suspend fun greet(): String {
        return ApiClient.apiService.get("greet").body<HttpResponse>().body<String>()
    }

    suspend fun sendRegisterData(registerPost: RegisterPost): HttpResponse{
        return ApiClient.apiService.post("auth/register"){
            contentType(ContentType.Application.Json)
            setBody(registerPost)
        }
    }
}