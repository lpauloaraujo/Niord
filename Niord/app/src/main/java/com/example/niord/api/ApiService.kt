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
import io.ktor.client.request.delete
import io.ktor.http.cookies


class ApiService(context: Context){
    var apiClient = ApiClient.createHttpClient(context)
    suspend fun greet(): String {
        return apiClient.get("greet").body<HttpResponse>().body<String>()
    }

    suspend fun getUser(): HttpResponse{
        return apiClient.get("/user")
    }

    suspend fun isAuth(): HttpResponse{
        return apiClient.get("auth/isauth")
    }

    suspend fun sendRegisterData(registerPost: RegisterPost): HttpResponse{
        return apiClient.post("auth/register"){
            contentType(ContentType.Application.Json)
            setBody(registerPost)
        }
    }

    suspend fun sendLoginData(loginPost: LoginPost): HttpResponse{
        return apiClient.post("auth/login"){
            url{
                parameters.append("email", loginPost.email)
                parameters.append("password", loginPost.password)
            }
        }
    }

    suspend fun logout(): HttpResponse{
        return apiClient.delete("auth/login"){
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

    suspend fun resendOtp(resendPayload: OtpResend): HttpResponse{
        return apiClient.post("auth/resend"){
            url{
                parameters.append("email", resendPayload.email)
            }
        }
    }
}