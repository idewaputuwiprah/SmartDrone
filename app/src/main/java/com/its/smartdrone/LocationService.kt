package com.its.smartdrone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.app.ActivityCompat

class LocationService(private val locationManager: LocationManager, private val context: Context) {
    private val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    private var location: Location? = null

    private fun updateLocation() {
        if (hasGps) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0F) { loc -> location = loc }
        }
    }

    fun getLocation(): Location? {
        updateLocation()
        return location
    }
}