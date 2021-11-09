package com.its.smartdrone.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothService(device: BluetoothDevice, private val adapter: BluetoothAdapter) {
    private val uuid = UUID.fromString("1e0ca4ea-299d-4335-93eb-27fcfe7fa848")
    private val mmSocket: BluetoothSocket? = device.createRfcommSocketToServiceRecord(uuid)
    private val mmInStream: InputStream? = mmSocket?.inputStream
    private val mmOutStream: OutputStream? = mmSocket?.outputStream
    private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

    fun isConnected(): Boolean? = mmSocket?.isConnected

    fun connect() {
        adapter.cancelDiscovery()
        mmSocket?.connect()
    }

    fun writeMsg(msg: String) {
        mmOutStream?.write(msg.toByteArray(Charsets.UTF_8))
    }

    fun disconnect() {
        try {
            mmSocket?.close()
        } catch (e: IOException) {
            Log.d("DEBUG", "Could not close the client socket", e)
        }
    }
}