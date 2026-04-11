package com.example.niord.api

import kotlinx.serialization.Serializable

@Serializable
data class Tokens(
    val access: String,
    val refresh: String?
)


@Serializable
data class GreetString(
    val text: String
)