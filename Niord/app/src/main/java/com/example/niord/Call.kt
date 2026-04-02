package com.example.niord

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

class CallManager {

    fun toCall(context: Context, phoneNumber: String) {

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            val intent = Intent(Intent.ACTION_CALL)
            intent.data = "tel:$phoneNumber".toUri()
            context.startActivity(intent)

        } else {
            println("Permissão CALL_PHONE não concedida")
        }
    }

}