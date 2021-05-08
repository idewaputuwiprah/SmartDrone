package com.its.smartdrone

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.viewbinding.library.activity.viewBinding
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.its.smartdrone.databinding.ActivityHomeBinding
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private var connectThread: ConnectThread? = null
    private val uuid = UUID.fromString("1e0ca4ea-299d-4335-93eb-27fcfe7fa848")
    private val homeBinding: ActivityHomeBinding by viewBinding()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val bluetoothEnable = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result->
        if (result.resultCode == RESULT_OK) printMsg("Bluetooth Enabled")
        else printMsg("Failed to Enabled Bluetooth")
    }
    private val requestMultiplePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissions.entries.forEach {
            Log.e("DEBUG", "${it.key} = ${it.value}")
        }
    }
    private val requestBackgroundLocPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission->
        Log.d("DEBUG", "ACCESS_BACKGROUND_LOCATION = $permission")
    }
    private val btDevices = ArrayList<BluetoothDevice>()
    private val btReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    printMsg("Device found!")
                    device?.let {
                        btDevices.add(device)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> printMsg("start scanning...")
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    printMsg("${btDevices.size}")
                    btDevices.forEach {
                        Log.d("DEBUG", "Device name: ${it.name}, device address: ${it.address}, device uuid: ${it.uuids}")
                        if (it.name == "raspberrypi") {
                            connectThread = ConnectThread(it)
                            connectThread?.start()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(homeBinding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) requestBackgroundLocPermission
            .launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        checkBluetooth()
        registerFilter()

        homeBinding.btnRequest.setOnClickListener {
            btDevices.clear()
            runBlocking {
                requestMultiplePermissions.launch(
                        arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                )
            }
            bluetoothAdapter?.startDiscovery()
        }
    }

    private fun registerFilter() {
        val filter = IntentFilter()
        filter.apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)

            registerReceiver(btReceiver, filter)
        }
    }

    private fun checkBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter?.isEnabled == false) bluetoothEnable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
        else printMsg("Device doesn't have bluetooth")
    }

    private fun printMsg(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(btReceiver)
    }

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(uuid)
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.use { socket ->
                socket.connect()

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
//                manageMyConnectedSocket(socket)
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