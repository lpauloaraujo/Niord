package com.example.niord

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.util.Log

object ContatosEmergenciaPreferences {
    private const val PREFS_NAME = "niord_contatos_emergencia"
    private const val KEY_CONTATOS = "contatos_selecionados"
    private val json = Json { ignoreUnknownKeys = true }

    fun getContatosSelecionados(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val data = prefs.getString(KEY_CONTATOS, "[]") ?: "[]"
        return try {
            json.decodeFromString<List<String>>(data)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun salvarContatosSelecionados(context: Context, contatos: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encoded = json.encodeToString(contatos)
        Log.d("ContatosEmergencia", "salvarContatosSelecionados: $encoded")
        prefs.edit().putString(KEY_CONTATOS, encoded).apply()
    }

    fun adicionarContato(context: Context, contatoId: String) {
        val atuais = getContatosSelecionados(context).toMutableList()
        if (!atuais.contains(contatoId) && atuais.size < 5) {
            atuais.add(contatoId)
            salvarContatosSelecionados(context, atuais)
            Log.d("ContatosEmergencia", "adicionarContato persisted: $contatoId, atuais=$atuais")
        }
    }

    fun removerContato(context: Context, contatoId: String) {
        val atuais = getContatosSelecionados(context).toMutableList()
        atuais.remove(contatoId)
        salvarContatosSelecionados(context, atuais)
        Log.d("ContatosEmergencia", "removerContato persisted: $contatoId, atuais=$atuais")
    }

    fun limparTodos(context: Context) {
        salvarContatosSelecionados(context, emptyList())
    }
}
