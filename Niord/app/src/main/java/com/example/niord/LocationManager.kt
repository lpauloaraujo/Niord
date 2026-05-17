package com.example.niord

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationManager (private val context: Context){

    private val permission = Permission(context)

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun getUserLocation(callback: (Location?) -> Unit) {

        if (permission.isLocationPermitted(context)) {

            fetchLocation(callback)

        } else {

            permission.requestLocationPermission { granted ->

                if (granted) {

                    fetchLocation(callback)

                } else {

                    callback(null)
                }
            }
        }
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