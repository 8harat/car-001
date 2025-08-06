package com.example.car_001.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.car_001.data.ESP32Data
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.util.*

class CarBluetoothManager(private val context: Context) {
    
    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private val gson = Gson()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    var onDataReceived: ((ESP32Data) -> Unit)? = null
    var onConnectionStatusChanged: ((ConnectionStatus) -> Unit)? = null
    
    enum class ConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }
    
    private var currentStatus = ConnectionStatus.DISCONNECTED
    
    fun scanForDevices(): List<com.example.car_001.data.BluetoothDevice> {
        val devices = mutableListOf<com.example.car_001.data.BluetoothDevice>()
        
        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.bondedDevices.forEach { device ->
                if (device.name?.contains("ESP32", ignoreCase = true) == true) {
                    devices.add(
                        com.example.car_001.data.BluetoothDevice(
                            name = device.name ?: "Unknown",
                            address = device.address,
                            isConnected = device.address == getLastConnectedDevice()
                        )
                    )
                }
            }
        }
        
        return devices
    }
    
    fun connectToDevice(deviceAddress: String) {
        scope.launch {
            try {
                updateConnectionStatus(ConnectionStatus.CONNECTING)
                
                val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
                bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_SPP)
                bluetoothSocket?.connect()
                
                inputStream = bluetoothSocket?.inputStream
                
                // Save connected device
                saveLastConnectedDevice(deviceAddress)
                
                updateConnectionStatus(ConnectionStatus.CONNECTED)
                
                // Start listening for data
                startDataListener()
                
            } catch (e: IOException) {
                Log.e(TAG, "Connection failed", e)
                updateConnectionStatus(ConnectionStatus.ERROR)
            }
        }
    }
    
    fun disconnect() {
        scope.launch {
            try {
                inputStream?.close()
                bluetoothSocket?.close()
                inputStream = null
                bluetoothSocket = null
                updateConnectionStatus(ConnectionStatus.DISCONNECTED)
            } catch (e: IOException) {
                Log.e(TAG, "Disconnect error", e)
            }
        }
    }
    
    private fun startDataListener() {
        scope.launch {
            val buffer = ByteArray(1024)
            
            while (currentStatus == ConnectionStatus.CONNECTED) {
                try {
                    val bytes = inputStream?.read(buffer)
                    if (bytes != null && bytes > 0) {
                        val data = String(buffer, 0, bytes)
                        parseAndProcessData(data)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Data reading error", e)
                    updateConnectionStatus(ConnectionStatus.ERROR)
                    break
                }
            }
        }
    }
    
    private fun parseAndProcessData(data: String) {
        try {
            // Split by newlines in case multiple JSON objects are received
            val lines = data.trim().split("\n")
            
            for (line in lines) {
                if (line.isNotEmpty()) {
                    val esp32Data = gson.fromJson(line, ESP32Data::class.java)
                    onDataReceived?.invoke(esp32Data)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing data: $data", e)
        }
    }
    
    private fun updateConnectionStatus(status: ConnectionStatus) {
        currentStatus = status
        onConnectionStatusChanged?.invoke(status)
    }
    
    private fun saveLastConnectedDevice(address: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_DEVICE, address)
            .apply()
    }
    
    private fun getLastConnectedDevice(): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_DEVICE, null)
    }
    
    fun isConnected(): Boolean = currentStatus == ConnectionStatus.CONNECTED
    
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
    
    companion object {
        private const val TAG = "CarBluetoothManager"
        private const val UUID_SPP = "00001101-0000-1000-8000-00805F9B34FB"
        private const val PREFS_NAME = "bluetooth_prefs"
        private const val KEY_LAST_DEVICE = "last_connected_device"
    }
} 