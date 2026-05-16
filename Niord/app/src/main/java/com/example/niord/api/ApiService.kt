package com.example.niord.api

import android.content.Context
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import com.example.niord.api.ApiClient


class ApiService(context: Context){
    var apiClient = ApiClient.createHttpClient(context)
    suspend fun greet(): String {
        return apiClient.get("greet").body<HttpResponse>().body<String>()
    }

    suspend fun sendRegisterData(registerPost: RegisterPost): HttpResponse{
        return apiClient.post("auth/register"){
            contentType(ContentType.Application.Json)
            setBody(registerPost)
        }
    }
    suspend fun verifyOtp(verifyPayload: OtpVerify): HttpResponse{
        return apiClient.post("auth/verify"){
            url{
                parameters.append("email", verifyPayload.email)
                parameters.append("code", verifyPayload.code.toString())
            }
        }
    }
}