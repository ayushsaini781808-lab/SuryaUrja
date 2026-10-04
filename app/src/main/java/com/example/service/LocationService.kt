package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class LocationCoordinates(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val altitudeMeters: Double? = null,
    val provider: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

interface LocationService {
    fun hasLocationPermission(): Boolean
    suspend fun getCurrentLocation(): LocationCoordinates?
    suspend fun getLastKnownLocation(): LocationCoordinates?
}

class PlayServicesLocationService(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
) : LocationService {

    override fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationCoordinates? {
        if (!hasLocationPermission()) return null

        return try {
            val cancellationTokenSource = CancellationTokenSource()
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(10_000)
                .setMaxUpdateAgeMillis(60_000)
                .build()

            val location: Location? = fusedLocationClient
                .getCurrentLocation(request, cancellationTokenSource.token)
                .awaitTask()

            location?.toCoordinates() ?: getLastKnownLocation()
        } catch (_: Exception) {
            getLastKnownLocation()
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): LocationCoordinates? {
        if (!hasLocationPermission()) return null

        return try {
            val location: Location? = fusedLocationClient.lastLocation.awaitTask()
            location?.toCoordinates()
        } catch (_: Exception) {
            null
        }
    }

    private fun Location.toCoordinates(): LocationCoordinates {
        return LocationCoordinates(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = if (hasAccuracy()) accuracy else null,
            altitudeMeters = if (hasAltitude()) altitude else null,
            provider = provider,
            timestamp = time
        )
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { exception ->
            if (cont.isActive) cont.resumeWithException(exception)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.cancel()
        }
    }
}
