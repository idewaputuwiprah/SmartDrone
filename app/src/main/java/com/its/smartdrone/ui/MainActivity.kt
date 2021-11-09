package com.its.smartdrone.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.viewbinding.library.activity.viewBinding
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.commit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.its.smartdrone.R
import com.its.smartdrone.service.BluetoothService
import com.its.smartdrone.service.LocationService
import com.its.smartdrone.databinding.ActivityHomeBinding
import com.its.smartdrone.ui.home.HomeFragment
import com.its.smartdrone.ui.home.HomeFragment.Companion.DESTINATION
import com.its.smartdrone.ui.home.HomeFragment.Companion.HOME_LOCATION
import kotlinx.coroutines.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private val homeBinding: ActivityHomeBinding by viewBinding()
    val mFragmentManager = supportFragmentManager
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
    private val _btDevices = MutableLiveData<List<BluetoothDevice>>()
    var btDevices: LiveData<List<BluetoothDevice>> = _btDevices
    private val btReceiver = object : BroadcastReceiver() {
        val devices = ArrayList<BluetoothDevice>()
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    printMsg("Device found!")
                    device?.let {
                        devices.add(device)
                        _btDevices.value = devices
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> printMsg("start scanning...")
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    printMsg("Found ${btDevices.value?.size} device")
                }
            }
        }
    }
    private val _destination = MutableLiveData<Location>()
    var destination: LiveData<Location> = _destination
    private val _home = MutableLiveData<Location>()
    var home: LiveData<Location> = _home
    private lateinit var locationService: LocationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(homeBinding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) requestBackgroundLocPermission
            .launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationService = LocationService(locationManager, this.applicationContext)

        val fragment = mFragmentManager.findFragmentByTag(HomeFragment::class.java.simpleName)
        if (fragment !is HomeFragment) {
            mFragmentManager.commit {
                add(R.id.fragment_container, HomeFragment(), HomeFragment::class.java.simpleName)
            }
        }
    }

    fun getLocation(location: String) {
        when(location) {
            DESTINATION -> {
                _destination.value = locationService.getLocation()
                if (destination.value != null) printMsg("${(destination.value)?.accuracy}")
            }
            HOME_LOCATION -> {
                _home.value = locationService.getLocation()
                if (home.value != null) printMsg("${(home.value)?.accuracy}")
            }
        }
    }

    fun requestLocationPermission() {
        _btDevices.value = emptyList()
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

    fun registerFilter() {
        val filter = IntentFilter()
        filter.apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)

            registerReceiver(btReceiver, filter)
        }
    }

    fun checkBluetooth() {
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter?.isEnabled == false) bluetoothEnable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
        else printMsg("Device doesn't have bluetooth")
    }

    fun printMsg(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun getBluetoothAdapter() = bluetoothAdapter

    fun getBluetoothService() = btService

    fun connect(device: BluetoothDevice?) {
        if (device != null) {
            btService = BluetoothService(device, bluetoothAdapter!!)
        }
        lifecycleScope.launch(Dispatchers.IO) {
            btService?.connect()
        }
    }

    fun disconnect() {
        btService?.disconnect()
    }

    override fun onDestroy() {
        super.onDestroy()
        btService?.disconnect()
        unregisterReceiver(btReceiver)
    }
}