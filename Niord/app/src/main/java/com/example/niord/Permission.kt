package com.example.niord
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kotlinx.coroutines.MainScope


class Permission(private val context: Context) : PermissionChecker(context) {

    private val caller = context as ActivityResultCaller

    private var activityCallback: ((ActivityResult) -> Unit)? = null
    private var permissionCallback: ((Boolean) -> Unit)? = null

    private val activityLauncher = caller.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> activityCallback?.invoke(result) }

    private val permissionLauncher = caller.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionCallback?.invoke(granted) }

    private val multiPermissionLauncher = caller.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        permissionCallback?.invoke(granted)
    }

    fun getOverlayPermissions(callback: (ActivityResult) -> Unit) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri()
        )
        activityCallback = callback
        activityLauncher.launch(intent)
    }

    fun requestCallPermission(callback: (Boolean) -> Unit) {
        permissionCallback = callback
        permissionLauncher.launch(Manifest.permission.CALL_PHONE)
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


    fun fullAppRequest(){
        multiPermissionLauncher.launch(
            getAllPermissions()
        )
    }

    fun requestVigiaPermissions(callback: (Boolean) -> Unit) {
        permissionCallback = callback
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        multiPermissionLauncher.launch(perms.toTypedArray())
    }

    fun requestLocationPermission(callback: (Boolean) -> Unit) {
        permissionCallback = callback
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun requestContactsPermission(callback: (Boolean) -> Unit) {
        permissionCallback = callback
        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    fun requestSmsPermission(callback: (Boolean) -> Unit) {
        permissionCallback = callback
        permissionLauncher.launch(Manifest.permission.SEND_SMS)
    }
}

open class PermissionChecker(private val context: Context) {

    fun getAllPermissions(): Array<String> {
        val perms = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return perms.toTypedArray()
    }

    fun getMissingPerms(): Array<String> {
        return getAllPermissions().filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }

    fun shouldShowRationaleForPerms(permissions: Array<String>): Boolean {
        return permissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, permission)
        }
    }

    fun isVigiaPermitted(): Boolean {
        val mic = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return mic && notif
    }

    fun isCallPermitted(): Boolean {
        return (ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED)
    }

    fun isLocationPermitted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isContactsPermitted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isSmsPermitted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
}


