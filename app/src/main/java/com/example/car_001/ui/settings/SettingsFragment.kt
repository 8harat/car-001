package com.example.car_001.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.car_001.data.Settings
import com.example.car_001.data.AppDatabase
import com.example.car_001.databinding.FragmentSettingsBinding
import com.example.car_001.mqtt.MqttManager
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var database: AppDatabase
    private lateinit var mqttManager: MqttManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        database = AppDatabase.getDatabase(requireContext())
        mqttManager = MqttManager(requireContext())
        
        setupClickListeners()
        loadCurrentSettings()
    }
    
    private fun setupClickListeners() {
        binding.saveSettingsButton.setOnClickListener {
            saveSettings()
        }
        
        binding.testConnectionButton.setOnClickListener {
            testMqttConnection()
        }
    }
    
    private fun loadCurrentSettings() {
        lifecycleScope.launch {
            val settings = database.settingsDao().getSettings()
            settings?.let { currentSettings ->
                binding.mqttIpInput.setText(currentSettings.mqttIp)
                binding.mqttPortInput.setText(currentSettings.mqttPort.toString())
                binding.mqttUsernameInput.setText(currentSettings.mqttUsername)
                binding.mqttPasswordInput.setText(currentSettings.mqttPassword)
                binding.bloodTypeInput.setText(currentSettings.bloodType)
                binding.emergencyContactInput.setText(currentSettings.emergencyContact)
            }
        }
    }
    
    private fun saveSettings() {
        val mqttIp = binding.mqttIpInput.text.toString()
        val mqttPort = binding.mqttPortInput.text.toString().toIntOrNull() ?: 1883
        val mqttUsername = binding.mqttUsernameInput.text.toString()
        val mqttPassword = binding.mqttPasswordInput.text.toString()
        val bloodType = binding.bloodTypeInput.text.toString()
        val emergencyContact = binding.emergencyContactInput.text.toString()
        
        if (mqttIp.isEmpty()) {
            Toast.makeText(requireContext(), "MQTT IP is required", Toast.LENGTH_SHORT).show()
            return
        }
        
        val settings = Settings(
            mqttIp = mqttIp,
            mqttPort = mqttPort,
            mqttUsername = mqttUsername,
            mqttPassword = mqttPassword,
            bloodType = bloodType,
            emergencyContact = emergencyContact
        )
        
        lifecycleScope.launch {
            database.settingsDao().insertSettings(settings)
            Toast.makeText(requireContext(), "Settings saved successfully", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun testMqttConnection() {
        val mqttIp = binding.mqttIpInput.text.toString()
        val mqttPort = binding.mqttPortInput.text.toString().toIntOrNull() ?: 1883
        val mqttUsername = binding.mqttUsernameInput.text.toString()
        val mqttPassword = binding.mqttPasswordInput.text.toString()
        
        if (mqttIp.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter MQTT IP first", Toast.LENGTH_SHORT).show()
            return
        }
        
        val serverUri = "tcp://$mqttIp:$mqttPort"
        
        mqttManager.onConnectionStatusChanged = { status ->
            when (status) {
                MqttManager.ConnectionStatus.CONNECTED -> {
                    Toast.makeText(requireContext(), "✅ MQTT connection successful!", Toast.LENGTH_LONG).show()
                    mqttManager.disconnect()
                }
                MqttManager.ConnectionStatus.ERROR -> {
                    Toast.makeText(requireContext(), "❌ MQTT connection failed", Toast.LENGTH_LONG).show()
                }
                else -> {
                    // Connecting or disconnected states
                }
            }
        }
        
        mqttManager.connect(serverUri, mqttUsername, mqttPassword)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        mqttManager.cleanup()
        _binding = null
    }
} 