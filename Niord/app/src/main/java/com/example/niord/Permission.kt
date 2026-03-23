package com.example.niord
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri


class Permission(var context: Context){
    //context == this and caller == this
    var caller = context as ActivityResultCaller

    //Immutable by design
    val permissionLauncherShowOverlay = caller.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (!Settings.canDrawOverlays(context)) {
            overlayCallback?.invoke(result)
        }
    }

    //This callback defines what will run after the user comes back from OS settings
    var overlayCallback: ((ActivityResult)->Unit)? = null
    fun getOverlayPermissions(callback: (ActivityResult)->Unit) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:$context.packageName".toUri()
        )
        overlayCallback = callback
        permissionLauncherShowOverlay.launch(intent)
        overlayCallback = null
    }
}

