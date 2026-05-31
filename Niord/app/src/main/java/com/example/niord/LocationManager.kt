package com.example.niord

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit


class LocationManager (private val context: Context){


    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun getUserLocation(callback: (Location?) -> Unit) {

        fetchLocation(callback)

    }

    @SuppressLint("MissingPermission")
    suspend fun fetchLocationRet(priority: Int): Location?{
        return try {
            fusedLocationClient.getCurrentLocation(
                priority,
                null
            ).await()
        } catch (e: Exception) { null }
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