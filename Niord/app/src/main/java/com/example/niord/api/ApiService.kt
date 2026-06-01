package com.example.niord.api

import android.content.Context
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import com.example.niord.api.ApiClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.delete
import io.ktor.http.cookies
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds


class ApiService(context: Context){
    var apiClient = ApiClient.createHttpClient(context)
    val isWebsocketConnected = MutableStateFlow(false)
    suspend fun greet(): String {
        return apiClient.get("greet").body<HttpResponse>().body<String>()
    }

    suspend fun askHelp(askData: HelpAsk): HttpResponse{
        return apiClient.post("/help/ask"){
            contentType(ContentType.Application.Json)
            setBody(askData)
        }
    }

    suspend fun answerHelp(answerData: HelpAnswer): HttpResponse{
        return apiClient.post("/help/answer"){
            contentType(ContentType.Application.Json)
            setBody(answerData)
        }
    }

    suspend fun answerHelpMulti(answerData: HelpAnswerMulti): HttpResponse{
        return apiClient.post("/help/answer_multi"){
            contentType(ContentType.Application.Json)
            setBody(answerData)
        }
    }
    suspend fun connectWsOverwatch(inCallback: suspend (Frame.Text) -> Unit, outChannel: Channel<Frame>){
        try {
            apiClient.webSocket(path = "/ws/") {
                isWebsocketConnected.value = true
                val periodicSenderJob = launch {
                    while (isActive) {
                        try {
                            for (frame in outChannel){
                                send(frame)
                            }
                        } catch (e: CancellationException) {
                            break
                        } catch (e: Exception) {
                            println("Failed to send periodic message: ${e.localizedMessage}")
                        }
                    }
                }

                try {
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            println(frame.readText())
                            inCallback(frame)
                        }
                    }
                } finally {
                    periodicSenderJob.cancelAndJoin()
                    println("Connection closed. Periodic sender stopped.")
                }

            }
        } catch (e: Exception) {
            println(e.localizedMessage)
        } finally {
            isWebsocketConnected.value = false
        }
    }

    suspend fun getUser(): HttpResponse{
        return apiClient.get("user/")
    }

    suspend fun requestAccountEmailOtp(email: String): HttpResponse{
        return apiClient.post("user/email-otp"){
            url{
                parameters.append("email", email)
            }
        }
    }

    suspend fun updateUser(userUpdate: UserUpdatePatch): HttpResponse{
        return apiClient.patch("user/"){
            contentType(ContentType.Application.Json)
            setBody(userUpdate)
        }
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
