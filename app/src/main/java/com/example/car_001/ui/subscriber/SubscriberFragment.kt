package com.example.car_001.ui.subscriber

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.car_001.R
import com.example.car_001.data.CrashAlertEntity
import com.example.car_001.data.Settings
import com.example.car_001.data.AppDatabase
import com.example.car_001.databinding.FragmentSubscriberBinding
import com.example.car_001.mqtt.MqttManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.car_001.R

class SubscriberFragment : Fragment() {
    
    private var _binding: FragmentSubscriberBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var mqttManager: MqttManager
    private lateinit var database: AppDatabase
    private lateinit var alertAdapter: AlertHistoryAdapter
    
    private var currentSettings: Settings? = null
    private var currentAlert: CrashAlertEntity? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriberBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeManagers()
        setupRecyclerView()
        setupClickListeners()
        loadSettings()
        observeAlerts()
    }
    
    private fun initializeManagers() {
        mqttManager = MqttManager(requireContext())
        database = AppDatabase.getDatabase(requireContext())
        
        // Setup MQTT callbacks
        mqttManager.onConnectionStatusChanged = { status ->
            updateMqttStatus(status)
        }
        
        mqttManager.onMessageReceived = { alert ->
            showCrashAlert(alert)
        }
    }
    
    private fun setupRecyclerView() {
        alertAdapter = AlertHistoryAdapter { alert ->
            showCrashAlert(alert)
        }
        
        binding.alertHistoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = alertAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.viewMapButton.setOnClickListener {
            currentAlert?.let { alert ->
                // Navigate to map with alert location
                val bundle = Bundle().apply {
                    putDouble("latitude", alert.latitude)
                    putDouble("longitude", alert.longitude)
                }
                findNavController().navigate(R.id.action_subscriberFragment_to_mapFragment, bundle)
            }
        }
        
        binding.callEmergencyButton.setOnClickListener {
            currentAlert?.let { alert ->
                callEmergency(alert.contact)
            }
        }
        
        binding.markHandledButton.setOnClickListener {
            currentAlert?.let { alert ->
                markAlertAsHandled(alert.id)
            }
        }
    }
    
    private fun loadSettings() {
        lifecycleScope.launch {
            currentSettings = database.settingsDao().getSettings()
            currentSettings?.let { settings ->
                // Connect to MQTT with settings
                val serverUri = "tcp://${settings.mqttIp}:${settings.mqttPort}"
                mqttManager.connect(serverUri, settings.mqttUsername, settings.mqttPassword)
                
                // Subscribe to crash alerts
                mqttManager.subscribeToCrashAlerts()
            }
        }
    }
    
    private fun observeAlerts() {
        lifecycleScope.launch {
            database.crashAlertDao().getAllAlerts().collectLatest { alerts ->
                alertAdapter.submitList(alerts)
            }
        }
    }
    
    private fun updateMqttStatus(status: MqttManager.ConnectionStatus) {
        val (emoji, text, color) = when (status) {
            MqttManager.ConnectionStatus.CONNECTED -> Triple("🟢", "Subscribed", R.color.status_connected)
            MqttManager.ConnectionStatus.CONNECTING -> Triple("🟡", "Connecting", R.color.status_connecting)
            MqttManager.ConnectionStatus.DISCONNECTED -> Triple("🔴", "Disconnected", R.color.status_disconnected)
            MqttManager.ConnectionStatus.ERROR -> Triple("🔴", "Error", R.color.status_error)
        }
        
        binding.mqttStatus.text = "$emoji $text"
        binding.mqttStatus.setTextColor(resources.getColor(color, null))
    }
    
    private fun showCrashAlert(alert: CrashAlertEntity) {
        currentAlert = alert
        
        binding.crashAlertCard.visibility = View.VISIBLE
        binding.alertTimeText.text = alert.crashTime
        binding.alertLocationText.text = "%.4f°N, %.4f°E".format(alert.latitude, alert.longitude)
        binding.alertBloodTypeText.text = alert.bloodType
        
        // Show notification
        showNotification(alert)
    }
    
    private fun showNotification(alert: CrashAlertEntity) {
        // Create and show notification
        Toast.makeText(
            requireContext(),
            "🚨 CRASH ALERT! Location: %.4f°N, %.4f°E".format(alert.latitude, alert.longitude),
            Toast.LENGTH_LONG
        ).show()
    }
    
    private fun callEmergency(contact: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$contact")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not open dialer", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun markAlertAsHandled(alertId: Int) {
        lifecycleScope.launch {
            database.crashAlertDao().updateAlertStatus(alertId, "handled")
            binding.crashAlertCard.visibility = View.GONE
            currentAlert = null
            Toast.makeText(requireContext(), "Alert marked as handled", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        mqttManager.cleanup()
        _binding = null
    }
}

// Simple adapter for alert history
class AlertHistoryAdapter(
    private val onAlertClick: (CrashAlertEntity) -> Unit
) : ListAdapter<CrashAlertEntity, AlertHistoryAdapter.ViewHolder>(AlertDiffCallback()) {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.alertTimeText)
        val locationText: TextView = view.findViewById(R.id.alertLocationText)
        val statusText: TextView = view.findViewById(R.id.alertStatusText)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert_history, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alert = getItem(position)
        holder.timeText.text = alert.crashTime
        holder.locationText.text = "%.4f°N, %.4f°E".format(alert.latitude, alert.longitude)
        holder.statusText.text = alert.status.capitalize()
        
        holder.itemView.setOnClickListener {
            onAlertClick(alert)
        }
    }
}

class AlertDiffCallback : DiffUtil.ItemCallback<CrashAlertEntity>() {
    override fun areItemsTheSame(oldItem: CrashAlertEntity, newItem: CrashAlertEntity): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: CrashAlertEntity, newItem: CrashAlertEntity): Boolean {
        return oldItem == newItem
    }
} 