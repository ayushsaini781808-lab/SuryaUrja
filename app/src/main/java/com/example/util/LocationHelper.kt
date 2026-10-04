package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

data class GeoLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String = ""
)

object LocationHelper {

    val PRESET_SOLAR_LOCATIONS = listOf(
        GeoLocation("Campus 200kW Plant (Delhi)", 28.6139, 77.2090, "Primary benchmark plant (28.61°N, 77.20°E)"),
        GeoLocation("Bhadla Solar Park (Rajasthan)", 27.5385, 71.9161, "High DNI arid desert (27.53°N, 71.91°E)"),
        GeoLocation("Mojave Solar Park (California)", 35.0110, -117.5620, "High irradiance Mojave basin (35.01°N, 117.56°W)"),
        GeoLocation("Andalusia Solar Field (Spain)", 37.3891, -5.9845, "Mediterranean solar corridor (37.38°N, 5.98°W)"),
        GeoLocation("Atacama Solar Facility (Chile)", -23.8634, -69.1328, "Highest UV/GHI index in the world (-23.86°S, 69.13°W)"),
        GeoLocation("Benban Solar Park (Egypt)", 24.4539, 32.7411, "North African desert mega-hub (24.45°N, 32.74°E)")
    )

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun getCurrentDeviceLocation(context: Context): GeoLocation? {
        if (!hasLocationPermission(context)) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var bestLocation: Location? = null
        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val location = locationManager.getLastKnownLocation(provider)
                    if (location != null) {
                        if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                            bestLocation = location
                        }
                    }
                }
            } catch (_: SecurityException) {
                // Ignore if security check failed
            } catch (_: Exception) {
                // Provider not available
            }
        }

        return bestLocation?.let { loc ->
            GeoLocation(
                name = "Device Location (${String.format("%.2f", loc.latitude)}°, ${String.format("%.2f", loc.longitude)}°)",
                latitude = loc.latitude,
                longitude = loc.longitude,
                description = "Live GPS coordinates via Android LocationManager"
            )
        }
    }
}
