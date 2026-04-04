package com.example.niord

import android.app.AlertDialog
import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Composable
fun CallDialog(
    number: String?,
    title: String,
    message: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {

    number?.let {

        AlertDialog(

            onDismissRequest = onDismiss,

            title = {
                Text(title)
            },

            text = {
                Text(message)
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(it)
                        onDismiss()
                    }
                ) {
                    Text("Confirmar")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}