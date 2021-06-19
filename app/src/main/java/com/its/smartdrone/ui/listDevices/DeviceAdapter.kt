package com.its.smartdrone.ui.listDevices

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.its.smartdrone.databinding.ItemsBluetoothDevicesBinding

class DeviceAdapter: RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {
    private var listDevice = ArrayList<BluetoothDevice>()
    private lateinit var onClickListener: DeviceCallback

    fun setOnClickListener(onClickListener: DeviceCallback) {
        this.onClickListener = onClickListener
    }

    fun setDevices(devices: List<BluetoothDevice>) {
        this.listDevice.clear()
        this.listDevice.addAll(devices)
    }

    class ViewHolder(private val binding: ItemsBluetoothDevicesBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(device: BluetoothDevice) {
            with(binding) {
                tvDevicesName.text = device.name
                tvDevicesAddress.text = device.address
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemsBluetoothDevicesBinding = ItemsBluetoothDevicesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemsBluetoothDevicesBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listDevice[position])
        holder.itemView.setOnClickListener {
            onClickListener.onClick(listDevice[position])
        }
    }

    override fun getItemCount(): Int = listDevice.size
}