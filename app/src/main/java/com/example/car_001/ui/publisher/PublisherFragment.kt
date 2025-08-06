package com.example.car_001.ui.publisher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.car_001.R
import com.example.car_001.bluetooth.CarBluetoothManager
import com.example.car_001.data.CrashAlert
import com.example.car_001.data.ESP32Data
import com.example.car_001.data.Settings
import com.example.car_001.data.AppDatabase
import com.example.car_001.databinding.FragmentPublisherBinding
import com.example.car_001.mqtt.MqttManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PublisherFragment : Fragment() {
    
    private var _binding: FragmentPublisherBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var bluetoothManager: CarBluetoothManager
    private lateinit var mqttManager: MqttManager
    private lateinit var database: AppDatabase
    
    private var currentSettings: Settings? = null
    private var lastData: ESP32Data? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPublisherBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeManagers()
        setupClickListeners()
        loadSettings()
        refreshDeviceList()
    }
    
    private fun initializeManagers() {
        bluetoothManager = CarBluetoothManager(requireContext())
        mqttManager = MqttManager(requireContext())
        database = AppDatabase.getDatabase(requireContext())
        
        // Setup Bluetooth callbacks
        bluetoothManager.onConnectionStatusChanged = { status ->
            updateBluetoothStatus(status)
        }
        
        bluetoothManager.onDataReceived = { data ->
            updateDataDisplay(data)
            checkForCrash(data)
        }
        
        // Setup MQTT callbacks
        mqttManager.onConnectionStatusChanged = { status ->
            updateMqttStatus(status)
        }
    }
    
    private fun setupClickListeners() {
        binding.connectButton.setOnClickListener {
            val selectedDevice = binding.deviceSpinner.selectedItem as? com.example.car_001.data.BluetoothDevice
            selectedDevice?.let { device ->
                bluetoothManager.connectToDevice(device.address)
            }
        }
        
        binding.disconnectButton.setOnClickListener {
            bluetoothManager.disconnect()
        }
        
        binding.testAlertButton.setOnClickListener {
            sendTestCrashAlert()
        }
        
        binding.mqttSettingsButton.setOnClickListener {
            // Navigate to settings
            findNavController().navigate(R.id.action_publisherFragment_to_settingsFragment)
        }
    }
    
    private fun loadSettings() {
        lifecycleScope.launch {
            currentSettings = database.settingsDao().getSettings()
            currentSettings?.let { settings ->
                // Connect to MQTT with settings
                val serverUri = "tcp://${settings.mqttIp}:${settings.mqttPort}"
                mqttManager.connect(serverUri, settings.mqttUsername, settings.mqttPassword)
            }
        }
    }
    
    private fun refreshDeviceList() {
        val devices = bluetoothManager.scanForDevices()
        val deviceNames = devices.map { "${it.name} (${it.address})" }
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            deviceNames
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        binding.deviceSpinner.adapter = adapter
    }
    
    private fun updateBluetoothStatus(status: CarBluetoothManager.ConnectionStatus) {
        val (emoji, text, color) = when (status) {
            CarBluetoothManager.ConnectionStatus.CONNECTED -> Triple("🟢", "Connected", R.color.status_connected)
            CarBluetoothManager.ConnectionStatus.CONNECTING -> Triple("🟡", "Connecting", R.color.status_connecting)
            CarBluetoothManager.ConnectionStatus.DISCONNECTED -> Triple("🔴", "Disconnected", R.color.status_disconnected)
            CarBluetoothManager.ConnectionStatus.ERROR -> Triple("🔴", "Error", R.color.status_error)
        }
        
        binding.bluetoothStatus.text = "$emoji $text"
        binding.bluetoothStatus.setTextColor(resources.getColor(color, null))
    }
    
    private fun updateMqttStatus(status: MqttManager.ConnectionStatus) {
        val (emoji, text, color) = when (status) {
            MqttManager.ConnectionStatus.CONNECTED -> Triple("🟢", "Connected", R.color.status_connected)
            MqttManager.ConnectionStatus.CONNECTING -> Triple("🟡", "Connecting", R.color.status_connecting)
            MqttManager.ConnectionStatus.DISCONNECTED -> Triple("🔴", "Disconnected", R.color.status_disconnected)
            MqttManager.ConnectionStatus.ERROR -> Triple("🔴", "Error", R.color.status_error)
        }
        
        binding.mqttStatus.text = "$emoji $text"
        binding.mqttStatus.setTextColor(resources.getColor(color, null))
    }
    
    private fun updateDataDisplay(data: ESP32Data) {
        lastData = data
        
        binding.timeText.text = data.timestamp.ifEmpty { "--:--:--" }
        binding.crashText.text = if (data.crash) "🚨 YES" else "No"
        binding.vibrationText.text = when (data.vibration) {
            0 -> "Normal"
            in 1..5 -> "Low"
            in 6..10 -> "Medium"
            else -> "High"
        }
        binding.accelerationText.text = "%.1fg".format(data.acceleration)
        binding.gpsText.text = "%.4f°N, %.4f°E".format(data.gpsLat, data.gpsLon)
    }
    
    private fun checkForCrash(data: ESP32Data) {
        if (data.crash && mqttManager.isConnected()) {
            sendCrashAlert(data)
        }
    }
    
    private fun sendCrashAlert(data: ESP32Data) {
        currentSettings?.let { settings ->
            val now = Date()
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            val crashAlert = CrashAlert(
                crash = true,
                latitude = data.gpsLat,
                longitude = data.gpsLon,
                time = timeFormat.format(now),
                date = dateFormat.format(now),
                bloodType = settings.bloodType,
                contact = settings.emergencyContact
            )
            
            mqttManager.publishCrashAlert(crashAlert)
            Toast.makeText(requireContext(), "Crash alert sent!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun sendTestCrashAlert() {
        lastData?.let { data ->
            sendCrashAlert(data.copy(crash = true))
        } ?: run {
            // Send test alert with default values
            currentSettings?.let { settings ->
                val now = Date()
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                
                val testAlert = CrashAlert(
                    crash = true,
                    latitude = 13.0827,
                    longitude = 80.2707,
                    time = timeFormat.format(now),
                    date = dateFormat.format(now),
                    bloodType = settings.bloodType,
                    contact = settings.emergencyContact
                )
                
                mqttManager.publishCrashAlert(testAlert)
                Toast.makeText(requireContext(), "Test crash alert sent!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        bluetoothManager.cleanup()
        mqttManager.cleanup()
        _binding = null
    }
} 