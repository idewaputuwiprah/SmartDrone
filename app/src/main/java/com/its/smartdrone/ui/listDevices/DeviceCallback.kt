package com.its.smartdrone.ui.listDevices

import android.bluetooth.BluetoothDevice

interface DeviceCallback {
    fun onClick(data: BluetoothDevice)
}