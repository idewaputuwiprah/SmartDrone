package com.its.smartdrone.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.its.smartdrone.R
import com.its.smartdrone.databinding.FragmentHomeBinding
import com.its.smartdrone.ui.MainActivity
import com.its.smartdrone.ui.listDevices.DevicesFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.StringBuilder

class HomeFragment : Fragment() {
    companion object {
        const val TURN_ON_BT = "Turn on bluetooth"
        const val START_DISCOVERY = "Find devices"
        const val HOME_LOCATION = "home"
        const val DESTINATION = "dest"
    }
    private lateinit var fragmentHomeBinding: FragmentHomeBinding
    private var destination: String? = null
    private var home: String? = null
    private lateinit var mainActivity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentHomeBinding = FragmentHomeBinding.inflate(inflater, container, false)
        return fragmentHomeBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (activity != null) {
            mainActivity = activity as MainActivity

            registerBtStateListener()
            mainActivity.registerFilter()
            registerObserver()

//            fragmentHomeBinding.btnRequest.setOnClickListener {
//                if ((it as Button).text == TURN_ON_BT) mainActivity.checkBluetooth()
//                else mainActivity.mFragmentManager.commit {
//                    replace(R.id.fragment_container, DevicesFragment(), DevicesFragment::class.java.simpleName)
//                }
//            }
            fragmentHomeBinding.apply {
                btnRenewHomeLocation.setOnClickListener {
                    mainActivity.getLocation(HOME_LOCATION)
                }
                cvDestination.setOnClickListener {
                    mainActivity.getLocation(DESTINATION)
                }
                llFindDevices.setOnClickListener {
                    tvFindDevices.let {
                        if (it.text == TURN_ON_BT) mainActivity.checkBluetooth()
                        else mainActivity.mFragmentManager.commit {
                            replace(R.id.fragment_container, DevicesFragment(), DevicesFragment::class.java.simpleName)
                        }
                    }
                }
                llTakeoff.setOnClickListener {
                    val btService = mainActivity.getBluetoothService()
                    if (destination != null && btService?.isConnected() != null) {
//                        val dest = destination as String
                        val msg = "$home,$destination"
                        if (btService.isConnected() == true) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                btService.writeMsg(msg)
                            }
                        }
                        else {
                            lifecycleScope.launch(Dispatchers.IO) {
                                btService.connect()
                                btService.writeMsg(msg)
                            }
                        }
                    }
                }
                llLand.setOnClickListener {
                    val btService = mainActivity.getBluetoothService()
                    if (btService?.isConnected() != null) {
                        val msg = "emergency"
                        if (btService.isConnected() == true) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                btService.writeMsg(msg)
                            }
                        }
                        else {
                            lifecycleScope.launch(Dispatchers.IO) {
                                btService.connect()
                                btService.writeMsg(msg)
                            }
                        }
                    }
                }
                llCamera.setOnClickListener {
                    TODO("not implemented yet")
                }
                tvDestination.visibility = View.GONE
            }
        }
    }

    private fun registerObserver() {
        mainActivity.destination.observe(requireActivity(), { location->
            if (location != null) {
                fragmentHomeBinding.apply {
                    tvDestination.visibility = View.VISIBLE
                    val str = "${location.latitude}, ${location.longitude}"
                    destination = str
                    tvDestination.text = StringBuilder(str)
                }
            }
        })
        mainActivity.home.observe(requireActivity(), { location ->
            if (location != null) {
                fragmentHomeBinding.apply {
                    tvDestination.visibility = View.VISIBLE
                    val str = "${location.latitude}, ${location.longitude}"
                    home = str
                    tvHomeLocation.text = StringBuilder(str)
                }
            }
        })
    }

    private fun registerBtStateListener() {
        val bluetoothAdapter = mainActivity.getBluetoothAdapter()
        val btService = mainActivity.getBluetoothService()
        lifecycleScope.launch(Dispatchers.IO) {
            while(true) {
                bluetoothAdapter?.let { adapter->
                    if (adapter.isEnabled) withContext(Dispatchers.Main) {
//                        fragmentHomeBinding.btnRequest.text = StringBuilder(START_DISCOVERY)
                        fragmentHomeBinding.apply {
                            ivFindDevices.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_search))
                            tvFindDevices.text = StringBuilder(START_DISCOVERY)
                        }
                    }
                    else {
                        withContext(Dispatchers.Main) {
//                            fragmentHomeBinding.btnRequest.text = StringBuilder(TURN_ON_BT)
                            fragmentHomeBinding.apply {
                                ivFindDevices.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_bluetooth))
                                tvFindDevices.text = StringBuilder(TURN_ON_BT)
                            }
                        }
                    }
                }
                delay(500)
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                if (btService?.isConnected() == null) withContext(Dispatchers.Main) {
                    fragmentHomeBinding.tvStatus.apply {
                        text = StringBuilder("disconnected")
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                    }
                }
                else {
                    withContext(Dispatchers.Main) {
//                        fragmentHomeBinding.btnSend.visibility = if (btService.isConnected()!!) View.VISIBLE else View.GONE}
                        fragmentHomeBinding.tvStatus.apply {
                            if (btService.isConnected()!!) {
                                text = StringBuilder("connected")
                                setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                            }
                            else {
                                text = StringBuilder("disconnected")
                                setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                            }
                        }
                    }
                }
            }
        }
    }
}