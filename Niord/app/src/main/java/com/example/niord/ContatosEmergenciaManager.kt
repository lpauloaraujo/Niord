package com.example.niord

import android.content.Context
import android.provider.ContactsContract

object ContatosEmergenciaManager {

    fun getNumerosContatosSelecionados(
        context: Context
    ): List<Pair<String, String>> {

        val idsSelecionados =
            ContatosEmergenciaPreferences.getContatosSelecionados(context)

        val contatos = mutableListOf<Pair<String, String>>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use {

            val idIndex =
                it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)

            val nomeIndex =
                it.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                )

            val numeroIndex =
                it.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )

            while (it.moveToNext()) {

                val id = it.getString(idIndex)

                if (idsSelecionados.contains(id)) {

                    val nome = it.getString(nomeIndex) ?: ""
                    val numero = it.getString(numeroIndex) ?: ""

                    contatos.add(numero to nome)
                }
            }
        }

        return contatos
    }
}