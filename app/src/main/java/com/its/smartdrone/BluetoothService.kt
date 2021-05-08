package com.its.smartdrone

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.util.*

object BluetoothService {
    private const val TAG = "MY_APP_DEBUG_TAG"
    const val MESSAGE_READ: Int = 0
    const val MESSAGE_WRITE: Int = 1
    const val MESSAGE_TOAST: Int = 2
    private val uuid = UUID.fromString("1e0ca4ea-299d-4335-93eb-27fcfe7fa848")

    class RequestConnection(private val device: BluetoothDevice, private val adapter: BluetoothAdapter) {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(uuid)
        }

        fun connect() {
            adapter.cancelDiscovery()

            mmSocket?.use { socket ->
                socket.connect()
            }
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.d("DEBUG", "Could not close the client socket", e)
            }
        }
    }
}