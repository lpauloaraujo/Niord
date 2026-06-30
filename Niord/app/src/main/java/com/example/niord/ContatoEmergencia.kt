package com.example.niord

data class ContatoEmergencia(
    val id: String,
    val nome: String,
    val telefone: String,
    val isSelected: Boolean = false
)
