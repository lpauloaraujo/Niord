package com.example.niord.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Tokens(
    val access: String,
    val refresh: String?
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