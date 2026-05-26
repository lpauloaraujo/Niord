package com.example.niord

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationManager (private val context: Context){


    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun getUserLocation(callback: (Location?) -> Unit) {

        fetchLocation(callback)

    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation(callback: (Location?) -> Unit) {

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->
            callback(location)
        }
    }

}