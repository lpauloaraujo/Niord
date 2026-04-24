package com.example.niord

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

class CallMonitor(
    private val context: Context,
    private val onCallStarted: () -> Unit,
    private val onCallEnded: () -> Unit
) {

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private var wasInCall = false

    private var legacyListener: PhoneStateListener? = null
    private var modernCallback: TelephonyCallback? = null

    fun start() {

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val callback = object : TelephonyCallback(),
                TelephonyCallback.CallStateListener {

                override fun onCallStateChanged(state: Int) {
                    handleState(state)
                }
            }

            modernCallback = callback
            telephonyManager.registerTelephonyCallback(
                context.mainExecutor,
                callback
            )

        } else {

            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleState(state)
                }
            }

            legacyListener = listener

            @Suppress("DEPRECATION")
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            modernCallback?.let {
                telephonyManager.unregisterTelephonyCallback(it)
            }
        } else {
            legacyListener?.let {
                @Suppress("DEPRECATION")
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        }
    }

    private fun handleState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                wasInCall = true
                onCallStarted()
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                if (wasInCall) {
                    wasInCall = false
                    onCallEnded()
                }
            }
        }
    }
}