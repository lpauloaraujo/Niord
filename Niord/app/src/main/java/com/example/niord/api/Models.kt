package com.example.niord.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Tokens(
    val access: String,
    val refresh: String?
)

@Serializable
data class User(
    val id: Int,
    val registration_plate: String,
    val email: String,
    val telephone: String,
    val is_verified: Boolean,
    val name: String,
    val password: String,
    val cpf: String,
    val blood_type: String
)

@Serializable
data class ErrorResponse(
    val detail: ErrorDetail
)

@Serializable
data class ErrorDetail(
    val message: String,
    val type: String,
    val field: String? = null
)
@Serializable
data class RegisterPost(
    val name: String,
    val email: String,
    val password: String,
    @SerialName("registration_plate")
    val registrationPlate: String,
    val cpf: String,
    val telephone: String,
    @SerialName("blood_type")
    val bloodType: String?
)

@Serializable
data class LoginPost(
    val email: String,
    val password: String
)

@Serializable
data class OtpResend(
    val email: String
)

@Serializable
data class OtpVerify(
    val email: String,
    val code: Int
)






