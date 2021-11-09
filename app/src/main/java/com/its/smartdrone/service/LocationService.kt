package com.its.smartdrone.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat

class LocationService(private val locationManager: LocationManager, private val context: Context): LocationListener {
    private val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    private val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    private var location: Location? = null

    private fun updateLocation() {
        if (hasGps) {
            Log.d("DEBUG_GPS", "Has Gps")
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d("DEBUG_GPS", "Don't have required permission")
                return
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0F, this)
            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastKnownLocation != null)
                location = lastKnownLocation
        }
        Log.d("DEBUG_GPS", "$location")
    }

    fun getLocation(): Location? {
        updateLocation()
        return location
    }

    override fun onLocationChanged(loc: Location) {
        location = loc
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Log.d("DEBUG_GPS", "onStatusChange")
    }
}