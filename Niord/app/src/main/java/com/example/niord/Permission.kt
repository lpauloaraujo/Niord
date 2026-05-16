package com.example.niord
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri


class Permission(var context: Context){
    //context == this and caller == this
    var caller = context as ActivityResultCaller

    //This callback defines what will run after the user comes back from other apps
    var activityCallback: ((ActivityResult)->Unit)? = null

    //Immutable by design

    // callback for permissions
    var permissionCallback: ((Boolean) -> Unit)? = null
    val activityLauncher = caller.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> activityCallback?.invoke(result) }

    val permissionLauncher = caller.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionCallback?.invoke(granted) }

    fun getOverlayPermissions(callback: (ActivityResult)->Unit) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:$context.packageName".toUri()
        )
        activityCallback = callback
        activityLauncher.launch(intent)
    }

    fun requestCallPermission(callback: (Boolean)->Unit){
        permissionCallback = callback
        permissionLauncher.launch(Manifest.permission.CALL_PHONE)
    }

    val multiPermissionLauncher = caller.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        val granted = permissions.values.all { it }
        permissionCallback?.invoke(granted)
    }

    fun requestCallAndPhoneStatePermission(callback: (Boolean) -> Unit) {
        permissionCallback = callback
        multiPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE
            )
        )
    }

    fun isCallPermitted(context: Context): Boolean {
        return ((ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED)
        and
        (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED))
    }

    fun requestLocationPermission(callback: (Boolean) -> Unit) {

        permissionCallback = callback

        permissionLauncher.launch(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    fun isLocationPermitted(context: Context): Boolean {

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

}

