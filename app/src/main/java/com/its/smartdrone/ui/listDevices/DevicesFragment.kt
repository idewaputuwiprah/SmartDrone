package com.its.smartdrone.ui.listDevices

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.commit
import androidx.recyclerview.widget.LinearLayoutManager
import com.its.smartdrone.R
import com.its.smartdrone.databinding.FragmentDevicesBinding
import com.its.smartdrone.ui.MainActivity
import com.its.smartdrone.ui.home.HomeFragment

class DevicesFragment : Fragment() {
    private lateinit var fragmentsDevicesBinding: FragmentDevicesBinding
    private lateinit var mainActivity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentsDevicesBinding = FragmentDevicesBinding.inflate(inflater, container, false)
        return fragmentsDevicesBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (activity != null) {
            mainActivity = activity as MainActivity
            val adapter = DeviceAdapter()

            adapter.setOnClickListener(object: DeviceCallback{
                override fun onClick(data: BluetoothDevice) {
                    mainActivity.connect(data)
                    mainActivity.mFragmentManager.commit {
                        replace(R.id.fragment_container, HomeFragment(), HomeFragment::class.java.simpleName)
                    }
                }
            })

            mainActivity.btDevices.observe(requireActivity(), { devices->
                if (devices.isNotEmpty()) {
                    adapter.setDevices(devices)
                    adapter.notifyDataSetChanged()
                }
            })

            with(fragmentsDevicesBinding.rvDevices) {
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
                this.adapter = adapter
            }

            mainActivity.requestLocationPermission()
        }
    }
}