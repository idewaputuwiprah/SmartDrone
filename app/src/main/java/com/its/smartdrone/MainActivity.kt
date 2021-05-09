package com.its.smartdrone

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.viewbinding.library.activity.viewBinding
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.its.smartdrone.databinding.ActivityHomeBinding
import kotlinx.coroutines.*
import java.lang.StringBuilder
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    companion object {
        const val TURN_ON_BT = "Turn On Bluetooth"
        const val START_DISCOVERY = "Start Discovery"
    }
    private val homeBinding: ActivityHomeBinding by viewBinding()
    private var btService: BluetoothService? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val bluetoothEnable = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result->
        if (result.resultCode == RESULT_OK) {
            printMsg("Bluetooth Enabled")
            if (bluetoothAdapter!!.isDiscovering) bluetoothAdapter?.cancelDiscovery()
        }
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
                            btService = BluetoothService(it, bluetoothAdapter!!)
                            lifecycleScope.launch(Dispatchers.IO) {
                                btService?.connect()
                            }
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

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        registerBtStateListener()
        registerFilter()

        homeBinding.btnRequest.setOnClickListener {
            if ((it as Button).text == TURN_ON_BT) checkBluetooth()
            else requestLocationPermission()
        }
        homeBinding.btnSend.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                btService?.writeMsg("Hai")
            }
        }
    }

    private fun registerBtStateListener() {
        lifecycleScope.launch(Dispatchers.IO) {
            while(true) {
                bluetoothAdapter?.let { adapter->
                    if (adapter.isEnabled) withContext(Dispatchers.Main) {
                        homeBinding.btnRequest.text = StringBuilder(START_DISCOVERY)
                    }
                    else {
                        withContext(Dispatchers.Main) {
                            homeBinding.btnRequest.text = StringBuilder(TURN_ON_BT)
                        }
                    }
                }
                delay(500)
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                if (btService?.isConnected() == null) withContext(Dispatchers.Main) {homeBinding.btnSend.visibility = View.GONE}
                else {
                    withContext(Dispatchers.Main) {homeBinding.btnSend.visibility = if (btService!!.isConnected()!!) View.VISIBLE else View.GONE}
                }
            }
        }
    }

    private fun requestLocationPermission() {
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
        btService?.disconnect()
        unregisterReceiver(btReceiver)
    }
}